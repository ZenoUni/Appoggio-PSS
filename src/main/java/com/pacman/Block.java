// File: Block.java
package com.pacman;

import javafx.scene.image.Image;

public class Block {
    public int x, y, width, height;
    public Image image;
    public final Image originalImage;

    /** Per i fantasmi: che tipo sono */
    public final GhostType ghostType;

    /** Direzione corrente del blocco (usata per i fantasmi) */
    public Direction direction;

    /** Se il fantasma è appena uscito dal portale */
    public boolean isExiting = false;
    /** Se il fantasma è in modalità spaventata */
    public boolean isScared  = false;

    // Velocità (puoi usarle o meno, a seconda dell’implementazione)
    public int velocityX = 0, velocityY = 0;

    /** Costruttore principale per elementi statici o Pac-Man */
    public Block(Image image, int x, int y) {
        this(image, x, y, PacMan.TILE_SIZE, PacMan.TILE_SIZE, null);
    }

    /** Costruttore esteso che accetta anche un GhostType (null se non è un fantasma) */
    public Block(Image image, int x, int y, int width, int height, GhostType ghostType) {
        this.image         = image;
        this.originalImage = image;
        this.x             = x;
        this.y             = y;
        this.width         = width;
        this.height        = height;
        this.ghostType     = ghostType;

        // Direzione iniziale: random per i fantasmi, altrimenti null
        this.direction = (ghostType != null) ? Direction.randomDirection() : null;
        this.isScared  = false;
    }

    /** Utility per cambiare immagine e velocità (non usato per i fantasmi) */
    public void updateDirection(char dir, Image img) {
        this.image = img;
        this.velocityX = (dir == 'L' ? -4 : dir == 'R' ? 4 : 0);
        this.velocityY = (dir == 'U' ? -4 : dir == 'D' ? 4 : 0);
    }

    /** I tipi di fantasmi disponibili */
    public enum GhostType {
        RED, BLUE, ORANGE, PINK
    }
}
