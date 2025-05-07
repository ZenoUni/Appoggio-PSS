package com.pacman;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.Light.Point;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.*;

public class GhostManager {

    private static final long SCARED_DURATION_MS = 6_000;
    private static final long ORANGE_PHASE_MS    = 5_000;
    private static final int  PINK_PREDICT_TILES = 4;
    private static final int  SPEED               = 2;

    private final List<Block>        ghosts;
    private final List<Block>        cagedGhosts;
    private final List<RespawnGhost> respawningGhosts;
    private Block                    ghostPortal;
    private List<Block>              powerFoods;
    private final GameMap            map;
    private final PacMan             game;

    private final Map<Block, Long> cageReleaseTime = new HashMap<>();
    private static final long BLUE_DELAY_MS   = 2_000;
    private static final long ORANGE_DELAY_MS = 4_000;
    private static final long PINK_DELAY_MS   = 6_000;
    private static final int RED_START_COL = 9;
    private static final int RED_START_ROW = 7;

    private boolean cageTimerStarted = false;
    private long cageStartTime = 0;

    private boolean ghostsAreScared = false;
    private long    scaredEndTime   = 0;
    private long    lastReleaseTime = 0;

    private final Image scaredGhostImage;
    private final Image whiteGhostImage;
    private final ImageLoader imageLoader;

    // ──────────────────────────────────────────────────────────────────────────
    // ‹‹‹ Nuovi campi per il timing delle direzioni random
    private final Map<Block, Long> nextChangeTime = new HashMap<>();
    private final Random          rand           = new Random();

    private long randomInterval() { 
        // tra 4000 e 6000 ms
        return (4 + rand.nextInt(3)) * 1000L; 
    }
    // ──────────────────────────────────────────────────────────────────────────

    public GhostManager(List<Block> ghosts, Block ghostPortal, List<Block> powerFoods, GameMap map, PacMan game) {
        this.imageLoader      = new ImageLoader();
        this.scaredGhostImage = imageLoader.getScaredGhostImage();
        this.whiteGhostImage  = imageLoader.getWhiteGhostImage();
        this.ghosts           = new ArrayList<>();
        this.cagedGhosts      = new ArrayList<>();
        this.respawningGhosts = new ArrayList<>();
        this.ghostPortal      = ghostPortal;
        this.powerFoods       = powerFoods;
        this.map              = map;
        this.game             = game;

        // primo fantasma libero, gli altri in gabbia
        if (!ghosts.isEmpty()) {
            this.ghosts.add(ghosts.get(0));
            for (int i = 1; i < ghosts.size(); i++) {
                this.cagedGhosts.add(ghosts.get(i));
            }
        }

        // ‹‹‹ inizializza i nextChangeTime
        long now = System.currentTimeMillis();
        for (Block g : this.ghosts) {
            nextChangeTime.put(g, now + randomInterval());
        }
    }

    public void resetGhosts(List<Block> newGhosts,
                        Block newPortal,
                        List<Block> newPowerFoods) {
        // 1) Pulisci le liste
        ghosts.clear();
        cagedGhosts.clear();
        respawningGhosts.clear();
        this.ghostPortal = newPortal;
        this.powerFoods  = newPowerFoods;

        // 2) Annulla lo stato "scared" e qualunque timer di rilascio
        ghostsAreScared   = false;
        scaredEndTime     = 0;
        cageTimerStarted  = false;
        cageReleaseTime.clear();

        // 3) Metti il RED nella lista libera e tutti gli altri in gabbia
        if (!newGhosts.isEmpty()) {
            ghosts.add(newGhosts.get(0));  // RED
            for (int i = 1; i < newGhosts.size(); i++) {
                cagedGhosts.add(newGhosts.get(i));
            }
        }

        // 4) Reinizializza i timer di direzione per i fantasmi già liberi (RED)
        long now = System.currentTimeMillis();
        nextChangeTime.clear();
        for (Block g : ghosts) {
            nextChangeTime.put(g, now + randomInterval());
        }
    }


    public void startCageTimers() {
        if (cageTimerStarted) return;
        cageTimerStarted = true;

        // Istante “zero” per tutti
        long zero = System.currentTimeMillis();
        cageStartTime = zero;

        // Imposta un timer distinto per ciascun colore, tutti a partire dallo stesso zero
        for (Block g : cagedGhosts) {
            long delay = switch (g.ghostType) {
                case BLUE   -> BLUE_DELAY_MS;
                case ORANGE -> ORANGE_DELAY_MS;
                case PINK   -> PINK_DELAY_MS;
                default     -> 0L;
            };
            cageReleaseTime.put(g, zero + delay);
        }
    }
    
