package com.pacman;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.util.ArrayList;
import java.util.List;

public class FruitManager {
    private final PacMan game;
    private final ImageLoader imageLoader;
    private final List<Fruit> fruits = new ArrayList<>();

    private Thread worker;
    private volatile boolean running = false;

    // fasi: 0 = wait primo spawn, 1 = wait disappear primo, 2 = wait secondo spawn, 3 = finito
    private int phase = 0;

    private long remainingDelay;    // ms rimanenti per la fase corrente
    private long lastPhaseStart;    // System.currentTimeMillis() all’inizio fase

    private static final int MAX_FRUITS_PER_LEVEL  = 2;
    private static final int FRUIT_VISIBLE_MS      = 8000;
    private static final int FIRST_DELAY_MS        = 20000;
    private static final int SECOND_DELAY_MS       = 20000;

    public FruitManager(PacMan game, ImageLoader loader) {
        this.game = game;
        this.imageLoader = loader;
        resetTimers();
    }

    private void resetTimers() {
        phase = 0;
        remainingDelay = FIRST_DELAY_MS;
        lastPhaseStart = 0;
    }

    public synchronized void startFruitTimer() {
        if (running) return;
        running = true;
        lastPhaseStart = System.currentTimeMillis();
        worker = new Thread(this::runLoop);
        worker.setDaemon(true);
        worker.start();
    }

    public synchronized void pauseFruitTimer() {
        if (!running) return;
        running = false;
        long elapsed = System.currentTimeMillis() - lastPhaseStart;
        remainingDelay = Math.max(0, remainingDelay - elapsed);
        worker.interrupt();
    }

    private void runLoop() {
        while (running && phase < 3) {
            if (phase == 0 || phase == 2) {
                try {
                    sleepWithPause(remainingDelay);
                } catch (InterruptedException e) {
                    continue; // sveglia o pausa
                }
                if (!running) break;
                spawnFruit();
                phase = (phase == 0) ? 1 : 3;
                lastPhaseStart = System.currentTimeMillis();
                remainingDelay = FRUIT_VISIBLE_MS;
            } else if (phase == 1) {
                long start = System.currentTimeMillis();
                while (running && !fruits.isEmpty() &&
                       System.currentTimeMillis() - start < FRUIT_VISIBLE_MS) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        break; // interrupt da collectFruit
                    }
                }
                if (!running) break;
                removeLastFruit();
                phase = 2;
                lastPhaseStart = System.currentTimeMillis();
                remainingDelay = SECOND_DELAY_MS;
            }
        }
        if (phase == 3 && running) {
            long start = System.currentTimeMillis();
            while (running && !fruits.isEmpty() &&
                   System.currentTimeMillis() - start < FRUIT_VISIBLE_MS) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {}
            }
            removeLastFruit();
        }
    }
    

    private void spawnFruit() {
        if (fruits.size() >= MAX_FRUITS_PER_LEVEL) return;
        int col = PacMan.COLUMN_COUNT / 2;
        int x = col * PacMan.TILE_SIZE;
        int y = game.getReadyRow() * PacMan.TILE_SIZE;
        int lvl = game.getCurrentLevel();
        FruitType type = FruitType.values()[(lvl - 1) % FruitType.values().length];
        fruits.add(new Fruit(x, y, type));
    }

    private void removeLastFruit() {
        if (!fruits.isEmpty()) {
            fruits.remove(fruits.size() - 1);
        }
    }

    public int collectFruit(Block pacman) {
        for (int i = 0; i < fruits.size(); i++) {
            Fruit f = fruits.get(i);
            if (f.getX() == pacman.x && f.getY() == pacman.y) {
                fruits.remove(i);
                // se siamo in fase1 (frutto mangiato entro 8s) salta subito a fase2
                if (phase == 1) {
                    phase = 2;
                    remainingDelay = SECOND_DELAY_MS;
                    lastPhaseStart = System.currentTimeMillis();
                    if (worker != null) {
                        worker.interrupt();
                    }
                }                
                return f.getType().getScore();
            }
        }
        return 0;
    }
    

    public void draw(GraphicsContext gc) {
        for (Fruit f : fruits) {
            Image img = switch (f.getType()) {
                case CHERRY     -> imageLoader.getCherryImage();
                case APPLE      -> imageLoader.getAppleImage();
                default         -> imageLoader.getStrawberryImage();
            };
            gc.drawImage(img, f.getX(), f.getY(), PacMan.TILE_SIZE, PacMan.TILE_SIZE);
        }
    }

    public synchronized void reset() {
        fruits.clear();
        if (running) {
            worker.interrupt();
            running = false;
        }
        resetTimers();
    }

    private static class Fruit {
        private final int x, y;
        private final FruitType type;
        Fruit(int x, int y, FruitType t) {
            this.x = x;
            this.y = y;
            this.type = t;
        }
        int getX() { return x; }
        int getY() { return y; }
        FruitType getType() { return type; }
    }

    public enum FruitType {
        CHERRY(200), APPLE(400), STRAWBERRY(800);
        private final int score;
        FruitType(int s) { score = s; }
        public int getScore() { return score; }
    }

    /**
     * Dorme per 'duration' ms, ma rispetta pauseFruitTimer():
     * se running diventa false interrompe il ciclo.
     */
    private void sleepWithPause(long duration) throws InterruptedException {
        long target = System.currentTimeMillis() + duration;
        while (running) {
            long remain = target - System.currentTimeMillis();
            if (remain <= 0) return;
            Thread.sleep(Math.min(remain, 200));
        }
    }
}
