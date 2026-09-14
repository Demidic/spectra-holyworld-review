package ru.spectra.client.util;
import ru.spectra.client.net.CloudConfigService;
import ru.spectra.client.Spectra;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class ConfigAutoSaveScheduler {
    public static final long autoSaveDelayMs = 1200;
    public static final Object lock = new Object();
    public static ScheduledFuture<?> pendingSave;

    public ConfigAutoSaveScheduler() {
    }

    public static void scheduleAutoSave() {
        CloudConfigService class357VarCloudConfigService = Spectra.INSTANCE.cloudConfigService();
        class357VarCloudConfigService.activeConfig().ifPresent(class304Var -> {
            if (Spectra.INSTANCE.configManager().menuStateConfig().isAutoSaveDisabled(class304Var.id())) {
                return;
            }
            synchronized (lock) {
                if (pendingSave != null && !pendingSave.isDone()) {
                    pendingSave.cancel(false);
                }
                pendingSave = Spectra.INSTANCE.executor().schedule(() -> {
                    class357VarCloudConfigService.saveConfig(class304Var.id(), Spectra.INSTANCE.moduleRepository(), Spectra.INSTANCE.widgetStack()).thenRun(() -> {
                        Spectra.LOGGER.info("Config auto-saved: {}", class304Var.id());
                    }).exceptionally(th -> {
                        Spectra.LOGGER.error("Failed to auto-save config", th);
                        return null;
                    });
                }, autoSaveDelayMs, TimeUnit.MILLISECONDS);
            }
        });
    }
}
