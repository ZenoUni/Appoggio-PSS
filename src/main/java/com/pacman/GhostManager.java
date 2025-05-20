package com.pacman;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import java.util.*;

public class GhostManager {

    private static final long SCARED_DURATION_MS = 6_000;
    private static final long ORANGE_PHASE_MS    = 5_000;
    private static final int  PINK_PREDICT_TILES = 4;
    private static final int  ghostSPEED               = 2;
    private final List<Block>        ghosts;
    private final List<Block>        cagedGhosts;
    private final List<RespawnGhost> respawningGhosts;
    private Block                    ghostPortal;
    private final GameMap            map;
    private final PacMan             game;
    private final Map<Block, Long> cageReleaseTime = new HashMap<>();
    private static final long BLUE_DELAY_MS   = 2_000;
    private static final long ORANGE_DELAY_MS = 4_000;
    private static final long PINK_DELAY_MS   = 6_000;
    private boolean cageTimerStarted = false;
    private boolean ghostsAreScared = false;
    private long    scaredEndTime   = 0;
    private final Image scaredGhostImage;
    private final Image whiteGhostImage;
    private final ImageLoader imageLoader;
    private final Map<Block, Long> nextChangeTime = new HashMap<>();
    private final Random          rand           = new Random();
    private final Set<Block> ghostsInTunnel = new HashSet<>();
    private boolean frozen = false;
    private long frozenEndTime = 0;

    private final Map<Block, Boolean> orangeChaseState = new HashMap<>();
    private static final int PINK_PHASE_MS = 10000;


    private long randomInterval() { 
        return (4 + rand.nextInt(3)) * 1000L; 
    }

    // Inizializza i fantasmi: RED parte subito, gli altri restano in gabbia
    public GhostManager(List<Block> allGhosts,
                        Block ghostPortal,
                        List<Block> powerFoods,
                        GameMap map,
                        PacMan game,
                        SoundManager soundManager) {
        this.map = map;
        this.imageLoader      = new ImageLoader();
        this.scaredGhostImage = imageLoader.getScaredGhostImage();
        this.whiteGhostImage  = imageLoader.getWhiteGhostImage();
        this.ghosts           = new ArrayList<>();
        this.cagedGhosts      = new ArrayList<>();
        this.respawningGhosts = new ArrayList<>();
        this.ghostPortal      = ghostPortal;
        this.game             = game;

        Block red = allGhosts.stream()
            .filter(g -> g.ghostType == Block.GhostType.RED)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Manca il fantasma RED!"));

        ghosts.add(red);
        allGhosts.stream()
                 .filter(g -> g != red)
                 .forEach(cagedGhosts::add);

        long now = System.currentTimeMillis();
        nextChangeTime.put(red, now + randomInterval());
    }

    // Resetta lo stato dei fantasmi tra un livello e l’altro
    public void resetGhosts(List<Block> allGhosts,
                            Block newPortal,
                            List<Block> newPowerFoods) {
        ghosts.clear();
        cagedGhosts.clear();
        respawningGhosts.clear();
        cageReleaseTime.clear();
        this.ghostPortal     = newPortal;
        ghostsAreScared      = false;
        scaredEndTime        = 0;
        cageTimerStarted     = false;

        Block red = allGhosts.stream()
            .filter(g -> g.ghostType == Block.GhostType.RED)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Manca il fantasma RED!"));

        ghosts.add(red);
        allGhosts.stream()
                 .filter(g -> g != red)
                 .forEach(cagedGhosts::add);

        long now = System.currentTimeMillis();
        nextChangeTime.clear();
        nextChangeTime.put(red, now + randomInterval());
        orangeChaseState.clear();
    }