    public void draw(GraphicsContext gc) {
        long timeLeft = Math.max(0, scaredEndTime - System.currentTimeMillis());
        boolean blinking = timeLeft > 0 && timeLeft <= 3000 && ((timeLeft / 500) % 2 == 1);

        for (Block g : ghosts) {
            Image img = (!g.isScared) ? g.image : (blinking ? whiteGhostImage : scaredGhostImage);
            gc.drawImage(img, g.x, g.y, PacMan.TILE_SIZE, PacMan.TILE_SIZE);
        }
        for (Block g : cagedGhosts) {
            Image img = (!g.isScared) ? g.image : (blinking ? whiteGhostImage : scaredGhostImage);
            gc.drawImage(img, g.x, g.y, PacMan.TILE_SIZE, PacMan.TILE_SIZE);
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
        scaredEndTime   = System.currentTimeMillis() + SCARED_DURATION_MS;
        for (Block g : ghosts)    g.isScared = true;
        for (Block g : cagedGhosts) g.isScared = true;
    }

    private void updateScaredState() {
        if (ghostsAreScared && System.currentTimeMillis() > scaredEndTime) {
            ghostsAreScared = false;
            for (Block g : ghosts) {
                g.isScared = false;
                g.image    = g.originalImage;
            }
            for (Block g : cagedGhosts) {
                g.isScared = false;
                g.image    = g.originalImage;
            }
        }
    }

    public int handleGhostCollisions(Block pacman, Runnable onHit) {
        updateScaredState();
        int points = 0;
        List<Block> eaten = new ArrayList<>();

        for (Block g : ghosts) {
            boolean collided = pacman.x < g.x + g.width &&
                               pacman.x + pacman.width > g.x &&
                               pacman.y < g.y + g.height &&
                               pacman.y + pacman.height > g.y;
            if (!collided) continue;
            if (g.isScared) {
                points += 200;
                eaten.add(g);
            } else {
                onHit.run();
                return 0;
            }
        }
        for (Block g : eaten) {
            ghosts.remove(g);
            scheduleGhostRespawn(g);
        }
        return points;
    }

    private void scheduleGhostRespawn(Block g) {
        g.isScared = false;
        g.image = g.originalImage;
        g.image     = g.isScared ? scaredGhostImage : g.originalImage;
        g.x         = ghostPortal.x + PacMan.TILE_SIZE/2;
        g.y         = ghostPortal.y + PacMan.TILE_SIZE/2;
        g.direction = Direction.UP;
        g.isExiting = true;
        respawningGhosts.add(new RespawnGhost(g, System.currentTimeMillis() + 1000));
    }

    private void checkRespawningGhosts() {
        long now = System.currentTimeMillis();
        for (Iterator<RespawnGhost> it = respawningGhosts.iterator(); it.hasNext();) {
            RespawnGhost rg = it.next();
            if (now >= rg.respawnTime) {
                ghosts.add(rg.ghost);
                // ‹‹‹ inizializza timer per questo fantasma
                nextChangeTime.put(rg.ghost, now + randomInterval());
                it.remove();
            }
        }
    }

    private void releaseCagedGhost() {
        if (cagedGhosts.isEmpty()) return;

        // ‹‹‹ fai uscire prima il PINK, se c'è
        Block toRelease = null;
        for (Block g : cagedGhosts) {
            if (g.ghostType == Block.GhostType.PINK) {
                toRelease = g;
                break;
            }
        }
        if (toRelease == null) {
            toRelease = cagedGhosts.get(0);
        }
        cagedGhosts.remove(toRelease);

        // mantieni scared-state
        toRelease.isScared = ghostsAreScared;
        toRelease.image    = toRelease.isScared ? scaredGhostImage : toRelease.originalImage;
        toRelease.x        = ghostPortal.x + PacMan.TILE_SIZE/2;
        toRelease.y        = ghostPortal.y + PacMan.TILE_SIZE/2;
        toRelease.direction= Direction.UP;
        toRelease.isExiting= true;
        ghosts.add(toRelease);

        // ‹‹‹ inizializza timer per lui
        long now = System.currentTimeMillis();
        nextChangeTime.put(toRelease, now + randomInterval());
    }

    private void checkCagedGhostsRelease() {
        if (!cageTimerStarted) return;
        long now = System.currentTimeMillis();
    
        Iterator<Block> it = cagedGhosts.iterator();
        while (it.hasNext()) {
            Block g = it.next();
            Long releaseAt = cageReleaseTime.get(g);
            if (releaseAt != null && now >= releaseAt) {
                it.remove();
    
                // Teletrasporta dentro il portale e avvia la fase “uscita”
                g.x         = ghostPortal.x + PacMan.TILE_SIZE / 2;
                g.y         = ghostPortal.y + PacMan.TILE_SIZE / 2;
                g.isExiting = true;
                g.direction = Direction.UP;
    
                // Mantieni lo stato scared se attivo
                g.isScared = ghostsAreScared;
                g.image    = g.isScared ? scaredGhostImage : g.originalImage;
    
                ghosts.add(g);
                // Imposta subito il primo cambio di direzione dopo l’uscita
                nextChangeTime.put(g, now + randomInterval());
            }
        }
    }

    public void moveGhosts() {
        long now = System.currentTimeMillis();
        updateScaredState();
        checkRespawningGhosts();
        checkCagedGhostsRelease();
    
        // fai muovere sempre RED (ordinal 0) per primo
        ghosts.sort(Comparator.comparingInt(g -> g.ghostType.ordinal()));
    
        for (Block g : ghosts) {
            // 1) se sta ancora es uscita dal portale
            if (g.isExiting) {
                g.y -= SPEED;
                if (g.y + g.height < ghostPortal.y) {
                    g.isExiting = false;
                    // buttalo subito in scared se tocca
                    g.isScared  = ghostsAreScared;
                    g.image     = g.isScared ? scaredGhostImage : g.originalImage;
                    
                    // adesso scegli la prima direzione random rigenerando l'intervallo
                    g.direction = randomAvailable(g);
                    nextChangeTime.put(g, now + randomInterval());
                }
                map.wrapAround(g);
                continue;
            }
    
            // 2) scegli la prossima direzione
            Direction next;
            if (g.isScared) {
                // scared mode: completamente casuale, ma rispettando il timer
                next = timedRandom(g, now);
            } else {
                switch (g.ghostType) {
                    case RED:
                    case BLUE:
                        // sempre random con timer
                        next = timedRandom(g, now);
                        break;
    
                    case ORANGE:
                        // alterna 5s inseguimento / 5s random
                        boolean chasePhase = ((now / ORANGE_PHASE_MS) % 2) == 1;
                        if (chasePhase) {
                            next = chase(g);
                        } else {
                            next = timedRandom(g, now);
                        }
                        break;
    
                    case PINK:
                        // se è ancora in gabbia, forza l'uscita verso l'alto
                        if (cagedGhosts.contains(g)) {
                            next = Direction.UP;
                        } else {
                            next = predictChase(g);
                        }
                        break;
    
                    default:
                        // caso di sicurezza
                        next = timedRandom(g, now);
                        break;
                }
            }
    
            moveAlong(g, next, now);
        }
    }
    private static class Point {
        final int x, y;
        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
    
    /** Se il timer è scaduto, pesca una nuova randomica; altrimenti mantieni la direzione. */
    private Direction timedRandom(Block g, long now) {
        Long t = nextChangeTime.getOrDefault(g, 0L);
        if (now >= t) {
            Direction d = randomAvailable(g);
            nextChangeTime.put(g, now + randomInterval());
            return d;
        }
        return g.direction;
    }

    private Direction chase(Block g) {
        Block pac = game.getPacmanBlock();
        Point target = new Point(pac.x, pac.y);
        return bestAvailableDirection(g, target);
    }

    private Direction predictChase(Block g) {
        Block pac = game.getPacmanBlock();
        Direction pd = game.getPacmanDirection();
        Point target = new Point(
            pac.x + pd.dx * PINK_PREDICT_TILES * PacMan.TILE_SIZE,
            pac.y + pd.dy * PINK_PREDICT_TILES * PacMan.TILE_SIZE
        );
        return bestAvailableDirection(g, target);
    }

    private Direction bestAvailableDirection(Block g, Point target) {
        double bestDist = Double.MAX_VALUE;
        Direction best = g.direction; // fallback
        for (Direction d : availableDirections(g)) {
            double nx = g.x + d.dx * SPEED;
            double ny = g.y + d.dy * SPEED;
            double dist = hypot(nx - target.x, ny - target.y);
            if (dist < bestDist) {
                bestDist = dist;
                best = d;
            }
        }
        return best;
    }
    

    /** Muove e, in caso di muro, forza cambio immediato e resetta il timer. */
    private void moveAlong(Block g, Direction d, long now) {
        g.direction = d;
        int nx = g.x + d.dx * SPEED;
        int ny = g.y + d.dy * SPEED;

        if (!collidesWithWall(nx, ny)) {
            g.x = nx; 
            g.y = ny;
        } else {
            // sbattuto → cambio immediato
            Direction nd = randomAvailable(g);
            g.direction = nd;
            nextChangeTime.put(g, now + randomInterval());
        }
        map.wrapAround(g);
    }

    private boolean collidesWithWall(int x, int y) {
        Block test = new Block(null, x, y, PacMan.TILE_SIZE, PacMan.TILE_SIZE, null);
        return map.isCollisionWithWallOrPortal(test);
    }

    private static double hypot(double dx, double dy) {
        return Math.hypot(dx, dy);
    }

    private static class RespawnGhost {
        final Block ghost;
        final long  respawnTime;
        RespawnGhost(Block g, long t) { ghost = g; respawnTime = t; }
    }

    // ──────────────────────────────────────────────────────────────────────────
    /** Tutte le mosse possibili da qui (senza muro) */
    private List<Direction> availableDirections(Block g) {
        List<Direction> ok = new ArrayList<>();
        for (Direction d : Direction.values()) {
            int nx = g.x + d.dx * SPEED;
            int ny = g.y + d.dy * SPEED;
            if (!collidesWithWall(nx, ny)) ok.add(d);
        }
        return ok;
    }

    /** Pesca completamente a caso tra quelle possibili */
    private Direction randomAvailable(Block g) {
        List<Direction> ok = availableDirections(g);
        if (ok.isEmpty()) return g.direction;
        return ok.get(rand.nextInt(ok.size()));
    }
}
