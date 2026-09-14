package ru.spectra.client.util;

import ru.spectra.client.Spectra;
import java.lang.reflect.Field;

/**
 * Optional runtime bridge to the bundled EntityCulling mod. Reflection keeps
 * Spectra buildable if that implementation is replaced or absent.
 */
public final class EntityCullingBridge {
    private static final String BASE_CLASS =
            "dev.tr7zw.entityculling.versionless.EntityCullingVersionlessBase";
    private static final String MOD_CLASS =
            "dev.tr7zw.entityculling.EntityCullingModBase";

    private boolean unavailableReported;

    public boolean apply(
            boolean enabled,
            boolean entityOcclusion,
            boolean blockEntityOcclusion,
            boolean tickCulling,
            int tracingDistance
    ) {
        try {
            Class<?> baseClass = Class.forName(BASE_CLASS);
            Class<?> modClass = Class.forName(MOD_CLASS);
            Object instance = field(modClass, "instance").get(null);
            if (instance == null) {
                return false;
            }

            field(baseClass, "enabled").setBoolean(null, enabled);
            Object config = field(baseClass, "config").get(instance);
            if (config == null) {
                return false;
            }
            setBoolean(config, "skipEntityCulling", !enabled || !entityOcclusion);
            setBoolean(config, "skipBlockEntityCulling", !enabled || !blockEntityOcclusion);
            setBoolean(config, "tickCulling", enabled && tickCulling);
            setInt(config, "tracingDistance", Math.max(32, Math.min(256, tracingDistance)));

            Object cullTask = field(modClass, "cullTask").get(instance);
            if (cullTask != null) {
                setBoolean(cullTask, "disableEntityCulling", !enabled || !entityOcclusion);
                setBoolean(cullTask, "disableBlockEntityCulling", !enabled || !blockEntityOcclusion);
            }
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            if (!this.unavailableReported) {
                this.unavailableReported = true;
                Spectra.LOGGER.warn(
                        "EntityCulling bridge is unavailable; keeping the renderer defaults",
                        error
                );
            }
            return false;
        }
    }

    private static Field field(Class<?> type, String name)
            throws NoSuchFieldException {
        Field field = type.getField(name);
        field.setAccessible(true);
        return field;
    }

    private static void setBoolean(Object owner, String name, boolean value)
            throws ReflectiveOperationException {
        field(owner.getClass(), name).setBoolean(owner, value);
    }

    private static void setInt(Object owner, String name, int value)
            throws ReflectiveOperationException {
        field(owner.getClass(), name).setInt(owner, value);
    }
}
