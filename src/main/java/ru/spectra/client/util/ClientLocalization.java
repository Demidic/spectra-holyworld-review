package ru.spectra.client.util;

import ru.spectra.client.Lang;
import ru.spectra.client.Spectra;
import ru.spectra.client.type.Language;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.Map;

/** Client-owned language selection, independent from Minecraft's language. */
public final class ClientLocalization {
    private static final Map<String, String> RU_OVERRIDES = Map.ofEntries(
            Map.entry("Selects one available option", "\u0412\u044B\u0431\u0438\u0440\u0430\u0435\u0442 \u043E\u0434\u0438\u043D \u0434\u043E\u0441\u0442\u0443\u043F\u043D\u044B\u0439 \u0432\u0430\u0440\u0438\u0430\u043D\u0442"),
            Map.entry("Selects which options are active", "\u0412\u044B\u0431\u0438\u0440\u0430\u0435\u0442 \u0430\u043A\u0442\u0438\u0432\u043D\u044B\u0435 \u0432\u0430\u0440\u0438\u0430\u043D\u0442\u044B"),
            Map.entry("Sets the keyboard shortcut for this action", "\u0417\u0430\u0434\u0430\u0451\u0442 \u043A\u043B\u0430\u0432\u0438\u0448\u0443 \u0434\u043B\u044F \u044D\u0442\u043E\u0433\u043E \u0434\u0435\u0439\u0441\u0442\u0432\u0438\u044F"),
            Map.entry("Example Cooldown", "\u041F\u0440\u0438\u043C\u0435\u0440 \u043F\u0435\u0440\u0435\u0437\u0430\u0440\u044F\u0434\u043A\u0438"),
            Map.entry("Example Effect", "\u041F\u0440\u0438\u043C\u0435\u0440 \u044D\u0444\u0444\u0435\u043A\u0442\u0430"),
            Map.entry("Example Keybind", "\u041F\u0440\u0438\u043C\u0435\u0440 \u0431\u0438\u043D\u0434\u0430"),
            Map.entry("Scale", "\u041C\u0430\u0441\u0448\u0442\u0430\u0431"),
            Map.entry("Custom placement", "\u0421\u0432\u043E\u0431\u043E\u0434\u043D\u043E\u0435 \u0440\u0430\u0441\u043F\u043E\u043B\u043E\u0436\u0435\u043D\u0438\u0435"),
            Map.entry("Remove background", "\u0423\u0431\u0440\u0430\u0442\u044C \u0444\u043E\u043D"),
            Map.entry("Menu scale", "\u041C\u0430\u0441\u0448\u0442\u0430\u0431 \u043C\u0435\u043D\u044E"),
            Map.entry("Changes the size of the client menu.", "\u041C\u0435\u043D\u044F\u0435\u0442 \u0440\u0430\u0437\u043C\u0435\u0440 \u043C\u0435\u043D\u044E \u043A\u043B\u0438\u0435\u043D\u0442\u0430."),
            Map.entry("HUD scale", "\u041C\u0430\u0441\u0448\u0442\u0430\u0431 HUD"),
            Map.entry("Changes the size of HUD elements.", "\u041C\u0435\u043D\u044F\u0435\u0442 \u0440\u0430\u0437\u043C\u0435\u0440 \u044D\u043B\u0435\u043C\u0435\u043D\u0442\u043E\u0432 HUD."),
            Map.entry("Menu background", "\u0424\u043E\u043D \u043C\u0435\u043D\u044E"),
            Map.entry("Choose dimming, blur, both, or no background effect.", "\u0412\u044B\u0431\u0435\u0440\u0438\u0442\u0435 \u0437\u0430\u0442\u0435\u043C\u043D\u0435\u043D\u0438\u0435, \u0440\u0430\u0437\u043C\u044B\u0442\u0438\u0435, \u043E\u0431\u0430 \u044D\u0444\u0444\u0435\u043A\u0442\u0430 \u0438\u043B\u0438 \u043D\u0438\u0447\u0435\u0433\u043E."),
            Map.entry("Menu key", "\u041A\u043B\u0430\u0432\u0438\u0448\u0430 \u043C\u0435\u043D\u044E"),
            Map.entry("Key used to open and close the client menu.", "\u041A\u043B\u0430\u0432\u0438\u0448\u0430 \u043E\u0442\u043A\u0440\u044B\u0442\u0438\u044F \u0438 \u0437\u0430\u043A\u0440\u044B\u0442\u0438\u044F \u043C\u0435\u043D\u044E \u043A\u043B\u0438\u0435\u043D\u0442\u0430."),
            Map.entry("Language", "\u042F\u0437\u044B\u043A"),
            Map.entry("Changes the language of descriptions and interface text.", "\u041C\u0435\u043D\u044F\u0435\u0442 \u044F\u0437\u044B\u043A \u043E\u043F\u0438\u0441\u0430\u043D\u0438\u0439 \u0438 \u0442\u0435\u043A\u0441\u0442\u0430 \u0438\u043D\u0442\u0435\u0440\u0444\u0435\u0439\u0441\u0430."),
            Map.entry("Animation", "\u0410\u043D\u0438\u043C\u0430\u0446\u0438\u044F"),
            Map.entry("Attack mode", "\u0420\u0435\u0436\u0438\u043C \u0430\u0442\u0430\u043A\u0438"),
            Map.entry("Auto fly", "\u0410\u0432\u0442\u043E\u043F\u043E\u043B\u0451\u0442"),
            Map.entry("Block disconnect", "\u0411\u043B\u043E\u043A\u0438\u0440\u043E\u0432\u0430\u0442\u044C \u043E\u0442\u043A\u043B\u044E\u0447\u0435\u043D\u0438\u0435"),
            Map.entry("Block in PvP", "\u0411\u043B\u043E\u043A\u0438\u0440\u043E\u0432\u0430\u0442\u044C \u0432 PvP"),
            Map.entry("Blur fog", "\u0420\u0430\u0437\u043C\u044B\u0432\u0430\u0442\u044C \u0442\u0443\u043C\u0430\u043D"),
            Map.entry("Branches", "\u0412\u0435\u0442\u043A\u0438"),
            Map.entry("Button", "\u041A\u043D\u043E\u043F\u043A\u0430"),
            Map.entry("Change hand key", "\u041A\u043B\u0430\u0432\u0438\u0448\u0430 \u0441\u043C\u0435\u043D\u044B \u0440\u0443\u043A\u0438"),
            Map.entry("Change Sky", "\u0421\u043C\u0435\u043D\u0438\u0442\u044C \u043D\u0435\u0431\u043E"),
            Map.entry("Change weather", "\u0421\u043C\u0435\u043D\u0438\u0442\u044C \u043F\u043E\u0433\u043E\u0434\u0443"),
            Map.entry("Client", "\u041A\u043B\u0438\u0435\u043D\u0442\u0441\u043A\u0438\u0439"),
            Map.entry("Classic", "\u041A\u043B\u0430\u0441\u0441\u0438\u0447\u0435\u0441\u043A\u0438\u0439"),
            Map.entry("Custom", "\u0421\u0432\u043E\u0439"),
            Map.entry("Shader", "\u0428\u0435\u0439\u0434\u0435\u0440"),
            Map.entry("Clear", "\u042F\u0441\u043D\u043E"),
            Map.entry("Rain", "\u0414\u043E\u0436\u0434\u044C"),
            Map.entry("Thunder", "\u0413\u0440\u043E\u0437\u0430"),
            Map.entry("All", "\u0412\u0441\u0435"),
            Map.entry("Single", "\u041E\u0434\u0438\u043D\u043E\u0447\u043D\u044B\u0439"),
            Map.entry("Critical hits", "\u041A\u0440\u0438\u0442\u0438\u0447\u0435\u0441\u043A\u0438\u0435 \u0443\u0434\u0430\u0440\u044B"),
            Map.entry("By type", "\u041F\u043E \u0442\u0438\u043F\u0443"),
            Map.entry("Wave", "\u0412\u043E\u043B\u043D\u0430"),
            Map.entry("Full Bright", "\u041F\u043E\u043B\u043D\u0430\u044F \u044F\u0440\u043A\u043E\u0441\u0442\u044C"),
            Map.entry("Night Vision", "\u041D\u043E\u0447\u043D\u043E\u0435 \u0437\u0440\u0435\u043D\u0438\u0435")
            ,Map.entry("Optimizer", "\u041E\u043F\u0442\u0438\u043C\u0438\u0437\u0430\u0442\u043E\u0440")
            ,Map.entry("Off-screen blocks", "\u0411\u043b\u043e\u043a\u0438 \u0437\u0430 \u044d\u043a\u0440\u0430\u043d\u043e\u043c")
            ,Map.entry("Caches terrain during camera turns; uses native rendering while moving or with shader packs", "\u041A\u044D\u0448\u0438\u0440\u0443\u0435\u0442 \u043C\u0438\u0440 \u043F\u0440\u0438 \u043F\u043E\u0432\u043E\u0440\u043E\u0442\u0430\u0445 \u043A\u0430\u043C\u0435\u0440\u044B; \u043F\u0440\u0438 \u0434\u0432\u0438\u0436\u0435\u043D\u0438\u0438 \u0438 \u0441 \u0448\u0435\u0439\u0434\u0435\u0440\u0430\u043C\u0438 \u0438\u0441\u043F\u043E\u043B\u044C\u0437\u0443\u0435\u0442 \u043E\u0431\u044B\u0447\u043D\u044B\u0439 \u0440\u0435\u043D\u0434\u0435\u0440")
            ,Map.entry("Entity occlusion", "\u041E\u0442\u0441\u0435\u0447\u0435\u043D\u0438\u0435 \u0441\u0443\u0449\u043D\u043E\u0441\u0442\u0435\u0439")
            ,Map.entry("Skips entities that are fully hidden behind world geometry", "\u041D\u0435 \u0440\u0435\u043D\u0434\u0435\u0440\u0438\u0442 \u0441\u0443\u0449\u043D\u043E\u0441\u0442\u0438, \u043F\u043E\u043B\u043D\u043E\u0441\u0442\u044C\u044E \u0441\u043A\u0440\u044B\u0442\u044B\u0435 \u043C\u0438\u0440\u043E\u043C")
            ,Map.entry("Block entity occlusion", "\u041E\u0442\u0441\u0435\u0447\u0435\u043D\u0438\u0435 \u0431\u043B\u043E\u043A-\u0441\u0443\u0449\u043D\u043E\u0441\u0442\u0435\u0439")
            ,Map.entry("Skips hidden chests, signs and other block entities", "\u041D\u0435 \u0440\u0435\u043D\u0434\u0435\u0440\u0438\u0442 \u0441\u043A\u0440\u044B\u0442\u044B\u0435 \u0441\u0443\u043D\u0434\u0443\u043A\u0438, \u0442\u0430\u0431\u043B\u0438\u0447\u043A\u0438 \u0438 \u0434\u0440\u0443\u0433\u0438\u0435 \u0431\u043B\u043E\u043A-\u0441\u0443\u0449\u043D\u043E\u0441\u0442\u0438")
            ,Map.entry("Risky tick culling", "\u0420\u0438\u0441\u043A\u043E\u0432\u0430\u043D\u043D\u043E\u0435 \u043E\u0442\u0441\u0435\u0447\u0435\u043D\u0438\u0435 \u0442\u0438\u043A\u043E\u0432")
            ,Map.entry("Stops updates for hidden entities; may conflict with gameplay mods", "\u041E\u0441\u0442\u0430\u043D\u0430\u0432\u043B\u0438\u0432\u0430\u0435\u0442 \u043E\u0431\u043D\u043E\u0432\u043B\u0435\u043D\u0438\u044F \u0441\u043A\u0440\u044B\u0442\u044B\u0445 \u0441\u0443\u0449\u043D\u043E\u0441\u0442\u0435\u0439; \u043C\u043E\u0436\u0435\u0442 \u043A\u043E\u043D\u0444\u043B\u0438\u043A\u0442\u043E\u0432\u0430\u0442\u044C \u0441 \u043C\u043E\u0434\u0430\u043C\u0438")
            ,Map.entry("Occlusion distance", "\u0414\u0430\u043B\u044C\u043D\u043E\u0441\u0442\u044C \u043E\u0442\u0441\u0435\u0447\u0435\u043D\u0438\u044F")
            ,Map.entry("Maximum distance used for entity visibility checks", "\u041C\u0430\u043A\u0441\u0438\u043C\u0430\u043B\u044C\u043D\u0430\u044F \u0434\u0430\u043B\u044C\u043D\u043E\u0441\u0442\u044C \u043F\u0440\u043E\u0432\u0435\u0440\u043A\u0438 \u0432\u0438\u0434\u0438\u043C\u043E\u0441\u0442\u0438 \u0441\u0443\u0449\u043D\u043E\u0441\u0442\u0435\u0439")
            ,Map.entry("Adds animated smoke around the hand and held item", "\u0414\u043E\u0431\u0430\u0432\u043B\u044F\u0435\u0442 \u0430\u043D\u0438\u043C\u0438\u0440\u043E\u0432\u0430\u043D\u043D\u044B\u0439 \u0434\u044B\u043C \u0432\u043E\u043A\u0440\u0443\u0433 \u0440\u0443\u043A\u0438 \u0438 \u043F\u0440\u0435\u0434\u043C\u0435\u0442\u0430")
            ,Map.entry("Allows Wave, Rising skin, or both at once", "\u041F\u043E\u0437\u0432\u043E\u043B\u044F\u0435\u0442 \u0432\u044B\u0431\u0440\u0430\u0442\u044C \u0432\u043E\u043B\u043D\u0443, \u043F\u043E\u0434\u043D\u0438\u043C\u0430\u044E\u0449\u0438\u0439\u0441\u044F \u0441\u043A\u0438\u043D \u0438\u043B\u0438 \u043E\u0431\u0430 \u044D\u0444\u0444\u0435\u043A\u0442\u0430")
            ,Map.entry("Blurs distant scenery instead of replacing it with a solid fog color", "\u0420\u0430\u0437\u043C\u044B\u0432\u0430\u0435\u0442 \u0434\u0430\u043B\u044C\u043D\u0438\u0435 \u043E\u0431\u044A\u0435\u043A\u0442\u044B \u0432\u043C\u0435\u0441\u0442\u043E \u0441\u043F\u043B\u043E\u0448\u043D\u043E\u0439 \u0437\u0430\u043B\u0438\u0432\u043A\u0438 \u0442\u0443\u043C\u0430\u043D\u0430")
            ,Map.entry("Changes the color and opacity of the contour smoke", "\u041C\u0435\u043D\u044F\u0435\u0442 \u0446\u0432\u0435\u0442 \u0438 \u043F\u0440\u043E\u0437\u0440\u0430\u0447\u043D\u043E\u0441\u0442\u044C \u043A\u043E\u043D\u0442\u0443\u0440\u043D\u043E\u0433\u043E \u0434\u044B\u043C\u0430")
            ,Map.entry("Changes the color of the hit wave", "\u041C\u0435\u043D\u044F\u0435\u0442 \u0446\u0432\u0435\u0442 \u0432\u043E\u043B\u043D\u044B \u043E\u0442 \u0443\u0434\u0430\u0440\u0430")
            ,Map.entry("Changes the sky color or applies a sky shader", "\u041C\u0435\u043D\u044F\u0435\u0442 \u0446\u0432\u0435\u0442 \u043D\u0435\u0431\u0430 \u0438\u043B\u0438 \u043F\u0440\u0438\u043C\u0435\u043D\u044F\u0435\u0442 \u0448\u0435\u0439\u0434\u0435\u0440 \u043D\u0435\u0431\u0430")
            ,Map.entry("Chooses which attacks create the effect", "\u0412\u044B\u0431\u0438\u0440\u0430\u0435\u0442 \u0443\u0434\u0430\u0440\u044B, \u0441\u043E\u0437\u0434\u0430\u044E\u0449\u0438\u0435 \u044D\u0444\u0444\u0435\u043A\u0442")
            ,Map.entry("Chorus Fruit key", "\u041A\u043B\u0430\u0432\u0438\u0448\u0430 \u043F\u043B\u043E\u0434\u0430 \u0445\u043E\u0440\u0443\u0441\u0430")
            ,Map.entry("Client color", "\u0426\u0432\u0435\u0442 \u043A\u043B\u0438\u0435\u043D\u0442\u0430")
            ,Map.entry("Color mode", "\u0420\u0435\u0436\u0438\u043C \u0446\u0432\u0435\u0442\u0430")
            ,Map.entry("Custom color", "\u0421\u0432\u043E\u0439 \u0446\u0432\u0435\u0442")
            ,Map.entry("Damage effect", "\u042D\u0444\u0444\u0435\u043A\u0442 \u0443\u0440\u043E\u043D\u0430")
            ,Map.entry("Ender Pearl key", "\u041A\u043B\u0430\u0432\u0438\u0448\u0430 \u044D\u043D\u0434\u0435\u0440-\u0436\u0435\u043C\u0447\u0443\u0433\u0430")
            ,Map.entry("Fill", "\u0417\u0430\u043B\u0438\u0432\u043A\u0430")
            ,Map.entry("Fill enabled", "\u0412\u043A\u043B\u044E\u0447\u0438\u0442\u044C \u0437\u0430\u043B\u0438\u0432\u043A\u0443")
            ,Map.entry("Fire Tornado key", "\u041A\u043B\u0430\u0432\u0438\u0448\u0430 \u043E\u0433\u043D\u0435\u043D\u043D\u043E\u0433\u043E \u0442\u043E\u0440\u043D\u0430\u0434\u043E")
            ,Map.entry("Full Bright makes every area bright; Night Vision preserves local light differences", "Full Bright \u043E\u0441\u0432\u0435\u0449\u0430\u0435\u0442 \u0432\u0441\u0451; Night Vision \u0441\u043E\u0445\u0440\u0430\u043D\u044F\u0435\u0442 \u0440\u0430\u0437\u043B\u0438\u0447\u0438\u044F \u043B\u043E\u043A\u0430\u043B\u044C\u043D\u043E\u0433\u043E \u043E\u0441\u0432\u0435\u0449\u0435\u043D\u0438\u044F")
            ,Map.entry("Gradient", "\u0413\u0440\u0430\u0434\u0438\u0435\u043D\u0442")
            ,Map.entry("Hit type", "\u0422\u0438\u043F \u0443\u0434\u0430\u0440\u0430")
            ,Map.entry("Hold this key to zoom the camera", "\u0423\u0434\u0435\u0440\u0436\u0438\u0432\u0430\u0439\u0442\u0435 \u043A\u043B\u0430\u0432\u0438\u0448\u0443 \u0434\u043B\u044F \u043F\u0440\u0438\u0431\u043B\u0438\u0436\u0435\u043D\u0438\u044F \u043A\u0430\u043C\u0435\u0440\u044B")
            ,Map.entry("Investment interval (minutes)", "\u0418\u043D\u0442\u0435\u0440\u0432\u0430\u043B \u0438\u043D\u0432\u0435\u0441\u0442\u0438\u0446\u0438\u0439 (\u043C\u0438\u043D\u0443\u0442\u044B)")
            ,Map.entry("Item color", "\u0426\u0432\u0435\u0442 \u043F\u0440\u0435\u0434\u043C\u0435\u0442\u0430")
            ,Map.entry("MEGA Grief", "\u041C\u0415\u0413\u0410 \u0413\u0440\u0438\u0444")
            ,Map.entry("Moves Chorus Fruit to the hotbar; press again to return it", "\u041F\u0435\u0440\u0435\u043C\u0435\u0449\u0430\u0435\u0442 \u043F\u043B\u043E\u0434 \u0445\u043E\u0440\u0443\u0441\u0430 \u0432 \u0445\u043E\u0442\u0431\u0430\u0440; \u043F\u043E\u0432\u0442\u043E\u0440\u043D\u043E\u0435 \u043D\u0430\u0436\u0430\u0442\u0438\u0435 \u0432\u043E\u0437\u0432\u0440\u0430\u0449\u0430\u0435\u0442 \u0435\u0433\u043E")
            ,Map.entry("Outline enabled", "\u0412\u043A\u043B\u044E\u0447\u0438\u0442\u044C \u043E\u0431\u0432\u043E\u0434\u043A\u0443")
            ,Map.entry("Overrides the visible world weather", "\u0417\u0430\u043C\u0435\u043D\u044F\u0435\u0442 \u043E\u0442\u043E\u0431\u0440\u0430\u0436\u0430\u0435\u043C\u0443\u044E \u043F\u043E\u0433\u043E\u0434\u0443 \u043C\u0438\u0440\u0430")
            ,Map.entry("Prevent duplicates", "\u041D\u0435 \u0434\u043E\u043F\u0443\u0441\u043A\u0430\u0442\u044C \u0434\u0443\u0431\u043B\u0438\u043A\u0430\u0442\u044B")
            ,Map.entry("Priority enchanted", "\u041F\u0440\u0438\u043E\u0440\u0438\u0442\u0435\u0442 \u0437\u0430\u0447\u0430\u0440\u043E\u0432\u0430\u043D\u043D\u044B\u0445")
            ,Map.entry("PvP analyzer", "\u0410\u043D\u0430\u043B\u0438\u0437\u0430\u0442\u043E\u0440 PvP")
            ,Map.entry("Random delay", "\u0421\u043B\u0443\u0447\u0430\u0439\u043D\u0430\u044F \u0437\u0430\u0434\u0435\u0440\u0436\u043A\u0430")
            ,Map.entry("Selects clear, rainy or thunder weather", "\u0412\u044B\u0431\u0438\u0440\u0430\u0435\u0442 \u044F\u0441\u043D\u0443\u044E \u043F\u043E\u0433\u043E\u0434\u0443, \u0434\u043E\u0436\u0434\u044C \u0438\u043B\u0438 \u0433\u0440\u043E\u0437\u0443")
            ,Map.entry("Selects the animated sky shader", "\u0412\u044B\u0431\u0438\u0440\u0430\u0435\u0442 \u0430\u043D\u0438\u043C\u0438\u0440\u043E\u0432\u0430\u043D\u043D\u044B\u0439 \u0448\u0435\u0439\u0434\u0435\u0440 \u043D\u0435\u0431\u0430")
            ,Map.entry("Sets the custom sky color", "\u0417\u0430\u0434\u0430\u0451\u0442 \u0441\u043E\u0431\u0441\u0442\u0432\u0435\u043D\u043D\u044B\u0439 \u0446\u0432\u0435\u0442 \u043D\u0435\u0431\u0430")
            ,Map.entry("Shift duration", "\u0414\u043B\u0438\u0442\u0435\u043B\u044C\u043D\u043E\u0441\u0442\u044C \u043F\u0440\u0438\u0441\u0435\u0434\u0430")
            ,Map.entry("Show other players", "\u041F\u043E\u043A\u0430\u0437\u044B\u0432\u0430\u0442\u044C \u0434\u0440\u0443\u0433\u0438\u0445 \u0438\u0433\u0440\u043E\u043A\u043E\u0432")
            ,Map.entry("Show own hitbox", "\u041F\u043E\u043A\u0430\u0437\u044B\u0432\u0430\u0442\u044C \u0441\u0432\u043E\u0439 \u0445\u0438\u0442\u0431\u043E\u043A\u0441")
            ,Map.entry("Skin color", "\u0426\u0432\u0435\u0442 \u0441\u043A\u0438\u043D\u0430")
            ,Map.entry("Sky color", "\u0426\u0432\u0435\u0442 \u043D\u0435\u0431\u0430")
            ,Map.entry("Sky mode", "\u0420\u0435\u0436\u0438\u043C \u043D\u0435\u0431\u0430")
            ,Map.entry("Sky shader", "\u0428\u0435\u0439\u0434\u0435\u0440 \u043D\u0435\u0431\u0430")
            ,Map.entry("Slots", "\u0421\u043B\u043E\u0442\u044B")
            ,Map.entry("Smart swap", "\u0423\u043C\u043D\u0430\u044F \u0437\u0430\u043C\u0435\u043D\u0430")
            ,Map.entry("Smoke", "\u0414\u044B\u043C")
            ,Map.entry("Smoke color", "\u0426\u0432\u0435\u0442 \u0434\u044B\u043C\u0430")
            ,Map.entry("Smooth block switch", "\u041F\u043B\u0430\u0432\u043D\u0430\u044F \u0441\u043C\u0435\u043D\u0430 \u0431\u043B\u043E\u043A\u0430")
            ,Map.entry("Swap notifications", "\u0423\u0432\u0435\u0434\u043E\u043C\u043B\u0435\u043D\u0438\u044F \u043E \u0437\u0430\u043C\u0435\u043D\u0435")
            ,Map.entry("Swap time", "\u0412\u0440\u0435\u043C\u044F \u0437\u0430\u043C\u0435\u043D\u044B")
            ,Map.entry("Switches the leading hand between right and left", "\u041F\u0435\u0440\u0435\u043A\u043B\u044E\u0447\u0430\u0435\u0442 \u0432\u0435\u0434\u0443\u0449\u0443\u044E \u0440\u0443\u043A\u0443 \u043C\u0435\u0436\u0434\u0443 \u043F\u0440\u0430\u0432\u043E\u0439 \u0438 \u043B\u0435\u0432\u043E\u0439")
            ,Map.entry("Tints the rising player skin", "\u041E\u043A\u0440\u0430\u0448\u0438\u0432\u0430\u0435\u0442 \u043F\u043E\u0434\u043D\u0438\u043C\u0430\u044E\u0449\u0438\u0439\u0441\u044F \u0441\u043A\u0438\u043D \u0438\u0433\u0440\u043E\u043A\u0430")
            ,Map.entry("Unload Gunpowder", "\u0412\u044B\u0433\u0440\u0443\u0437\u0438\u0442\u044C \u043F\u043E\u0440\u043E\u0445")
            ,Map.entry("Uses an Ender Pearl and returns to the previous slot", "\u0418\u0441\u043F\u043E\u043B\u044C\u0437\u0443\u0435\u0442 \u044D\u043D\u0434\u0435\u0440-\u0436\u0435\u043C\u0447\u0443\u0433 \u0438 \u0432\u043E\u0437\u0432\u0440\u0430\u0449\u0430\u0435\u0442 \u043F\u0440\u0435\u0434\u044B\u0434\u0443\u0449\u0438\u0439 \u0441\u043B\u043E\u0442")
            ,Map.entry("Uses Fire Tornado from the inventory", "\u0418\u0441\u043F\u043E\u043B\u044C\u0437\u0443\u0435\u0442 \u043E\u0433\u043D\u0435\u043D\u043D\u044B\u0439 \u0442\u043E\u0440\u043D\u0430\u0434\u043E \u0438\u0437 \u0438\u043D\u0432\u0435\u043D\u0442\u0430\u0440\u044F")
            ,Map.entry("Wave color", "\u0426\u0432\u0435\u0442 \u0432\u043E\u043B\u043D\u044B")
            ,Map.entry("Weather", "\u041F\u043E\u0433\u043E\u0434\u0430")
            ,Map.entry("Zoom key", "\u041A\u043B\u0430\u0432\u0438\u0448\u0430 \u043F\u0440\u0438\u0431\u043B\u0438\u0436\u0435\u043D\u0438\u044F")
            ,Map.entry("Effects", "\u042D\u0444\u0444\u0435\u043A\u0442\u044B")
            ,Map.entry("Critical hits only", "\u0422\u043E\u043B\u044C\u043A\u043E \u043A\u0440\u0438\u0442\u0438\u0447\u0435\u0441\u043A\u0438\u0435 \u0443\u0434\u0430\u0440\u044B")
            ,Map.entry("All hits", "\u0412\u0441\u0435 \u0443\u0434\u0430\u0440\u044B")
            ,Map.entry("Rising skin", "\u041F\u043E\u0434\u043D\u0438\u043C\u0430\u044E\u0449\u0438\u0439\u0441\u044F \u0441\u043A\u0438\u043D")
            ,Map.entry("Fill opacity", "\u041F\u0440\u043E\u0437\u0440\u0430\u0447\u043D\u043E\u0441\u0442\u044C \u0437\u0430\u043B\u0438\u0432\u043A\u0438")
            ,Map.entry("Line width", "\u0422\u043E\u043B\u0449\u0438\u043D\u0430 \u043B\u0438\u043D\u0438\u0438")
            ,Map.entry("Outline opacity", "\u041F\u0440\u043E\u0437\u0440\u0430\u0447\u043D\u043E\u0441\u0442\u044C \u043E\u0431\u0432\u043E\u0434\u043A\u0438")
            ,Map.entry("Skin lifetime", "\u0412\u0440\u0435\u043C\u044F \u0436\u0438\u0437\u043D\u0438 \u0441\u043A\u0438\u043D\u0430")
            ,Map.entry("Skin opacity", "\u041F\u0440\u043E\u0437\u0440\u0430\u0447\u043D\u043E\u0441\u0442\u044C \u0441\u043A\u0438\u043D\u0430")
            ,Map.entry("Skin rise height", "\u0412\u044B\u0441\u043E\u0442\u0430 \u043F\u043E\u0434\u044A\u0451\u043C\u0430 \u0441\u043A\u0438\u043D\u0430")
            ,Map.entry("Wave opacity", "\u041F\u0440\u043E\u0437\u0440\u0430\u0447\u043D\u043E\u0441\u0442\u044C \u0432\u043E\u043B\u043D\u044B")
            ,Map.entry("Wave radius", "\u0420\u0430\u0434\u0438\u0443\u0441 \u0432\u043E\u043B\u043D\u044B")
            ,Map.entry("Wave speed", "\u0421\u043A\u043E\u0440\u043E\u0441\u0442\u044C \u0432\u043E\u043B\u043D\u044B")
            ,Map.entry("World effects", "\u042D\u0444\u0444\u0435\u043A\u0442\u044B \u043C\u0438\u0440\u0430")
            ,Map.entry("Selects events that receive shader effects", "\u0412\u044B\u0431\u0438\u0440\u0430\u0435\u0442 \u0441\u043E\u0431\u044B\u0442\u0438\u044F, \u0434\u043B\u044F \u043A\u043E\u0442\u043E\u0440\u044B\u0445 \u0432\u043A\u043B\u044E\u0447\u0430\u044E\u0442\u0441\u044F \u0448\u0435\u0439\u0434\u0435\u0440\u043D\u044B\u0435 \u044D\u0444\u0444\u0435\u043A\u0442\u044B")
            ,Map.entry("Effect intensity", "\u0421\u0438\u043B\u0430 \u044D\u0444\u0444\u0435\u043A\u0442\u043E\u0432")
            ,Map.entry("Controls refraction, flash and distortion strength", "\u041C\u0435\u043D\u044F\u0435\u0442 \u0441\u0438\u043B\u0443 \u043F\u0440\u0435\u043B\u043E\u043C\u043B\u0435\u043D\u0438\u044F, \u0432\u0441\u043F\u044B\u0448\u043A\u0438 \u0438 \u0438\u0441\u043A\u0430\u0436\u0435\u043D\u0438\u044F")
            ,Map.entry("Blur strength", "\u0421\u0438\u043B\u0430 \u0440\u0430\u0437\u043C\u044B\u0442\u0438\u044F")
            ,Map.entry("Controls the liquid-glass blur radius", "\u041C\u0435\u043D\u044F\u0435\u0442 \u0440\u0430\u0434\u0438\u0443\u0441 \u0440\u0430\u0437\u043C\u044B\u0442\u0438\u044F \u0436\u0438\u0434\u043A\u043E\u0433\u043E \u0441\u0442\u0435\u043A\u043B\u0430")
            ,Map.entry("Shader screen shake", "\u0428\u0435\u0439\u0434\u0435\u0440\u043D\u0430\u044F \u0442\u0440\u044F\u0441\u043A\u0430 \u044D\u043A\u0440\u0430\u043D\u0430")
            ,Map.entry("Shakes only the rendered world, leaving the HUD stable", "\u0422\u0440\u044F\u0441\u0451\u0442 \u0442\u043E\u043B\u044C\u043A\u043E \u043E\u0442\u0440\u0438\u0441\u043E\u0432\u0430\u043D\u043D\u044B\u0439 \u043C\u0438\u0440, \u043E\u0441\u0442\u0430\u0432\u043B\u044F\u044F HUD \u043D\u0435\u043F\u043E\u0434\u0432\u0438\u0436\u043D\u044B\u043C")
            ,Map.entry("TNT explosion", "\u0412\u0437\u0440\u044B\u0432 TNT")
            ,Map.entry("Mace smash", "\u0423\u0434\u0430\u0440 \u0431\u0443\u043B\u0430\u0432\u043E\u0439")
            ,Map.entry("Arrow blood", "\u041A\u0440\u043E\u0432\u044C \u043E\u0442 \u0441\u0442\u0440\u0435\u043B")
    );

