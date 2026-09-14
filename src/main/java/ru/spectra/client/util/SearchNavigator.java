package ru.spectra.client.util;
import ru.spectra.client.ui.AbstractFrame;
import ru.spectra.client.ui.MenuTabElement;
import ru.spectra.client.ui.setting.Setting;

public interface SearchNavigator {
    void focusFrame(AbstractFrame class757Var, MenuTabElement class732Var);

    void focusSetting(MenuTabElement class732Var, AbstractFrame class757Var, Setting class661Var);
}
