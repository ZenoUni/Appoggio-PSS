package com.pacman;

import javafx.scene.canvas.GraphicsContext;
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

    private final Map<Block, Deque<GameMap.Point>> orangePaths = new HashMap<>();
    private final Map<Block, Deque<GameMap.Point>> pinkPaths   = new HashMap<>();
    private final Map<GameMap.Point, List<GameMap.Point>> navGraph;
    private final Map<Block, Boolean> orangeChaseState = new HashMap<>();

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
        this.navGraph = map.buildNavigationGraph();
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
                g.y -= SPEED;
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
                        // Pink: anticipa Pac-Man scegliendo tra le direzioni libere
                        Block pac = game.getPacmanBlock();
                        Direction pd = game.getPacmanDirection();
                        // calcola target
                        int tx = pac.x + pd.dx * PINK_PREDICT_TILES * PacMan.TILE_SIZE;
                        int ty = pac.y + pd.dy * PINK_PREDICT_TILES * PacMan.TILE_SIZE;
                        next = bestAvailableDirection(g, new Point(tx, ty));
                        break;

                    default:
                        next = timedRandom(g, now);
                }
            }

            moveAlong(g, next);
            handleWrap(g);
        }
    }

    /**
     * Tenta di muovere il fantasma di un passo in `d`. Se è bloccato, ne sceglie subito
     * un altro tra `availableDirections(g)`. Aggiorna sempre `g.direction`.
     */
    private void moveAlong(Block g, Direction d) {
        int nx = g.x + d.dx * SPEED;
        int ny = g.y + d.dy * SPEED;

        if (!collidesWithWall(nx, ny)) {
            g.x = nx;
            g.y = ny;
            g.direction = d;
        } else {
            // collisione: scegli subito una direzione libera
            List<Direction> free = availableDirections(g);
            Direction alt = free.isEmpty() ? g.direction
                                        : free.get(rand.nextInt(free.size()));
            g.x = g.x + alt.dx * SPEED;
            g.y = g.y + alt.dy * SPEED;
            g.direction = alt;
        }
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
            int nx = g.x + d.dx * SPEED;
            int ny = g.y + d.dy * SPEED;
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

     // nuovo metodo A*
    private Deque<GameMap.Point> findPath(GameMap.Point start, GameMap.Point goal) {
        record Node(GameMap.Point p, double f) {}
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(n->n.f));
        Map<GameMap.Point, GameMap.Point> cameFrom = new HashMap<>();
        Map<GameMap.Point, Double> gScore = new HashMap<>();
        gScore.put(start, 0.0);
        open.add(new Node(start, heuristic(start, goal)));
        
        while (!open.isEmpty()) {
            Node current = open.poll();
            if (current.p.equals(goal)) break;
            for (GameMap.Point neighbor : navGraph.getOrDefault(current.p, List.of())) {
                double tentative = gScore.get(current.p) + 1;
                if (tentative < gScore.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    cameFrom.put(neighbor, current.p);
                    gScore.put(neighbor, tentative);
                    double f = tentative + heuristic(neighbor, goal);
                    open.add(new Node(neighbor, f));
                }
            }
        }
        Deque<GameMap.Point> path = new ArrayDeque<>();
        GameMap.Point cur = goal;
        while (cur != null && !cur.equals(start)) {
            path.addFirst(cur);
            cur = cameFrom.get(cur);
        }
        return path;
    }

    private double heuristic(GameMap.Point a, GameMap.Point b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }

    // restituisce il nodo più vicino alla posizione del block
    private GameMap.Point nearestNode(Block g) {
        double best = Double.MAX_VALUE;
        GameMap.Point bestP = null;
        for (GameMap.Point p : navGraph.keySet()) {
            double dx = g.x - p.x * PacMan.TILE_SIZE;
            double dy = g.y - p.y * PacMan.TILE_SIZE;
            double d2 = dx*dx + dy*dy;
            if (d2 < best) {
                best = d2;
                bestP = p;
            }
        }
        return bestP;
    }

   /**
     * Dato un percorso di nodi, ritorna la direzione verso il primo passo,
     * salta automaticamente i nodi in caso di collisione.
     */
    private Direction directionAlongPath(Block g, Deque<GameMap.Point> path) {
        if (path.isEmpty()) return g.direction;

        GameMap.Point next = path.peekFirst();
        GameMap.Point curr = nearestNode(g);

        // primo step
        final int diffX = next.x - curr.x;
        final int diffY = next.y - curr.y;
        Direction d = Arrays.stream(Direction.values())
            .filter(dir -> dir.dx == diffX && dir.dy == diffY)
            .findFirst()
            .orElse(g.direction);

        // verifichiamo collisione immediata
        int nx = g.x + d.dx * SPEED;
        int ny = g.y + d.dy * SPEED;
        if (collidesWithWall(nx, ny) && path.size() > 1) {
            // saltiamo il nodo corrente e ricalcoliamo
            path.pollFirst();
            GameMap.Point alt = path.peekFirst();
            final int altX = alt.x - curr.x;
            final int altY = alt.y - curr.y;
            d = Arrays.stream(Direction.values())
                .filter(dir -> dir.dx == altX && dir.dy == altY)
                .findFirst()
                .orElse(g.direction);
        } else {
            // possiamo consumare il nodo
            path.pollFirst();
        }

        return d;
    }


    private GameMap.Point randomNode() {
        List<GameMap.Point> keys = new ArrayList<>(navGraph.keySet());
        return keys.get(rand.nextInt(keys.size()));
    }

    private GameMap.Point nearestNodeAnticipatedPac() {
        Block pac = game.getPacmanBlock();
        Direction pd = game.getPacmanDirection();
        Block tmp = new Block(null,
            pac.x + pd.dx * PINK_PREDICT_TILES * PacMan.TILE_SIZE,
            pac.y + pd.dy * PINK_PREDICT_TILES * PacMan.TILE_SIZE,
            PacMan.TILE_SIZE, PacMan.TILE_SIZE,
            null);
        return nearestNode(tmp);
    }

    // classe di supporto per A*
    private static class Node {
        final GameMap.Point p;
        final double f;
        Node(GameMap.Point p, double f) { this.p = p; this.f = f; }
    }

}