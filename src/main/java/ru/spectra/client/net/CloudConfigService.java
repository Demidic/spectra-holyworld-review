package ru.spectra.client.net;
import ru.spectra.client.model.AccountProfile;
import ru.spectra.client.util.AtomicFileWriter;
import ru.spectra.client.model.ConfigReference;
import ru.spectra.client.config.ConfigSerializer;
import ru.spectra.client.Lang;
import ru.spectra.client.config.LoadedConfig;
import ru.spectra.client.config.ModuleRepository;
import ru.spectra.client.model.Translation;
import ru.spectra.client.ui.WidgetStack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class CloudConfigService {
    private static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    private static final int MAX_JSON_BYTES = 8 * 1024 * 1024;
    private static final int MAX_NAME_LENGTH = 128;
    private static final int MAX_AUTHOR_LENGTH = 128;
    public static final String configRoot = "spectra/config/configs";
    public static final String legacyConfigRoot = "expensive/config/configs";
    public final ConfigSerializer serializer = new ConfigSerializer();

    public final AtomicReference<ConfigReference> activeConfigRef = new AtomicReference<>();

    public volatile AccountProfile accountProfile;

    public final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public final Path configDir;
    private final boolean migrateLegacy;

    public CloudConfigService() {
        this(Path.of(configRoot), true);
    }

    CloudConfigService(Path configDir, boolean migrateLegacy) {
        this.configDir = configDir.toAbsolutePath().normalize();
        this.migrateLegacy = migrateLegacy;
    }

    public CompletableFuture<Void> initialize(String str, String str2, String str3, String str4, String str5) {
        return CompletableFuture.runAsync(() -> {
            this.accountProfile = new AccountProfile(str, str2, str3, str4, str5);
            ensureConfigDir();
        });
    }

    public boolean initialized() {
        return (this.accountProfile == null || this.accountProfile.signature() == null) ? false : true;
    }

    public boolean configActive(String str) {
        ConfigReference class304Var = this.activeConfigRef.get();
        return (class304Var == null || str == null || !str.equals(class304Var.id())) ? false : true;
    }

    public Optional<ConfigReference> activeConfig() {
        return Optional.ofNullable(this.activeConfigRef.get());
    }

    public CompletableFuture<List<CloudConfigMetadata>> listConfigs() {
        return CompletableFuture.supplyAsync(() -> {
            ArrayList<CloudConfigMetadata> arrayList = new ArrayList<>();
            ensureConfigDir();
            try (Stream<Path> streamList = Files.list(this.configDir)) {
                streamList.filter(path -> {
                    return path.getFileName().toString().endsWith(".json");
                }).forEach(path2 -> {
                    CloudConfigDto class376VarRead = readDto(path2);
                    if (class376VarRead != null) {
                        CloudConfigMetadata class163Var = new CloudConfigMetadata();
                        class163Var.id = class376VarRead.id();
                        class163Var.name = class376VarRead.name();
                        class163Var.author = class376VarRead.author();
                        class163Var.createdAt = class376VarRead.createdAt();
                        class163Var.updatedAt = class376VarRead.updatedAt();
                        class163Var.authorAvatarUrl = null;
                        class163Var.localOverride = class376VarRead.isLocalOverride();
                        arrayList.add(class163Var);
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException("Failed to list configs", e);
            }
            return arrayList;
        });
    }

    public CompletableFuture<CloudConfigDto> fetchConfig(String str) {
        return CompletableFuture.supplyAsync(() -> {
            CloudConfigDto class376VarRead = readDto(pathFor(str));
            if (class376VarRead == null) {
                throw new RuntimeException("Config not found");
            }
            return class376VarRead;
        });
    }

    public CompletableFuture<String> createConfig(String str, ModuleRepository class795Var, WidgetStack class814Var) {
        return CompletableFuture.supplyAsync(() -> {
            ensureConfigDir();
            if (nameExists(str)) {
                throw new RuntimeException("name already exists");
            }
            String strUuid = UUID.randomUUID().toString();
            String strNow = Instant.now().toString();
            CloudConfigDto class376Var = new CloudConfigDto();
            class376Var.id = strUuid;
            class376Var.name = str;
            class376Var.base64 = serializeToBase64(class795Var, class814Var);
            class376Var.author = currentLogin();
            class376Var.createdAt = strNow;
            class376Var.updatedAt = strNow;
            class376Var.localOverride = true;
            writeDto(class376Var);
            return strUuid;
        });
    }

    public CompletableFuture<Void> saveConfig(String str, ModuleRepository class795Var, WidgetStack class814Var) {
        return CompletableFuture.runAsync(() -> {
            CloudConfigDto class376VarRead = readDto(pathFor(str));
            if (class376VarRead == null) {
                throw new RuntimeException("Config not found");
            }
            class376VarRead.base64 = serializeToBase64(class795Var, class814Var);
            class376VarRead.updatedAt = Instant.now().toString();
            writeDto(class376VarRead);
        });
    }

    public CompletableFuture<Void> renameConfig(String str, String str2) {
        return CompletableFuture.runAsync(() -> {
            CloudConfigDto class376VarRead = readDto(pathFor(str));
            if (class376VarRead == null) {
                throw new RuntimeException("Config not found");
            }
            if (nameExists(str2)) {
                throw new RuntimeException("name already exists");
            }
            class376VarRead.name = str2;
            class376VarRead.updatedAt = Instant.now().toString();
            writeDto(class376VarRead);
        });
    }

    public CompletableFuture<Void> deleteConfig(String str) {
        return CompletableFuture.runAsync(() -> {
            try {
                Files.deleteIfExists(pathFor(str));
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete config", e);
            }
        });
    }

    public CompletableFuture<Void> importConfig(String str) {
        return CompletableFuture.runAsync(() -> {
            if (readDto(pathFor(str)) == null) {
                throw new RuntimeException("Config not found");
            }
        });
    }

    public CompletableFuture<LoadedConfig> downloadConfig(String str) {
        return fetchConfig(str).thenApply(class376Var -> {
            try {
                return new LoadedConfig(class376Var, this.serializer.deserialize(class376Var.decodeData()));
            } catch (IOException e) {
                throw new RuntimeException("Failed to deserialize config", e);
            }
        });
    }

    public void applyConfig(LoadedConfig class089Var) {
        try {
            this.serializer.applyConfig(class089Var);
            this.activeConfigRef.set(new ConfigReference(class089Var.details().id(), class089Var.details().name()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to apply config", e);
        }
    }

    public Translation errorMessage(Throwable th) {
        String strMethod007 = extractErrorMessage(th);
        return strMethod007 == null ? Lang.CLOUD_ERROR_GENERIC : mapErrorMessage(strMethod007);
    }

    public String extractErrorMessage(Throwable th) {
        if (th.getMessage() != null) {
            return th.getMessage();
        }
        if (th.getCause() != null) {
            return th.getCause().getMessage();
        }
        return null;
    }

    public Translation mapErrorMessage(String str) {
        if (str.contains("name already exists") || str.contains("name already named")) {
            return Lang.CLOUD_ERROR_NAME_EXISTS;
        }
        if (str.contains("not found")) {
            return Lang.CLOUD_ERROR_NOT_FOUND;
        }
        if (str.contains("already in user list")) {
            return Lang.CLOUD_ERROR_ALREADY_LISTED;
        }
        if (str.contains("unauthorized")) {
            return Lang.CLOUD_ERROR_UNAUTHORIZED;
        }
        if (str.contains("Failed to create")) {
            return Lang.CLOUD_ERROR_CREATE_FAILED;
        }
        return str.contains("Failed to get") ? Lang.CLOUD_ERROR_GET_FAILED : Lang.CLOUD_ERROR_GENERIC;
    }

    public String serializeToBase64(ModuleRepository class795Var, WidgetStack class814Var) {
        try {
            return Base64.getEncoder().encodeToString(this.serializer.serialize(class795Var, class814Var));
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize modules config", e);
        }
    }

    public String currentLogin() {
        AccountProfile class013Var = this.accountProfile;
        return class013Var == null ? "" : class013Var.login();
    }

    public void ensureConfigDir() {
        try {
            Files.createDirectories(this.configDir, new FileAttribute[0]);
            if (Files.isSymbolicLink(this.configDir)
                    || !Files.isDirectory(this.configDir, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Config root is not a physical directory");
            }
            if (this.migrateLegacy) {
                migrateLegacyConfigs();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create config directory", e);
        }
    }

    private void migrateLegacyConfigs() throws IOException {
        try (Stream<Path> current = Files.list(this.configDir)) {
            if (current.anyMatch(this::isJsonConfig)) {
                return;
            }
        }
        Path legacyDir = Path.of(legacyConfigRoot);
        if (!Files.isDirectory(legacyDir)) {
            return;
        }
        try (Stream<Path> legacy = Files.list(legacyDir)) {
            for (Path source : legacy.filter(this::isJsonConfig).toList()) {
                Path target = this.configDir.resolve(source.getFileName());
                if (!Files.exists(target)) {
                    Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
        }
    }

    private boolean isJsonConfig(Path path) {
        return Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                && !Files.isSymbolicLink(path)
                && path.getFileName().toString().toLowerCase().endsWith(".json");
    }

    public Path pathFor(String str) {
        if (str == null || !ID_PATTERN.matcher(str).matches()) {
            throw new IllegalArgumentException("Invalid config id");
        }
        Path candidate = this.configDir.resolve(str + ".json").normalize();
        if (!candidate.getParent().equals(this.configDir)) {
            throw new IllegalArgumentException("Config path escapes its root");
        }
        return candidate;
    }

    public CloudConfigDto readDto(Path path) {
        Path candidate = path.toAbsolutePath().normalize();
        String fileName = candidate.getFileName().toString();
        if (!candidate.getParent().equals(this.configDir)
                || !fileName.endsWith(".json")
                || !Files.isRegularFile(candidate, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(candidate)) {
            return null;
        }
        try {
            long size = Files.size(candidate);
            if (size <= 0L || size > MAX_JSON_BYTES) {
                return null;
            }
            String strRead = Files.readString(candidate, StandardCharsets.UTF_8);
            CloudConfigDto dto = this.gson.fromJson(strRead, CloudConfigDto.class);
            String expectedId = fileName.substring(0, fileName.length() - ".json".length());
            return validDto(dto, expectedId) ? dto : null;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    public void writeDto(CloudConfigDto class376Var) {
        ensureConfigDir();
        if (!validDto(class376Var, class376Var == null ? null : class376Var.id())) {
            throw new IllegalArgumentException("Invalid config record");
        }
        byte[] bytes = this.gson.toJson(class376Var).getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= 0 || bytes.length > MAX_JSON_BYTES) {
            throw new IllegalArgumentException("Config record is too large");
        }
        try {
            AtomicFileWriter.writeBytes(pathFor(class376Var.id()), bytes, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write config", e);
        }
    }

    public boolean nameExists(String str) {
        try (Stream<Path> streamList = Files.list(this.configDir)) {
            return streamList.filter(path -> {
                return path.getFileName().toString().endsWith(".json");
            }).map(this::readDto).filter(class376Var -> {
                return class376Var != null;
            }).anyMatch(class376Var2 -> {
                return str.equals(class376Var2.name());
            });
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean validDto(CloudConfigDto dto, String expectedId) {
        if (dto == null
                || expectedId == null
                || !ID_PATTERN.matcher(expectedId).matches()
                || !expectedId.equals(dto.id)
                || dto.name == null
                || dto.name.isBlank()
                || dto.name.length() > MAX_NAME_LENGTH
                || dto.author == null
                || dto.author.length() > MAX_AUTHOR_LENGTH
                || dto.base64 == null
                || dto.base64.length() > MAX_JSON_BYTES
                || parseInstant(dto.createdAt) == null
                || parseInstant(dto.updatedAt) == null) {
            return false;
        }
        try {
            return Base64.getDecoder().decode(dto.base64).length <= MAX_JSON_BYTES;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static Instant parseInstant(String value) {
        try {
            return Instant.parse(value);
        } catch (RuntimeException exception) {
            return null;
        }
    }

}
