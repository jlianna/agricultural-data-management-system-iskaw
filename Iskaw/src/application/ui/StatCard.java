package application.ui;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Reusable "caption + big number" tile used on dashboards and the wallet screen. */
public class StatCard extends VBox {
    private final Label value = new Label();

    /** @param accent one of stat-blue, stat-butter, stat-green, stat-red */
    public StatCard(String caption, String accent) {
        getStyleClass().addAll("stat-card", accent);
        Label cap = new Label(caption);
        cap.getStyleClass().add("stat-caption");
        value.getStyleClass().add("stat-value");
        getChildren().addAll(cap, value);
    }

    public void setValue(String text) {
        value.setText(text);
    }
}