    private static volatile Language minecraftLanguage;
    private static volatile TranslationStorage minecraftTranslations;
    private record LiteralIndex(Map<String, String> english, Map<String, String> russian,
                                Map<String, String> translations) { }
    private static volatile LiteralIndex literalIndex;

    private ClientLocalization() {
    }

    public static Language language() {
        try {
            if (Spectra.INSTANCE != null && Spectra.INSTANCE.languages() != null
                    && Spectra.INSTANCE.languages().current() != null) {
                return Spectra.INSTANCE.languages().current();
            }
        } catch (RuntimeException ignored) {
        }
        return Language.PRIMARY;
    }

    public static boolean isRussian() {
        return language() == Language.ru_RU;
    }

    public static String text(String english, String russian) {
        return isRussian() ? russian : english;
    }

    public static String literal(String original) {
        if (original == null || !isRussian()) {
            return original;
        }
        String explicit = RU_OVERRIDES.get(original);
        if (explicit != null) {
            return explicit;
        }
        if (Lang.languageLookups.get(Language.en_US) instanceof StringMappingLookup english
                && Lang.languageLookups.get(Language.ru_RU) instanceof StringMappingLookup russian
                && english.mappings != null && russian.mappings != null) {
            Map<String, String> englishMap = english.mappings;
            Map<String, String> russianMap = russian.mappings;
            LiteralIndex index = literalIndex;
            if (index == null || index.english() != englishMap || index.russian() != russianMap) {
                // Dictionaries are populated by Gson before publication. A
                // replacement dictionary gets a new index; ordinary labels
                // no longer scan the complete English dictionary each frame.
                index = new LiteralIndex(englishMap, russianMap, indexLiterals(englishMap, russianMap));
                literalIndex = index;
            }
            return index.translations().getOrDefault(original, original);
        }
        return original;
    }

