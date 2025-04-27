package com.pacman;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.List;
import java.util.ArrayList;

public class GhostManager {

    private List<Block> ghosts;
    private Block ghostPortal;
    private List<Block> powerFoods;

    // Stato di scared mode
    private boolean ghostsAreScared = false;
    private long    scaredEndTime   = 0;

    public GhostManager(List<Block> ghosts, Block ghostPortal, List<Block> powerFoods) {
        this.ghosts       = new ArrayList<>(ghosts);
        this.ghostPortal  = ghostPortal;
        this.powerFoods   = powerFoods;
    }

    public void draw(GraphicsContext gc) {
        for (Block ghost : ghosts) {
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

    /** Attiva la scared mode per un tot di millisecondi */
    public void activateScaredMode() {
        ghostsAreScared = true;
        // dura, per esempio, 7 secondi
        scaredEndTime   = System.currentTimeMillis() + 7_000;
        // qui potresti anche cambiare le immagini dei fantasmi
    }

    /** Da chiamare ogni frame per disattivare la scared mode se scaduta */
    public void updateScaredState() {
        if (ghostsAreScared && System.currentTimeMillis() > scaredEndTime) {
            ghostsAreScared = false;
            // ripristina immagini normali dei fantasmi
        }
    }

    /**
     * Gestisce la collisione con i fantasmi:
     * - se spaventati, restituisce punti e "elimina" il fantasma
     * - altrimenti richiama onPacmanHit.run() e non restituisce punti
     */
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
                // fantasma mangiato: dà 200 punti, lo rimuovo temporaneamente
                points += 200;
                eaten.add(ghost);
            } else {
                // Pac-Man perde vita
                onPacmanHit.run();
                return 0;
            }
        }

        // Rimuovo i fantasmi "mangiati"
        ghosts.removeAll(eaten);
        return points;
    }

    /**
     * Ripristina la lista dei fantasmi nella posizione iniziale,
     * mantenendo portal e powerFoods correnti.
     */
    public void resetGhosts(List<Block> newGhosts, Block newPortal, List<Block> newPowerFoods) {
        // Copio i riferimenti della mappa
        this.ghosts      = new ArrayList<>(newGhosts);
        this.ghostPortal = newPortal;
        this.powerFoods  = newPowerFoods;
        // Resetto lo stato di scared mode
        this.ghostsAreScared = false;
        this.scaredEndTime   = 0;
    }
}
