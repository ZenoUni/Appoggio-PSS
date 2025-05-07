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

    private static final int MAX_FRUITS_PER_LEVEL      = 2;
    private static final int FRUIT_VISIBLE_TIME_MS     = 8000;
    private static final int FIRST_FRUIT_DELAY_MS      = 20000;
    private static final int SECOND_FRUIT_DELAY_MS     = 20000;

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

        fruitThread = new Thread(() -> {
            try {
                // Primo frutto
                Thread.sleep(FIRST_FRUIT_DELAY_MS);
                if (!running) return;
                spawnFruitAtFixedPoint();
                waitForFruitToBeEatenOrExpire();

                // Secondo frutto
                Thread.sleep(SECOND_FRUIT_DELAY_MS);
                if (!running) return;
                spawnFruitAtFixedPoint();
                waitForFruitToBeEatenOrExpire();
            } catch (InterruptedException ignored) { }
        });
        fruitThread.setDaemon(true);
        fruitThread.start();
    }

    public void pauseFruitTimer() {
        running = false;
        if (fruitThread != null) fruitThread.interrupt();
    }

    private void spawnFruitAtFixedPoint() {
        if (fruitsSpawnedThisLevel >= MAX_FRUITS_PER_LEVEL) return;

        int readyRow = game.getReadyRow();
        int col      = PacMan.COLUMN_COUNT / 2;
        int x        = col * PacMan.TILE_SIZE;
        int y        = readyRow * PacMan.TILE_SIZE;

        // Ciclo cherry→apple→strawberry
        int level = game.getCurrentLevel();
        FruitType type;
        switch ((level - 1) % 3) {
            case 0 -> type = FruitType.CHERRY;
            case 1 -> type = FruitType.APPLE;
            default -> type = FruitType.STRAWBERRY;
        }

        fruits.add(new Fruit(x, y, type));
        fruitsSpawnedThisLevel++;
    }

    private void waitForFruitToBeEatenOrExpire() throws InterruptedException {
        if (fruits.isEmpty()) return;
        Fruit active = fruits.get(fruits.size() - 1);
        long start    = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < FRUIT_VISIBLE_TIME_MS) {
            if (!fruits.contains(active)) return;  // mangiato
            Thread.sleep(200);
        }
        fruits.remove(active);  // scaduto
    }

    public int collectFruit(Block pacman) {
        for (int i = 0; i < fruits.size(); i++) {
            Fruit f = fruits.get(i);
            if (f.getX() == pacman.x && f.getY() == pacman.y) {
                fruits.remove(i);
                return f.getType().getScore();
            }
        }
        return 0;
    }

    public void draw(GraphicsContext gc) {
        for (Fruit fruit : fruits) {
            Image img = switch (fruit.getType()) {
                case CHERRY     -> imageLoader.getCherryImage();
                case APPLE      -> imageLoader.getAppleImage();
                case STRAWBERRY -> imageLoader.getStrawberryImage();
            };
            gc.drawImage(img, fruit.getX(), fruit.getY(), PacMan.TILE_SIZE, PacMan.TILE_SIZE);
        }
    }

    public void reset() {
        fruits.clear();
        pauseFruitTimer();
    }

    private static class Fruit {
        private final int x, y;
        private final FruitType type;

        Fruit(int x, int y, FruitType type) {
            this.x = x; this.y = y; this.type = type;
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
