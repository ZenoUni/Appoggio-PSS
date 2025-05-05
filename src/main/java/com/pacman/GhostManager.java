package com.pacman;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

public class GhostManager {

    private static final long SCARED_DURATION_MS = 6_000; // 6 secondi

    private List<Block> ghosts;
    private List<Block> cagedGhosts;
    private List<RespawnGhost> respawningGhosts;
    private Block ghostPortal;
    private List<Block> powerFoods;
    private GameMap map;

    private boolean ghostsAreScared = false;
    private long scaredEndTime = 0;
    private long lastReleaseTime = 0;

    private final Image scaredGhostImage;
    private final Image whiteGhostImage;
    private final ImageLoader imageLoader;  // per ricaricare le immagini base

    public GhostManager(List<Block> ghosts, Block ghostPortal, List<Block> powerFoods, GameMap map) {
        this.imageLoader = new ImageLoader();
        this.scaredGhostImage = imageLoader.getScaredGhostImage();
        this.whiteGhostImage  = imageLoader.getWhiteGhostImage();
        this.ghosts = new ArrayList<>();
        this.cagedGhosts = new ArrayList<>();
        this.respawningGhosts = new ArrayList<>();
        this.ghostPortal = ghostPortal;
        this.powerFoods = powerFoods;
        this.map = map;

        // Primo fantasma libero, gli altri in gabbia
        if (!ghosts.isEmpty()) {
            this.ghosts.add(ghosts.get(0));
            for (int i = 1; i < ghosts.size(); i++) {
                this.cagedGhosts.add(ghosts.get(i));
            }
        }
    }

    public void draw(GraphicsContext gc) {
        long timeLeft = getTimeLeft();
    
        // Ora lampeggia ogni 500 ms anziché ogni 1000 ms
        boolean blinkingPhase = false;
        if (timeLeft > 0 && timeLeft <= 3000) {
            // Dividiamo per 500 ms e guardiamo il bit di parità
            long halfSecondsLeft = timeLeft / 500;
            blinkingPhase = (halfSecondsLeft % 2 == 1);
        }
    
        for (Block ghost : ghosts) {
            Image img;
            if (!ghost.isScared) {
                img = ghost.image;
            } else if (blinkingPhase) {
                img = whiteGhostImage;
            } else {
                img = scaredGhostImage;
            }
            gc.drawImage(img, ghost.x, ghost.y, PacMan.TILE_SIZE, PacMan.TILE_SIZE);
        }
        for (Block ghost : cagedGhosts) {
            Image img;
            if (!ghost.isScared) {
                img = ghost.image;
            } else if (blinkingPhase) {
                img = whiteGhostImage;
            } else {
                img = scaredGhostImage;
            }
            gc.drawImage(img, ghost.x, ghost.y, PacMan.TILE_SIZE, PacMan.TILE_SIZE);
        }
    }
    
    

    public void drawPortal(GraphicsContext gc) {
        if (ghostPortal != null) {
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(4);
            gc.strokeLine(
                ghostPortal.x, ghostPortal.y + 2,
                ghostPortal.x + PacMan.TILE_SIZE, ghostPortal.y + 2
            );
        }
    }

    public void activateScaredMode() {
        ghostsAreScared = true;
        scaredEndTime = System.currentTimeMillis() + SCARED_DURATION_MS;
        for (Block ghost : ghosts) ghost.isScared = true;
        for (Block ghost : cagedGhosts) ghost.isScared = true;
    }
    

    private void updateScaredState() {
        if (ghostsAreScared && System.currentTimeMillis() > scaredEndTime) {
            ghostsAreScared = false;
            for (Block ghost : ghosts) {
                ghost.isScared = false;
                resetBlockImage(ghost);
            }
            for (Block ghost : cagedGhosts) {
                ghost.isScared = false;
                resetBlockImage(ghost);
            }
        }
    }
    

    /** Ricarica l'immagine base da ImageLoader in base al colore o tipo del fantasma */
    /** Ripristina l'immagine base dal campo originalImage di Block */
    private void resetBlockImage(Block ghost) {
        ghost.image = ghost.originalImage;
    }