    static Map<String, String> indexLiterals(Map<String, String> english, Map<String, String> russian) {
        Map<String, String> result = new java.util.HashMap<>();
        for (Map.Entry<String, String> entry : english.entrySet()) {
            String translated = russian.get(entry.getKey());
            if (entry.getValue() != null && translated != null && !translated.isBlank()) {
                // Match the old scan: the first nonblank translation wins when
                // several keys share the same English wording.
                result.putIfAbsent(entry.getValue(), translated);
            }
        }
        return java.util.Collections.unmodifiableMap(result);
    }

    public static String minecraft(String translationKey, String fallback) {
        if (translationKey == null || translationKey.isBlank()) {
            return fallback;
        }
        ensureMinecraftTranslations();
        return minecraftTranslations == null
                ? fallback
                : minecraftTranslations.get(translationKey, fallback);
    }

    public static String itemName(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        if (stack.get(DataComponentTypes.CUSTOM_NAME) != null) {
            return stack.getName().getString();
        }
        return minecraft(stack.getItem().getTranslationKey(), stack.getName().getString());
    }

    public static void invalidateMinecraftTranslations() {
        minecraftLanguage = null;
        minecraftTranslations = null;
    }

    private static synchronized void ensureMinecraftTranslations() {
        Language selected = language();
        if (minecraftLanguage == selected && minecraftTranslations != null) {
            return;
        }
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            List<String> definitions = selected == Language.ru_RU
                    ? List.of("en_us", "ru_ru")
                    : List.of("en_us");
            minecraftTranslations = TranslationStorage.load(
                    client.getResourceManager(), definitions, false);
        } catch (Exception exception) {
            if (Spectra.LOGGER != null) {
                Spectra.LOGGER.warn("Failed to load Minecraft {} translations", selected, exception);
            }
            minecraftTranslations = null;
        }
        minecraftLanguage = selected;
    }
}
