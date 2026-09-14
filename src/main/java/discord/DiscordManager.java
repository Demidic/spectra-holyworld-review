package discord;

import ru.spectra.client.Spectra;
import ru.spectra.client.net.UserSession;
import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.DiscordBuild;
import com.jagrosh.discordipc.entities.RichPresence;
import com.jagrosh.discordipc.entities.pipe.PipeStatus;
import com.jagrosh.discordipc.exceptions.NoDiscordClientException;
import net.minecraft.client.MinecraftClient;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Discord IPC used by the old Spectra client, kept as a self-contained local
 * implementation so buttons and animated external assets are preserved.
 */
public final class DiscordManager {
    private static final long APPLICATION_ID = 1461762274934128724L;
    private static final String WEBSITE = "https://spectravisuals.su";
    private static final String TELEGRAM = "https://t.me/spectravisuals";
    private static final String ANIMATED_IMAGE =
            "https://res.cloudinary.com/dhphlnntc/image/upload/v1779546314/spec_htjr8f.gif";

    private volatile IPCClient client;
    private volatile Thread worker;
    private volatile boolean running;
    private volatile Instant startedAt;

    public synchronized void init() {
        if (running) {
            return;
        }
        running = true;
        startedAt = Instant.now();
        worker = new Thread(this::runLoop, "spectra-discord-rpc");
        worker.setDaemon(true);
        worker.start();
    }

    public synchronized void stopRPC() {
        running = false;
        Thread currentWorker = worker;
        worker = null;
        if (currentWorker != null) {
            currentWorker.interrupt();
        }
        closeClient();
    }

    private void runLoop() {
        while (running) {
            try {
                ensureConnected();
                IPCClient current = client;
                if (current != null && current.getStatus() == PipeStatus.CONNECTED) {
                    current.sendRichPresence(buildPresence());
                }
                Thread.sleep(4_000L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return;
            } catch (Throwable ignored) {
                closeClient();
                try {
                    Thread.sleep(5_000L);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void ensureConnected() {
        IPCClient current = client;
        if (current != null && current.getStatus() == PipeStatus.CONNECTED) {
            return;
        }
        closeClient();
        IPCClient replacement = new IPCClient(APPLICATION_ID);
        replacement.setListener(new IPCListener() {
            @Override
            public void onDisconnect(IPCClient ipcClient, Throwable throwable) {
                if (client == ipcClient) {
                    client = null;
                }
            }
        });
        try {
            replacement.connect(DiscordBuild.ANY);
            client = replacement;
        } catch (NoDiscordClientException ignored) {
            client = null;
        }
    }

    private RichPresence buildPresence() {
        UserSession session = Spectra.INSTANCE.userSession;
        String role = safe(session == null ? null : session.role(), "Default");
        String uid = safe(session == null ? null : session.uid(), "-1");
        int online = onlinePlayers();
        int capacity = Math.max(online, 100);
        return new RichPresence.Builder()
                .setName("Spectra Visuals | 1.21.4")
                .setType(0)
                .setDetails("Role: " + role)
                .setState("UID: " + uid)
                .setStartTimestamp(OffsetDateTime.ofInstant(
                        startedAt == null ? Instant.now() : startedAt, ZoneOffset.UTC))
                .setLargeImage(ANIMATED_IMAGE, "Spectra Visuals · 1.21.4")
                .setParty("spectra-" + uid, online, capacity)
                .addButton("Website", WEBSITE)
                .addButton("Telegram", TELEGRAM)
                .build();
    }

    private static int onlinePlayers() {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        return minecraft.getNetworkHandler() == null
                ? 0
                : minecraft.getNetworkHandler().getPlayerList().size();
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private synchronized void closeClient() {
        IPCClient current = client;
        client = null;
        if (current != null) {
            try {
                current.close();
            } catch (Throwable ignored) {
            }
        }
    }
}
