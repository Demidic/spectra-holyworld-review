package ru.spectra.client;
import ru.spectra.client.util.ActivityLogger;
import ru.spectra.client.annotation.Aliases;
import ru.spectra.client.util.AshfieldChatHandler;
import ru.spectra.client.render.AvatarCache;
import ru.spectra.client.command.BindCommand;
import ru.spectra.client.resource.ClasspathResource;
import ru.spectra.client.model.ClientInfo;
import ru.spectra.client.model.SystemStatusState;
import ru.spectra.client.net.CloudConfigService;
import ru.spectra.client.util.CommandManager;
import ru.spectra.client.config.ConfigFile;
import ru.spectra.client.config.ConfigManager;
import ru.spectra.client.type.ConfigOrigin;
import ru.spectra.client.config.ConfigSerializer;
import ru.spectra.client.ui.ConfigTabLayout;
import ru.spectra.client.util.DrawEngine;
import ru.spectra.client.util.EventDispatcher;
import ru.spectra.client.command.FriendCommand;
import ru.spectra.client.render.GlTexture;
import ru.spectra.client.config.HandlerRepository;
import ru.spectra.client.net.InventoryService;
import ru.spectra.client.config.ListenerRepository;
import ru.spectra.client.command.MacroCommand;
import ru.spectra.client.config.MacroRepository;
import ru.spectra.client.ui.MenuWindow;
import ru.spectra.client.ui.MenuTabElement;
import ru.spectra.client.ui.ModuleCard;
import ru.spectra.client.module.WidgetsModule;
import ru.spectra.client.util.ModuleProvider;
import ru.spectra.client.config.ModuleRepository;
import ru.spectra.client.ui.ModuleTab;
import ru.spectra.client.config.NotificationRepository;
import ru.spectra.client.util.PinnedServersController;
import ru.spectra.client.command.PrefixCommand;
import ru.spectra.client.command.ReconnectCommand;
import ru.spectra.client.util.ServerUtil;
import ru.spectra.client.util.TabsController;
import ru.spectra.client.render.Theme;
import ru.spectra.client.ui.ThemeCard;
import ru.spectra.client.ui.ThemeCard2;
import ru.spectra.client.model.ThemeData;
import ru.spectra.client.type.ThemeMode;
import ru.spectra.client.ui.ThemeTabLayout;
import ru.spectra.client.net.UserSession;
import ru.spectra.client.command.WaypointCommand;
import ru.spectra.client.config.WaysRepository;
import ru.spectra.client.ui.WidgetStack;
import ru.spectra.client.ui.SystemStatusOverlay;
import ru.spectra.client.ui.HudEditorOverlays;
import ru.spectra.client.util.WindowController;
import ru.spectra.client.render.WindowControllerAdapter;

