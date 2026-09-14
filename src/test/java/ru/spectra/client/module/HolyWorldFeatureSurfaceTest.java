package ru.spectra.client.module;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/** Source-surface regression checks; these do not substitute for a live server test. */
final class HolyWorldFeatureSurfaceTest {
    private static final Path JAVA = Path.of("src/main/java/ru/spectra/client");

    @Test
    void helperSourceContainsNoSpecialItemAutomationOrItsSettings() throws Exception {
        for (String helper : new String[]{"FT", "HW", "RW"}) {
            String source = Files.readString(JAVA.resolve("module/" + helper + "HelperModule.java"));
            for (String removed : new String[]{"KeybindSetting", "HotbarItemUseController",
                    "useByBind", "windowClick", "useFoundItem", "pendingUse",
                    "chorusStoredWindowSlot", "interactEntity", "pickLockpickSetting",
                    "eventHelperSetting", "Auto Use Items"}) {
                assertFalse(source.contains(removed), helper + ": " + removed);
            }
        }
    }

    @Test
    void autoEatOnlySelectsExistingHotbarFood() throws Exception {
        String source = Files.readString(JAVA.resolve("module/AutoEatModule.java"));
        assertTrue(source.contains("for (int slot = 0; slot < 9; slot++)"));
        assertTrue(source.contains("slot >= 0 && slot < 9"));
        for (String forbidden : new String[]{"InventoryScope.INVENTORY", "windowClick", "SlotActionType",
                "getOffHandStack", "Hand.OFF_HAND", "pendingSwapSlot", "SwapUtil", "findAllItems"}) {
            assertFalse(source.contains(forbidden), forbidden);
        }
        assertTrue(source.contains("selectedSlot != this.eatingHotbarSlot"));
        assertTrue(source.contains("ItemUseController.INSTANCE.setUseItem(false)"));
    }

    @Test
    void seeInvisiblesIsNotPartOfTheSourceOrRegistration() throws Exception {
        assertFalse(Files.exists(JAVA.resolve("module/SeeInvisiblesModule.java")));
        try (var files = Files.walk(Path.of("src/main"))) {
            for (Path path : files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java") || path.toString().endsWith(".json"))
                    .toList()) {
                assertFalse(Files.readString(path).toLowerCase(java.util.Locale.ROOT).contains("seeinvisibles"),
                        path.toString());
            }
        }
    }

    @Test
    void hwStructuresAndCooldownNotificationsUseTheExistingHudAndSetting() throws Exception {
        String helper = Files.readString(JAVA.resolve("module/HWHelperModule.java"));
        String hud = Files.readString(JAVA.resolve("ui/StructuresWidget.java"));
        assertTrue(helper.contains("Lang.FTHELPER_COOLDOWN_NOTIFY"));
        assertTrue(helper.contains("cooldownNotifications.update"));
        assertTrue(hud.contains("hwHelper.getStructures().stream()"));
        assertTrue(hud.contains("hwHelper.structureTimerSetting.isValue()"));
        assertTrue(hud.contains("HW_STUN(Items.NETHER_STAR"));
        assertTrue(hud.contains("HW_EXPLOSIVE_TRAP(Items.PRISMARINE_SHARD"));
    }

    @Test
    void allUiActivationAndBindSurfacesRespectApiPolicy() throws Exception {
        assertTrue(Files.readString(JAVA.resolve("module/Module.java"))
                .contains("HolyWorldFeatureControl.allows(getFeatureId())"));
        assertTrue(Files.readString(JAVA.resolve("ui/HotkeysWidget.java"))
                .contains("isVisibleInMenu() && class605Var.hasKeyBind()"));
        assertTrue(Files.readString(JAVA.resolve("ui/ModuleCard.java"))
                .contains("this.keybindWindow.availability = this.module::isVisibleInMenu"));
        assertTrue(Files.readString(JAVA.resolve("ui/NotificationWidget.java"))
                .contains("!class080Var.module().isVisibleInMenu()"));
        assertTrue(Files.readString(JAVA.resolve("util/KeybindHandler.java")).contains("!class605Var.isAvailable()"));
        assertTrue(Files.readString(JAVA.resolve("util/ModuleArgumentParser.java")).contains("Module::isVisibleInMenu"));
        assertTrue(Files.readString(JAVA.resolve("config/MacroRepository.java")).contains("allows(\"macros\")"));
        assertTrue(Files.readString(JAVA.resolve("util/DropAllHandler.java")).contains("allows(\"dropall\")"));
        assertTrue(Files.readString(JAVA.resolve("util/CommandRegistry.java")).contains("filter(ClientCommand::isAvailable)"));
        assertTrue(Files.readString(JAVA.resolve("ui/Draggable.java")).contains("this.availability.getAsBoolean()"));
        assertTrue(Files.readString(JAVA.resolve("module/HudModules.java")).contains("this.widget.availability = this::isAvailable"));
    }
}
