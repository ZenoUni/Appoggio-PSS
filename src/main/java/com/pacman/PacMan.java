package com.pacman;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.HashSet;
import java.util.Random;

public class PacMan extends Pane {
    private static final int TILE_SIZE = 32;
    private static final int ROW_COUNT = 21;
    private static final int COLUMN_COUNT = 19;
    private static final int BOARD_WIDTH = COLUMN_COUNT * TILE_SIZE;
    private static final int BOARD_HEIGHT = ROW_COUNT * TILE_SIZE;

    private Image wallImage;
    private Image pacmanUpImage, pacmanDownImage, pacmanLeftImage, pacmanRightImage;
    private Image blueGhostImage, orangeGhostImage, pinkGhostImage, redGhostImage;

    private String[] tileMap = {
            "XXXXXXXXXXXXXXXXXXX",
            "X        X        X",
            "X XX XXX X XXX XX X",
            "X                 X",
            "X XX X XXXXX X XX X",
            "X    X       X    X",
            "XXXX XXXX XXXX XXXX",
            "OOOX X       X XOOO",
            "XXXX X XXrXX X XXXX",
            "O       bpo       O",
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

    private HashSet<Block> walls, foods, ghosts;
    private Block pacman;
    private GraphicsContext gc;
    private AnimationTimer gameLoop;
    private Random random = new Random();
    private int score = 0;
    private int lives = 3;
    private boolean gameOver = false;

    public PacMan() {
        Canvas canvas = new Canvas(BOARD_WIDTH, BOARD_HEIGHT);
        gc = canvas.getGraphicsContext2D();
        getChildren().add(canvas);

        loadImages();
        loadMap();

        // Gestione della tastiera
        setFocusTraversable(true);
        setOnKeyPressed(e -> handleKeyPress(e.getCode()));

        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!gameOver) {
                    move();
                    draw();
                }
            }
        };
        gameLoop.start();
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
    }

    private void loadMap() {
        walls = new HashSet<>();
        foods = new HashSet<>();
        ghosts = new HashSet<>();

        for (int r = 0; r < ROW_COUNT; r++) {
            for (int c = 0; c < COLUMN_COUNT; c++) {
                int x = c * TILE_SIZE;
                int y = r * TILE_SIZE;
                char tile = tileMap[r].charAt(c);

                if (tile == 'X') walls.add(new Block(wallImage, x, y));
                else if (tile == 'b') ghosts.add(new Block(blueGhostImage, x, y));
                else if (tile == 'o') ghosts.add(new Block(orangeGhostImage, x, y));
                else if (tile == 'p') ghosts.add(new Block(pinkGhostImage, x, y));
                else if (tile == 'r') ghosts.add(new Block(redGhostImage, x, y));
                else if (tile == 'P') pacman = new Block(pacmanRightImage, x, y);
                //else if (tile == ' ') foods.add(new Block(null, x + 14, y + 14, 4, 4));
            }
        }
    }

    private void draw() {
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, BOARD_WIDTH, BOARD_HEIGHT);

        walls.forEach(wall -> gc.drawImage(wall.image, wall.x, wall.y, TILE_SIZE, TILE_SIZE));
        foods.forEach(food -> {
            gc.setFill(Color.WHITE);
            gc.fillRect(food.x, food.y, food.width, food.height);
        });

        gc.drawImage(pacman.image, pacman.x, pacman.y, TILE_SIZE, TILE_SIZE);
        ghosts.forEach(ghost -> gc.drawImage(ghost.image, ghost.x, ghost.y, TILE_SIZE, TILE_SIZE));

        gc.setFill(Color.WHITE);
        gc.setFont(new Font("Arial", 18));
        gc.fillText("Lives: " + lives + " Score: " + score, TILE_SIZE / 2.0, TILE_SIZE / 2.0);

        if (gameOver) {
            gc.setFill(Color.RED);
            gc.fillText("Game Over! Press any key to restart.", BOARD_WIDTH / 3.0, BOARD_HEIGHT / 2.0);
        }
    }

    private void move() {
        pacman.x += pacman.velocityX;
        pacman.y += pacman.velocityY;

        walls.forEach(wall -> {
            if (collision(pacman, wall)) {
                pacman.x -= pacman.velocityX;
                pacman.y -= pacman.velocityY;
            }
        });

        ghosts.forEach(ghost -> {
            if (collision(ghost, pacman)) {
                lives--;
                if (lives == 0) gameOver = true;
                resetPositions();
            }
        });

        foods.removeIf(food -> collision(pacman, food));
        if (foods.isEmpty()) {
            loadMap();
            resetPositions();
        }
    }

    private boolean collision(Block a, Block b) {
        return a.x < b.x + b.width && a.x + a.width > b.x && a.y < b.y + b.height && a.y + a.height > b.y;
    }

    private void resetPositions() {
        pacman.x = pacman.startX;
        pacman.y = pacman.startY;
        pacman.velocityX = 0;
        pacman.velocityY = 0;
    }

    private void handleKeyPress(KeyCode key) {
        if (gameOver) {
            loadMap();
            resetPositions();
            lives = 3;
            score = 0;
            gameOver = false;
            return;
        }

        switch (key) {
            case UP -> pacman.updateDirection('U', pacmanUpImage);
            case DOWN -> pacman.updateDirection('D', pacmanDownImage);
            case LEFT -> pacman.updateDirection('L', pacmanLeftImage);
            case RIGHT -> pacman.updateDirection('R', pacmanRightImage);
        }
    }

    private static class Block {
        int x, y, width = TILE_SIZE, height = TILE_SIZE, startX, startY;
        int velocityX = 0, velocityY = 0;
        Image image;
        char direction = 'R';

        Block(Image image, int x, int y) {
            this.image = image;
            this.x = this.startX = x;
            this.y = this.startY = y;
        }

        void updateDirection(char direction, Image image) {
            this.direction = direction;
            this.image = image;
            velocityX = (direction == 'L' ? -4 : direction == 'R' ? 4 : 0);
            velocityY = (direction == 'U' ? -4 : direction == 'D' ? 4 : 0);
        }
    }
}