package ru.spectra.client.net;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Regression names are test assertions, never executable client features. */
final class HolyWorldSourcePurityTest {
    @Test
    void removedModuleTypesAndLegacyAimHooksAreNotInTheActiveSourceTree() throws Exception {
        List<String> removed = List.of("SeeInvisiblesModule", "PhaseModule", "SpiderModule",
                "AutoSoupModule", "NoBreakDelayModule", "FlightMode", "SpeedMode", "VelocityMode",
                "NoFallMode", "NoSlowMode", "NukerDiggingMode", "NukerWorkMode", "MoveCorrectionMode",
                "GrimAdvancedMode", "DuelContext", "DuelStrategy", "AutoDuelArmorType", "AutoDuelOffhandItem",
                "DuelKitType", "RotationConfig", "ScheduledRotation", "PlayerInteractItemC2SPacketMixin",
                "hookSilentRotationYaw", "hookSilentRotationPitch", "moveBypass$$$", "packetRotate");
        try (var paths = Files.walk(Path.of("src/main"))) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".java") || p.toString().endsWith(".json")).toList()) {
                String source = Files.readString(path);
                for (String identifier : removed) {
                    assertFalse(source.contains(identifier), path + " still references " + identifier);
                }
                for (String prefix : List.of("combat.attackaura.", "combat.autototem.", "misc.ancientxray.",
                        "movement.flight.", "movement.nofall.", "movement.speed.", "player.nuker.",
                        "player.cheststealer.", "misc.autoduel.", "render.armtweaks.swinganimation.onlyAura")) {
                    assertFalse(source.contains(prefix), path + " still contains a removed feature setting");
                }
            }
        }
    }
}
