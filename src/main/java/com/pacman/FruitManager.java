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

    public FruitManager(PacMan game, ImageLoader imageLoader) {
        this.game = game;
        this.imageLoader = imageLoader;
        this.fruits = new ArrayList<>();
        this.random = new Random();
    }

    public void startFruitTimer() {
        new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(10000); // Fa spawnare una frutta ogni 10 secondi
                    spawnFruit();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void spawnFruit() {
        int x = random.nextInt(PacMan.COLUMN_COUNT) * PacMan.TILE_SIZE;
        int y = random.nextInt(PacMan.ROW_COUNT) * PacMan.TILE_SIZE;
        FruitType type = FruitType.values()[random.nextInt(FruitType.values().length)];
        fruits.add(new Fruit(x, y, type));
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

    public void reset() {
        fruits.clear();
    }
}

class Fruit {
    private final int x, y;
    private final FruitType type;

    public Fruit(int x, int y, FruitType type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public FruitType getType() {
        return type;
    }
}

enum FruitType {
    CHERRY(100),
    APPLE(200),
    STRAWBERRY(300);

    private final int score;

    FruitType(int score) {
        this.score = score;
    }

    public int getScore() {
        return score;
    }
}
