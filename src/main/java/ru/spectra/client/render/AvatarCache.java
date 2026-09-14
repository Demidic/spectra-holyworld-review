package ru.spectra.client.render;
import ru.spectra.client.resource.ByteArrayResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public final class AvatarCache {
    private static final Set<String> ALLOWED_HOSTS = Set.of(
            "api.spectravisuals.su",
            "textures.minecraft.net"
    );
    public static final ConcurrentHashMap<String, GlTexture> textureCache = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, CompletableFuture<GlTexture>> pendingLoads = new ConcurrentHashMap<>();
    public static final int connectTimeoutMs = 5000;
    public static final int readTimeoutMs = 10000;
    public static final int maxSizeBytes = 5242880;

    public static CompletableFuture<GlTexture> load(String str, GlTexture class073Var, Executor executor) {
        if (str == null || str.isBlank() || str.endsWith("/null")) {
            return CompletableFuture.completedFuture(class073Var);
        }
        GlTexture class073Var2 = textureCache.get(str);
        if (class073Var2 != null) return CompletableFuture.completedFuture(class073Var2);
        CompletableFuture<GlTexture> result = new CompletableFuture<>();
        CompletableFuture<GlTexture> existing = pendingLoads.putIfAbsent(str, result);
        if (existing != null) return existing;
        // Publish before scheduling: even an inline/very fast executor can now
        // finish without recursively changing computeIfAbsent's map entry.
        try {
            executor.execute(() -> {
                GlTexture loaded = class073Var;
                try {
                    GlTexture cached = textureCache.get(str);
                    GlTexture downloaded = cached != null ? cached : loadWithRetry(str);
                    if (downloaded != null) {
                        textureCache.put(str, downloaded);
                        loaded = downloaded;
                    }
                } catch (Throwable failure) {
                    System.out.println("[AvatarCache] FAIL host=" + safeHost(str)
                            + " err=" + String.valueOf(failure.getCause()));
                } finally {
                    pendingLoads.remove(str, result);
                    result.complete(loaded);
                }
            });
        } catch (RuntimeException rejected) {
            pendingLoads.remove(str, result);
            result.complete(class073Var);
        }
        return result;
    }

    public static GlTexture loadWithRetry(String str) {
        RuntimeException runtimeException = null;
        for (int i = 1; i <= 5; i++) {
            try {
                return download(str);
            } catch (RuntimeException e) {
                runtimeException = e;
                if (!isRetryable(e)) {
                    throw e;
                }
                if (i == 5) break;
                try {
                    Thread.sleep(500 << (i - 1));
                } catch (InterruptedException e2) {
                    Thread.currentThread().interrupt();
                    throw new CompletionException(e2);
                }
                System.out.println("[AvatarCache] RETRY " + i
                        + "/5 host=" + safeHost(str) + " cause="
                        + String.valueOf(e.getCause()));
            }
        }
        throw runtimeException;
    }

    public static boolean isRetryable(Throwable th) {
        Throwable cause = th;
        while (true) {
            Throwable th2 = cause;
            if (th2 == null) {
                return false;
            }
            if ((th2 instanceof SocketTimeoutException) || (th2 instanceof ConnectException) || (th2 instanceof UnknownHostException)) {
                return true;
            }
            cause = th2.getCause();
        }
    }

    public static GlTexture download(String str) {
        HttpURLConnection httpURLConnection = null;
        try {
            try {
                URI avatarUri = validateAvatarUri(str);
                rejectNonPublicAddress(avatarUri.getHost());
                HttpURLConnection httpURLConnection2 = (HttpURLConnection) avatarUri.toURL().openConnection();
                httpURLConnection = httpURLConnection2;
                httpURLConnection2.setConnectTimeout(connectTimeoutMs);
                httpURLConnection2.setReadTimeout(readTimeoutMs);
                httpURLConnection2.setRequestProperty("User-Agent", "Mozilla/5.0");
                httpURLConnection2.setInstanceFollowRedirects(false);
                int responseCode = httpURLConnection2.getResponseCode();
                if (responseCode != 200) {
                    System.out.println("[AvatarCache] HTTP " + responseCode
                            + " host=" + avatarUri.getHost());
                    throw new RuntimeException("HTTP " + responseCode);
                }
                String contentType = httpURLConnection2.getContentType();
                if (contentType == null
                        || !(contentType.toLowerCase(Locale.ROOT).startsWith("image/png")
                        || contentType.toLowerCase(Locale.ROOT).startsWith("image/jpeg"))) {
                    throw new RuntimeException("Unsupported avatar content type");
                }
                int declaredSize = httpURLConnection2.getContentLength();
                if (declaredSize > maxSizeBytes) {
                    throw new RuntimeException("Avatar too large");
                }
                InputStream inputStream = httpURLConnection2.getInputStream();
                try {
                    GlTexture class073Var = new GlTexture(new ByteArrayResource(readAllBytes(inputStream)));
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    if (httpURLConnection2 != null) {
                        httpURLConnection2.disconnect();
                    }
                    return class073Var;
                } catch (Throwable th) {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
                throw th3;
            }
        } catch (Exception e) {
            throw new CompletionException(e);
        }
    }

    static URI validateAvatarUri(String value) {
        URI uri = URI.create(value).normalize();
        String host = uri.getHost() == null
                ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || !ALLOWED_HOSTS.contains(host)
                || uri.getUserInfo() != null
                || uri.getPort() != -1
                || uri.getFragment() != null) {
            throw new IllegalArgumentException("Avatar URL is not approved");
        }
        return uri;
    }

    private static void rejectNonPublicAddress(String host) throws Exception {
        for (InetAddress address : InetAddress.getAllByName(host)) {
            if (address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress()
                    || address.isMulticastAddress()) {
                throw new SecurityException("Avatar host resolved privately");
            }
        }
    }

    private static String safeHost(String value) {
        try {
            String host = URI.create(value).getHost();
            return host == null ? "invalid" : host;
        } catch (RuntimeException exception) {
            return "invalid";
        }
    }

    public static byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(8192);
        byte[] bArr = new byte[8192];
        int i = 0;
        while (true) {
            int i2 = inputStream.read(bArr);
            if (i2 == -1) {
                return byteArrayOutputStream.toByteArray();
            }
            i += i2;
            if (i > maxSizeBytes) {
                throw new RuntimeException("Avatar too large: " + i);
            }
            byteArrayOutputStream.write(bArr, 0, i2);
        }
    }
}
