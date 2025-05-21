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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;



public class MainMenu {

    private final Stage primaryStage;
    private final StackPane root;
    private final Scene scene;
    private final Font menuFont;
    private final ImageLoader imageLoader;


    /** Costruisce il menu principale, prepara stage, root e font e chiama buildMenu(). */
    public MainMenu(Stage stage) {
        this.primaryStage = stage;
        this.root = new StackPane();
        this.scene = new Scene(root, PacMan.BOARD_WIDTH, PacMan.BOARD_HEIGHT + PacMan.TILE_SIZE);
        this.menuFont = loadMenuFont();
        this.imageLoader = new ImageLoader();
        buildMenu();
    }

    /** Carica il font custom delle voci di menu, o usa il default in caso d’errore. */
    private Font loadMenuFont() {
        URL fontUrl = getClass().getResource("/assets/fonts/PressStart2P.ttf");
        if (fontUrl == null) return Font.getDefault();
        Font loaded = Font.loadFont(fontUrl.toExternalForm(), 14);
        return (loaded != null ? loaded : Font.getDefault());
    }

    /** Costruisce e mostra la schermata delle voci di menu e i loro gestori di evento. */
    private void buildMenu() {
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

        startButton.setOnAction(e -> launchGame());

        instructions.setOnMouseClicked((MouseEvent e) -> {
            
            VBox instructionsBox = new VBox(20);
            instructionsBox.setAlignment(Pos.CENTER);
            instructionsBox.setStyle("-fx-background-color: black;");

            Label title = new Label("ISTRUZIONI");
            title.setFont(menuFont);
            title.setTextFill(Color.YELLOW);

            VBox moveBox = new VBox(10);
            moveBox.setAlignment(Pos.CENTER);
            Label moveLabel = new Label("• Usa le FRECCE per muoverti");
            moveLabel.setFont(menuFont);
            moveLabel.setTextFill(Color.WHITE);
            ImageView controlsImage = new ImageView(imageLoader.arrowInstructionImage());
            controlsImage.setFitWidth(100);
            controlsImage.setPreserveRatio(true);
            moveBox.getChildren().addAll(moveLabel, controlsImage);

            Label bulletPoints = new Label("""
                • Mangia i dots per fare punti
                • Evita i fantasmi
                """);
            bulletPoints.setFont(menuFont);
            bulletPoints.setTextFill(Color.WHITE);
            bulletPoints.setWrapText(true);
            bulletPoints.setMaxWidth(500);

            // Sezione con immagine frutta
            HBox fruitBox = new HBox(10);
            fruitBox.setAlignment(Pos.CENTER);
            Label fruitLabel = new Label("• Raccogli la frutta per punti extra");
            fruitLabel.setFont(menuFont);
            fruitLabel.setTextFill(Color.WHITE);
            ImageView cherry = new ImageView(imageLoader.getCherryImage());
            ImageView apple = new ImageView(imageLoader.getAppleImage());
            ImageView strawberry = new ImageView(imageLoader.getStrawberryImage());
            cherry.setFitHeight(24); cherry.setPreserveRatio(true);
            apple.setFitHeight(24); apple.setPreserveRatio(true);
            strawberry.setFitHeight(24); strawberry.setPreserveRatio(true);
            fruitBox.getChildren().addAll(fruitLabel, cherry, apple, strawberry);

            // Sezione con immagine Power Pill
            HBox pillBox = new HBox(10);
            pillBox.setAlignment(Pos.CENTER);
            Label pillLabel = new Label("• Raccogli il POWER FOOD per mangiare i fantasmi temporaneamente");
            pillLabel.setFont(menuFont);
            pillLabel.setTextFill(Color.WHITE);
            ImageView pill = new ImageView(imageLoader.getPowerFoodImage());
            pill.setFitHeight(24);
            pill.setPreserveRatio(true);
            pillBox.getChildren().addAll(pillLabel, pill);

            Label livesLabel = new Label("• Hai 3 vite: se vieni toccato da un fantasma, perdi una vita");
            livesLabel.setFont(menuFont);
            livesLabel.setTextFill(Color.WHITE);
            livesLabel.setWrapText(true);
            livesLabel.setMaxWidth(500);

            Button back = new Button("INDIETRO");
            back.setFont(menuFont);
            back.setTextFill(Color.YELLOW);
            back.setStyle("-fx-background-color: transparent;");
            back.setOnAction(ev -> root.getChildren().setAll(menuBox));

            instructionsBox.getChildren().setAll(title, moveBox, bulletPoints, fruitBox, pillBox, livesLabel, back);
            root.getChildren().setAll(instructionsBox);
        });


    skinCloset.setOnMouseClicked(e -> System.out.println("ARMADIO SKIN: da implementare"));

    menuBox.getChildren().addAll(startButton, instructions, skinCloset);
    root.getChildren().setAll(menuBox);
}


    /** Inizia una nuova partita creando il pannello di gioco e sostituendo il menu. */
    private void launchGame() {
        PacMan pacmanGame = new PacMan(this); 
        root.getChildren().setAll(pacmanGame);
        pacmanGame.requestFocus();
    }

    /** Rende visibile questo menu sullo stage principale. */
    public void show() {
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    /** Torna alla schermata del menu principale ricostruendolo da zero. */
    public void returnToMenu() {
        buildMenu();
        show();
    }
}