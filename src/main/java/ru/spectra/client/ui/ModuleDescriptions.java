package ru.spectra.client.ui;

import ru.spectra.client.util.ClientLocalization;
import java.util.Map;

/**
 * Compact one-line copy for module cards. Descriptions are intentionally
 * concise because the card header has a single secondary-text line.
 */
public final class ModuleDescriptions {
    private static final Map<String, String> DESCRIPTIONS = Map.ofEntries(
            Map.entry("Widgets", "Controls the Spectra HUD"),
            Map.entry("Watermark", "Shows client and session info"),
            Map.entry("Active Effects", "Shows active potion effects"),
            Map.entry("Keybinds", "Shows enabled bound functions"),
            Map.entry("Target HUD", "Shows information about a target"),
            Map.entry("Coordinates", "Shows your world position"),
            Map.entry("Notifications", "Shows client status messages"),
            Map.entry("Structures", "Shows active trap and plast timers"),
            Map.entry("Inventory", "Shows inventory contents"),
            Map.entry("Cooldowns", "Shows item cooldown timers"),
            Map.entry("Target ESP", "Highlights the selected target"),
            Map.entry("Self Name Tag", "Renders your own name tag"),
            Map.entry("Item Physic", "Adds realistic dropped-item physics"),
            Map.entry("Trails", "Draws movement trails"),
            Map.entry("Hit Effect", "Draws a surface wave when you hit"),
            Map.entry("Skeleton", "Shows visible player poses without rendering through blocks"),
            Map.entry("Shulker Preview", "Shows shulker contents when you hover over it"),
            Map.entry("HitBox Customizer", "Customizes visible hitboxes"),
            Map.entry("Item Radius", "Shows nearby item locations"),
            Map.entry("Kill Effect", "Plays an effect after a kill"),
            Map.entry("Block Outline", "Customizes selected-block outlines"),
            Map.entry("Shader Hand", "Applies shaders to first-person hands"),
            Map.entry("Removals", "Hides selected visual distractions"),
            Map.entry("Aspect Ratio", "Changes the rendered aspect ratio"),
            Map.entry("Full Bright", "Keeps dark areas clearly visible"),
            Map.entry("Pop Chams", "Shows a fading player snapshot"),
            Map.entry("Jump Circle", "Draws a circle when you land"),
            Map.entry("Chams", "Customizes visible entity rendering"),
            Map.entry("World Tweaks", "Adjusts time, fog and world visuals"),
            Map.entry("View Model", "Customizes first-person hand motion"),
            Map.entry("Crosshair", "Replaces the vanilla crosshair"),
            Map.entry("Zoom", "Smoothly magnifies the camera view"),
            Map.entry("Particles", "Customizes client-side particles"),
            Map.entry("Projectile Prediction", "Previews projectile trajectories"),
            Map.entry("ItemScroller", "Moves item stacks more quickly"),
            Map.entry("Death Coordinates", "Saves your last death position"),
            Map.entry("AutoRespawn", "Respawns automatically after death"),
            Map.entry("Auto Resell", "Repeats configured sale actions"),
            Map.entry("Auto Eat", "Eats food already in the hotbar; never moves inventory items"),
            Map.entry("Free Look", "Lets the camera rotate independently"),
            Map.entry("TP Accept", "Accepts allowed teleport requests"),
            Map.entry("Better Chat", "Improves chat appearance and timing"),
            Map.entry("Name Protect", "Replaces protected player names"),
            Map.entry("Tape Mouse", "Repeats a configured mouse action"),
            Map.entry("Elytra Helper", "Automates common elytra actions"),
            Map.entry("FT Helper", "Shows FunTime structure timers, event waypoints and finished cooldowns"),
            Map.entry("RW Helper", "Filters restricted chat messages and closes the join menu"),
            Map.entry("HW Helper", "Shows HolyWorld structure timers and finished cooldowns"),
            Map.entry("Sounds", "Plays Spectra interface sounds"),
            Map.entry("RW Grief Joiner", "Helps connect to grief servers"),
            Map.entry("Hit Sounds", "Plays a sound when you hit"),
            Map.entry("Clan Invest", "Schedules clan investments"),
            Map.entry("Mine Helper", "Shows the next mine reset"),
            Map.entry("PvP Safe", "Disables selected features for safety"),
            Map.entry("Shift Tap", "Performs a controlled sneak tap"),
            Map.entry("Lock Slot", "Protects a selected inventory slot"),
            Map.entry("Auto Swap", "Swaps configured items by context"),
            Map.entry("Auto Sprint", "Starts sprinting while you move"),
            Map.entry("Change Hand", "Switches the player's preferred hand")
    );

