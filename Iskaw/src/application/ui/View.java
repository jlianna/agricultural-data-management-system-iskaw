package application.ui;

import javafx.scene.layout.VBox;

// Base class for every content screen displayed inside the Shell container
public abstract class View extends VBox {
    // Initializes the view container with the default page CSS style class
    protected View() {
        getStyleClass().add("page");
    }

    // Reloads current data from the system into the screen's UI controls whenever the view is shown
    public abstract void refresh();
}