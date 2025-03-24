package com.pacman;

import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;
import java.util.HashSet;
import java.util.Random;

public class PacMan extends Pane {
    private static final int TILE_SIZE = 32;
    private static final int ROW_COUNT = 21;
    private static final int COLUMN_COUNT = 19;
    private static final int BOARD_WIDTH = COLUMN_COUNT * TILE_SIZE;
    private static final int BOARD_HEIGHT = ROW_COUNT * TILE_SIZE;

    private Image wallImage, pacmanUpImage, pacmanDownImage, pacmanLeftImage, pacmanRightImage;
    private Image blueGhostImage, orangeGhostImage, pinkGhostImage, redGhostImage;
    private HashSet<Block> walls, foods, ghosts;

    private AnimationTimer blinkTimer;
    private boolean blinking = false;
    private Image whiteGhostImage;

    private Block pacman;
    private GraphicsContext gc;
    private AnimationTimer gameLoop;
    private Random random = new Random();

    private int score = 0;
    private int lives = 3;
    private int level = 1;

    private boolean flashing = false;
    private KeyCode storedDirection = null;
    private boolean gameOver = false;

    private HashSet<Block> powerFoods;
    private Image powerFoodImage, scaredGhostImage;
    private boolean ghostsAreScared = false;
    private Block ghostPortal;
    private PauseTransition scaredTimer;

    private String[] tileMap = {
        "XXXXXXXXXXXXXXXXXXX",
        "X        X        X",
        "X XX XXX X XXX XX X",
        "X                 X",
        "X XX X XXXXX X XX X",
        "X    X       X    X",
        "XXXX XXXX XXXX XXXX",
        "OOOX X   r   X XOOO",
        "XXXX X XX-XX X XXXX",
        "O      XbpoX      O",
        "XXXX X XXXXX X XXXX",
        "OOOX X       X XOOO",
        "XXXX X XXXXX X XXXX",
        "X        X        X",
        "X XX XXX X XXX XX X",
        "X  X     P     X  X",
        "XX X X XXXXX X X XX",
        "X    X   X   X    X",
        "X XXXXXX X XXXXXX X",
        "X                 X",
        "XXXXXXXXXXXXXXXXXXX"
    };

    private void loadMap() {
        walls = new HashSet<>();
        foods = new HashSet<>();
        ghosts = new HashSet<>();
        powerFoods = new HashSet<>();
        
        Block redGhost = null;
        
        for (int r = 0; r < ROW_COUNT; r++) {
            for (int c = 0; c < COLUMN_COUNT; c++) {
                int x = c * TILE_SIZE;
                int y = r * TILE_SIZE;
                char tile = tileMap[r].charAt(c);
                
                if (tile == 'X') walls.add(new Block(wallImage, x, y));
                else if (tile == 'b') ghosts.add(new Block(blueGhostImage, x, y));
                else if (tile == 'o') ghosts.add(new Block(orangeGhostImage, x, y));
                else if (tile == 'p') ghosts.add(new Block(pinkGhostImage, x, y));
                else if (tile == 'r') redGhost = new Block(redGhostImage, x, y);
                else if (tile == 'P') pacman = new Block(pacmanRightImage, x, y);
                else if (tile == ' ') foods.add(new Block(null, x + TILE_SIZE / 2 - 2, y + TILE_SIZE / 2 - 2, 4, 4));
                else if (tile == '-') ghostPortal = new Block(null, x, y, TILE_SIZE, 4); 
            }
        }
        if (redGhost != null) ghosts.add(redGhost);
        powerFoods.add(new Block(powerFoodImage, TILE_SIZE, TILE_SIZE));
        powerFoods.add(new Block(powerFoodImage, BOARD_WIDTH - TILE_SIZE * 2, TILE_SIZE));
        powerFoods.add(new Block(powerFoodImage, TILE_SIZE, BOARD_HEIGHT - TILE_SIZE * 2));
        powerFoods.add(new Block(powerFoodImage, BOARD_WIDTH - TILE_SIZE * 2, BOARD_HEIGHT - TILE_SIZE * 2));
    }

