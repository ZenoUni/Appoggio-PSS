package com.pacman;
import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

public class SoundManager {
    private static final HashMap<String, Clip> soundClips = new HashMap<>();
    private static boolean muted = false;

    /** Carica un file audio in memoria sotto la chiave specificata. */
    public static void loadSound(String name, String resourcePath) {
        try {
            URL soundURL = SoundManager.class.getClassLoader().getResource(resourcePath);
            if (soundURL == null) {
                System.err.println("Impossibile trovare il file audio: " + resourcePath);
                return;
            }

            AudioInputStream audioInput = AudioSystem.getAudioInputStream(soundURL);
            Clip clip = AudioSystem.getClip();
            clip.open(audioInput);
            soundClips.put(name, clip);

            System.out.println("Suono caricato con successo: " + name);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Errore nel caricamento del suono: " + resourcePath);
            e.printStackTrace();
        }
    }

    /** Riproduce una singola istanza del suono indicato, riavviandolo se già in esecuzione. */
    public static void playSound(String name) {
        if (muted) return;
        Clip clip = soundClips.get(name);
        if (clip != null) {
            if (clip.isRunning()) {
                clip.stop();
            }
            clip.setFramePosition(0);
            clip.start();
        }
    }

    public static void muteAll() {
        muted = true;
        // Ferma i suoni in loop, se vuoi
        for (Clip clip : soundClips.values()) {
            if (clip.isRunning()) {
                clip.stop();
            }
        }
    }

    public static void unmuteAll() {
        muted = false;
        // Non far partire nessun suono automaticamente
    }

    /** Esegue in loop continuo il suono indicato finché non viene fermato. */
    public static void loopSound(String name) {
        if (muted) return;
        Clip clip = soundClips.get(name);
        if (clip != null) {
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    /** Interrompe la riproduzione in corso del suono indicato, se è in esecuzione. */
    public static void stopSound(String name) {
        Clip clip = soundClips.get(name);
        if (clip != null && clip.isRunning()) {
            clip.stop();
        }
    }

    /** Restituisce il Clip associato al nome, per poter aggiungere listener o controllarlo manualmente. */
    public static Clip getClip(String name) {
        return soundClips.get(name);
    }
}
