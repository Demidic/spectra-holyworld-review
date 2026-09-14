package ru.spectra.client.ui.setting;

import ru.spectra.client.Spectra;
import ru.spectra.client.model.Translation;
import ru.spectra.client.ui.PopupContent;
import ru.spectra.client.util.ClientLocalization;

import java.util.Objects;
import java.util.function.Supplier;

/** Action setting that opens arbitrary content in the shared menu popup. */
public final class PopupSetting extends ButtonSetting {
    private final Supplier<? extends PopupContent> contentSupplier;

    public PopupSetting(Translation name, Translation description,
                        Supplier<? extends PopupContent> contentSupplier) {
        super(name, description);
        this.contentSupplier = Objects.requireNonNull(contentSupplier, "contentSupplier");
        setButtonName(Translation.clearText(ClientLocalization.text("Open", "Открыть")));
        setRunnable(() -> Spectra.INSTANCE.menuWindow().popupWindow()
                .open(this.contentSupplier.get()));
    }
}
