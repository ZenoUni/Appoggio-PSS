package com.pacman;

import javafx.scene.image.Image;

public class Block {
    public int x, y, width, height;
    public Image image;
    public final Image originalImage;
    public final GhostType ghostType;
    public Direction direction;
    public boolean isExiting = false;
    public boolean isScared  = false;
    public int velocityX = 0, velocityY = 0;

    // Costruttore per blocchi di dimensione di default (tile Pac-Man)
    public Block(Image image, int x, int y) {
        this(image, x, y, PacMan.TILE_SIZE, PacMan.TILE_SIZE, null);
    }

    // Costruttore completo per blocchi (muri, cibo, fantasmi) con tipo GhostType opzionale
    public Block(Image image, int x, int y, int width, int height, GhostType ghostType) {
        this.image         = image;
        this.originalImage = image;
        this.x             = x;
        this.y             = y;
        this.width         = width;
        this.height        = height;
        this.ghostType     = ghostType;
        this.direction = (ghostType != null) ? Direction.randomDirection() : null;
        this.isScared  = false;
    }

    // Aggiorna la direzione di movimento e l’immagine in base al carattere di direzione (L/R/U/D)
    public void updateDirection(char dir, Image img) {
        this.image = img;
        this.velocityX = (dir == 'L' ? -4 : dir == 'R' ? 4 : 0);
        this.velocityY = (dir == 'U' ? -4 : dir == 'D' ? 4 : 0);
    }

    public enum GhostType {
        RED, BLUE, ORANGE, PINK
    }
}