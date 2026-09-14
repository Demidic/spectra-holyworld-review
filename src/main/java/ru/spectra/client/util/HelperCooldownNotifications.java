package ru.spectra.client.util;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.text.Text;
import ru.spectra.client.Lang;
import ru.spectra.client.Spectra;

import java.util.List;
import java.util.concurrent.TimeUnit;

/** Passive cooldown transitions only: no item use, inventory access or keybinds. */
public final class HelperCooldownNotifications {
    private final List<Item> items;
    private final CooldownTransitionTracker<Item> transitions = new CooldownTransitionTracker<>();
    private ClientPlayerEntity observedPlayer;

    public HelperCooldownNotifications(List<Item> items) {
        this.items = List.copyOf(items);
    }

    public void update(ClientPlayerEntity player, boolean enabled) {
        if (!enabled || player == null || player != this.observedPlayer) {
            clear();
            this.observedPlayer = player;
            if (!enabled || player == null) {
                return;
            }
        }
        for (Item item : this.items) {
            boolean cooling = player.getItemCooldownManager().isCoolingDown(item.getDefaultStack());
            if (this.transitions.observe(item, cooling)) {
                Spectra.INSTANCE.notificationRepository().post(
                        Text.literal(Lang.FTHELPER_COOLDOWN_READY.effective()
                                .replace("{item}", item.getName().getString())),
                        item.getDefaultStack(), 4L, TimeUnit.SECONDS);
            }
        }
    }

    public void clear() {
        this.transitions.clear();
        this.observedPlayer = null;
    }
}