    private static final Map<String, String> RU_DESCRIPTIONS = Map.ofEntries(
            Map.entry("Widgets", "\u0423\u043F\u0440\u0430\u0432\u043B\u044F\u0435\u0442 \u044D\u043B\u0435\u043C\u0435\u043D\u0442\u0430\u043C\u0438 HUD Spectra"),
            Map.entry("Watermark", "\u041F\u043E\u043A\u0430\u0437\u044B\u0432\u0430\u0435\u0442 \u0438\u043D\u0444\u043E\u0440\u043C\u0430\u0446\u0438\u044E \u043E \u043A\u043B\u0438\u0435\u043D\u0442\u0435 \u0438 \u0441\u0435\u0441\u0441\u0438\u0438"),
            Map.entry("Active Effects", "\u041F\u043E\u043A\u0430\u0437\u044B\u0432\u0430\u0435\u0442 \u0430\u043A\u0442\u0438\u0432\u043D\u044B\u0435 \u044D\u0444\u0444\u0435\u043A\u0442\u044B \u0437\u0435\u043B\u0438\u0439"),
            Map.entry("Keybinds", "\u041F\u043E\u043A\u0430\u0437\u044B\u0432\u0430\u0435\u0442 \u0432\u043A\u043B\u044E\u0447\u0451\u043D\u043D\u044B\u0435 \u0444\u0443\u043D\u043A\u0446\u0438\u0438 \u0441 \u0431\u0438\u043D\u0434\u0430\u043C\u0438"),
            Map.entry("Target HUD", "\u041F\u043E\u043A\u0430\u0437\u044B\u0432\u0430\u0435\u0442 \u0438\u043D\u0444\u043E\u0440\u043C\u0430\u0446\u0438\u044E \u043E \u0446\u0435\u043B\u0438"),
            Map.entry("Coordinates", "\u041F\u043E\u043A\u0430\u0437\u044B\u0432\u0430\u0435\u0442 \u043A\u043E\u043E\u0440\u0434\u0438\u043D\u0430\u0442\u044B \u0432 \u043C\u0438\u0440\u0435"),
            Map.entry("Notifications", "\u041F\u043E\u043A\u0430\u0437\u044B\u0432\u0430\u0435\u0442 \u0443\u0432\u0435\u0434\u043E\u043C\u043B\u0435\u043D\u0438\u044F \u043A\u043B\u0438\u0435\u043D\u0442\u0430"),
            Map.entry("Structures", "\u041F\u043E\u043A\u0430\u0437\u044B\u0432\u0430\u0435\u0442 \u0442\u0430\u0439\u043C\u0435\u0440\u044B \u0442\u0440\u0430\u043F\u043E\u043A \u0438 \u043F\u043B\u0430\u0441\u0442\u043E\u0432"),
            Map.entry("Inventory", "\u041F\u043E\u043A\u0430\u0437\u044B\u0432\u0430\u0435\u0442 \u0441\u043E\u0434\u0435\u0440\u0436\u0438\u043C\u043E\u0435 \u0438\u043D\u0432\u0435\u043D\u0442\u0430\u0440\u044F"),
            Map.entry("Cooldowns", "\u041F\u043E\u043A\u0430\u0437\u044B\u0432\u0430\u0435\u0442 \u0442\u0430\u0439\u043C\u0435\u0440\u044B \u043F\u0435\u0440\u0435\u0437\u0430\u0440\u044F\u0434\u043A\u0438 \u043F\u0440\u0435\u0434\u043C\u0435\u0442\u043E\u0432"),
            Map.entry("Target ESP", "\u041F\u043E\u0434\u0441\u0432\u0435\u0447\u0438\u0432\u0430\u0435\u0442 \u0432\u044B\u0431\u0440\u0430\u043D\u043D\u0443\u044E \u0446\u0435\u043B\u044C"),
            Map.entry("Self Name Tag", "\u041E\u0442\u043E\u0431\u0440\u0430\u0436\u0430\u0435\u0442 \u0441\u043E\u0431\u0441\u0442\u0432\u0435\u043D\u043D\u044B\u0439 \u043D\u0435\u0439\u043C\u0442\u0435\u0433"),
            Map.entry("Item Physic", "\u0414\u043E\u0431\u0430\u0432\u043B\u044F\u0435\u0442 \u0444\u0438\u0437\u0438\u043A\u0443 \u0432\u044B\u0431\u0440\u043E\u0448\u0435\u043D\u043D\u044B\u043C \u043F\u0440\u0435\u0434\u043C\u0435\u0442\u0430\u043C"),
            Map.entry("Trails", "\u0420\u0438\u0441\u0443\u0435\u0442 \u0441\u043B\u0435\u0434\u044B \u0434\u0432\u0438\u0436\u0435\u043D\u0438\u044F"),
            Map.entry("Hit Effect", "\u0420\u0438\u0441\u0443\u0435\u0442 \u0432\u043E\u043B\u043D\u0443 \u043D\u0430 \u043F\u043E\u0432\u0435\u0440\u0445\u043D\u043E\u0441\u0442\u0438 \u043F\u0440\u0438 \u0443\u0434\u0430\u0440\u0435"),
            Map.entry("Skeleton", "Показывает позы видимых игроков без отображения сквозь блоки"),
            Map.entry("Shulker Preview", "\u041F\u043E\u043A\u0430\u0437\u044B\u0432\u0430\u0435\u0442 \u0441\u043E\u0434\u0435\u0440\u0436\u0438\u043C\u043E\u0435 \u0448\u0430\u043B\u043A\u0435\u0440\u0430 \u043F\u0440\u0438 \u043D\u0430\u0432\u0435\u0434\u0435\u043D\u0438\u0438"),
            Map.entry("HitBox Customizer", "\u041D\u0430\u0441\u0442\u0440\u0430\u0438\u0432\u0430\u0435\u0442 \u043E\u0442\u043E\u0431\u0440\u0430\u0436\u0435\u043D\u0438\u0435 \u0445\u0438\u0442\u0431\u043E\u043A\u0441\u043E\u0432"),
            Map.entry("Item Radius", "\u041F\u043E\u043A\u0430\u0437\u044B\u0432\u0430\u0435\u0442 \u0440\u0430\u0441\u043F\u043E\u043B\u043E\u0436\u0435\u043D\u0438\u0435 \u0431\u043B\u0438\u0436\u0430\u0439\u0448\u0438\u0445 \u043F\u0440\u0435\u0434\u043C\u0435\u0442\u043E\u0432"),
            Map.entry("Kill Effect", "\u0412\u043E\u0441\u043F\u0440\u043E\u0438\u0437\u0432\u043E\u0434\u0438\u0442 \u044D\u0444\u0444\u0435\u043A\u0442 \u043F\u043E\u0441\u043B\u0435 \u0443\u0431\u0438\u0439\u0441\u0442\u0432\u0430"),
            Map.entry("Block Outline", "\u041D\u0430\u0441\u0442\u0440\u0430\u0438\u0432\u0430\u0435\u0442 \u043E\u0431\u0432\u043E\u0434\u043A\u0443 \u0432\u044B\u0431\u0440\u0430\u043D\u043D\u043E\u0433\u043E \u0431\u043B\u043E\u043A\u0430"),
            Map.entry("Shader Hand", "\u041F\u0440\u0438\u043C\u0435\u043D\u044F\u0435\u0442 \u0448\u0435\u0439\u0434\u0435\u0440\u044B \u043A \u0440\u0443\u043A\u0430\u043C \u043E\u0442 \u043F\u0435\u0440\u0432\u043E\u0433\u043E \u043B\u0438\u0446\u0430"),
            Map.entry("Removals", "\u0421\u043A\u0440\u044B\u0432\u0430\u0435\u0442 \u0432\u044B\u0431\u0440\u0430\u043D\u043D\u044B\u0435 \u0432\u0438\u0437\u0443\u0430\u043B\u044C\u043D\u044B\u0435 \u043F\u043E\u043C\u0435\u0445\u0438"),
            Map.entry("Aspect Ratio", "\u041C\u0435\u043D\u044F\u0435\u0442 \u0441\u043E\u043E\u0442\u043D\u043E\u0448\u0435\u043D\u0438\u0435 \u0441\u0442\u043E\u0440\u043E\u043D \u0438\u0437\u043E\u0431\u0440\u0430\u0436\u0435\u043D\u0438\u044F"),
            Map.entry("Full Bright", "\u0414\u0435\u043B\u0430\u0435\u0442 \u0442\u0451\u043C\u043D\u044B\u0435 \u043E\u0431\u043B\u0430\u0441\u0442\u0438 \u0445\u043E\u0440\u043E\u0448\u043E \u0432\u0438\u0434\u0438\u043C\u044B\u043C\u0438"),
            Map.entry("Pop Chams", "\u041F\u043E\u043A\u0430\u0437\u044B\u0432\u0430\u0435\u0442 \u0438\u0441\u0447\u0435\u0437\u0430\u044E\u0449\u0438\u0439 \u0441\u043D\u0438\u043C\u043E\u043A \u0438\u0433\u0440\u043E\u043A\u0430"),
            Map.entry("Jump Circle", "\u0420\u0438\u0441\u0443\u0435\u0442 \u043A\u0440\u0443\u0433 \u043F\u0440\u0438 \u043F\u0440\u0438\u0437\u0435\u043C\u043B\u0435\u043D\u0438\u0438"),
            Map.entry("Chams", "\u041D\u0430\u0441\u0442\u0440\u0430\u0438\u0432\u0430\u0435\u0442 \u043E\u0442\u043E\u0431\u0440\u0430\u0436\u0435\u043D\u0438\u0435 \u0441\u0443\u0449\u043D\u043E\u0441\u0442\u0435\u0439"),
            Map.entry("World Tweaks", "\u041D\u0430\u0441\u0442\u0440\u0430\u0438\u0432\u0430\u0435\u0442 \u0432\u0440\u0435\u043C\u044F, \u0442\u0443\u043C\u0430\u043D \u0438 \u0432\u0438\u0434 \u043C\u0438\u0440\u0430"),
            Map.entry("View Model", "\u041D\u0430\u0441\u0442\u0440\u0430\u0438\u0432\u0430\u0435\u0442 \u0434\u0432\u0438\u0436\u0435\u043D\u0438\u0435 \u0440\u0443\u043A\u0438 \u043E\u0442 \u043F\u0435\u0440\u0432\u043E\u0433\u043E \u043B\u0438\u0446\u0430"),
            Map.entry("Crosshair", "\u0417\u0430\u043C\u0435\u043D\u044F\u0435\u0442 \u0441\u0442\u0430\u043D\u0434\u0430\u0440\u0442\u043D\u044B\u0439 \u043F\u0440\u0438\u0446\u0435\u043B"),
            Map.entry("Zoom", "\u041F\u043B\u0430\u0432\u043D\u043E \u043F\u0440\u0438\u0431\u043B\u0438\u0436\u0430\u0435\u0442 \u043A\u0430\u043C\u0435\u0440\u0443"),
            Map.entry("Particles", "\u041D\u0430\u0441\u0442\u0440\u0430\u0438\u0432\u0430\u0435\u0442 \u043A\u043B\u0438\u0435\u043D\u0442\u0441\u043A\u0438\u0435 \u0447\u0430\u0441\u0442\u0438\u0446\u044B"),
            Map.entry("Projectile Prediction", "\u041F\u043E\u043A\u0430\u0437\u044B\u0432\u0430\u0435\u0442 \u0442\u0440\u0430\u0435\u043A\u0442\u043E\u0440\u0438\u0438 \u0441\u043D\u0430\u0440\u044F\u0434\u043E\u0432"),
            Map.entry("ItemScroller", "\u0411\u044B\u0441\u0442\u0440\u0435\u0435 \u043F\u0435\u0440\u0435\u043C\u0435\u0449\u0430\u0435\u0442 \u0441\u0442\u0430\u043A\u0438 \u043F\u0440\u0435\u0434\u043C\u0435\u0442\u043E\u0432"),
            Map.entry("Death Coordinates", "\u0421\u043E\u0445\u0440\u0430\u043D\u044F\u0435\u0442 \u043A\u043E\u043E\u0440\u0434\u0438\u043D\u0430\u0442\u044B \u043F\u043E\u0441\u043B\u0435\u0434\u043D\u0435\u0439 \u0441\u043C\u0435\u0440\u0442\u0438"),
            Map.entry("AutoRespawn", "\u0410\u0432\u0442\u043E\u043C\u0430\u0442\u0438\u0447\u0435\u0441\u043A\u0438 \u0432\u043E\u0437\u0440\u043E\u0436\u0434\u0430\u0435\u0442 \u043F\u043E\u0441\u043B\u0435 \u0441\u043C\u0435\u0440\u0442\u0438"),
            Map.entry("Auto Resell", "\u041F\u043E\u0432\u0442\u043E\u0440\u044F\u0435\u0442 \u043D\u0430\u0441\u0442\u0440\u043E\u0435\u043D\u043D\u044B\u0435 \u0434\u0435\u0439\u0441\u0442\u0432\u0438\u044F \u043F\u0440\u043E\u0434\u0430\u0436\u0438"),
            Map.entry("Auto Eat", "Ест только еду из хотбара, не перемещая предметы из инвентаря"),
            Map.entry("Free Look", "\u041F\u043E\u0437\u0432\u043E\u043B\u044F\u0435\u0442 \u0432\u0440\u0430\u0449\u0430\u0442\u044C \u043A\u0430\u043C\u0435\u0440\u0443 \u043D\u0435\u0437\u0430\u0432\u0438\u0441\u0438\u043C\u043E"),
            Map.entry("TP Accept", "\u041F\u0440\u0438\u043D\u0438\u043C\u0430\u0435\u0442 \u0440\u0430\u0437\u0440\u0435\u0448\u0451\u043D\u043D\u044B\u0435 \u0437\u0430\u043F\u0440\u043E\u0441\u044B \u0442\u0435\u043B\u0435\u043F\u043E\u0440\u0442\u0430\u0446\u0438\u0438"),
            Map.entry("Better Chat", "\u0423\u043B\u0443\u0447\u0448\u0430\u0435\u0442 \u0432\u043D\u0435\u0448\u043D\u0438\u0439 \u0432\u0438\u0434 \u0438 \u0440\u0430\u0431\u043E\u0442\u0443 \u0447\u0430\u0442\u0430"),
            Map.entry("Name Protect", "\u0417\u0430\u043C\u0435\u043D\u044F\u0435\u0442 \u0437\u0430\u0449\u0438\u0449\u0430\u0435\u043C\u044B\u0435 \u0438\u043C\u0435\u043D\u0430 \u0438\u0433\u0440\u043E\u043A\u043E\u0432"),
            Map.entry("Tape Mouse", "\u041F\u043E\u0432\u0442\u043E\u0440\u044F\u0435\u0442 \u043D\u0430\u0441\u0442\u0440\u043E\u0435\u043D\u043D\u043E\u0435 \u0434\u0435\u0439\u0441\u0442\u0432\u0438\u0435 \u043C\u044B\u0448\u0438"),
            Map.entry("Elytra Helper", "\u0410\u0432\u0442\u043E\u043C\u0430\u0442\u0438\u0437\u0438\u0440\u0443\u0435\u0442 \u0434\u0435\u0439\u0441\u0442\u0432\u0438\u044F \u0441 \u044D\u043B\u0438\u0442\u0440\u0430\u043C\u0438"),
            Map.entry("FT Helper", "Таймеры структур, точки событий и уведомления о перезарядке FunTime"),
            Map.entry("RW Helper", "Фильтр сообщений и закрытие меню при входе на ReallyWorld"),
            Map.entry("HW Helper", "Таймеры структур HolyWorld и уведомления о перезарядке"),
            Map.entry("Sounds", "\u0412\u043E\u0441\u043F\u0440\u043E\u0438\u0437\u0432\u043E\u0434\u0438\u0442 \u0437\u0432\u0443\u043A\u0438 \u0438\u043D\u0442\u0435\u0440\u0444\u0435\u0439\u0441\u0430 Spectra"),
            Map.entry("RW Grief Joiner", "\u041F\u043E\u043C\u043E\u0433\u0430\u0435\u0442 \u043F\u043E\u0434\u043A\u043B\u044E\u0447\u0430\u0442\u044C\u0441\u044F \u043A \u0433\u0440\u0438\u0444-\u0441\u0435\u0440\u0432\u0435\u0440\u0430\u043C"),
            Map.entry("Hit Sounds", "\u0412\u043E\u0441\u043F\u0440\u043E\u0438\u0437\u0432\u043E\u0434\u0438\u0442 \u0437\u0432\u0443\u043A \u043F\u0440\u0438 \u043F\u043E\u043F\u0430\u0434\u0430\u043D\u0438\u0438"),
            Map.entry("Clan Invest", "\u0410\u0432\u0442\u043E\u043C\u0430\u0442\u0438\u0437\u0438\u0440\u0443\u0435\u0442 \u0438\u043D\u0432\u0435\u0441\u0442\u0438\u0446\u0438\u0438 \u0432 \u043A\u043B\u0430\u043D"),
            Map.entry("Mine Helper", "\u041F\u043E\u043A\u0430\u0437\u044B\u0432\u0430\u0435\u0442 \u0432\u0440\u0435\u043C\u044F \u0434\u043E \u043E\u0431\u043D\u043E\u0432\u043B\u0435\u043D\u0438\u044F \u0448\u0430\u0445\u0442\u044B"),
            Map.entry("PvP Safe", "\u041E\u0442\u043A\u043B\u044E\u0447\u0430\u0435\u0442 \u0432\u044B\u0431\u0440\u0430\u043D\u043D\u044B\u0435 \u0444\u0443\u043D\u043A\u0446\u0438\u0438 \u0434\u043B\u044F \u0431\u0435\u0437\u043E\u043F\u0430\u0441\u043D\u043E\u0441\u0442\u0438"),
            Map.entry("Shift Tap", "\u0412\u044B\u043F\u043E\u043B\u043D\u044F\u0435\u0442 \u043A\u043E\u043D\u0442\u0440\u043E\u043B\u0438\u0440\u0443\u0435\u043C\u043E\u0435 \u043D\u0430\u0436\u0430\u0442\u0438\u0435 \u043F\u0440\u0438\u0441\u0435\u0434\u0430"),
            Map.entry("Lock Slot", "\u0417\u0430\u0449\u0438\u0449\u0430\u0435\u0442 \u0432\u044B\u0431\u0440\u0430\u043D\u043D\u044B\u0439 \u0441\u043B\u043E\u0442 \u0438\u043D\u0432\u0435\u043D\u0442\u0430\u0440\u044F"),
            Map.entry("Auto Swap", "\u041C\u0435\u043D\u044F\u0435\u0442 \u043D\u0430\u0441\u0442\u0440\u043E\u0435\u043D\u043D\u044B\u0435 \u043F\u0440\u0435\u0434\u043C\u0435\u0442\u044B \u043F\u043E \u0441\u0438\u0442\u0443\u0430\u0446\u0438\u0438"),
            Map.entry("Auto Sprint", "Включает бег во время движения"),
            Map.entry("Change Hand", "Переключает ведущую руку игрока")
    );

    private ModuleDescriptions() {
    }

    public static String forModule(String name) {
        if (ClientLocalization.isRussian()) {
            return RU_DESCRIPTIONS.getOrDefault(name, "\u041D\u0430\u0441\u0442\u0440\u0430\u0438\u0432\u0430\u0435\u0442 \u0444\u0443\u043D\u043A\u0446\u0438\u044E \u043A\u043B\u0438\u0435\u043D\u0442\u0430 Spectra");
        }
        return DESCRIPTIONS.getOrDefault(name, "Customizes a Spectra client feature");
    }
}
