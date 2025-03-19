package com.pacman;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class App extends Application {
    private static final int TILE_SIZE = 32;
    private static final int ROW_COUNT = 21;
    private static final int COLUMN_COUNT = 19;
    private static final int BOARD_WIDTH = COLUMN_COUNT * TILE_SIZE;
    private static final int BOARD_HEIGHT = ROW_COUNT * TILE_SIZE;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Pac-Man");

        // Usa BorderPane come layout di base
        BorderPane root = new BorderPane();

        // Inizializza la classe del gioco (deve estendere JavaFX Canvas o Pane)
        PacMan pacmanGame = new PacMan();
        root.setCenter(pacmanGame);

        // Creazione della scena
        Scene scene = new Scene(root, BOARD_WIDTH, BOARD_HEIGHT);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        // Imposta il focus sulla scena per catturare gli input da tastiera
        pacmanGame.requestFocus();
    }

    public static void main(String[] args) {
        launch(args); // Metodo per avviare JavaFX
    }
}
