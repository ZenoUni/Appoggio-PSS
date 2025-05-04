package com.pacman;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.net.URL;

public class App extends Application {
    private static final int TILE_SIZE = 32;
    private static final int ROW_COUNT = 21;
    private static final int COLUMN_COUNT = 19;
    private static final int BOARD_WIDTH = COLUMN_COUNT * TILE_SIZE;
    private static final int BOARD_HEIGHT = ROW_COUNT * TILE_SIZE;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Pac-Man");

        StackPane root = new StackPane();
        Scene scene = new Scene(root, BOARD_WIDTH, BOARD_HEIGHT + TILE_SIZE);

        // === CARICAMENTO FONT ===
        Font menuFont;
        URL fontUrl = getClass().getResource("/assets/fonts/PressStart2P.ttf");

        if (fontUrl == null) {
            System.err.println("⚠️ Font non trovato nel classpath!");
            menuFont = Font.getDefault();
        } else {
            // Provo a caricare il font
            Font loaded = Font.loadFont(fontUrl.toExternalForm(), 14);
            if (loaded == null) {
                System.err.println("⚠️ loadFont ha restituito null, ricaduta su default");
                menuFont = Font.getDefault();
            } else {
                System.out.println("✅ Font caricato da: " + fontUrl);
                menuFont = loaded;
            }
        }

        // === MENÙ INIZIALE ===
        VBox menuBox = new VBox(20);
        menuBox.setAlignment(Pos.CENTER);
        menuBox.setStyle("-fx-background-color: black;");

        Button startButton = new Button("START");
        Label instructions = new Label("ISTRUZIONI");
        Label skinCloset = new Label("ARMADIO SKIN");

        // Imposto il font solo se non è null (ma per sicurezza qui è sempre non-null)
        startButton.setFont(menuFont);
        instructions.setFont(menuFont);
        skinCloset.setFont(menuFont);

        startButton.setTextFill(Color.YELLOW);
        instructions.setTextFill(Color.WHITE);
        skinCloset.setTextFill(Color.WHITE);

        startButton.setStyle("-fx-background-color: transparent;");
        instructions.setStyle("-fx-cursor: hand;");
        skinCloset.setStyle("-fx-cursor: hand;");

        menuBox.getChildren().addAll(startButton, instructions, skinCloset);

        // === GIOCO ===
        PacMan pacmanGame = new PacMan();

        // === EVENTI ===
        startButton.setOnAction(e -> {
            root.getChildren().setAll(pacmanGame);
            pacmanGame.requestFocus();
        });

        instructions.setOnMouseClicked((MouseEvent e) -> {
            VBox instructionsBox = new VBox(20);
            instructionsBox.setAlignment(Pos.CENTER);
            instructionsBox.setStyle("-fx-background-color: black;");

            Label title = new Label("ISTRUZIONI");
            Label info = new Label("""
                    • Usa le FRECCE per muoverti •
                    • Mangia i puntini per fare punti •
                    • Evita i fantasmi •
                    • Raccogli la frutta per punti extra •
                    """);

            Button backButton = new Button("INDIETRO");

            // Qui puoi riutilizzare lo stesso menuFont, se vuoi
            title.setFont(menuFont);
            info.setFont(menuFont);
            backButton.setFont(menuFont);

            title.setTextFill(Color.YELLOW);
            info.setTextFill(Color.WHITE);
            backButton.setTextFill(Color.YELLOW);
            backButton.setStyle("-fx-background-color: transparent;");

            instructionsBox.getChildren().addAll(title, info, backButton);

            root.getChildren().setAll(instructionsBox);

            backButton.setOnAction(ev -> {
                root.getChildren().setAll(menuBox);
            });
        });

        skinCloset.setOnMouseClicked((MouseEvent e) -> {
            System.out.println("Mostra l'armadio skin...");
            // Da implementare
        });

        root.getChildren().add(menuBox);

        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.setOnCloseRequest(e -> {
            System.exit(0);
        });
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
