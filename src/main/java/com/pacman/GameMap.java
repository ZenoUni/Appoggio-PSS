package com.pacman;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.Light.Point;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class GameMap {
    private static final String[] tileMap = {
        "XXXXXXXXXXXXXXXXXXX",
        "XO       X       OX",
        "X XX XXX X XXX XX X",
        "X                 X",
        "X XX X XXXXX X XX X",
        "X    X       X    X",
        "XXXX XXXXnXXXX XXXX",
        "nnnX XnnnrnnnX Xnnn",
        "XXXX XnXX-XXnX XXXX",
        "Tnnn nnXpboXnn nnnT",
        "XXXX XnXXXXXnX XXXX",
        "nnnX XnREADY!X Xnnn",
        "XXXX XnXXXXXnX XXXX",
        "X        X        X",
        "X XX XXX X XXX XX X",
        "X  X     P     X  X",
        "XX X X XXXXX X X XX",
        "X    X   X   X    X",
        "X XXXXXX X XXXXXX X",
        "XO               OX",
        "XXXXXXXXXXXXXXXXXXX"
    };
    private final HashSet<Block> walls      = new HashSet<>();
    private final HashSet<Block> foods      = new HashSet<>();
    private final HashSet<Block> ghosts     = new HashSet<>();
    private final HashSet<Block> powerFoods = new HashSet<>();
    private final List<Block>    tunnels    = new ArrayList<>();
    private Block                ghostPortal;
    private Block                pacman;
    private final List<Image>    collectedFruits = new ArrayList<>();
    private final ImageLoader    loader;
    private boolean firstLoad = true;


    public GameMap(ImageLoader loader) {
        this.loader = loader;
        loadMap();
    }

    // Carica da tileMap murature, cibo, fantasmi, tunnel e Pac-Man
    public void loadMap() {
        walls.clear();
        foods.clear();
        ghosts.clear();
        powerFoods.clear();
        tunnels.clear();
        collectedFruits.clear();
        ghostPortal = null;
    
        for (int r = 0; r < tileMap.length; r++) {
            for (int c = 0; c < tileMap[r].length(); c++) {
                int x = c * PacMan.TILE_SIZE;
                int y = r * PacMan.TILE_SIZE;
                char tile = tileMap[r].charAt(c);
    
                switch (tile) {
                    case 'X':
                        walls.add(new Block(loader.getWallImage(), x, y, PacMan.TILE_SIZE, PacMan.TILE_SIZE, null));
                        break;
                    case ' ':
                        foods.add(new Block(null,
                            x + PacMan.TILE_SIZE/2 - 2,
                            y + PacMan.TILE_SIZE/2 - 2,
                            4, 4,
                            null));
                        break;
                    case 'n':
                        break;
                    case 'P':
                        pacman = new Block(loader.getPacmanRightImage(), x, y, PacMan.TILE_SIZE, PacMan.TILE_SIZE, null);
                        break;
                    case '-':
                        ghostPortal = new Block(null, x, y, PacMan.TILE_SIZE, 4, null);
                        break;
                    case 'O':
                        powerFoods.add(new Block(
                            loader.getPowerFoodImage(), x, y,
                            PacMan.TILE_SIZE, PacMan.TILE_SIZE,
                            null));
                        break;
                    case 'b':
                        ghosts.add(new Block(
                            loader.getBlueGhostImage(), x, y,
                            PacMan.TILE_SIZE, PacMan.TILE_SIZE,
                            Block.GhostType.BLUE));
                        break;
                    case 'o':
                        ghosts.add(new Block(
                            loader.getOrangeGhostImage(), x, y,
                            PacMan.TILE_SIZE, PacMan.TILE_SIZE,
                            Block.GhostType.ORANGE));
                        break;
                    case 'p':
                        ghosts.add(new Block(
                            loader.getPinkGhostImage(), x, y,
                            PacMan.TILE_SIZE, PacMan.TILE_SIZE,
                            Block.GhostType.PINK));
                        break;
                    case 'r':
                        ghosts.add(new Block(
                            loader.getRedGhostImage(), x, y,
                            PacMan.TILE_SIZE, PacMan.TILE_SIZE,
                            Block.GhostType.RED));
                        break;
                    case 'T':
                        tunnels.add(new Block(null, x, y,
                            PacMan.TILE_SIZE, PacMan.TILE_SIZE,
                            null));
                        break;
                    default:
                        break;
                }
            }
        }
    }
    
    // Ripristina solo posizioni di Pac-Man e fantasmi, mantenendo la mappa statica
    public void resetEntities() {
        ghosts.clear();
        for (int r = 0; r < tileMap.length; r++) {
            for (int c = 0; c < tileMap[r].length(); c++) {
                int x = c * PacMan.TILE_SIZE;
                int y = r * PacMan.TILE_SIZE;
                char tile = tileMap[r].charAt(c);
    
                switch (tile) {
                    case 'P':
                        pacman = new Block(loader.getPacmanRightImage(), x, y, PacMan.TILE_SIZE, PacMan.TILE_SIZE, null);
                        break;
                    case 'b':
                        ghosts.add(new Block(
                            loader.getBlueGhostImage(), x, y,
                            PacMan.TILE_SIZE, PacMan.TILE_SIZE,
                            Block.GhostType.BLUE));
                        break;
                    case 'o':
                        ghosts.add(new Block(
                            loader.getOrangeGhostImage(), x, y,
                            PacMan.TILE_SIZE, PacMan.TILE_SIZE,
                            Block.GhostType.ORANGE));
                        break;
                    case 'p':
                        ghosts.add(new Block(
                            loader.getPinkGhostImage(), x, y,
                            PacMan.TILE_SIZE, PacMan.TILE_SIZE,
                            Block.GhostType.PINK));
                        break;
                    case 'r':
                        ghosts.add(new Block(
                            loader.getRedGhostImage(), x, y,
                            PacMan.TILE_SIZE, PacMan.TILE_SIZE,
                            Block.GhostType.RED));
                        break;
                    default:
                        break;
                }
            }
        }
    }

    public void setFirstLoad(boolean v) { firstLoad = v; }

    // Disegna muri, cibo, power-food e “READY!” al primo caricamento
    public void draw(GraphicsContext gc) {
        for (Block w : walls) {
            if (w.image != null)
                gc.drawImage(w.image, w.x, w.y,
                             PacMan.TILE_SIZE, PacMan.TILE_SIZE);
        }
        gc.setFill(Color.WHITE);
        for (Block f : foods) {
            gc.fillRect(f.x, f.y, f.width, f.height);
        }
        for (Block pf : powerFoods) {
            gc.drawImage(pf.image, pf.x, pf.y,
                         PacMan.TILE_SIZE, PacMan.TILE_SIZE);
        }

        if (firstLoad) {
            String msg = "READY!";
            Font   f   = Font.font("PressStart2P",
                           FontWeight.BOLD, PacMan.TILE_SIZE);
            gc.setFont(f);
            gc.setFill(Color.YELLOW);

            Text measure = new Text(msg);
            measure.setFont(f);
            double textW = measure.getLayoutBounds().getWidth();

            for (int r = 0; r < tileMap.length; r++) {
                int c = tileMap[r].indexOf(msg);
                if (c >= 0) {
                    double x = (PacMan.BOARD_WIDTH - textW) / 2;
                    double y = r * PacMan.TILE_SIZE + PacMan.TILE_SIZE;
                    gc.fillText(msg, x, y);
                    break;
                }
            }
        }
    }

    public Block getPacman()    { return pacman; }
    // Restituisce un nuovo block di Pac-Man e ricarica la mappa
    public Block resetPacman()  { loadMap(); return pacman; }
    public HashSet<Block> getWalls()    { return walls; }
    public HashSet<Block> getFoods()    { return foods; }
    public List<Block>    getGhosts()   { return new ArrayList<>(ghosts); }
    public Block          getGhostPortal() { return ghostPortal; }
    public List<Block>    getPowerFoods(){ return new ArrayList<>(powerFoods); }
    public List<Image>    getCollectedFruits(){return new ArrayList<>(collectedFruits);}
    public List<Block>    getTunnels()  { return tunnels; }

    // Sposta Pac-Man e fantasmi da un tunnel all’altro
    public void wrapAround(Block b) {
        for (Block t : tunnels) {
            if (collision(b, t)) {
                for (Block o : tunnels) {
                    if (o != t) {
                        b.x = o.x;
                        b.y = o.y;
                        return;
                    }
                }
            }
        }
    }

    private boolean collision(Block a, Block c) {
        return a.x < c.x + c.width &&
               a.x + a.width > c.x &&
               a.y < c.y + c.height &&
               a.y + a.height > c.y;
    }

    // Verifica collisione con muro o portale
    public boolean isCollisionWithWallOrPortal(Block b) {
        for (Block w : walls) if (collision(b, w)) return true;
        if (ghostPortal != null && collision(b, ghostPortal)) return true;
        return false;
    }

    // Controlla se un blocco può muoversi in base a un tasto freccia
    public boolean canMove(Block b, KeyCode key) {
        int nx = b.x, ny = b.y;
        switch (key) {
            case UP    -> ny -= 4;
            case DOWN  -> ny += 4;
            case LEFT  -> nx -= 4;
            case RIGHT -> nx += 4;
            default    -> { }
        }
        Block test = new Block(null, nx, ny);
        return !isCollisionWithWallOrPortal(test);
    }

    // Rimuove un punto cibo se Pac-Man lo raccoglie e restituisce punteggio
    public int collectFood(Block b) {
        Iterator<Block> it = foods.iterator();
        while (it.hasNext()) {
            Block f = it.next();
            if (collision(b, f)) {
                it.remove();
                return 10;
            }
        }
        return 0;
    }

    // Rimuove una power-food se Pac-Man la raccoglie, attivando il superpotere
    public boolean collectPowerFood(Block b) {
        Iterator<Block> it = powerFoods.iterator();
        while (it.hasNext()) {
            Block pf = it.next();
            if (collision(b, pf)) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    public int getPowerFoodCount() { return powerFoods.size(); }
    
    // Chiama il callback una volta che tutte le pareti sono state temporaneamente "spente"
    public void flashWalls(Runnable onFinished) {
        walls.forEach(w->w.image=null);
        onFinished.run();
    }

    // Ricarica completamente la mappa da tileMap
    public void reload() { loadMap(); }

    // Imposta l’immagine di tutte le pareti (usato per l’effetto lampeggio)
    public void setWallImage(Image img) {
        for (Block w : walls) {
            w.image = img;
        }
    }
    /** Nodo di grafo: coordina col, row (non pixel!) */
    public static class Point {
        public final int x, y;
        public Point(int x, int y) { this.x = x; this.y = y; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Point)) return false;
            Point p = (Point)o;
            return x == p.x && y == p.y;
        }
        @Override public int hashCode() {
            return 31 * x + y;
        }
    }


    /**
     * Costruisce un grafo di nodi (intersezioni/tunnel) per il pathfinding.
     * Ritorna mappa di posizione -> lista di posizioni adiacenti.
     */
    public Map<Point, List<Point>> buildNavigationGraph() {
        Map<Point, List<Point>> graph = new HashMap<>();
        int rows = tileMap.length;
        int cols = tileMap[0].length();
        // identifica nodi: intersezioni o vicini a tunnel
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int x = c * PacMan.TILE_SIZE;
                int y = r * PacMan.TILE_SIZE;
                Block b = new Block(null, x, y, PacMan.TILE_SIZE, PacMan.TILE_SIZE, null);
                if (!isCollisionWithWallOrPortal(b)) {
                    // conta direzioni libere
                    int freeDirs = 0;
                    for (Direction d : Direction.values()) {
                        Block nb = new Block(null, x + d.dx*PacMan.TILE_SIZE, y + d.dy*PacMan.TILE_SIZE,
                                             PacMan.TILE_SIZE, PacMan.TILE_SIZE, null);
                        if (!isCollisionWithWallOrPortal(nb)) freeDirs++;
                    }
                    if (freeDirs != 2 || isOnTunnel(b) || r==11) {
                        // nodo valido
                        Point p = new Point(c, r);
                        graph.put(p, new ArrayList<>());
                    }
                }
            }
        }
        // connetti archi orizz/vert tra nodi
        for (Point p : graph.keySet()) {
            for (Direction d : Direction.values()) {
                int nc = p.x + d.dx;
                int nr = p.y + d.dy;
                // scendi lungo il corridoio fino al prossimo nodo
                while (nc>=0 && nr>=0 && nr<rows && nc<cols) {
                    Point q = new Point(nc, nr);
                    if (graph.containsKey(q)) {
                        graph.get(p).add(q);
                        break;
                    }
                    nc += d.dx;
                    nr += d.dy;
                }
            }
        }
        return graph;
    }
    
    /**
     * Verifica se un blocco si trova dentro un tunnel (per considerarlo nodo di pathfinding).
     */
    private boolean isOnTunnel(Block b) {
        for (Block t : tunnels) {
            if (collision(b, t)) return true;
        }
        return false;
    }

}