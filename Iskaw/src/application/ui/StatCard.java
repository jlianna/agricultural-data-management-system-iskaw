package application.ui;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

// Reusable summary tile displaying a caption label above a prominent metric value
public class StatCard extends VBox {
    private final Label value = new Label();

    // Initializes the stat card with a descriptive caption and an accent color style class (stat-blue, stat-butter, stat-green, or stat-red)
    public StatCard(String caption, String accent) {
        getStyleClass().addAll("stat-card", accent);
        Label cap = new Label(caption);
        cap.getStyleClass().add("stat-caption");
        value.getStyleClass().add("stat-value");
        getChildren().addAll(cap, value);
    }

    // Updates the displayed metric value text
    public void setValue(String text) {
        value.setText(text);
    }
}