import discord.DiscordManager;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Spectra {
    public static final Logger LOGGER = LoggerFactory.getLogger(Spectra.class);
    public static Spectra INSTANCE;
    public final SystemStatusState systemStatusState = new SystemStatusState();
    public final SystemStatusOverlay systemStatusOverlay =
            new SystemStatusOverlay(this.systemStatusState);
    public final WidgetStack widgetStack = new WidgetStack();

    public final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "spectra-worker");
        thread.setDaemon(true);
        return thread;
    });

    public final WaysRepository waysRepository = new WaysRepository();

    public final CloudConfigService cloudConfigService = new CloudConfigService();

    public final WindowController windowController = new WindowController();

    public final WindowControllerAdapter windowControllerAdapter = new WindowControllerAdapter(this.windowController);

    public final TabsController tabsController = new TabsController();

    public final PinnedServersController pinnedServersController = new PinnedServersController();

    public final ActivityLogger activityLogger = new ActivityLogger();

    public final boolean devMode = detectDevMode();

    public ConfigManager configManager;

    public CommandManager commandDispatcher;

    public final MacroRepository macroRepository;

    public final DiscordManager discordManager;

    public final InventoryService inventoryService;

    public final HandlerRepository handlerRepository;

    public final EventDispatcher eventDispatcher;

    public final NotificationRepository notificationRepository;

    public final ModuleProvider moduleProvider;

    public final ModuleRepository moduleRepository;

    public final ListenerRepository listenerRepository;

    public ConfigFile defaultConfigFile;

    public final Lang languages;

    public DrawEngine drawEngine;

    public UserSession userSession;

    public final ClientInfo clientInfo;

    public MenuWindow menuWindow;

    public Theme theme;
    public boolean viaLoaded;

    public AshfieldChatHandler ashfieldChatHandler;

    public String token;

    public Spectra(UserSession class385Var) {
        if (INSTANCE != null) {
            throw new IllegalStateException(getClass().getName() + " already initialized!");
        }
        INSTANCE = this;
        this.userSession = class385Var;
        this.activityLogger.start();
        this.languages = new Lang();
        this.languages.primaryLanguage();
        this.pinnedServersController.init();
        this.eventDispatcher = new EventDispatcher();
        this.macroRepository = new MacroRepository();
        this.notificationRepository = new NotificationRepository();
        this.listenerRepository = new ListenerRepository();
        this.moduleProvider = new ModuleProvider();
        this.moduleRepository = new ModuleRepository(this.moduleProvider.getAll());
        HudEditorOverlays.initialize(this.widgetStack);
        captureDefaultConfig();
        AvatarCache.load(class385Var.avatarUrl(), new GlTexture(new ClasspathResource("assets/spectra/textures/avatar.png")), this.executor).thenAccept(class073Var -> {
            this.userSession = class385Var.withTexture(class073Var);
        });
        this.handlerRepository = new HandlerRepository();
        this.inventoryService = new InventoryService();
        this.clientInfo = new ClientInfo("4.1.3", "Stable", "09.03.2026");
        this.discordManager = new DiscordManager();
        this.discordManager.init();
        initConfigs();
        try {
            initCloud("", class385Var.hwid());
        } catch (Exception e) {
            LOGGER.error("Failed to init cloud", e);
        }
        registerCommands();
        setupMenu();
        this.configManager.loadLocalConfig();
        this.executor.scheduleAtFixedRate(this.configManager::saveLocalConfig, 30L, 30L, TimeUnit.SECONDS);
        registerShutdownHook();
        try {
            Class.forName("com.viaversion.viafabricplus.ViaFabricPlus");
            if (com.viaversion.viafabricplus.ViaFabricPlus.getImpl() != null) {
                this.viaLoaded = true;
                ServerUtil.registerViaVersionClamp();
            }
        } catch (ClassNotFoundException e2) {
        }
    }

    public void captureDefaultConfig() {
        try {
            ConfigSerializer class351Var = new ConfigSerializer();
            this.defaultConfigFile = class351Var.deserialize(class351Var.serialize(this.moduleRepository, this.widgetStack));
        } catch (IOException e) {
            LOGGER.error("Failed to capture default config", e);
        }
    }

    public void initCloud(String str, String str2) {
        String strAvatarUrl = this.userSession.avatarUrl();
        try {
            this.ashfieldChatHandler = new AshfieldChatHandler(URI.create("ws://127.0.0.1:1/ws"));
            this.ashfieldChatHandler.createUser(this.userSession.uid(), this.userSession.username(), strAvatarUrl, this.userSession.role());
            LOGGER.info("AshfieldChat initialized");
            this.cloudConfigService.initialize(String.valueOf(this.userSession.uid()), this.userSession.username(), str, str2, strAvatarUrl).thenCompose(r3 -> {
                return this.cloudConfigService.listConfigs();
            }).thenAccept(list -> {
                LOGGER.info("Available configs: {}", Integer.valueOf(list.size()));
                if (this.configManager.menuStateConfig().isConfigAutoLoadEnabled()) {
                    this.configManager.loadLastConfigID();
                }
                this.cloudConfigService.activeConfig().ifPresent(class304Var -> {
                    this.executor.scheduleAtFixedRate(() -> {
                        this.cloudConfigService.saveConfig(class304Var.id(), this.moduleRepository, this.widgetStack).exceptionally(th -> {
                            LOGGER.error("Auto-save failed", th);
                            return null;
                        });
                    }, 2L, 2L, TimeUnit.MINUTES);
                });
            }).exceptionally(th -> {
                LOGGER.error("Cloud init failed", th);
                return null;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String urlEncode(String str) {
        return URLEncoder.encode(str, StandardCharsets.UTF_8);
    }

    public void registerCommands() {
        this.commandDispatcher = new CommandManager();
        this.commandDispatcher.registerCommand(new BindCommand());
        this.commandDispatcher.registerCommand(new MacroCommand());
        this.commandDispatcher.registerCommand(new ReconnectCommand());
        this.commandDispatcher.registerCommand(new WaypointCommand());
        this.commandDispatcher.registerCommand(new FriendCommand());
        this.commandDispatcher.registerCommand(new PrefixCommand());
    }

    public void setupMenu() {
        this.menuWindow = new MenuWindow();
        this.windowController.newWindow(this.menuWindow);
        this.theme = ThemeData.defaultDark();
        this.moduleRepository.getModules().forEach(module -> {
            if (module instanceof WidgetsModule) {
                return;
            }
            MenuTabElement target = module.getModuleTab() == ModuleTab.RENDER
                    ? this.tabsController.render()
                    : this.tabsController.misc();
            target.newFrame(new ModuleCard(
                    module,
                    module.getClass().isAnnotationPresent(Aliases.class)
                            ? module.getClass().getAnnotation(Aliases.class).aliases()
                            : new String[0]
            ));
        });
        ConfigTabLayout class797Var = new ConfigTabLayout();
        this.tabsController.config().setRenderStrategy(class797Var);
        class797Var.updateConfigList();
        this.tabsController.theme().setRenderStrategy(new ThemeTabLayout());
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < 25; i++) {
            int iNextInt = ThreadLocalRandom.current().nextInt(8);
            boolean zNextBoolean = ThreadLocalRandom.current().nextBoolean();
            arrayList.add(new ThemeCard2(new ThemeCard(Theme.of("id" + iNextInt, "Element" + iNextInt, zNextBoolean ? "owner" + iNextInt : this.userSession.username(), zNextBoolean ? ConfigOrigin.OFFICIAL : ConfigOrigin.USER, zNextBoolean ? ThemeMode.DARK : ThemeMode.LIGHT, ThemeData.defaultDark().palette(), Instant.now(), Instant.now()), this.userSession.texture())));
        }
        this.tabsController.theme().updateFramesSafe(arrayList);
        this.menuWindow.headerContainer.showDefaultView();
    }

    public void initConfigs() {
        try {
            this.configManager = new ConfigManager(Path.of("spectra/config", new String[0]), this.waysRepository, this.macroRepository);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create config storage", e);
        }
        // Language lives in menu-state. Load every independent file in its
        // own boundary so one damaged shared config cannot silently leave the
        // interface in English or prevent the remaining preferences loading.
        loadConfigPart("menu state", this.configManager::loadMenuState);
        loadConfigPart("macros", this.configManager::loadMacros);
        loadConfigPart("friends", this.configManager::loadFriends);
        loadConfigPart("ways", this.configManager::loadWays);
        loadConfigPart("staff", this.configManager::loadStaff);
        loadConfigPart("session nickname", () -> ServerUtil.applySessionNickname(
                this.configManager.loadSessionNickname()));
    }

    private void loadConfigPart(String name, ConfigLoad operation) {
        try {
            operation.run();
        } catch (Exception exception) {
            LOGGER.error("Failed to load {} config", name, exception);
        }
    }

    @FunctionalInterface
    private interface ConfigLoad {
        void run() throws Exception;
    }

    public void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            this.executor.shutdownNow();
            try {
                this.executor.awaitTermination(5L, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            this.discordManager.stopRPC();
            try {
                this.configManager.saveMacros();
                this.configManager.saveFriends();
                this.configManager.saveWays();
                this.configManager.saveMenuState();
                this.configManager.saveLastConfigID();
                this.configManager.saveSessionNickname();
                this.configManager.saveStaff();
                this.configManager.saveLocalConfig();
            } catch (Exception e2) {
                LOGGER.error("Failed to save on shutdown", e2);
            }
        }));
    }

    public boolean devMode() {
        return this.devMode;
    }

    public static boolean detectDevMode() {
        return ManagementFactory.getRuntimeMXBean().getInputArguments().stream().anyMatch(str -> {
            return str.contains("-Ddebugger.agent.enable.coroutines=true");
        });
    }

    public WidgetStack widgetStack() {
        return this.widgetStack;
    }

    public SystemStatusState systemStatusState() {
        return this.systemStatusState;
    }

    public SystemStatusOverlay systemStatusOverlay() {
        return this.systemStatusOverlay;
    }

    public ScheduledExecutorService executor() {
        return this.executor;
    }

    public WaysRepository waysRepository() {
        return this.waysRepository;
    }

    public CloudConfigService cloudConfigService() {
        return this.cloudConfigService;
    }

    public WindowController windowController() {
        return this.windowController;
    }

    public WindowControllerAdapter windowControllerAdapter() {
        return this.windowControllerAdapter;
    }

    public TabsController tabsController() {
        return this.tabsController;
    }

    public PinnedServersController pinnedServersController() {
        return this.pinnedServersController;
    }

    public ActivityLogger activityLogger() {
        return this.activityLogger;
    }

    public ConfigManager configManager() {
        return this.configManager;
    }

    public CommandManager commandDispatcher() {
        return this.commandDispatcher;
    }

    public MacroRepository macroRepository() {
        return this.macroRepository;
    }

    public DiscordManager discordManager() {
        return this.discordManager;
    }

    public InventoryService inventoryService() {
        return this.inventoryService;
    }

    public HandlerRepository handlerRepository() {
        return this.handlerRepository;
    }

    public EventDispatcher eventDispatcher() {
        return this.eventDispatcher;
    }

    public NotificationRepository notificationRepository() {
        return this.notificationRepository;
    }

    public ModuleProvider moduleProvider() {
        return this.moduleProvider;
    }

    public ModuleRepository moduleRepository() {
        return this.moduleRepository;
    }

    public ListenerRepository listenerRepository() {
        return this.listenerRepository;
    }

    public ConfigFile defaultConfigFile() {
        return this.defaultConfigFile;
    }

    public Lang languages() {
        return this.languages;
    }

    public DrawEngine drawEngine() {
        return this.drawEngine;
    }

    public UserSession userSession() {
        return this.userSession;
    }

    public ClientInfo clientInfo() {
        return this.clientInfo;
    }

    public MenuWindow menuWindow() {
        return this.menuWindow;
    }

    public Theme theme() {
        return this.theme;
    }

    public boolean viaLoaded() {
        return this.viaLoaded;
    }

    public AshfieldChatHandler ashfieldChatHandler() {
        return this.ashfieldChatHandler;
    }

    public String token() {
        return this.token;
    }

    public Spectra drawEngine(DrawEngine class154Var) {
        this.drawEngine = class154Var;
        return this;
    }

    public Spectra token(String str) {
        this.token = str;
        return this;
    }
}
