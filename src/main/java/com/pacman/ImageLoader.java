package com.pacman;

import javafx.scene.image.Image;

public class ImageLoader {
    private final Image wallImage;
    private final Image pacmanUpImage;
    private final Image pacmanDownImage;
    private final Image pacmanLeftImage;
    private final Image pacmanRightImage;
    private final Image pacmanCloseImage;
    private final Image pacmanDeath1Image;
    private final Image pacmanDeath2Image;
    private final Image pacmanDeath3Image;
    private final Image blueGhostImage;
    private final Image orangeGhostImage;
    private final Image pinkGhostImage;
    private final Image redGhostImage;
    private final Image powerFoodImage;
    private final Image cherryImage;
    private final Image appleImage;
    private final Image strawberryImage;
    private final Image scaredGhostImage;
    private final Image whiteGhostImage;
    private final Image wallWhiteImage;
    private final Image volumeOnImage;
    private final Image volumeOffImage;
    private final Image arrowInstructionImage;


    public ImageLoader() {
        wallImage        = load("/assets/wall.png");
        pacmanUpImage    = load("/assets/pacmanUp.png");
        pacmanDownImage  = load("/assets/pacmanDown.png");
        pacmanLeftImage  = load("/assets/pacmanLeft.png");
        pacmanRightImage = load("/assets/pacmanRight.png");
        pacmanCloseImage = load("/assets/pacmanClose.png");
        pacmanDeath1Image = load("/assets/Death/pacmanDeath1.png");
        pacmanDeath2Image = load("/assets/Death/pacmanDeath2.png");
        pacmanDeath3Image = load("/assets/Death/pacmanDeath3.png");
        blueGhostImage   = load("/assets/blueGhost.png");
        orangeGhostImage = load("/assets/orangeGhost.png");
        pinkGhostImage   = load("/assets/pinkGhost.png");
        redGhostImage    = load("/assets/redGhost.png");
        powerFoodImage   = load("/assets/powerFood.png");
        cherryImage      = load("/assets/cherry.png");
        appleImage       = load("/assets/apple.png");
        strawberryImage  = load("/assets/strawberry.png");
        scaredGhostImage = load("/assets/scaredGhost.png");
        whiteGhostImage  = load("/assets/whiteGhost.png");
        wallWhiteImage   = load("/assets/wallWhite.png");
        volumeOnImage    = load("/assets/volume_on.png");
        volumeOffImage    = load("/assets/volume_off.png");
        arrowInstructionImage = load("/assets/arrow_instruction.png");
    }

    private Image load(String path) {
        return new Image(getClass().getResource(path).toExternalForm());
    }

    public Image getWallImage()           { return wallImage; }
    public Image getPacmanUpImage()       { return pacmanUpImage; }
    public Image getPacmanDownImage()     { return pacmanDownImage; }
    public Image getPacmanLeftImage()     { return pacmanLeftImage; }
    public Image getPacmanRightImage()    { return pacmanRightImage; }
    public Image getPacmanClosedImage()   { return pacmanCloseImage; }
    public Image getPacmanDeath1Image()   { return pacmanDeath1Image; }
    public Image getPacmanDeath2Image()   { return pacmanDeath2Image; }
    public Image getPacmanDeath3Image()   { return pacmanDeath3Image; }
    public Image getBlueGhostImage()      { return blueGhostImage; }
    public Image getOrangeGhostImage()    { return orangeGhostImage; }
    public Image getPinkGhostImage()      { return pinkGhostImage; }
    public Image getRedGhostImage()       { return redGhostImage; }
    public Image getPowerFoodImage()      { return powerFoodImage; }
    public Image getCherryImage()         { return cherryImage; }
    public Image getAppleImage()          { return appleImage; }
    public Image getStrawberryImage()     { return strawberryImage; }
    public Image getScaredGhostImage()    { return scaredGhostImage; }
    public Image getWhiteGhostImage()     { return whiteGhostImage; }
    public Image getWallWhiteImage()      { return wallWhiteImage; }
    public Image getVolumeOnImage()       { return volumeOnImage; }
    public Image getVolumeOffImage()      { return volumeOffImage; }
    public Image arrowInstructionImage()  { return arrowInstructionImage; }

}