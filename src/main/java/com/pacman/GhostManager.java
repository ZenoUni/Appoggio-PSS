package com.pacman;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.List;
import java.util.ArrayList;

public class GhostManager {

    private List<Block> ghosts;
    private List<Block> cagedGhosts;
    private List<RespawnGhost> ghostsToRespawn;
    private Block ghostPortal;
    private List<Block> powerFoods;
    private GameMap map;

    private boolean ghostsAreScared = false;
    private long scaredEndTime = 0;

    private long nextReleaseTime = 0; // timer per rilascio dei fantasmi

    public GhostManager(List<Block> ghosts, Block ghostPortal, List<Block> powerFoods, GameMap map) {
        this.ghosts = new ArrayList<>();
        this.cagedGhosts = new ArrayList<>(ghosts);
        this.ghostsToRespawn = new ArrayList<>();
        this.ghostPortal = ghostPortal;
        this.powerFoods = powerFoods;
        this.map = map;

        // All'inizio libero subito 1-2 fantasmi
        releaseGhost();
        releaseGhost();
        nextReleaseTime = System.currentTimeMillis() + 5000; // primo rilascio dopo 5 secondi
    }

    public void draw(GraphicsContext gc) {
        for (Block ghost : ghosts) {
            gc.drawImage(ghost.image, ghost.x, ghost.y, PacMan.TILE_SIZE, PacMan.TILE_SIZE);
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
        scaredEndTime = System.currentTimeMillis() + 7000;
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
                ghostsToRespawn.add(new RespawnGhost(ghost, System.currentTimeMillis() + 5000)); // respawn in 5 secondi
            } else {
                onPacmanHit.run();
                return 0;
            }
        }

        ghosts.removeAll(eaten);
        return points;
    }

    public void resetGhosts(List<Block> newGhosts, Block newPortal, List<Block> newPowerFoods) {
        this.ghosts = new ArrayList<>();
        this.cagedGhosts = new ArrayList<>(newGhosts);
        this.ghostsToRespawn.clear();
        this.ghostPortal = newPortal;
        this.powerFoods = newPowerFoods;
        this.ghostsAreScared = false;
        this.scaredEndTime = 0;

        // Libero subito 1-2 fantasmi
        releaseGhost();
        releaseGhost();
        nextReleaseTime = System.currentTimeMillis() + 5000; // prossimo rilascio tra 5 secondi
    }

    public void moveGhosts() {
        updateGhostRespawns();
        releaseGhostsIfNeeded();

        for (Block ghost : ghosts) {
            int speed = 2;
            int newX = ghost.x;
            int newY = ghost.y;

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

        // Anche i fantasmi nella gabbia si possono muovere su/giù
        for (Block ghost : cagedGhosts) {
            int speed = 1;
            if (ghost.direction == null) {
                ghost.direction = Direction.UP;
            }

            int newX = ghost.x;
            int newY = ghost.y;

            if (ghost.direction == Direction.UP) {
                newY -= speed;
                if (newY < ghostPortal.y) {
                    ghost.direction = Direction.DOWN;
                }
            } else if (ghost.direction == Direction.DOWN) {
                newY += speed;
                if (newY > ghostPortal.y + PacMan.TILE_SIZE) {
                    ghost.direction = Direction.UP;
                }
            }
            ghost.x = newX;
            ghost.y = newY;
        }
    }

    private void updateGhostRespawns() {
        long now = System.currentTimeMillis();
        List<RespawnGhost> toRespawn = new ArrayList<>();

        for (RespawnGhost respawnGhost : ghostsToRespawn) {
            if (now >= respawnGhost.respawnTime) {
                // Respawn fantasma all'interno della gabbia
                respawnGhost.ghost.x = ghostPortal.x + PacMan.TILE_SIZE / 2 - respawnGhost.ghost.width / 2;
                respawnGhost.ghost.y = ghostPortal.y + PacMan.TILE_SIZE / 2 - respawnGhost.ghost.height / 2;
                cagedGhosts.add(respawnGhost.ghost);
                toRespawn.add(respawnGhost);
            }
        }

        ghostsToRespawn.removeAll(toRespawn);
    }

    private void releaseGhostsIfNeeded() {
        if (cagedGhosts.isEmpty()) return;

        long now = System.currentTimeMillis();
        if (now >= nextReleaseTime) {
            releaseGhost();
            nextReleaseTime = now + 5000; // rilascio ogni 5 secondi
        }
    }

    private void releaseGhost() {
        if (!cagedGhosts.isEmpty()) {
            Block ghost = cagedGhosts.remove(0);
            ghosts.add(ghost);
            ghost.direction = Direction.UP; // escono andando su
        }
    }

    private boolean collidesWithWall(int x, int y) {
        Block temp = new Block(null, x, y, PacMan.TILE_SIZE, PacMan.TILE_SIZE);
        return map.isCollisionWithWallOrPortal(temp);
    }

    /** Classe interna per gestire il respawn */
    private static class RespawnGhost {
        public Block ghost;
        public long respawnTime;

        public RespawnGhost(Block ghost, long respawnTime) {
            this.ghost = ghost;
            this.respawnTime = respawnTime;
        }
    }
}
