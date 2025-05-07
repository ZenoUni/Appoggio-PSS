package com.pacman;

import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
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
    private final Font gameOverFont;      // per il “GAME OVER” grande
    private final Font returnKeyFont;     // per il “PRESS ANY KEY” piccolo
    private final MainMenu mainMenu;

    private int  animationCounter = 0;
    private boolean mouthOpen     = true;

    public PacMan(MainMenu menu) {
        this.mainMenu = menu;
        Canvas canvas = new Canvas(BOARD_WIDTH, BOARD_HEIGHT + TILE_SIZE);
        gc = canvas.getGraphicsContext2D();
        getChildren().add(canvas);

        scoreFont   = Font.loadFont(getClass().getResource("/assets/fonts/PressStart2P.ttf").toExternalForm(), 12);
        messageFont = Font.loadFont(getClass().getResource("/assets/fonts/PressStart2P.ttf").toExternalForm(), 24);
        gameOverFont    = Font.loadFont(getClass().getResource("/assets/fonts/PressStart2P.ttf").toExternalForm(), 48);
        returnKeyFont   = Font.loadFont(getClass().getResource("/assets/fonts/PressStart2P.ttf").toExternalForm(), 16);
        
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

        // 1) Mostra subito mappa + “READY!”
        draw();  

        // 2) Al primo tasto, lancio startAfterReady con la direzione scelta
        setOnKeyPressed(e -> {
            if (!started) {
                startAfterReady(e.getCode());
            } else {
                handleKeyPress(e.getCode());
            }
        });
        // (opzionale) mantieni il click per dare il focus, ma non per avviare il gioco
        setOnMouseClicked(e -> requestFocus());
    }

    private void startAfterReady(KeyCode initialDir) {
        if (started) return;
        started = true;
        gameMap.setFirstLoad(false);
    
        // 1) Primo input divent i direzione iniziale
        currentDirection = initialDir;
        applyImage(currentDirection);
    
        // 2) Avvio timer frutta
        fruitManager.startFruitTimer();
    
        // 3) Ripristino listener “reali”
        setOnMouseClicked(e -> requestFocus());
        setOnKeyPressed(e -> handleKeyPress(e.getCode()));
    
        // 4) Avvio loop
        startGameLoop();
    }

    private void startGameLoop() {
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!gameOver && !flashing) {
                    // 1) Gestione input e movimento Pac-Man
                    if (storedDirection != null && isAligned(pacman) && gameMap.canMove(pacman, storedDirection)) {
                        currentDirection = storedDirection;
                        applyImage(currentDirection);
                        storedDirection = null;
                    }
                    movePacman();
                    // 2) Movimento fantasmi
                    ghostManager.moveGhosts();
                    // 3) Collisione **sempre** controllata  
                    score += ghostManager.handleGhostCollisions(pacman, PacMan.this::loseLife);
                    // 4) Disegno
                    draw();
                    // 5) Passaggio livello
                    if (gameMap.getFoods().isEmpty() && gameMap.getPowerFoodCount() == 0) {
                        nextLevel();
                    }
                }
            }
        };
        gameLoop.start();
    }


    public int getReadyRow() {
        return 11; // il numero corretto della riga dove mostri "READY!" -1
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
        if (gameOver) {
            // torniamo al menu al primo tasto
            mainMenu.returnToMenu();
            return;
        }
        if (waitingForLifeKey) {
            waitingForLifeKey = false;
            // 1) reset mappa e frutta
            gameMap.resetEntities();
            pacman = gameMap.getPacman();
            ghostManager.resetGhosts(gameMap.getGhosts(), gameMap.getGhostPortal(), gameMap.getPowerFoods());
            fruitManager.startFruitTimer();
        
            // 2) prendo proprio questo tasto come direzione iniziale
            currentDirection = key;
            applyImage(currentDirection);
        
            // 3) riavvio loop
            gameLoop.start();
            return;
        }
        
        if (waitingForRestart) {
            waitingForRestart = false;
            // 1) Reset logico del livello successivo
            gameMap.reload();                // ricarica cibo e power‐food
            gameMap.resetEntities();         // riposiziona Pac‐Man e fantasmi
            pacman = gameMap.getPacman();
            ghostManager.resetGhosts(
                gameMap.getGhosts(),
                gameMap.getGhostPortal(),
                gameMap.getPowerFoods()
            );
            fruitManager.reset();
            fruitManager.startFruitTimer();
            // 2) Usa questo primo tasto come direzione iniziale di Pac‐Man
            currentDirection = key;
            applyImage(currentDirection);
            // 3) Avvia subito il loop di gioco
            startGameLoop();
            return;
        }
        // gioco normale: registriamo la direzione
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
        fruitManager.pauseFruitTimer();
        gameLoop.stop();  // blocca sempre
        if (lives <= 0) {
            gameOver = true;
            drawGameOver();
        } else {
            waitingForLifeKey = true;
            drawLifeLost();
        }
    }
    
    private void drawLifeLost() {
        draw();  // ridisegna
        // semplice messaggio al centro
        gc.setFill(Color.WHITE);
        gc.setFont(returnKeyFont);
        String msg = "PRESS ANY KEY TO CONTINUE";
        double w = getTextWidth(msg, returnKeyFont);
        gc.fillText(msg, (BOARD_WIDTH - w) / 2, (BOARD_HEIGHT + TILE_SIZE) / 2);
    }

    private void drawGameOver() {
        draw();  // ridisegna il campo sottostante
    
        // Titolo arancione, molto grande
        gc.setFill(Color.ORANGE);
        gc.setFont(gameOverFont);
        String msg = "GAME OVER";
        double w = getTextWidth(msg, gameOverFont);
        gc.fillText(msg, (BOARD_WIDTH - w) / 2, (BOARD_HEIGHT + TILE_SIZE) / 2);
    
        // Sotto, invito in giallo, più piccolo
        String prompt = "PRESS ANY KEY TO RETURN";
        gc.setFill(Color.YELLOW);
        gc.setFont(returnKeyFont);
        double pw = getTextWidth(prompt, returnKeyFont);
        gc.fillText(prompt, (BOARD_WIDTH - pw) / 2, (BOARD_HEIGHT + TILE_SIZE) / 2 + 40);
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

    public int getCurrentLevel() {
        return level;
    }
    
}
