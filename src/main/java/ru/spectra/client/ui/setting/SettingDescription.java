package ru.spectra.client.ui.setting;

import ru.spectra.client.Lang;
import ru.spectra.client.Spectra;
import ru.spectra.client.model.Translation;
import ru.spectra.client.type.Language;
import ru.spectra.client.util.ClientLocalization;
import ru.spectra.client.util.StringLookup;

import java.util.Locale;

/** Produces concise, semantic and language-aware descriptions for settings. */
public final class SettingDescription {
    private SettingDescription() {
    }

    public static Translation resolve(String moduleName, Setting setting) {
        Translation description = setting.getDescription();
        if (description != null && isUseful(description.effective())) {
            return description;
        }
        return new GeneratedDescription(moduleName, setting);
    }

    private static final class GeneratedDescription implements Translation {
        private final String moduleName;
        private final Setting setting;

        private GeneratedDescription(String moduleName, Setting setting) {
            this.moduleName = moduleName;
            this.setting = setting;
        }

        @Override
        public void lookupFromDictionary(StringLookup lookup) {
            // Resolved on every use so switching languages updates open cards.
        }

        @Override
        public String original() {
            return this.setting.getName().original() + ".generated.desc";
        }

        @Override
        public String effective() {
            String localized = localizedDescription();
            if (localized != null) {
                return localized;
            }
            String visibleName = cleanName(this.setting.getName().effective());
            String localizedModule = ClientLocalization.literal(this.moduleName);
            return currentLanguage() == Language.ru_RU
                    ? describeRussian(localizedModule, visibleName, this.setting)
                    : describeEnglish(localizedModule, visibleName, this.setting);
        }

        @Override
        public String firstLetterUppercase() {
            String value = effective();
            return value.isEmpty() ? value
                    : Character.toUpperCase(value.charAt(0)) + value.substring(1);
        }

        private String localizedDescription() {
            String key = this.setting.getName().original();
            if (key == null || key.isBlank() || key.indexOf(' ') >= 0) {
                return null;
            }
            StringLookup lookup = Lang.languageLookups.get(currentLanguage());
            if (lookup == null) {
                return null;
            }
            String value = lookup.lookup(key + ".desc");
            return isUseful(value) ? value : null;
        }
    }