    private void loadImages() {
        wallImage = new Image(getClass().getResource("/wall.png").toExternalForm());
        blueGhostImage = new Image(getClass().getResource("/blueGhost.png").toExternalForm());
        orangeGhostImage = new Image(getClass().getResource("/orangeGhost.png").toExternalForm());
        pinkGhostImage = new Image(getClass().getResource("/pinkGhost.png").toExternalForm());
        redGhostImage = new Image(getClass().getResource("/redGhost.png").toExternalForm());
        pacmanUpImage = new Image(getClass().getResource("/pacmanUp.png").toExternalForm());
        pacmanDownImage = new Image(getClass().getResource("/pacmanDown.png").toExternalForm());
        pacmanLeftImage = new Image(getClass().getResource("/pacmanLeft.png").toExternalForm());
        pacmanRightImage = new Image(getClass().getResource("/pacmanRight.png").toExternalForm());
        powerFoodImage = new Image(getClass().getResource("/powerFood.png").toExternalForm());
        scaredGhostImage = new Image(getClass().getResource("/scaredGhost.png").toExternalForm());
        whiteGhostImage = new Image(getClass().getResource("/whiteGhost.png").toExternalForm());
    }

    public PacMan() {
        Canvas canvas = new Canvas(BOARD_WIDTH, BOARD_HEIGHT);
        gc = canvas.getGraphicsContext2D();
        getChildren().add(canvas);

        loadImages();
        loadMap();

        setFocusTraversable(true);
        setOnMouseClicked(e -> requestFocus());
        setOnKeyPressed(e -> handleKeyPress(e.getCode()));

        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!gameOver && !flashing) {
                    checkStoredDirection();
                    move();
                    draw();
                    if (foods.isEmpty()) {
                        nextLevel();
                    }
                }
            }
        };
        gameLoop.start();
    }

    private void move() {
        pacman.x += pacman.velocityX;
        pacman.y += pacman.velocityY;
        if (pacman.x < -TILE_SIZE) pacman.x = BOARD_WIDTH;
        else if (pacman.x > BOARD_WIDTH) pacman.x = -TILE_SIZE;
        
        for (Block wall : walls) {
            if (collision(pacman, wall) || collision(pacman, ghostPortal)) {
                pacman.x -= pacman.velocityX;
                pacman.y -= pacman.velocityY;
                return;
            }
        }
    
        powerFoods.removeIf(powerFood -> {
            if (collision(pacman, powerFood)) {
                activateScaredMode();
                return true;
            }
            return false;
        });
    
        foods.removeIf(food -> {
            if (collision(pacman, food)) {
                score += 10;
                return true;
            }
            return false;
        });
    
        HashSet<Block> eatenGhosts = new HashSet<>();
        for (Block ghost : ghosts) {
            if (collision(pacman, ghost)) {
                if (ghostsAreScared) {
                    score += 200; // Aggiunge solo 200 punti
                    eatenGhosts.add(ghost); // Aggiunge alla lista dei fantasmi mangiati
                } else {
                    loseLife();
                    return;
                }
            }
        }
    
        // Reset di ogni fantasma mangiato
        for (Block ghost : eatenGhosts) {
            resetGhost(ghost);
        }
    }

    private void handleKeyPress(KeyCode key) {
        // Se il movimento è possibile, aggiorna subito la direzione
        if (canMove(key)) {
            applyDirection(key);
            storedDirection = null; // Reset della direzione memorizzata
        } else {
            // Memorizza il comando se non è possibile muoversi ora
            storedDirection = key;
        }
    }

    private void applyDirection(KeyCode key) {
        switch (key) {
            case UP -> pacman.updateDirection('U', pacmanUpImage);
            case DOWN -> pacman.updateDirection('D', pacmanDownImage);
            case LEFT -> pacman.updateDirection('L', pacmanLeftImage);
            case RIGHT -> pacman.updateDirection('R', pacmanRightImage);
        }
    }

    private void checkStoredDirection() {
        if (storedDirection != null) {
            if (canMove(storedDirection)) {
                handleKeyPress(storedDirection);
                storedDirection = null; // Reset della direzione memorizzata dopo il movimento
            }
        }
    }

    private boolean collision(Block a, Block b) {
        return a.x < b.x + b.width && a.x + a.width > b.x && a.y < b.y + b.height && a.y + a.height > b.y;
    }

    private boolean canMove(KeyCode key) {
        int newX = pacman.x;
        int newY = pacman.y;
        switch (key) {
            case UP -> newY -= 4;
            case DOWN -> newY += 4;
            case LEFT -> newX -= 4;
            case RIGHT -> newX += 4;
        }
        for (Block wall : walls) {
            if (collision(new Block(null, newX, newY, TILE_SIZE, TILE_SIZE), wall)) {
                return false;
            }
        }
        if (collision(new Block(null, newX, newY, TILE_SIZE, TILE_SIZE), ghostPortal)) {
            return false;
        }
        return true;
    }

    private void activateScaredMode() {
        ghostsAreScared = true;
        blinking = false;
    
        for (Block ghost : ghosts) {
            ghost.image = scaredGhostImage;
        }
    
        if (scaredTimer != null) {
            scaredTimer.stop();
        }
        if (blinkTimer != null) {
            blinkTimer.stop();
        }
    
        scaredTimer = new PauseTransition(Duration.seconds(3));
        scaredTimer.setOnFinished(e -> startBlinking());
        scaredTimer.play();
    }
    
    private void startBlinking() {
        blinking = true;
        int blinkCount = 3;
    
        blinkTimer = new AnimationTimer() {
            private long lastBlinkTime = 0;
            private int blinkTimes = 0;
    
            @Override
            public void handle(long now) {
                if (blinkTimes < blinkCount * 2) {
                    if (now - lastBlinkTime > 500_000_000) {
                        toggleGhostsBlinking();
                        lastBlinkTime = now;
                        blinkTimes++;
                    }
                } else {
                    blinkTimer.stop();
                    deactivateScaredMode();
                }
            }
        };
        blinkTimer.start();
    }
    
    private void toggleGhostsBlinking() {
        for (Block ghost : ghosts) {
            if (ghost.image == scaredGhostImage || ghost.image == whiteGhostImage) {
                ghost.image = (ghost.image == scaredGhostImage) ? whiteGhostImage : scaredGhostImage;
            }
        }
    }
    
    private void deactivateScaredMode() {
        ghostsAreScared = false;
        blinking = false;
        if (blinkTimer != null) {
            blinkTimer.stop();
        }
        resetGhostImages();
    }
    
    private void resetGhost(Block ghost) {
        ghosts.remove(ghost); 
        ghost.image = getOriginalGhostImage(ghost); 
    
        PauseTransition pause = new PauseTransition(Duration.seconds(1));
        pause.setOnFinished(e -> {
            ghost.x = COLUMN_COUNT / 2 * TILE_SIZE;
            ghost.y = (ROW_COUNT / 2 - 2) * TILE_SIZE;
            ghosts.add(ghost);
        });
        pause.play();
    }
    
    private Image getOriginalGhostImage(Block ghost) {
        int index = 0;
        for (Block g : ghosts) {
            if (g == ghost) break;
            index++;
        }
        return switch (index % 4) {
            case 0 -> blueGhostImage;
            case 1 -> orangeGhostImage;
            case 2 -> pinkGhostImage;
            default -> redGhostImage;
        };
    }    
    
    private void resetGhostImages() {
        int i = 0;
        for (Block ghost : ghosts) {
            ghost.image = switch (i % 4) {
                case 0 -> blueGhostImage;
                case 1 -> orangeGhostImage;
                case 2 -> pinkGhostImage;
                default -> redGhostImage;
            };
            i++;
        }
    }

    private Image getGhostImage(Block ghost) {
        if (ghosts.contains(ghost)) {
            int index = 0;
            for (Block g : ghosts) {
                if (g == ghost) break;
                index++;
            }
            return switch (index % 4) {
                case 0 -> blueGhostImage;
                case 1 -> orangeGhostImage;
                case 2 -> pinkGhostImage;
                default -> redGhostImage;
            };
        }
        return null;
    }

    private void resetPacmanPosition() {
        // Trova la posizione iniziale di Pac-Man nella mappa
        for (int r = 0; r < ROW_COUNT; r++) {
            for (int c = 0; c < COLUMN_COUNT; c++) {
                if (tileMap[r].charAt(c) == 'P') {
                    pacman.x = c * TILE_SIZE;
                    pacman.y = r * TILE_SIZE;
                    pacman.velocityX = 0;
                    pacman.velocityY = 0;
                    return;
                }
            }
        }
    }
    
    private void loseLife() {
        lives--; // Decrementa il numero di vite
        if (lives <= 0) {
            gameOver = true;
            gameLoop.stop();
        } else {
            resetPacmanPosition();
        }
    }

    private void nextLevel() {
        flashing = true;
        PauseTransition pause = new PauseTransition(Duration.seconds(0.5));
        pause.setOnFinished(e -> {
            walls.forEach(w -> w.image = (w.image == null) ? wallImage : null);
            draw();
        });
        pause.play();
        pause.setOnFinished(e -> {
            // Risolvi il problema della modalità "spaventata"
            if (scaredTimer != null) {
                scaredTimer.stop(); // Ferma il timer della modalità spaventata
            }
            deactivateScaredMode(); // Resetta i fantasmi alla loro forma normale
    
            walls.forEach(w -> w.image = wallImage);
            draw();
            level++;
            loadMap();
            flashing = false;
        });
    }

    private void draw() {
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, BOARD_WIDTH, BOARD_HEIGHT);
        
        // Disegna i muri
        walls.forEach(wall -> {
            if (wall.image != null) gc.drawImage(wall.image, wall.x, wall.y, TILE_SIZE, TILE_SIZE);
        });
        
        // Disegna i pallini
        gc.setFill(Color.WHITE);
        foods.forEach(food -> gc.fillRect(food.x, food.y, food.width, food.height));
        
        // Disegna Pac-Man
        gc.drawImage(pacman.image, pacman.x, pacman.y, TILE_SIZE, TILE_SIZE);
        
        // Disegna i fantasmi
        ghosts.forEach(ghost -> gc.drawImage(ghost.image, ghost.x, ghost.y, TILE_SIZE, TILE_SIZE));
        
        // Disegna le vite rimanenti con l'immagine di Pac-Man
        for (int i = 0; i < lives; i++) {
            gc.drawImage(pacmanRightImage, TILE_SIZE * (i + 1), TILE_SIZE / 2.0, TILE_SIZE / 1.5, TILE_SIZE / 1.5);
        }
        
        // Disegna il punteggio e il livello
        gc.setFill(Color.WHITE);
        gc.setFont(new Font("Arial", 18));
        gc.fillText("Score: " + score + " Level: " + level, TILE_SIZE * 5, TILE_SIZE / 2.0);

        // Disegna la linea del portale
        if (ghostPortal != null) {
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(4);
            gc.strokeLine(ghostPortal.x, ghostPortal.y + 2, ghostPortal.x + TILE_SIZE, ghostPortal.y + 2);
        }
        
        // Disegna "GAME OVER" se il gioco è finito
        if (gameOver) {
            gc.setFill(Color.ORANGE);
            gc.setFont(new Font("Arial", 50));
            gc.fillText("GAME OVER", BOARD_WIDTH / 4.0, BOARD_HEIGHT / 2.0);
        }
        powerFoods.forEach(powerFood -> gc.drawImage(powerFood.image, powerFood.x, powerFood.y, TILE_SIZE, TILE_SIZE));
    }

    private static class Block {
        int x, y, width, height, velocityX = 0, velocityY = 0;
        Image image;
        
        Block(Image image, int x, int y) {
            this(image, x, y, TILE_SIZE, TILE_SIZE);
        }
        
        Block(Image image, int x, int y, int width, int height) {
            this.image = image;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        void updateDirection(char direction, Image image) {
            this.image = image;
            velocityX = (direction == 'L' ? -4 : direction == 'R' ? 4 : 0);
            velocityY = (direction == 'U' ? -4 : direction == 'D' ? 4 : 0);
        }
    }
}
