package ru.spectra.client.util;

import ru.spectra.client.Spectra;
import ru.spectra.client.resource.ClasspathResource;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class WavSoundPlayer {
    private static final long FAILURE_LOG_INTERVAL_MILLIS = 30_000L;
    public static final WavSoundPlayer INSTANCE = new WavSoundPlayer();
    private final AtomicLong lastFailureLog = new AtomicLong();
    private final ExecutorService playbackExecutor = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "spectra-ui-sound");
        thread.setDaemon(true);
        return thread;
    });
    public volatile Clip currentClip;

    public void playSound(String str, float f, boolean z) {
        this.playbackExecutor.execute(() -> {
            try {
                Clip clip = AudioSystem.getClip();
                BufferedInputStream bufferedInputStream = new BufferedInputStream(
                        new ClasspathResource("sounds/" + str + ".wav").stream()
                );
                try {
                    AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bufferedInputStream);
                    try {
                        clip.open(audioInputStream);
                        setVolume(clip, f);
                        if (!z) {
                            clip.addLineListener(event -> {
                                if (event.getType() == LineEvent.Type.STOP) {
                                    clip.close();
                                    if (this.currentClip == clip) {
                                        this.currentClip = null;
                                    }
                                }
                            });
                        }
                        clip.start();
                        if (z) {
                            clip.loop(-1);
                        }
                        synchronized (this) {
                            try {
                                this.currentClip = clip;
                            } catch (Throwable th) {
                                throw th;
                            }
                        }
                        if (audioInputStream != null) {
                            audioInputStream.close();
                        }
                        bufferedInputStream.close();
                    } catch (Throwable th2) {
                        if (audioInputStream != null) {
                            try {
                                audioInputStream.close();
                            } catch (Throwable th3) {
                                th2.addSuppressed(th3);
                            }
                        }
                        throw th2;
                    }
                } catch (Throwable th4) {
                    try {
                        bufferedInputStream.close();
                    } catch (Throwable th5) {
                        th4.addSuppressed(th5);
                    }
                    throw th4;
                }
            } catch (UnsupportedAudioFileException | LineUnavailableException
                     | IOException | RuntimeException e) {
                reportFailure(str, e);
            }
        });
    }

    public synchronized void playSoundSequentially(List<String> list, float f, long j) {
        new Thread(() -> {
            Iterator it = list.iterator();
            while (it.hasNext()) {
                String str = (String) it.next();
                try {
                    Clip clip = AudioSystem.getClip();
                    BufferedInputStream bufferedInputStream = new BufferedInputStream(
                            new ClasspathResource("sounds/" + str + ".wav").stream()
                    );
                    try {
                        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bufferedInputStream);
                        try {
                            clip.open(audioInputStream);
                            setVolume(clip, f);
                            clip.start();
                            Thread.sleep(j);
                            clip.close();
                            if (audioInputStream != null) {
                                audioInputStream.close();
                            }
                            bufferedInputStream.close();
                        } catch (Throwable th) {
                            if (audioInputStream != null) {
                                try {
                                    audioInputStream.close();
                                } catch (Throwable th2) {
                                    th.addSuppressed(th2);
                                }
                            }
                            throw th;
                        }
                    } catch (Throwable th3) {
                        try {
                            bufferedInputStream.close();
                        } catch (Throwable th4) {
                            th3.addSuppressed(th4);
                        }
                        throw th3;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (UnsupportedAudioFileException | LineUnavailableException
                         | IOException | RuntimeException e) {
                    reportFailure(str, e);
                }
            }
        }).start();
    }

    public synchronized void stopSound() {
        if (this.currentClip != null) {
            this.currentClip.stop();
            this.currentClip.close();
            this.currentClip = null;
        }
    }

    public void setVolume(Clip clip, float f) {
        FloatControl control = (FloatControl) (clip.getControl(FloatControl.Type.MASTER_GAIN));
        float minimum = control.getMinimum();
        control.setValue(minimum + ((control.getMaximum() - minimum) * (f / 100.0f)));
    }

    private void reportFailure(String sound, Exception failure) {
        long now = System.currentTimeMillis();
        long previous = this.lastFailureLog.get();
        if (now - previous >= FAILURE_LOG_INTERVAL_MILLIS
                && this.lastFailureLog.compareAndSet(previous, now)) {
            Spectra.LOGGER.warn("Cannot play UI sound {}", sound, failure);
        }
    }
}
