package com.pacman;

import javafx.animation.PauseTransition;
import javafx.scene.canvas.GraphicsContext;
import javafx.util.Duration;

import java.util.Random;

public class CherryManager {
    private final PacMan game;
    private final ImageLoader loader;
    private final Random random = new Random();
    private Block cherry;
    private PauseTransition cherryTimer;

    public CherryManager(PacMan game, ImageLoader loader) {
        this.game = game;
        this.loader = loader;
    }

    public void startCherryTimer() {
        cherryTimer = new PauseTransition(Duration.seconds(20));
        cherryTimer.setOnFinished(e -> spawnCherry());
        cherryTimer.play();
    }

    private void spawnCherry() {
        int x, y;
        do {
            x = random.nextInt(PacMan.COLUMN_COUNT) * PacMan.TILE_SIZE;
            y = random.nextInt(PacMan.ROW_COUNT) * PacMan.TILE_SIZE;
        } while (game.gameMap.isWall(x, y));

        cherry = new Block(loader.getCherryImage(), x, y);

        PauseTransition hide = new PauseTransition(Duration.seconds(10));
        hide.setOnFinished(e -> cherry = null);
        hide.play();

        cherryTimer.playFromStart();
    }

    public void draw(GraphicsContext gc) {
        if (cherry != null) {
            gc.drawImage(cherry.image, cherry.x, cherry.y, PacMan.TILE_SIZE, PacMan.TILE_SIZE);
        }
    }

    public int collectCherry(Block pacman) {
        if (cherry == null) return 0;

        // collision basic AABB
        if (pacman.x < cherry.x + cherry.width &&
            pacman.x + pacman.width > cherry.x &&
            pacman.y < cherry.y + cherry.height &&
            pacman.y + pacman.height > cherry.y) {
            // raccolto!
            collectedFruits().add(cherry.image);
            cherry = null;
            return 400;
        }
        return 0;
    }

    // helper: esponi la lista interna dei frutti (collectedFruits è in GameMap)
    private java.util.List<javafx.scene.image.Image> collectedFruits() {
        return game.gameMap.getCollectedFruits();
    }
}
