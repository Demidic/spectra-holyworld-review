package ru.spectra.client.type;
import ru.spectra.client.model.DisplayNamed;
import ru.spectra.client.model.Translation;

public enum CameraMode implements DisplayNamed {
    ZOOM(Translation.clearText("Zoom")),
    PERSPECTIVE(Translation.clearText("Perspective"));

    final Translation displayName;

    CameraMode(Translation class254Var) {
        this.displayName = class254Var;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
