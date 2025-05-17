package com.pacman;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import java.util.ArrayList;
import java.util.List;

public class ScoreManager {
    private final Font scoreFont;
    private final ImageLoader loader;
    private final List<Image> collectedFruits = new ArrayList<>();
    private boolean muted = false;

    public void toggleMute() {
        muted = !muted;
    }

    public boolean isMuted() {
        return muted;
    }

    /** Inizializza il gestore punteggio con il font per il testo e il loader immagini. */
    public ScoreManager(Font scoreFont, ImageLoader loader) {
        this.scoreFont = scoreFont;
        this.loader    = loader;
    }

    /** Disegna la barra in alto con vite, punteggio, livello e frutti raccolti. */
    public void drawScoreboard(GraphicsContext gc,
                               int lives,
                               int score,
                               int level) {
        int tileSize   = PacMan.TILE_SIZE;
        int boardWidth = PacMan.BOARD_WIDTH;
        int boardHeight= PacMan.BOARD_HEIGHT;

        // Sfondo barra in alto
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, boardWidth, tileSize);

        gc.setFont(scoreFont);
        gc.setFill(Color.YELLOW);

        // Vita
        Image lifeImg = loader.getPacmanRightImage();
        for (int i = 0; i < lives; i++) {
            gc.drawImage(
                lifeImg,
                tileSize * (i + 0.2),
                tileSize / 6.0,
                tileSize / 1.5,
                tileSize / 1.5
            );
        }

        // Punteggio
        String scoreText = String.format("SCORE %06d", score);
        Text scoreNode   = new Text(scoreText);
        scoreNode.setFont(scoreFont);
        double scoreWidth = scoreNode.getLayoutBounds().getWidth();
        gc.fillText(scoreText, (boardWidth - scoreWidth) / 2, tileSize / 1.5);

        // Livello
        String levelText = String.format("LVL %02d", level);
        Text levelNode   = new Text(levelText);
        levelNode.setFont(scoreFont);
        double levelWidth = levelNode.getLayoutBounds().getWidth();
        gc.fillText(levelText, boardWidth - levelWidth - 10, tileSize / 1.5);

        // Frutti raccolti in basso
        int fruitY = boardHeight + 2;
        for (int i = 0; i < collectedFruits.size(); i++) {
            Image img = collectedFruits.get(i);
            gc.drawImage(img, i * tileSize, fruitY, tileSize, tileSize);
        }

        // Icona volume in basso a destra
        Image volumeImage = muted ? loader.getVolumeOffImage() : loader.getVolumeOnImage();
        double iconSize   = tileSize * 0.8;
        double iconX      = boardWidth - iconSize - 5;
        double iconY      = boardHeight + tileSize - iconSize - 5;
        gc.drawImage(volumeImage, iconX, iconY, iconSize, iconSize);
    }

    /** Aggiunge l’immagine del frutto raccolto alla lista per il display successivo. */
    public void addCollectedFruit(FruitManager.FruitType type) {
        Image img = switch (type) {
            case CHERRY     -> loader.getCherryImage();
            case APPLE      -> loader.getAppleImage();
            case STRAWBERRY -> loader.getStrawberryImage();
        };
        collectedFruits.add(img);
    }

    /** Restituisce il raccolto corrente. */
    public List<Image> getCollectedFruits() {
        return collectedFruits;
    }
}