    // Avvia i timer per liberare progressivamente i fantasmi dalla gabbia
    public void startCageTimers() {
        if (cageTimerStarted) return;
        cageTimerStarted = true;
        long zero = System.currentTimeMillis();
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
    
    // Disegna i fantasmi, gestendo l’effetto “spaventato” e il blinking
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

    // Disegna il portale da cui escono i fantasmi
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

    
    // Attiva la modalità spaventato per tutti i fantasmi per un tempo definito
    public void activateScaredMode() {
        ghostsAreScared = true;
        scaredEndTime   = System.currentTimeMillis() + SCARED_DURATION_MS;
        for (Block g : ghosts)    g.isScared = true;
        for (Block g : cagedGhosts) g.isScared = true;

        SoundManager.loopSound("siren_ghost");
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
            SoundManager.stopSound("siren_ghost");
        }
    }

    // Gestisce collisioni Pac-Man vs fantasmi, restituendo punti o invocando onHit
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
                SoundManager.playSound("eat_ghost");
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
        g.x = ghostPortal.x + (ghostPortal.width  - g.width)  / 2;
        g.y = ghostPortal.y + (ghostPortal.height - g.height) / 2;        
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
                nextChangeTime.put(rg.ghost, now + randomInterval());
                it.remove();
            }
        }
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
                g.x = ghostPortal.x + (ghostPortal.width  - g.width)  / 2;
                g.y = ghostPortal.y + (ghostPortal.height - g.height) / 2;
                g.isExiting = true;
                g.direction = Direction.UP;
                g.isScared = ghostsAreScared;
                g.image    = g.isScared ? scaredGhostImage : g.originalImage;
                ghosts.add(g);
                nextChangeTime.put(g, now + randomInterval());
            }
        }
    }
    
    public void moveGhosts() {
        long now = System.currentTimeMillis();
        if (frozen && now < frozenEndTime) return;
        frozen = false;

        updateScaredState();
        checkRespawningGhosts();
        checkCagedGhostsRelease();

        ghosts.sort(Comparator.comparingInt(g -> g.ghostType.ordinal()));

        // Fase globale per Orange: 5s chase, 5s random
        boolean chasePhase = ((now / ORANGE_PHASE_MS) % 2) == 1;

        for (Block g : ghosts) {
            if (g.isExiting) {
                // Uscita dalla gabbia
                g.y -= ghostSPEED;
                if (g.y + g.height < ghostPortal.y) {
                    g.isExiting = false;
                    g.image      = g.originalImage;
                    g.direction  = randomAvailable(g);
                }
                handleWrap(g);
                continue;
            }

            Direction next;

            if (g.isScared) {
                // Fantasma spaventato: comportamento casuale con timer
                next = timedRandom(g, now);

            } else {
                switch (g.ghostType) {
                    case RED:
                    case BLUE:
                        // Esplorazione casuale
                        next = timedRandom(g, now);
                        break;

                    case ORANGE:
                        // Orange alterna chase/random ogni 5s,
                        // ricalcola la direzione solo quando la sotto‐fase cambia.
                        Boolean last = orangeChaseState.get(g);
                        if (last == null || last != chasePhase) {
                            next = chasePhase
                                ? chase(g)
                                : randomAvailable(g);
                            orangeChaseState.put(g, chasePhase);
                        } else {
                            next = g.direction;
                        }
                        break;

                   case PINK:
                        long pinkPhaseTime = now % PINK_PHASE_MS;
                        if (pinkPhaseTime < 6000) {  // Prime 6s: modalità predittiva
                            next = bestAvailableDirection(g, predictedPacmanTarget());
                        } else {  // 4s random
                            next = timedRandom(g, now);
                        }
                        break;
                    default:
                        next = timedRandom(g, now);
                }
            }

            moveAlong(g, next);
            handleWrap(g);
        }
    }

    private Point predictedPacmanTarget() {
        Block pac = game.getPacmanBlock();
        Direction pd = game.getPacmanDirection();
        return new Point(
            pac.x + pd.dx * PINK_PREDICT_TILES * PacMan.TILE_SIZE,
            pac.y + pd.dy * PINK_PREDICT_TILES * PacMan.TILE_SIZE
        );
    }

    /**
     * Tenta di muovere il fantasma di un passo in `d`. Se è bloccato, ne sceglie subito
     * un altro tra `availableDirections(g)`. Aggiorna sempre `g.direction`.
     */
   private void moveAlong(Block g, Direction d) {
        // Calcola posizione “raw” se muovessi in d
        int nx = g.x + d.dx * ghostSPEED;
        int ny = g.y + d.dy * ghostSPEED;

        // Verifica collisione in quella posizione
        boolean free = !collidesWithWall(nx, ny);

        // Solo quando sono esattamente su confini di cella (multipli di TILE_SIZE)
        // posso cambiare direzione; altrimenti proseguo nella dir attuale
        boolean onGridX = (g.x % PacMan.TILE_SIZE) == 0;
        boolean onGridY = (g.y % PacMan.TILE_SIZE) == 0;
        if (!(onGridX && onGridY)) {
            // non sono centrato: continuo nella direzione corrente se possibile
            Direction cur = g.direction;
            int cx = g.x + cur.dx * ghostSPEED;
            int cy = g.y + cur.dy * ghostSPEED;
            if (!collidesWithWall(cx, cy)) {
                g.x = cx;
                g.y = cy;
                return;
            }
            // se pure la direzione corrente è bloccata, cadremo più sotto a scegliere un'alternativa
        }

        // Se sono centrato o direzione sbagliata, e d è libero, uso d
        if (free) {
            g.x = nx;
            g.y = ny;
            g.direction = d;
            return;
        }

        // Collisione: cerco subito tra le libere
        List<Direction> freeDirs = availableDirections(g);
        if (!freeDirs.isEmpty()) {
            Direction alt = freeDirs.get(rand.nextInt(freeDirs.size()));
            g.x += alt.dx * ghostSPEED;
            g.y += alt.dy * ghostSPEED;
            g.direction = alt;
        }
        // se non ci sono libere (rare), rimane fermo
    }
    
    private void handleWrap(Block g) {
        if (isOnTunnel(g)) {
            if (!ghostsInTunnel.contains(g)) {
                map.wrapAround(g);
                ghostsInTunnel.add(g);
            }
        } else {
            ghostsInTunnel.remove(g);
        }
    }

    private static class Point {
        final int x, y;
        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

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

    private Direction bestAvailableDirection(Block g, Point target) {
        double bestDist = Double.MAX_VALUE;
        Direction best = g.direction; // fallback
        for (Direction d : availableDirections(g)) {
            double nx = g.x + d.dx * ghostSPEED;
            double ny = g.y + d.dy * ghostSPEED;
            double dist = hypot(nx - target.x, ny - target.y);
            if (dist < bestDist) {
                bestDist = dist;
                best = d;
            }
        }
        return best;
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

    private List<Direction> availableDirections(Block g) {
        List<Direction> ok = new ArrayList<>();
        for (Direction d : Direction.values()) {
            int nx = g.x + d.dx * ghostSPEED;
            int ny = g.y + d.dy * ghostSPEED;
            if (!collidesWithWall(nx, ny)) ok.add(d);
        }
        return ok;
    }

    private Direction randomAvailable(Block g) {
        List<Direction> ok = availableDirections(g);
        if (ok.isEmpty()) return g.direction;
        return ok.get(rand.nextInt(ok.size()));
    }

    private boolean isOnTunnel(Block g) {
        for (Block t : map.getTunnels()) {
            if (g.x < t.x + t.width &&
                g.x + g.width > t.x &&
                g.y < t.y + t.height &&
                g.y + g.height > t.y) {
                return true;
            }
        }
        return false;
    }

    // Congela i movimenti dei fantasmi per un certo intervallo
    public void freeze(long durationMs) {
        frozen = true;
        frozenEndTime = System.currentTimeMillis() + durationMs;
    }
    // Disgela i movimenti dei fantasmi
    public void unfreeze() {
        frozen = false;
    }
}