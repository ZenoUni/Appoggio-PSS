package com.pacman;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

public class GhostManager {

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

    public GhostManager(List<Block> ghosts, Block ghostPortal, List<Block> powerFoods, GameMap map) {
        this.scaredGhostImage = new Image(getClass().getResource("/assets/scaredGhost.png").toExternalForm());
        this.ghosts = new ArrayList<>();
        this.cagedGhosts = new ArrayList<>();
        this.respawningGhosts = new ArrayList<>();
        this.ghostPortal = ghostPortal;
        this.powerFoods = powerFoods;
        this.map = map;

        if (!ghosts.isEmpty()) {
            this.ghosts.add(ghosts.get(0)); // il primo fantasma è libero
            for (int i = 1; i < ghosts.size(); i++) {
                this.cagedGhosts.add(ghosts.get(i)); // gli altri in gabbia
            }
        }
    }

    public void draw(GraphicsContext gc) {
        for (Block ghost : ghosts) {
            if (ghostsAreScared) {
                gc.drawImage(scaredGhostImage, ghost.x, ghost.y, PacMan.TILE_SIZE, PacMan.TILE_SIZE);
            } else {
                gc.drawImage(ghost.image, ghost.x, ghost.y, PacMan.TILE_SIZE, PacMan.TILE_SIZE);
            }
        }
        for (Block ghost : cagedGhosts) {
            gc.drawImage(ghost.image, ghost.x, ghost.y, PacMan.TILE_SIZE, PacMan.TILE_SIZE);
        }
    }

    public void drawPortal(GraphicsContext gc) {
        if (ghostPortal != null) {
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(4);
            gc.strokeLine(
                ghostPortal.x,
                ghostPortal.y + 2,
                ghostPortal.x + PacMan.TILE_SIZE,
                ghostPortal.y + 2
            );
        }
    }

    public void drawPowerFoods(GraphicsContext gc) {
        for (Block powerFood : powerFoods) {
            gc.drawImage(powerFood.image, powerFood.x, powerFood.y, PacMan.TILE_SIZE, PacMan.TILE_SIZE);
        }
    }

    public void activateScaredMode() {
        ghostsAreScared = true;
        scaredEndTime = System.currentTimeMillis() + 7_000;
    }

    public void updateScaredState() {
        if (ghostsAreScared && System.currentTimeMillis() > scaredEndTime) {
            ghostsAreScared = false;
        }
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

            if (ghostsAreScared) {
                points += 200;
                eaten.add(ghost);
            } else {
                onPacmanHit.run();
                return 0;
            }
        }

        for (Block ghost : eaten) {
            ghosts.remove(ghost);
            scheduleGhostRespawn(ghost);
        }
        return points;
    }

    private void scheduleGhostRespawn(Block ghost) {
        ghost.x = ghostPortal.x + (PacMan.TILE_SIZE / 2);
        ghost.y = ghostPortal.y + (PacMan.TILE_SIZE / 2);
        ghost.direction = Direction.UP;
        ghost.isExiting = true;
        respawningGhosts.add(new RespawnGhost(ghost, System.currentTimeMillis() + 5000)); // 5 secondi di delay
    }

    private void checkRespawningGhosts() {
        long now = System.currentTimeMillis();
        Iterator<RespawnGhost> iterator = respawningGhosts.iterator();
        while (iterator.hasNext()) {
            RespawnGhost rg = iterator.next();
            if (now >= rg.respawnTime) {
                ghosts.add(rg.ghost);
                iterator.remove();
            }
        }
    }

    private void releaseCagedGhost() {
        if (!cagedGhosts.isEmpty()) {
            Block ghost = cagedGhosts.remove(0);
            ghost.x = ghostPortal.x + (PacMan.TILE_SIZE / 2);
            ghost.y = ghostPortal.y + (PacMan.TILE_SIZE / 2);
            ghost.direction = Direction.UP;
            ghost.isExiting = true;
            ghosts.add(ghost);
        }
    }

    private void checkCagedGhostsRelease() {
        long now = System.currentTimeMillis();
        if (now - lastReleaseTime > 4000) { // ogni 4 secondi
            releaseCagedGhost();
            lastReleaseTime = now;
        }
    }

    public void moveGhosts() {
        checkRespawningGhosts();
        checkCagedGhostsRelease();

        for (Block ghost : ghosts) {
            int speed = 2;
            int newX = ghost.x;
            int newY = ghost.y;

            if (ghost.isExiting) {
                newY -= speed;
                ghost.y = newY;

                if (ghost.y + ghost.height < ghostPortal.y) {
                    ghost.isExiting = false;
                    ghost.direction = Direction.randomDirection();
                }
                continue;
            }

            switch (ghost.direction) {
                case UP -> newY -= speed;
                case DOWN -> newY += speed;
                case LEFT -> newX -= speed;
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
            this.ghosts.add(newGhosts.get(0)); // 1 subito libero
            for (int i = 1; i < newGhosts.size(); i++) {
                this.cagedGhosts.add(newGhosts.get(i)); // gli altri in gabbia
            }
        }

        this.ghostsAreScared = false;
        this.scaredEndTime = 0;
        this.lastReleaseTime = System.currentTimeMillis();
    }

    // Classe interna per gestire il tempo di respawn
    private static class RespawnGhost {
        Block ghost;
        long respawnTime;

        RespawnGhost(Block ghost, long respawnTime) {
            this.ghost = ghost;
            this.respawnTime = respawnTime;
        }
    }
}
