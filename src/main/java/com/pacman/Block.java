package com.pacman;

import javafx.scene.image.Image;

public class Block {
    public int x, y, width, height, velocityX = 0, velocityY = 0;
    public Image image;
    public final Image originalImage;    // <— NUOVO CAMPO

    // Direzione dei fantasmi
    public Direction direction;

    // Stato di uscita dal portale
    public boolean isExiting = false;

    public Block(Image image, int x, int y) {
        this(image, x, y, PacMan.TILE_SIZE, PacMan.TILE_SIZE);
    }

    public Block(Image image, int x, int y, int width, int height) {
        this.image = image;
        this.originalImage = image;      // <— inizializziamo qui
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.direction = Direction.randomDirection();
    }

    public void updateDirection(char direction, Image image) {
        this.image = image;
        this.velocityX = (direction == 'L' ? -4 : direction == 'R' ? 4 : 0);
        this.velocityY = (direction == 'U' ? -4 : direction == 'D' ? 4 : 0);
    }
}