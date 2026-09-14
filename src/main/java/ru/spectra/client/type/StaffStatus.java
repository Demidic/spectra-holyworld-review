package ru.spectra.client.type;

public enum StaffStatus {
    PLAYING("Playing", 0xFF55FF55),
    SPEC("Spectating", 0xFFFFFF55),
    VANISH("Vanished", 0xFFFF5555);

    private final String status;
    private final int color;

    StaffStatus(String status, int color) {
        this.status = status;
        this.color = color;
    }

    public int getColor() {
        return this.color;
    }

    public String getStatus() {
        return this.status;
    }
}