    public int handleGhostCollisions(Block pacman, Runnable onPacmanHit) {
        updateScaredState();

        int points = 0;
        List<Block> eaten = new ArrayList<>();

        for (Block ghost : ghosts) {
            boolean collided = pacman.x < ghost.x + ghost.width &&
                               pacman.x + pacman.width > ghost.x &&
                               pacman.y < ghost.y + ghost.height &&
                               pacman.y + pacman.height > ghost.y;
            if (!collided) continue;
            if (ghost.isScared) {
                points += 200;
                eaten.add(ghost);
            } else {
                onPacmanHit.run();
                return 0;
            }
        }

        for (Block ghost : eaten) {
            ghosts.remove(ghost);
            // Quando mangiato, resettalo alla modalità normale
            resetBlockImage(ghost);
            scheduleGhostRespawn(ghost);
        }
        return points;
    }

    private void scheduleGhostRespawn(Block ghost) {
        ghost.isScared = false;
        resetBlockImage(ghost); // assicura l'immagine normale
        ghost.x = ghostPortal.x + (PacMan.TILE_SIZE / 2);
        ghost.y = ghostPortal.y + (PacMan.TILE_SIZE / 2);
        ghost.direction = Direction.UP;
        ghost.isExiting = true;
        respawningGhosts.add(new RespawnGhost(ghost, System.currentTimeMillis() + 1000));
    }    

    private void checkRespawningGhosts() {
        long now = System.currentTimeMillis();
        Iterator<RespawnGhost> it = respawningGhosts.iterator();
        while (it.hasNext()) {
            RespawnGhost rg = it.next();
            if (now >= rg.respawnTime) {
                // al respawn, il fantasma è già in modalità normale
                ghosts.add(rg.ghost);
                it.remove();
            }
        }
    }

    private void releaseCagedGhost() {
        if (!cagedGhosts.isEmpty()) {
            Block ghost = cagedGhosts.remove(0);
            resetBlockImage(ghost);  // assicurati che sia normale
            ghost.x = ghostPortal.x + (PacMan.TILE_SIZE / 2);
            ghost.y = ghostPortal.y + (PacMan.TILE_SIZE / 2);
            ghost.direction = Direction.UP;
            ghost.isExiting = true;
            ghosts.add(ghost);
        }
    }

    private void checkCagedGhostsRelease() {
        long now = System.currentTimeMillis();
        if (now - lastReleaseTime > 4000) {
            releaseCagedGhost();
            lastReleaseTime = now;
        }
    }

    public void moveGhosts() {
        int speed = 2;
        updateScaredState();
        checkRespawningGhosts();
        checkCagedGhostsRelease();

        for (Block ghost : ghosts) {
            if (ghost.isExiting) {
                ghost.y -= speed;
                if (ghost.y + ghost.height < ghostPortal.y) {
                    ghost.isExiting = false;
                    ghost.direction = Direction.randomDirection();
                }
                map.wrapAround(ghost);
                continue;
            }
            int newX = ghost.x, newY = ghost.y;
            switch (ghost.direction) {
                case UP    -> newY -= speed;
                case DOWN  -> newY += speed;
                case LEFT  -> newX -= speed;
                case RIGHT -> newX += speed;
            }
            if (!collidesWithWall(newX, newY)) {
                ghost.x = newX;
                ghost.y = newY;
            } else {
                ghost.direction = Direction.randomDirection();
            }
            map.wrapAround(ghost);
        }
    }

    private boolean collidesWithWall(int x, int y) {
        Block temp = new Block(null, x, y, PacMan.TILE_SIZE, PacMan.TILE_SIZE);
        return map.isCollisionWithWallOrPortal(temp);
    }

    public void resetGhosts(List<Block> newGhosts, Block newPortal, List<Block> newPowerFoods) {
        this.ghosts.clear();
        this.cagedGhosts.clear();
        this.respawningGhosts.clear();
        this.ghostPortal = newPortal;
        this.powerFoods = newPowerFoods;

        if (!newGhosts.isEmpty()) {
            this.ghosts.add(newGhosts.get(0));
            for (int i = 1; i < newGhosts.size(); i++) {
                this.cagedGhosts.add(newGhosts.get(i));
            }
        }

        this.ghostsAreScared = false;
        this.scaredEndTime = 0;
        this.lastReleaseTime = System.currentTimeMillis();
    }

    private static class RespawnGhost {
        Block ghost;
        long respawnTime;
        RespawnGhost(Block ghost, long respawnTime) {
            this.ghost = ghost;
            this.respawnTime = respawnTime;
        }
    }

    /** Restituisce quanto manca (in ms) alla fine del scared mode */
    private long getTimeLeft() {
        return Math.max(0, scaredEndTime - System.currentTimeMillis());
    }

}