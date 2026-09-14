package ru.spectra.client.resource;
import ru.spectra.client.Spectra;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class ClasspathResource implements ResourceSource {
    public final String path;
    static final String assetPrefix = "/assets/spectra/";

    @Override
    public InputStream stream() {
        try {
            String strSubstring = normalizedPath();
            InputStream protectedStream = ProtectedAssetInput.open(strSubstring);
            if (protectedStream != null) {
                return protectedStream;
            }
            String str = "/" + strSubstring;
            Spectra.LOGGER.debug("Trying to load classpath resource: {}", str);
            URL resource = ClasspathResource.class.getResource(str);
            if (resource == null) {
                throw new IllegalStateException("Classpath resource not found: " + str);
            }
            Spectra.LOGGER.debug("Found resource URL: {}", resource);
            URLConnection uRLConnectionOpenConnection = resource.openConnection();
            uRLConnectionOpenConnection.setUseCaches(false);
            return uRLConnectionOpenConnection.getInputStream();
        } catch (Exception e) {
            Spectra.LOGGER.error("Failed to open classpath resource: {}", this.path, e);
            throw new RuntimeException("Failed to open classpath resource: " + this.path, e);
        }
    }

    public ClasspathResource(String str) {
        this.path = str;
    }

    public String normalizedPath() {
        String normalized = this.path;
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (!normalized.startsWith("assets/spectra/")) {
            normalized = "assets/spectra/" + normalized;
        }
        return normalized;
    }
}
