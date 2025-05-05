package com.pacman;

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

public class MainMenu {

    private final Stage primaryStage;
    private final StackPane root;
    private final Scene scene;
    private final Font menuFont;

    public MainMenu(Stage stage) {
        this.primaryStage = stage;
        this.root = new StackPane();
        this.scene = new Scene(root, PacMan.BOARD_WIDTH, PacMan.BOARD_HEIGHT + PacMan.TILE_SIZE);
        this.menuFont = loadMenuFont();
        buildMenu();
    }

    private Font loadMenuFont() {
        URL fontUrl = getClass().getResource("/assets/fonts/PressStart2P.ttf");
        if (fontUrl == null) return Font.getDefault();
        Font loaded = Font.loadFont(fontUrl.toExternalForm(), 14);
        return (loaded != null ? loaded : Font.getDefault());
    }

    private void buildMenu() {
        VBox menuBox = new VBox(20);
        menuBox.setAlignment(Pos.CENTER);
        menuBox.setStyle("-fx-background-color: black;");

        Button startButton    = new Button("START");
        Label instructions    = new Label("ISTRUZIONI");
        Label skinCloset      = new Label("ARMADIO SKIN");

        startButton.setFont(menuFont);
        instructions.setFont(menuFont);
        skinCloset.setFont(menuFont);

        startButton.setTextFill(Color.YELLOW);
        instructions.setTextFill(Color.WHITE);
        skinCloset.setTextFill(Color.WHITE);

        startButton.setStyle("-fx-background-color: transparent;");
        instructions.setStyle("-fx-cursor: hand;");
        skinCloset.setStyle("-fx-cursor: hand;");

        // Avvia il gioco
        startButton.setOnAction(e -> launchGame());

        // Sezione Istruzioni
        instructions.setOnMouseClicked((MouseEvent e) -> {
            VBox instructionsBox = new VBox(20);
            instructionsBox.setAlignment(Pos.CENTER);
            instructionsBox.setStyle("-fx-background-color: black;");

            Label title = new Label("ISTRUZIONI");
            Label info  = new Label("""
                    • Usa le FRECCE per muoverti •
                    • Mangia i puntini per fare punti •
                    • Evita i fantasmi •
                    • Raccogli la frutta per punti extra •
                    """);
            Button back  = new Button("INDIETRO");

            title.setFont(menuFont);
            info.setFont(menuFont);
            back.setFont(menuFont);

            title.setTextFill(Color.YELLOW);
            info.setTextFill(Color.WHITE);
            back.setTextFill(Color.YELLOW);
            back.setStyle("-fx-background-color: transparent;");

            back.setOnAction(ev -> root.getChildren().setAll(menuBox));
            instructionsBox.getChildren().setAll(title, info, back);
            root.getChildren().setAll(instructionsBox);
        });

        // Skin closet (da implementare)
        skinCloset.setOnMouseClicked(e -> System.out.println("ARMADIO SKIN: da implementare"));

        menuBox.getChildren().addAll(startButton, instructions, skinCloset);
        root.getChildren().add(menuBox);
    }

    private void launchGame() {
        PacMan pacmanGame = new PacMan(this); 
        root.getChildren().setAll(pacmanGame);
        pacmanGame.requestFocus();
    }

    public void show() {
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    // Metodo richiamato da PacMan quando finisce il game over
    public void returnToMenu() {
        buildMenu();            // ricostruisce il menu da zero
        show();
    }
}