    private static boolean isUseful(String value) {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) {
            return false;
        }
        String lower = value.trim().toLowerCase(Locale.ROOT);
        return !lower.equals("enable the feature.")
                && !lower.equals("changes this setting.")
                && !lower.equals("adjusts this setting.")
                && !lower.equals("sets this setting.")
                && !lower.equals("включает функцию.")
                && !lower.equals("включить функцию.")
                && !lower.equals("изменяет эту настройку.")
                && !lower.equals("настраивает эту настройку.")
                && !lower.equals("задаёт эту настройку.")
                && !lower.contains("р·р°")
                && !lower.contains("рїрѕ")
                && !lower.contains("сђр");
    }

    private static Language currentLanguage() {
        if (Spectra.INSTANCE != null && Spectra.INSTANCE.languages() != null
                && Spectra.INSTANCE.languages().current() != null) {
            return Spectra.INSTANCE.languages().current();
        }
        return Language.PRIMARY;
    }

    private static String cleanName(String value) {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) {
            return currentLanguage() == Language.ru_RU ? "этот параметр" : "this setting";
        }
        String result = value.trim().replaceAll("^[\"']|[\"']$", "")
                .replaceAll("(?i)\\s+button$", "").replaceAll("\\s+", " ");
        return result.isBlank() ? "this setting" : result;
    }

    private static String lowerFirst(String value) {
        return value.isEmpty() ? value
                : Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    private static String describeEnglish(String module, String visibleName, Setting setting) {
        String name = lowerFirst(visibleName);
        String lower = name.toLowerCase(Locale.ROOT);
        if (setting instanceof KeybindSetting) {
            return "Activates " + module + " when this key is pressed.";
        }
        if (setting instanceof ColorSetting) {
            return "Colors the " + name + " rendered by " + module + ".";
        }
        if (setting instanceof TextFieldSetting) {
            return "Provides the " + name + " used by " + module + ".";
        }
        if (setting instanceof MultiSelectSetting<?>) {
            return "Selects which objects " + module + " includes.";
        }
        if (setting instanceof ModeSetting<?> || setting instanceof EnumSetting<?>) {
            return "Selects how " + module + " performs its action.";
        }
        if (setting instanceof NumberSetting) {
            if (containsAny(lower, "delay", "interval")) {
                return "Controls how long " + module + " waits between actions.";
            }
            if (containsAny(lower, "duration", "lifetime", "time")) {
                return "Controls how long the " + name + " remains active or visible.";
            }
            if (containsAny(lower, "distance", "range")) {
                return "Limits how far " + module + " can detect or affect targets.";
            }
            if (containsAny(lower, "speed", "rate")) {
                return "Controls how quickly the " + name + " progresses.";
            }
            if (containsAny(lower, "opacity", "alpha", "transparency")) {
                return "Controls how visible the " + name + " is.";
            }
            if (lower.contains("brightness")) {
                return "Sets the light level applied by " + module + ".";
            }
            if (lower.contains("volume")) {
                return "Sets the playback volume for Spectra interface sounds.";
            }
            if (containsAny(lower, "size", "scale", "radius", "width", "height", "thickness", "gap")) {
                return "Controls the rendered size of the " + name + ".";
            }
            if (containsAny(lower, "amount", "count", "maximum", "minimum", "max ", "min ")) {
                return "Limits how many elements " + module + " creates or handles.";
            }
            return "Defines the " + name + " threshold used by " + module + ".";
        }
        if (setting instanceof BooleanSetting || setting instanceof ExpandableSetting) {
            if (lower.startsWith("show ") || lower.startsWith("display ") || lower.startsWith("render ")) {
                return "Shows " + name.substring(name.indexOf(' ') + 1) + ".";
            }
            if (lower.startsWith("hide ")) return "Hides " + name.substring(5) + ".";
            if (lower.startsWith("ignore ")) return "Excludes " + name.substring(7) + " from automatic handling.";
            if (lower.startsWith("block ")) return "Prevents " + name.substring(6) + " while " + module + " is active.";
            if (lower.startsWith("remove ")) return "Removes " + name.substring(7) + " from the rendered result.";
            if (containsAny(lower, "through walls", "through blocks")) return "Keeps the effect visible behind blocks.";
            if (lower.contains("outline")) return "Draws an outline around the rendered effect.";
            if (lower.contains("fill")) return "Fills the rendered shape inside its outline.";
            if (containsAny(lower, "animation", "smooth")) return "Animates changes smoothly instead of switching instantly.";
            if (lower.contains("client color")) return "Uses the current Spectra accent color.";
            if (containsAny(lower, "notification", "notify")) return "Shows a notification when " + module + " performs its action.";
            return "Includes " + name + " in the behavior of " + module + ".";
        }
        if (setting instanceof ButtonSetting) return "Runs the " + name + " action immediately.";
        return "Defines how " + module + " uses " + name + ".";
    }

    private static String describeRussian(String module, String visibleName, Setting setting) {
        String name = lowerFirst(visibleName);
        String lower = name.toLowerCase(Locale.ROOT);
        if (setting instanceof KeybindSetting) return "Включает «" + module + "» при нажатии этой клавиши.";
        if (setting instanceof ColorSetting) return "Задаёт цвет элемента «" + visibleName + "» в функции «" + module + "».";
        if (setting instanceof TextFieldSetting) return "Задаёт текст или значение «" + visibleName + "» для функции «" + module + "».";
        if (setting instanceof MultiSelectSetting<?>) return "Выбирает объекты, которые обрабатывает функция «" + module + "».";
        if (setting instanceof ModeSetting<?> || setting instanceof EnumSetting<?>) return "Выбирает способ работы функции «" + module + "».";
        if (setting instanceof NumberSetting) {
            if (containsAny(lower, "задерж", "интервал", "delay", "interval")) return "Определяет паузу между действиями функции «" + module + "».";
            if (containsAny(lower, "время", "длитель", "продолжитель", "lifetime", "duration")) return "Определяет, как долго элемент «" + visibleName + "» остаётся активным или видимым.";
            if (containsAny(lower, "дистанц", "дальность", "радиус", "distance", "range")) return "Ограничивает дальность обнаружения или действия функции «" + module + "».";
            if (containsAny(lower, "скорост", "частот", "speed", "rate")) return "Определяет скорость изменения элемента «" + visibleName + "».";
            if (containsAny(lower, "прозрач", "альфа", "opacity", "alpha")) return "Определяет видимость элемента «" + visibleName + "».";
            if (containsAny(lower, "яркост", "brightness")) return "Задаёт уровень освещения, применяемый функцией «" + module + "».";
            if (containsAny(lower, "громкост", "volume")) return "Задаёт громкость звуков интерфейса Spectra.";
            if (containsAny(lower, "размер", "масштаб", "ширин", "высот", "толщин", "отступ", "size", "scale")) return "Определяет отображаемый размер элемента «" + visibleName + "».";
            return "Задаёт порог «" + visibleName + "», используемый функцией «" + module + "».";
        }
        if (setting instanceof BooleanSetting || setting instanceof ExpandableSetting) {
            if (lower.startsWith("показы") || lower.startsWith("отображ") || lower.startsWith("show ")) return "Показывает элемент «" + visibleName + "» в интерфейсе или мире.";
            if (lower.startsWith("скры") || lower.startsWith("hide ")) return "Скрывает элемент «" + visibleName + "» из результата.";
            if (lower.startsWith("игнор") || lower.startsWith("ignore ")) return "Исключает «" + visibleName + "» из автоматической обработки.";
            if (lower.startsWith("блокир") || lower.startsWith("запрет") || lower.startsWith("block ")) return "Блокирует действие «" + visibleName + "», пока функция активна.";
            if (containsAny(lower, "обводк", "outline")) return "Рисует контур вокруг отображаемого объекта.";
            if (containsAny(lower, "заливк", "fill")) return "Заполняет отображаемую фигуру цветом внутри контура.";
            if (containsAny(lower, "анимац", "плавн", "animation", "smooth")) return "Плавно анимирует изменения вместо мгновенного переключения.";
            if (containsAny(lower, "цвет клиент", "client color")) return "Использует текущий акцентный цвет Spectra.";
            if (containsAny(lower, "уведом", "notification", "notify")) return "Показывает уведомление при срабатывании функции «" + module + "».";
            return "Добавляет «" + visibleName + "» в поведение функции «" + module + "».";
        }
        if (setting instanceof ButtonSetting) return "Сразу выполняет действие «" + visibleName + "».";
        return "Определяет, как функция «" + module + "» использует «" + visibleName + "».";
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) if (value.contains(needle)) return true;
        return false;
    }
}
