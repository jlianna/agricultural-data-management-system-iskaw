package application.ui;

import application.model.ProductSystem;
import application.model.User;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

// Handles window switching between the login screen and the main application shell
public class SceneManager {
    private final Stage stage;
    private final ProductSystem system;

    // Initializes the primary stage title, minimum window dimensions, and application icon
    public SceneManager(Stage stage, ProductSystem system) {
        this.stage = stage;
        this.system = system;
        
        stage.setTitle("Iskaw - Dairy, Beef, and Cowhide Products");
        stage.setMinWidth(1100);
        stage.setMinHeight(680);
        stage.getIcons().add(new Image(getClass().getResource("iskawlogo.png").toExternalForm()));
    }

    // Displays the login screen and transitions to the main shell upon successful authentication
    public void showLogin() {
        setRoot(new LoginView(system, this::showShell));
    }

    // Displays the main application shell configured for the authenticated user
    public void showShell(User user) {
        setRoot(new Shell(this, system, user));
    }

    // Swaps the root node of the existing scene so the window preserves its current size and position
    private void setRoot(Parent root) {
        if (stage.getScene() == null) {
            // Create the initial scene with default dimensions and apply the global stylesheet
            Scene scene = new Scene(root, 1280, 780);
            scene.getStylesheets().add(Ui.STYLESHEET);
            stage.setScene(scene);
        } else {
            stage.getScene().setRoot(root);
        }
    }
}