package com.pacman;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class SoundManager {
    private static final Map<String, Clip> soundClips = new HashMap<>();
    private static final Map<String, BooleanControl> muteControls = new HashMap<>();
    private static final Map<String, FloatControl> volumeControls = new HashMap<>();

    /*Carica un file audio in memoria sotto la chiave specificata.*/
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

            // Se supporta BooleanControl.MUTE, usalo per silenziare completamente
            if (clip.isControlSupported(BooleanControl.Type.MUTE)) {
                BooleanControl bc = (BooleanControl) clip.getControl(BooleanControl.Type.MUTE);
                muteControls.put(name, bc);
                bc.setValue(false); // inizialmente non mutato
            }
            // Se supporta MASTER_GAIN, salvalo per fade o controllo volume
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl fc = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                volumeControls.put(name, fc);
                fc.setValue(0.0f); // default 0 dB
            }

            soundClips.put(name, clip);
            System.out.println("Suono caricato con successo: " + name);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Errore nel caricamento del suono: " + resourcePath);
            e.printStackTrace();
        }
    }

    /* Riproduce una singola istanza del suono indicato.*/
    public static void playSound(String name) {
        Clip clip = soundClips.get(name);
        if (clip == null) return;
        if (clip.isRunning()) clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }

    /*Mette in muto tutti i suoni, usando BooleanControl se disponibile o volume a minimo.*/
    public static void muteAll() {
        // Boolean mute
        for (BooleanControl bc : muteControls.values()) {
            bc.setValue(true);
        }
        // fallback volume
        for (FloatControl fc : volumeControls.values()) {
            fc.setValue(fc.getMinimum());
        }
    }

    /*Rimuove il muto, ripristinando BooleanControl e volume default.*/
    public static void unmuteAll() {
        for (BooleanControl bc : muteControls.values()) {
            bc.setValue(false);
        }
        for (FloatControl fc : volumeControls.values()) {
            fc.setValue(0.0f);
        }
    }

    /*Esegue in loop continuo il suono indicato finché non viene fermato.*/
    public static void loopSound(String name) {
        Clip clip = soundClips.get(name);
        if (clip == null) return;
        if (!clip.isRunning()) {
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    /*Interrompe la riproduzione in corso del suono indicato, se è in esecuzione.*/
    public static void stopSound(String name) {
        Clip clip = soundClips.get(name);
        if (clip != null && clip.isRunning()) {
            clip.stop();
        }
    }

    /*Restituisce il Clip associato al nome.*/
    public static Clip getClip(String name) {
        return soundClips.get(name);
    }

}
