package application;

import application.model.DairySystem;
import application.ui.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

/** Entry point of DairyHub. */
public class Main extends Application {
    private DairySystem system;

    @Override
    public void start(Stage stage) {
        system = new DairySystem();       // loads saved data (or sample data on first run)
        SceneManager scenes = new SceneManager(stage, system);
        scenes.showLogin();
        stage.show();
    }

    @Override
    public void stop() {
        if (system != null) system.save(); // final safety save when the window closes
    }

    public static void main(String[] args) {
        launch(args);
    }
}
