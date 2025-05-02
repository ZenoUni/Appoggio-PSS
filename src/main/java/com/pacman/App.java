package com.pacman;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
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

        StackPane root = new StackPane();
        Scene scene = new Scene(root, BOARD_WIDTH, BOARD_HEIGHT + TILE_SIZE);

        // Font retro (PressStart2P) — assicurati che sia nei resources
        Font menuFont = Font.loadFont(getClass().getResource("/assets/fonts/PressStart2P.ttf").toExternalForm(), 14);

        // === MENÙ INIZIALE ===
        VBox menuBox = new VBox(20);
        menuBox.setAlignment(Pos.CENTER);
        menuBox.setStyle("-fx-background-color: black;");

        Button startButton = new Button("START");
        Label instructions = new Label("ISTRUZIONI");
        Label skinCloset = new Label("ARMADIO SKIN");

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
            root.getChildren().clear();
            root.getChildren().add(pacmanGame);
            pacmanGame.requestFocus();
        });

        Font menuFontSmall = Font.loadFont(getClass().getResource("/assets/fonts/PressStart2P.ttf").toExternalForm(), 14);

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

            title.setTextFill(Color.YELLOW);
            info.setTextFill(Color.WHITE);
            backButton.setTextFill(Color.YELLOW);
            backButton.setStyle("-fx-background-color: transparent;");

            instructionsBox.getChildren().addAll(title, info, backButton);

            root.getChildren().clear();
            root.getChildren().add(instructionsBox);

            backButton.setOnAction(ev -> {
                root.getChildren().clear();
                root.getChildren().add(menuBox);
            });
        });


        skinCloset.setOnMouseClicked((MouseEvent e) -> {
            System.out.println("Mostra l'armadio skin...");
            // Da implementare eventualmente
        });

        root.getChildren().add(menuBox);

        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
