package com.pacman;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FruitManager {
    private final PacMan game;
    private final ImageLoader imageLoader;
    private final List<Fruit> fruits;
    private final Random random;

    private Thread fruitThread;
    private volatile boolean running = false;

    private int fruitsSpawnedThisLevel = 0;
    private boolean waitingForSecondFruit = false;

    private static final int MAX_FRUITS_PER_LEVEL = 2;
    private static final int FRUIT_VISIBLE_TIME_MS = 8000;
    private static final int FIRST_FRUIT_DELAY_MS = 20000;
    private static final int SECOND_FRUIT_DELAY_MS = 20000;

    public FruitManager(PacMan game, ImageLoader imageLoader) {
        this.game = game;
        this.imageLoader = imageLoader;
        this.fruits = new ArrayList<>();
        this.random = new Random();
    }

    public void startFruitTimer() {
        if (running) return;
        running = true;
        fruitsSpawnedThisLevel = 0;
        waitingForSecondFruit = false;

        fruitThread = new Thread(() -> {
            try {
                Thread.sleep(FIRST_FRUIT_DELAY_MS);
                if (!running) return;

                spawnFruitAtFixedPoint();

                // Aspetta che venga mangiata o scadano gli 8 secondi
                waitForFruitToBeEatenOrExpire();

                if (!running) return;

                Thread.sleep(SECOND_FRUIT_DELAY_MS);
                if (!running) return;

                spawnFruitAtFixedPoint();
                waitForFruitToBeEatenOrExpire();

            } catch (InterruptedException ignored) {
            }
        });
        fruitThread.setDaemon(true);
        fruitThread.start();
    }

    public void pauseFruitTimer() {
        running = false;
        if (fruitThread != null) {
            fruitThread.interrupt();
        }
    }

    private void spawnFruitAtFixedPoint() {
        if (fruitsSpawnedThisLevel >= MAX_FRUITS_PER_LEVEL) return;
        int readyRow = game.getReadyRow(); // ← Implementato in PacMan
        int col = PacMan.COLUMN_COUNT / 2;
        int x = col * PacMan.TILE_SIZE;
        int y = readyRow * PacMan.TILE_SIZE;
        FruitType type;
        int level = game.getCurrentLevel(); // ← Implementa questo in PacMan
        if (level == 1) {
            type = FruitType.CHERRY;
        } else if (level == 2) {
            type = FruitType.APPLE;
        } else if (level == 3) {
            type = FruitType.STRAWBERRY;
        } else {
            // Se superato il livello 3, smetti di generare frutta
            return;
        }
        fruits.add(new Fruit(x, y, type));
        fruitsSpawnedThisLevel++;
    }
    

    private void waitForFruitToBeEatenOrExpire() throws InterruptedException {
        final Fruit activeFruit = fruits.isEmpty() ? null : fruits.get(fruits.size() - 1);
        if (activeFruit == null) return;

        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < FRUIT_VISIBLE_TIME_MS) {
            if (!fruits.contains(activeFruit)) {
                // Frutto mangiato
                return;
            }
            Thread.sleep(200);
        }

        // Frutto non mangiato in tempo
        fruits.remove(activeFruit);
    }

    public int collectFruit(Block pacman) {
        for (int i = 0; i < fruits.size(); i++) {
            Fruit fruit = fruits.get(i);
            if (fruit.getX() == pacman.x && fruit.getY() == pacman.y) {
                fruits.remove(i);
                return fruit.getType().getScore();
            }
        }
        return 0;
    }

    public void draw(GraphicsContext gc) {
        for (Fruit fruit : fruits) {
            Image fruitImage = switch (fruit.getType()) {
                case CHERRY -> imageLoader.getCherryImage();
                case APPLE -> imageLoader.getAppleImage();
                case STRAWBERRY -> imageLoader.getStrawberryImage();
            };
            gc.drawImage(fruitImage, fruit.getX(), fruit.getY(), PacMan.TILE_SIZE, PacMan.TILE_SIZE);
        }
    }

    public void reset() {
        fruits.clear();
        pauseFruitTimer();
    }

    // Le classi interne rimangono invariate
    private static class Fruit {
        private final int x, y;
        private final FruitType type;

        Fruit(int x, int y, FruitType type) {
            this.x = x;
            this.y = y;
            this.type = type;
        }
        int getX() { return x; }
        int getY() { return y; }
        FruitType getType() { return type; }
    }

    public enum FruitType {
        CHERRY(200), APPLE(400), STRAWBERRY(800);
        private final int score;
        FruitType(int score) { this.score = score; }
        public int getScore() { return score; }
    }
}
