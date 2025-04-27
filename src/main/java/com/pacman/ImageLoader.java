package com.pacman;

import javafx.scene.image.Image;

public class ImageLoader {
    private final Image wallImage;
    private final Image pacmanUpImage;
    private final Image pacmanDownImage;
    private final Image pacmanLeftImage;
    private final Image pacmanRightImage;
    private final Image blueGhostImage;
    private final Image orangeGhostImage;
    private final Image pinkGhostImage;
    private final Image redGhostImage;
    private final Image powerFoodImage;
    private final Image cherryImage;
    private final Image scaredGhostImage;
    private final Image whiteGhostImage;

    public ImageLoader() {
        wallImage        = load("/assets/wall.png");
        pacmanUpImage    = load("/assets/pacmanUp.png");
        pacmanDownImage  = load("/assets/pacmanDown.png");
        pacmanLeftImage  = load("/assets/pacmanLeft.png");
        pacmanRightImage = load("/assets/pacmanRight.png");

        blueGhostImage   = load("/assets/blueGhost.png");
        orangeGhostImage = load("/assets/orangeGhost.png");
        pinkGhostImage   = load("/assets/pinkGhost.png");
        redGhostImage    = load("/assets/redGhost.png");

        powerFoodImage   = load("/assets/powerFood.png");
        cherryImage      = load("/assets/cherry.png");
        scaredGhostImage = load("/assets/scaredGhost.png");
        whiteGhostImage  = load("/assets/whiteGhost.png");
    }

    private Image load(String path) {
        return new Image(getClass().getResource(path).toExternalForm());
    }

    public Image getWallImage()           { return wallImage; }
    public Image getPacmanUpImage()       { return pacmanUpImage; }
    public Image getPacmanDownImage()     { return pacmanDownImage; }
    public Image getPacmanLeftImage()     { return pacmanLeftImage; }
    public Image getPacmanRightImage()    { return pacmanRightImage; }

    public Image getBlueGhostImage()      { return blueGhostImage; }
    public Image getOrangeGhostImage()    { return orangeGhostImage; }
    public Image getPinkGhostImage()      { return pinkGhostImage; }
    public Image getRedGhostImage()       { return redGhostImage; }

    public Image getPowerFoodImage()      { return powerFoodImage; }
    public Image getCherryImage()         { return cherryImage; }
    public Image getScaredGhostImage()    { return scaredGhostImage; }
    public Image getWhiteGhostImage()     { return whiteGhostImage; }
}
