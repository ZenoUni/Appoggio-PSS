// GameMap.java
package com.pacman;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

public class GameMap {
    private static final String[] tileMap = {
        "XXXXXXXXXXXXXXXXXXX",
        "XO       X       OX",
        "X XX XXX X XXX XX X",
        "X                 X",
        "X XX X XXXXX X XX X",
        "X    X       X    X",
        "XXXX XXXX XXXX XXXX",
        "nnnX X   r   X Xnnn",
        "XXXX X XX-XX X XXXX",
        "       XbpoX       ",
        "XXXX X XXXXX X XXXX",
        "nnnX X       X Xnnn",
        "XXXX X XXXXX X XXXX",
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
    private       Block         ghostPortal;
    private       Block         pacman;
    private final List<Image>   collectedFruits = new ArrayList<>();
    private final ImageLoader   loader;

    public GameMap(ImageLoader loader) {
        this.loader = loader;
        loadMap();
    }

    public void loadMap() {
        walls.clear();
        foods.clear();
        ghosts.clear();
        powerFoods.clear();
        collectedFruits.clear();
        ghostPortal = null;

        for (int r = 0; r < tileMap.length; r++) {
            for (int c = 0; c < tileMap[r].length(); c++) {
                int x = c * PacMan.TILE_SIZE;
                int y = r * PacMan.TILE_SIZE;
                char tile = tileMap[r].charAt(c);

                switch (tile) {
                    case 'X' -> walls.add(new Block(loader.getWallImage(), x, y));
                    case ' ' -> foods.add(new Block(null,
                                    x + PacMan.TILE_SIZE/2 - 2,
                                    y + PacMan.TILE_SIZE/2 - 2, 4, 4));
                    case 'n' -> { /* spazio vuoto */ }
                    case 'P' -> pacman = new Block(loader.getPacmanRightImage(), x, y);
                    case '-' -> ghostPortal = new Block(null, x, y, PacMan.TILE_SIZE, 4);
                    case 'O' -> powerFoods.add(new Block(loader.getPowerFoodImage(), x, y));
                    case 'b' -> ghosts.add(new Block(loader.getBlueGhostImage(), x, y));
                    case 'o' -> ghosts.add(new Block(loader.getOrangeGhostImage(), x, y));
                    case 'p' -> ghosts.add(new Block(loader.getPinkGhostImage(), x, y));
                    case 'r' -> ghosts.add(new Block(loader.getRedGhostImage(), x, y));
                    default  -> { /* ignorato */ }
                }
            }
        }
    }

    /** Riposiziona solo Pac-Man e fantasmi. Lascia foods e powerFoods così come sono. */
    public void resetEntities() {
        ghosts.clear();
        for (int r = 0; r < tileMap.length; r++) {
            for (int c = 0; c < tileMap[r].length(); c++) {
                int x = c * PacMan.TILE_SIZE;
                int y = r * PacMan.TILE_SIZE;
                char tile = tileMap[r].charAt(c);

                switch (tile) {
                    case 'P' -> pacman = new Block(loader.getPacmanRightImage(), x, y);
                    case 'b' -> ghosts.add(new Block(loader.getBlueGhostImage(), x, y));
                    case 'o' -> ghosts.add(new Block(loader.getOrangeGhostImage(), x, y));
                    case 'p' -> ghosts.add(new Block(loader.getPinkGhostImage(), x, y));
                    case 'r' -> ghosts.add(new Block(loader.getRedGhostImage(), x, y));
                    default  -> {}
                }
            }
        }
    }

    public Block getPacman() {
        return pacman;
    }

    public Block resetPacman() {
        loadMap();
        return pacman;
    }

    public void draw(GraphicsContext gc) {
        // disegno muri
        for (Block wall : walls) {
            if (wall.image != null) {
                gc.drawImage(wall.image, wall.x, wall.y, PacMan.TILE_SIZE, PacMan.TILE_SIZE);
            }
        }
        // disegno cibi normali
        gc.setFill(Color.WHITE);
        for (Block food : foods) {
            gc.fillRect(food.x, food.y, food.width, food.height);
        }
        // disegno powerFoods aggiornati
        for (Block pf : powerFoods) {
            gc.drawImage(pf.image, pf.x, pf.y, PacMan.TILE_SIZE, PacMan.TILE_SIZE);
        }
    }

    public boolean isWall(int x, int y) {
        return walls.stream().anyMatch(w -> w.x == x && w.y == y);
    }

    public HashSet<Block> getWalls()            { return walls; }
    public HashSet<Block> getFoods()            { return foods; }
    public List<Block>    getGhosts()           { return new ArrayList<>(ghosts); }
    public Block          getGhostPortal()      { return ghostPortal; }
    public List<Block>    getPowerFoods()       { return new ArrayList<>(powerFoods); }
    public List<Image>    getCollectedFruits()  { return new ArrayList<>(collectedFruits); }

    public void wrapAround(Block b) {
        if (b.x < -PacMan.TILE_SIZE) b.x = PacMan.BOARD_WIDTH;
        else if (b.x > PacMan.BOARD_WIDTH) b.x = -PacMan.TILE_SIZE;
    }

    private boolean collision(Block a, Block c) {
        return a.x < c.x + c.width &&
               a.x + a.width > c.x &&
               a.y < c.y + c.height &&
               a.y + a.height > c.y;
    }

    public boolean isCollisionWithWallOrPortal(Block b) {
        for (Block wall : walls) {
            if (collision(b, wall)) return true;
        }
        if (ghostPortal != null && collision(b, ghostPortal)) return true;
        return false;
    }

    public boolean canMove(Block b, KeyCode key) {
        int newX = b.x, newY = b.y;
        switch (key) {
            case UP    -> newY -= 4;
            case DOWN  -> newY += 4;
            case LEFT  -> newX -= 4;
            case RIGHT -> newX += 4;
            default    -> {}
        }
        Block ghost = new Block(null, newX, newY, b.width, b.height);
        return !isCollisionWithWallOrPortal(ghost);
    }

    public int collectFood(Block b) {
        Iterator<Block> it = foods.iterator();
        while (it.hasNext()) {
            Block food = it.next();
            if (collision(b, food)) {
                it.remove();
                return 10;
            }
        }
        return 0;
    }

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

    public int getPowerFoodCount() {
        return powerFoods.size();
    }

    public void flashWalls(Runnable onFinished) {
        walls.forEach(w -> w.image = null);
        onFinished.run();
    }

    public void reload() {
        loadMap();
    }
}
