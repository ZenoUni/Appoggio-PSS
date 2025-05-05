package com.pacman;

import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Pac-Man");
        primaryStage.setOnCloseRequest(e -> {
            System.exit(0);
        });        
        MainMenu menu = new MainMenu(primaryStage);
        menu.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
