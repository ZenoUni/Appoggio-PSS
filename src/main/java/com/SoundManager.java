package com.pacman;
import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

public class SoundManager {
    private static final HashMap<String, Clip> soundClips = new HashMap<>();

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


    public static void playSound(String name) {
        Clip clip = soundClips.get(name);
        if (clip != null) {
            if (clip.isRunning()) {
                clip.stop();
            }
            clip.setFramePosition(0);
            clip.start();
        }
    }

    public static void loopSound(String name) {
        Clip clip = soundClips.get(name);
        if (clip != null) {
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    public static void stopSound(String name) {
        Clip clip = soundClips.get(name);
        if (clip != null && clip.isRunning()) {
            clip.stop();
        }
    }
}
