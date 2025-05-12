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
    public static final int ROW_COUNT    = 22;
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
    private final Font messageFont;
    private final Font gameOverFont;      // per il “GAME OVER” grande
    private final Font returnKeyFont;     // per il “PRESS ANY KEY” piccolo
    private final MainMenu mainMenu;

    private int  animationCounter = 0;
    private boolean mouthOpen     = true;

    private final SoundManager soundManager = new SoundManager();


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

        soundManager.loadSound("start", "sounds/start.wav");
        soundManager.loadSound("death", "sounds/death.wav");
        soundManager.loadSound("dot", "sounds/dot.wav");
        soundManager.loadSound("fruit", "sounds/fruit.wav");
        soundManager.loadSound("eat_ghost", "sounds/eat_ghost.wav");

    }

    /** Avvia il gioco solo se premi una freccia */
    private void startAfterReady(KeyCode initialDir) {
        soundManager.playSound("start");

        // Ignora se già avviato o tasto non direzionale
        if (started) return;
        if (keyToDir(initialDir) == null) return;

        started = true;
        gameMap.setFirstLoad(false);

        // 1) Primo input diventa direzione iniziale
        currentDirection = initialDir;
        applyImage(currentDirection);

        // 2) Avvio timer frutta e fantasmi
        fruitManager.startFruitTimer();
        ghostManager.startCageTimers();

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
                    if (storedDirection != null && gameMap.canMove(pacman, storedDirection)) {
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

    private void movePacman() {
        if (currentDirection == null) return;

        // ripetiamo N volte il passo base (4px) in base al moltiplicatore intero
        int steps = (int) Math.round(speedMultiplier);
        for (int s = 0; s < steps; s++) {
            if (!gameMap.canMove(pacman, currentDirection)) break;
            // passo base
            switch (currentDirection) {
                case UP    -> pacman.y -= 4;
                case DOWN  -> pacman.y += 4;
                case LEFT  -> pacman.x -= 4;
                case RIGHT -> pacman.x += 4;
                default    -> {}
            }
        }

        // animazione bocca invariata
        animationCounter++;
        if (animationCounter >= 10) {
            mouthOpen = !mouthOpen;
            animationCounter = 0;
        }
        applyImage(currentDirection);

        // tunnel e raccolta cibo/frutta rimangono identici
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

        int foodScore = gameMap.collectFood(pacman);
        if (foodScore > 0) {
            soundManager.playSound("dot");
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
            soundManager.playSound("fruit");

        }

    }


    /** Converte una freccia da tastiera in Direction */
    private Direction keyToDir(KeyCode k) {
        return switch (k) {
            case UP    -> Direction.UP;
            case DOWN  -> Direction.DOWN;
            case LEFT  -> Direction.LEFT;
            case RIGHT -> Direction.RIGHT;
            default    -> null;
        };
    }

    /** Rende visibile a GhostManager la posizione di Pac-Man */
    public Block getPacmanBlock() {
        return pacman;
    }

    /** Rende visibile a GhostManager la direzione corrente di Pac-Man */
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
        // Se il gioco è finito, torna al menu
        if (gameOver) {
            mainMenu.returnToMenu();
            return;
        }
    
        // Durante il recupero di una vita persa, ignora tutto tranne le frecce
        if (waitingForLifeKey) {
            waitingForLifeKey = false;
    
            // 1) reset mappa e frutta
            gameMap.resetEntities();
            pacman = gameMap.getPacman();
    
            // 2) ripristina i fantasmi (tutti immobili) e poi avvia il timer
            ghostManager.resetGhosts(
                gameMap.getGhosts(),
                gameMap.getGhostPortal(),
                gameMap.getPowerFoods()
            );
            ghostManager.startCageTimers();
    
            fruitManager.startFruitTimer();
    
            // 3) direzione iniziale (solo se è freccia)
            if (keyToDir(key) != null) {
                currentDirection = key;
                applyImage(currentDirection);
            }
    
            // 4) riprendi loop
            gameLoop.start();
            return;
        }
    
        // Durante il restart del livello, ignora tutto tranne le frecce
        if (waitingForRestart) {
            waitingForRestart = false;
    
            // 1) reset livello
            gameMap.reload();
            gameMap.resetEntities();
            pacman = gameMap.getPacman();
    
            // 2) reset fantasmi immobilizzati, poi avvia il timer
            ghostManager.resetGhosts(
                gameMap.getGhosts(),
                gameMap.getGhostPortal(),
                gameMap.getPowerFoods()
            );
            ghostManager.startCageTimers();
    
            fruitManager.reset();
            fruitManager.startFruitTimer();
    
            // 3) direzione iniziale (solo se è freccia)
            if (keyToDir(key) != null) {
                currentDirection = key;
                applyImage(currentDirection);
            }
    
            // 4) riavvia loop
            startGameLoop();
            return;
        }
    
        // Nel gioco normale, accetta solo le frecce; ignora tutti gli altri tasti
        if (keyToDir(key) != null) {
            storedDirection = key;
        }
        // altrimenti non fare nulla
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

    // Nel tuo PacMan.java, trova e sostituisci il metodo loseLife() con questa versione:

    private void loseLife() {

        soundManager.playSound("death");

        // **Resetta subito il superpotere se era attivo**
        setSpeedMultiplier(1.0);
        ghostManager.unfreeze();

        lives--;
        fruitManager.pauseFruitTimer();
        gameLoop.stop();
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
        setSpeedMultiplier(1.0);
        ghostManager.unfreeze();

        level++;
        flashing = true;
        // Aggiunta vita ogni 3 livelli, fino a un massimo di 3
        if (level % 3 == 1 && lives < 3) {
            lives++;
        }
        flashing = true;
        gameMap.flashWalls(() -> {
            gameMap.reload();
            pacman = gameMap.resetPacman();
            ghostManager.resetGhosts(
                gameMap.getGhosts(),
                gameMap.getGhostPortal(),
                gameMap.getPowerFoods()
            );
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

    public void setSpeedMultiplier(double m) {
        this.speedMultiplier = m;
    }

    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    public void freezeGhosts(long durationMs) {
    ghostManager.freeze(durationMs);
}

}
