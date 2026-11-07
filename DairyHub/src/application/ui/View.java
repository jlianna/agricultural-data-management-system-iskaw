package application.ui;

import javafx.scene.layout.VBox;

/**
 * Base class for every content screen inside the {@link Shell}.
 * The shell calls {@link #refresh()} each time a screen is shown so it always displays current data.
 */
public abstract class View extends VBox {
    protected View() {
        getStyleClass().add("page");
    }

    /** Reload data from the system into the controls. */
    public abstract void refresh();
}
