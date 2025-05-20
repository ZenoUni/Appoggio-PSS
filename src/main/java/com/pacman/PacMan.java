package com.pacman;

import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class PacMan extends Pane {
    public static final int TILE_SIZE    = 32;
    public static final int ROW_COUNT    = 22;
    public static final int COLUMN_COUNT = 19;
    public static final int BOARD_WIDTH  = COLUMN_COUNT * TILE_SIZE;
    public static final int BOARD_HEIGHT = ROW_COUNT * TILE_SIZE;
    private static final int pacmanSPEED = 2;
    private boolean started = false;
    private final GraphicsContext gc;
    private AnimationTimer       gameLoop;
    private Block pacman;
    private int   score  = 0;
    private int   lives  = 3;
    private int   level  = 1;
    private boolean gameOver          = false;
    private boolean flashing          = false;
    private boolean waitingForRestart = false;
    private boolean waitingForLifeKey = false;
    private double speedMultiplier = 1.0;
    private KeyCode currentDirection = null;
    private KeyCode storedDirection  = null;
    private boolean inTunnel         = false;
    private final ImageLoader   imageLoader;
    final GameMap               gameMap;
    private final FruitManager fruitManager;
    private final GhostManager  ghostManager;
    private final ScoreManager  scoreManager;
    private final Font scoreFont;
    private final Font gameOverFont;
    private final Font returnKeyFont;
    private final MainMenu mainMenu;
    private int  animationCounter = 0;
    private boolean mouthOpen     = true;
    private final SoundManager soundManager = new SoundManager();
    private boolean waitingForStartSound = false;
    private boolean waitingForDeathSound = false;

    public PacMan(MainMenu menu) {
        this.mainMenu = menu;

        SoundManager.loadSound("start",     "sounds/start.wav");
        SoundManager.loadSound("death",     "sounds/death.wav");
        SoundManager.loadSound("dot",       "sounds/dot.wav");
        SoundManager.loadSound("fruit",     "sounds/fruit.wav");
        SoundManager.loadSound("eat_ghost", "sounds/eat_ghost.wav");
        SoundManager.loadSound("siren_ghost", "sounds/siren_ghost.wav");

        Canvas canvas = new Canvas(BOARD_WIDTH, BOARD_HEIGHT + TILE_SIZE);
        gc = canvas.getGraphicsContext2D();
        getChildren().add(canvas);
        scoreFont     = Font.loadFont(getClass().getResource("/assets/fonts/PressStart2P.ttf").toExternalForm(), 12);
        gameOverFont  = Font.loadFont(getClass().getResource("/assets/fonts/PressStart2P.ttf").toExternalForm(), 48);
        returnKeyFont = Font.loadFont(getClass().getResource("/assets/fonts/PressStart2P.ttf").toExternalForm(), 16);
        imageLoader  = new ImageLoader();
        gameMap      = new GameMap(imageLoader);
        fruitManager = new FruitManager(this, imageLoader);
        ghostManager = new GhostManager(
            gameMap.getGhosts(),
            gameMap.getGhostPortal(),
            gameMap.getPowerFoods(),
            gameMap,
            this,
            soundManager
        );

        gameMap.resetEntities();
        ghostManager.resetGhosts(gameMap.getGhosts(), gameMap.getGhostPortal(), gameMap.getPowerFoods());
        scoreManager = new ScoreManager(scoreFont, imageLoader);
        pacman       = gameMap.getPacman();

        setFocusTraversable(true);

        setOnKeyPressed(e -> {
            if (waitingForStartSound || waitingForDeathSound) return;
            if (!started) {
                startAfterReady(e.getCode());
            } else {
                handleKeyPress(e.getCode());
            }
        });

        draw();

        waitingForStartSound = true;
        Clip startClip = SoundManager.getClip("start");
        if (startClip != null) {
            startClip.addLineListener(evt -> {
                if (evt.getType() == LineEvent.Type.STOP) {
                    Platform.runLater(() -> waitingForStartSound = false);
                }
            });
            startClip.setFramePosition(0);
            startClip.start();
        }
        setOnMouseClicked(e -> {
            requestFocus();

            double mouseX = e.getX();
            double mouseY = e.getY();

            double iconSize = TILE_SIZE * 0.8;
            double iconX = BOARD_WIDTH - iconSize - 5;
            double iconY    = BOARD_HEIGHT + TILE_SIZE - iconSize - 5;

            if (mouseX >= iconX && mouseX <= iconX + iconSize &&
                mouseY >= iconY && mouseY <= iconY + iconSize) {
                scoreManager.toggleMute();
                if (scoreManager.isMuted()) {
                    SoundManager.muteAll();
                } else {
                    SoundManager.unmuteAll();
                }
                draw();
            }
        });
    }

    private void startAfterReady(KeyCode initialDir) {
        if (started) return;
        if (keyToDir(initialDir) == null) return;
        started = true;
        gameMap.setFirstLoad(false);
        currentDirection = initialDir;
        applyImage(currentDirection);
        fruitManager.startFruitTimer();
        ghostManager.startCageTimers();
        startGameLoop();
    }

    private void startGameLoop() {
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!gameOver && !flashing) {
                    if (storedDirection != null && gameMap.canMove(pacman, storedDirection)) {
                        currentDirection = storedDirection;
                        applyImage(currentDirection);
                        storedDirection = null;
                    }
                    movePacman();
                    ghostManager.moveGhosts(); 
                    score += ghostManager.handleGhostCollisions(pacman, PacMan.this::loseLife);
                    draw();
                    if (gameMap.getFoods().isEmpty() && gameMap.getPowerFoodCount() == 0) {
                        nextLevel();
                    }
                }
            }
        };
        gameLoop.start();
    }

    public int getReadyRow() {
        return 11;
    }

    private void movePacman() {
        if (currentDirection == null) return;
        Direction dir = keyToDir(currentDirection);
        if (dir == null) return;

        // 1) Calcola quante volte devo eseguire un “passo” da pacmanSPEED pixel
        int steps = (int) Math.round(speedMultiplier);
        // assicuriamoci almeno 1
        steps = Math.max(1, steps);

        // 2) Cerco di muovermi "steps" volte di pacmanSPEED pixel, allineando alla griglia
        for (int s = 0; s < steps; s++) {
            // ---- allineamento automatico come prima ----
            if (dir == Direction.LEFT || dir == Direction.RIGHT) {
                int targetY = Math.round((float) pacman.y / TILE_SIZE) * TILE_SIZE;
                int deltaY = targetY - pacman.y;
                if (deltaY != 0) {
                    pacman.y += Integer.signum(deltaY) * Math.min(pacmanSPEED, Math.abs(deltaY));
                    if (pacman.y != targetY) break;  // salto il resto di questo passo
                }
            } else {
                int targetX = Math.round((float) pacman.x / TILE_SIZE) * TILE_SIZE;
                int deltaX = targetX - pacman.x;
                if (deltaX != 0) {
                    pacman.x += Integer.signum(deltaX) * Math.min(pacmanSPEED, Math.abs(deltaX));
                    if (pacman.x != targetX) break;
                }
            }
            // --------------------------------------------

            // 3) Provo a muovermi di pacmanSPEED pixel in dir
            int nx = pacman.x + dir.dx * pacmanSPEED;
            int ny = pacman.y + dir.dy * pacmanSPEED;
            Block test = new Block(null, nx, ny, pacman.width, pacman.height, null);
            if (!gameMap.isCollisionWithWallOrPortal(test)) {
                pacman.x = nx;
                pacman.y = ny;
            } else {
                // ho sbattuto: interrompo la sequenza di passi
                break;
            }
        }

        // 4) Bocca + tunnel + cibo/frutta come prima...
        animationCounter++;
        if (animationCounter >= 10) {
            mouthOpen = !mouthOpen;
            animationCounter = 0;
        }
        applyImage(currentDirection);

        // … tunnel
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
            for (Block t : gameMap.getTunnels()) {
                if (collision(pacman, t)) { still = true; break; }
            }
            if (!still) inTunnel = false;
        }

        // … cibo e frutta
        int foodScore = gameMap.collectFood(pacman);
        if (foodScore > 0) {
            SoundManager.playSound("dot");
            score += foodScore;
        }
        if (gameMap.collectPowerFood(pacman)) {
            score += 50;
            ghostManager.activateScaredMode();
        }
        int prevScore = score;
        score += fruitManager.collectFruit(pacman);
        if (score > prevScore) {
            int gained = score - prevScore;
            FruitManager.FruitType type = switch (gained) {
                case 200 -> FruitManager.FruitType.CHERRY;
                case 400 -> FruitManager.FruitType.APPLE;
                case 800 -> FruitManager.FruitType.STRAWBERRY;
                default  -> null;
            };
            if (type != null) scoreManager.addCollectedFruit(type);
            SoundManager.playSound("fruit");
        }
    }

    private Direction keyToDir(KeyCode k) {
        return switch (k) {
            case UP    -> Direction.UP;
            case DOWN  -> Direction.DOWN;
            case LEFT  -> Direction.LEFT;
            case RIGHT -> Direction.RIGHT;
            default    -> null;
        };
    }

    public Block getPacmanBlock() {
        return pacman;
    }

    public Direction getPacmanDirection() {
        if (currentDirection != null) {
            Direction d = keyToDir(currentDirection);
            if (d != null) return d;
        }
        return Direction.randomDirection();
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
        scoreManager.drawScoreboard(gc,lives,score,level);
        if (gameOver) {
            drawGameOver();
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
        if (flashing) return;
        if (gameOver) { mainMenu.returnToMenu(); return; }
        if (waitingForLifeKey) {
            waitingForLifeKey = false;
            gameMap.resetEntities();
            pacman = gameMap.getPacman();
            ghostManager.resetGhosts(
                gameMap.getGhosts(),
                gameMap.getGhostPortal(),
                gameMap.getPowerFoods()
            );
            ghostManager.startCageTimers();
            fruitManager.startFruitTimer();
            if (keyToDir(key) != null) { currentDirection = key; applyImage(currentDirection); }
            gameLoop.start();
            return;
        }
        if (waitingForRestart) {
            waitingForRestart = false;
            if (keyToDir(key) != null) { currentDirection = key; applyImage(currentDirection); }
            fruitManager.startFruitTimer();
            ghostManager.startCageTimers();
            startGameLoop();
            return;
        }
        if (keyToDir(key) != null) storedDirection = key;
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
        SoundManager.stopSound("siren_ghost");
        gameLoop.stop();
        speedMultiplier = 1.0;  // reset moltiplicatore
        ghostManager.unfreeze();
        fruitManager.pauseFruitTimer();
        currentDirection = null;
        storedDirection  = null;
        lives--;
        waitingForDeathSound = true;
        Clip deathClip = SoundManager.getClip("death");
        if (deathClip != null) {
            deathClip.addLineListener(evt -> {
                if (evt.getType() == LineEvent.Type.STOP) {
                    Platform.runLater(this::proceedAfterDeathSound);
                }
            });
            deathClip.setFramePosition(0);
            deathClip.start();
        }
    }

    private void drawGameOver() {
        gc.setFill(Color.ORANGE);
        gc.setFont(gameOverFont);
        String msg = "GAME OVER";
        double w = getTextWidth(msg, gameOverFont);
        gc.fillText(msg, (BOARD_WIDTH - w) / 2, (BOARD_HEIGHT + TILE_SIZE) / 2);
        String prompt = "PRESS ANY KEY TO RETURN";
        gc.setFill(Color.YELLOW);
        gc.setFont(returnKeyFont);
        double pw = getTextWidth(prompt, returnKeyFont);
        gc.fillText(prompt, (BOARD_WIDTH - pw) / 2, (BOARD_HEIGHT + TILE_SIZE) / 2 + 40);
    }

    private void nextLevel() {
        SoundManager.stopSound("siren_ghost");
        speedMultiplier = 1.0;
        ghostManager.unfreeze();
        level++;
        if (level % 3 == 1 && lives < 3) lives++;
        flashing = true;
        gameLoop.stop();
        draw();
        flashWalls();
    }

    public int getCurrentLevel() { return level; }
    public void setSpeedMultiplier(double m) { speedMultiplier = m; }
    public double getSpeedMultiplier()   { return speedMultiplier; }
    public void freezeGhosts(long durationMs) { ghostManager.freeze(durationMs); }

    private void flashWalls() {
        new Thread(() -> {
            try {
                for (int i = 0; i < 3; i++) {
                    gameMap.setWallImage(imageLoader.getWallWhiteImage()); draw(); Thread.sleep(500);
                    gameMap.setWallImage(imageLoader.getWallImage());      draw(); Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                flashing = false;
                fruitManager.reset();
                gameMap.reload();
                gameMap.resetEntities();
                pacman = gameMap.getPacman();
                ghostManager.resetGhosts(
                    gameMap.getGhosts(),
                    gameMap.getGhostPortal(),
                    gameMap.getPowerFoods()
                );
                waitingForRestart = true;
                draw();
            }
        }).start();
    }

    private void proceedAfterDeathSound() {
        waitingForDeathSound = false;
        if (lives <= 0) { gameOver = true; draw(); return; }
        gameMap.resetEntities(); pacman = gameMap.getPacman();
        ghostManager.resetGhosts(
            gameMap.getGhosts(),
            gameMap.getGhostPortal(),
            gameMap.getPowerFoods()
        );
        ghostManager.startCageTimers();
        waitingForLifeKey = true;
        draw();
    }
}
