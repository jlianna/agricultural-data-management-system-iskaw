package application.ui;

import application.model.ProductSystem;
import application.model.User;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/** Handles window switching between the login screen and the main application shell. */
public class SceneManager {
    private final Stage stage;
    private final ProductSystem system;

    public SceneManager(Stage stage, ProductSystem system) {
        this.stage = stage;
        this.system = system;
        
        stage.setTitle("Iskaw - Dairy, Beef, and Cowhide Products");
        stage.setMinWidth(1100);
        stage.setMinHeight(680);
        stage.getIcons().add(new Image(getClass().getResource("iskawlogo.png").toExternalForm()));
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
