package ru.spectra.client.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class CooldownTransitionTrackerTest {
    @Test
    void idleItemsDoNotProduceNotifications() {
        CooldownTransitionTracker<String> tracker = new CooldownTransitionTracker<>();
        assertFalse(tracker.observe("stun", false));
        assertFalse(tracker.observe("stun", false));
    }

    @Test
    void finishedCooldownProducesExactlyOneNotification() {
        CooldownTransitionTracker<String> tracker = new CooldownTransitionTracker<>();
        assertFalse(tracker.observe("stun", true));
        assertFalse(tracker.observe("stun", true));
        assertTrue(tracker.observe("stun", false));
        assertFalse(tracker.observe("stun", false));
        assertFalse(tracker.observe("stun", true));
        assertTrue(tracker.observe("stun", false));
    }

    @Test
    void itemsAreTrackedIndependently() {
        CooldownTransitionTracker<String> tracker = new CooldownTransitionTracker<>();
        tracker.observe("trap", true);
        assertFalse(tracker.observe("stun", false));
        assertTrue(tracker.observe("trap", false));
    }

    @Test
    void disableOrReconnectClearsOldCooldowns() {
        CooldownTransitionTracker<String> tracker = new CooldownTransitionTracker<>();
        tracker.observe("trap", true);
        tracker.clear();
        assertFalse(tracker.observe("trap", false));
    }
}
