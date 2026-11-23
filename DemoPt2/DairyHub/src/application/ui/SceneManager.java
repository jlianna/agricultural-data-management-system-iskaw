package application.ui;

import application.model.DairySystem;
import application.model.User;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Handles window switching between the login screen and the main application shell. */
public class SceneManager {
    private final Stage stage;
    private final DairySystem system;

    public SceneManager(Stage stage, DairySystem system) {
        this.stage = stage;
        this.system = system;
        stage.setTitle("DairyHub – Dairy Products Data Management System");
        stage.setMinWidth(1100);
        stage.setMinHeight(680);
    }

    public void showLogin() {
        setRoot(new LoginView(system, this::showShell));
    }

    public void showShell(User user) {
        setRoot(new Shell(this, system, user));
    }

    /** Swaps the root of the existing scene so the window keeps its size and position. */
    private void setRoot(Parent root) {
        if (stage.getScene() == null) {
            Scene scene = new Scene(root, 1280, 780);
            scene.getStylesheets().add(Ui.STYLESHEET);
            stage.setScene(scene);
        } else {
            stage.getScene().setRoot(root);
        }
    }
}
