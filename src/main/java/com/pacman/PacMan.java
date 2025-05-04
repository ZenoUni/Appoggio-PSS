package com.pacman;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class PacMan extends Pane {
    public static final int TILE_SIZE    = 32;
    public static final int ROW_COUNT    = 21;
    public static final int COLUMN_COUNT = 19;
    public static final int BOARD_WIDTH  = COLUMN_COUNT * TILE_SIZE;
    public static final int BOARD_HEIGHT = ROW_COUNT * TILE_SIZE;

    private final GraphicsContext gc;
    private AnimationTimer       gameLoop;

    private Block pacman;
    private int   score  = 0;
    private int   lives  = 3;
    private int   level  = 1;
    private boolean gameOver          = false;
    private boolean flashing          = false;
    private boolean waitingForRestart = false;

    private KeyCode currentDirection = null;
    private KeyCode storedDirection  = null;
    private boolean inTunnel         = false;

    private final ImageLoader   imageLoader;
    final GameMap               gameMap;
    private final FruitManager fruitManager;
    private final GhostManager  ghostManager;
    private final ScoreManager  scoreManager;

    private final Font scoreFont;
    private final Font messageFont;

    private int  animationCounter = 0;
    private boolean mouthOpen     = true;

    public PacMan() {
        Canvas canvas = new Canvas(BOARD_WIDTH, BOARD_HEIGHT + TILE_SIZE);
        gc = canvas.getGraphicsContext2D();
        getChildren().add(canvas);

        scoreFont   = Font.loadFont(getClass().getResource("/assets/fonts/PressStart2P.ttf").toExternalForm(), 12);
        messageFont = Font.loadFont(getClass().getResource("/assets/fonts/PressStart2P.ttf").toExternalForm(), 24);

        imageLoader  = new ImageLoader();
        gameMap      = new GameMap(imageLoader);
        fruitManager = new FruitManager(this, imageLoader);
        ghostManager = new GhostManager(gameMap.getGhosts(), gameMap.getGhostPortal(), gameMap.getPowerFoods(), gameMap);

        gameMap.resetEntities();
        ghostManager.resetGhosts(gameMap.getGhosts(), gameMap.getGhostPortal(), gameMap.getPowerFoods());

        scoreManager = new ScoreManager(scoreFont, imageLoader);
        pacman       = gameMap.getPacman();

        setFocusTraversable(true);
        setOnMouseClicked(e -> requestFocus());
        setOnKeyPressed(e -> handleKeyPress(e.getCode()));

        fruitManager.startFruitTimer();
        startGameLoop();
    }

    private void startGameLoop() {
        gameLoop = new AnimationTimer() {
            @Override public void handle(long now) {
                if (!gameOver && !flashing) {
                    if (storedDirection != null && isAligned(pacman) && gameMap.canMove(pacman, storedDirection)) {
                        currentDirection = storedDirection;
                        applyImage(currentDirection);
                        storedDirection = null;
                    }
                    movePacman();
                    ghostManager.moveGhosts();
                    draw();
                    if (gameMap.getFoods().isEmpty() && gameMap.getPowerFoodCount() == 0) nextLevel();
                }
            }
        };
        gameLoop.start();
    }

    private boolean isAligned(Block b) {
        return (b.x % TILE_SIZE == 0) && (b.y % TILE_SIZE == 0);
    }

    private void movePacman() {
        if (currentDirection == null) return;
        if (gameMap.canMove(pacman, currentDirection)) {
            switch (currentDirection) {
                case UP    -> pacman.y -= 4;
                case DOWN  -> pacman.y += 4;
                case LEFT  -> pacman.x -= 4;
                case RIGHT -> pacman.x += 4;
                default    -> { }
            }

            animationCounter++;
            if (animationCounter >= 10) {
                mouthOpen = !mouthOpen;
                animationCounter = 0;
            }
            applyImage(currentDirection);

            if (!inTunnel) {
                for (Block t : gameMap.getTunnels()) {
                    if (collision(pacman, t)) {
                        inTunnel = true;
                        KeyCode prevDir    = currentDirection;
                        KeyCode prevStored = storedDirection;
                        gameMap.wrapAround(pacman);
                        currentDirection = prevDir;
                        storedDirection  = prevStored;
                        break;
                    }
                }
            } else {
                boolean still = false;
                for (Block t : gameMap.getTunnels()) if (collision(pacman, t)) still = true;
                if (!still) inTunnel = false;
            }

            score += gameMap.collectFood(pacman);
            if (gameMap.collectPowerFood(pacman)) {
                score += 50;
                ghostManager.activateScaredMode();
            }
            score += ghostManager.handleGhostCollisions(pacman, this::loseLife);
            score += fruitManager.collectFruit(pacman);
        }
    }

    private void draw() {
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, BOARD_WIDTH, BOARD_HEIGHT + TILE_SIZE);

        gc.save();
        gc.translate(0, TILE_SIZE);
        gameMap.draw(gc);
        fruitManager.draw(gc);
        gc.drawImage(pacman.image, pacman.x, pacman.y, TILE_SIZE, TILE_SIZE);
        ghostManager.draw(gc);
        ghostManager.drawPortal(gc);
        gc.restore();

        scoreManager.drawScoreboard(gc, lives, gameMap.getCollectedFruits(), score, level);

        if (gameOver) {
            gc.setFill(Color.RED);
            gc.setFont(messageFont);
            String m = "GAME OVER";
            double w = getTextWidth(m, messageFont);
            gc.fillText(m, (BOARD_WIDTH - w) / 2, (BOARD_HEIGHT + TILE_SIZE) / 2);
            String r = "PRESS ANY KEY";
            double rw = getTextWidth(r, messageFont);
            gc.setFill(Color.YELLOW);
            gc.fillText(r, (BOARD_WIDTH - rw) / 2, (BOARD_HEIGHT + TILE_SIZE) / 2 + 30);
        }
    }

    private void applyImage(KeyCode dir) {
        if (!mouthOpen) {
            pacman.image = imageLoader.getPacmanClosedImage();
            return;
        }
        switch (dir) {
            case UP    -> pacman.image = imageLoader.getPacmanUpImage();
            case DOWN  -> pacman.image = imageLoader.getPacmanDownImage();
            case LEFT  -> pacman.image = imageLoader.getPacmanLeftImage();
            case RIGHT -> pacman.image = imageLoader.getPacmanRightImage();
            default    -> { }
        }
    }

    private void handleKeyPress(KeyCode key) {
        if (waitingForRestart) {
            if (gameOver) resetGame();
            waitingForRestart = false;
            gameLoop.start();
            return;
        }
        storedDirection = key;
    }

    private boolean collision(Block a, Block c) {
        return a.x < c.x + c.width &&
               a.x + a.width > c.x &&
               a.y < c.y + c.height &&
               a.y + a.height > c.y;
    }

    private double getTextWidth(String text, Font font) {
        javafx.scene.text.Text t = new javafx.scene.text.Text(text);
        t.setFont(font);
        return t.getLayoutBounds().getWidth();
    }

    private void loseLife() {
        lives--;
        if (lives <= 0) {
            gameOver = true;
            waitingForRestart = true;
            gameLoop.stop();
            draw();
        } else {
            gameMap.resetEntities();
            pacman = gameMap.getPacman();
            ghostManager.resetGhosts(gameMap.getGhosts(), gameMap.getGhostPortal(), gameMap.getPowerFoods());
            currentDirection = null;
            storedDirection  = null;
            waitingForRestart = true;
            gameLoop.stop();
            draw();
        }
    }

    private void resetGame() {
        gameOver = false;
        score    = 0;
        lives    = 3;
        level    = 1;
        currentDirection = null;
        storedDirection  = null;
        gameMap.reload();
        pacman = gameMap.resetPacman();
        ghostManager.resetGhosts(gameMap.getGhosts(), gameMap.getGhostPortal(), gameMap.getPowerFoods());
        fruitManager.reset();
    }

    private void nextLevel() {
        level++;
        flashing = true;
        gameMap.flashWalls(() -> {
            gameMap.reload();
            pacman = gameMap.resetPacman();
            currentDirection = null;
            storedDirection  = null;
            flashing = false;
            waitingForRestart = true;
            gameLoop.stop();
            draw();
        });
    }
}
