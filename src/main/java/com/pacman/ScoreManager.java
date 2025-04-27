package com.pacman;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.List;

public class ScoreManager {
    private final Font         scoreFont;
    private final ImageLoader  loader;

    public ScoreManager(Font scoreFont, ImageLoader loader) {
        this.scoreFont = scoreFont;
        this.loader    = loader;
    }

    public void drawScoreboard(GraphicsContext gc,
                               int lives,
                               List<Image> collectedFruits,
                               int score,
                               int level) {
        int tileSize   = PacMan.TILE_SIZE;
        int boardWidth = PacMan.BOARD_WIDTH;
        int boardHeight = PacMan.BOARD_HEIGHT;

        // Sfondo barra
        gc.setFill(Color.BLACK);
        gc.fillRect(0, boardHeight, boardWidth, tileSize);

        gc.setFill(Color.YELLOW);
        gc.setFont(scoreFont);

        // Vite (sinistra)
        Image lifeImg = loader.getPacmanRightImage();
        for (int i = 0; i < lives; i++) {
            gc.drawImage(
                lifeImg,
                tileSize * (i + 0.2),
                boardHeight + tileSize / 6.0,
                tileSize / 1.5,
                tileSize / 1.5
            );
        }

        // Frutta raccolta (centro)
        int fruitSize = tileSize / 2;
        int startX = boardWidth / 2 - (collectedFruits.size() * (fruitSize + 4)) / 2;
        for (int i = 0; i < collectedFruits.size(); i++) {
            gc.drawImage(
                collectedFruits.get(i),
                startX + i * (fruitSize + 4),
                boardHeight + tileSize / 6.0,
                fruitSize,
                fruitSize
            );
        }

        // Punteggio e livello (destra)
        String scoreText = String.format("SCORE %06d  LVL %02d", score, level);
        gc.fillText(scoreText, boardWidth - 310, boardHeight + tileSize / 1.5);
    }
}
