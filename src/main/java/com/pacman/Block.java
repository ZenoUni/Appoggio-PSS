package com.pacman;

import javafx.scene.image.Image;

public class Block {
    public int x, y, width, height, velocityX = 0, velocityY = 0;
    public Image image;

    public Block(Image image, int x, int y) {
        this(image, x, y, PacMan.TILE_SIZE, PacMan.TILE_SIZE);
    }

    public Block(Image image, int x, int y, int width, int height) {
        this.image = image;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void updateDirection(char direction, Image image) {
        this.image = image;
        velocityX = (direction == 'L' ? -4 : direction == 'R' ? 4 : 0);
        velocityY = (direction == 'U' ? -4 : direction == 'D' ? 4 : 0);
    }
}



