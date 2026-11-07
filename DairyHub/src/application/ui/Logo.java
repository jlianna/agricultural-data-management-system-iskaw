package application.ui;

import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;

/** The DairyHub logo: a milk drop on a butter-yellow disc, drawn with shapes so no image file is needed. */
public class Logo extends StackPane {
    public Logo(double size) {
        Circle disc = new Circle(size / 2, Color.web("#f2b84b"));
        SVGPath drop = new SVGPath();
        drop.setContent("M 0 -22 C 10 -8 18 0 18 10 A 18 18 0 0 1 -18 10 C -18 0 -10 -8 0 -22 Z");
        drop.setFill(Color.WHITE);
        drop.setStroke(Color.web("#1d3047"));
        drop.setStrokeWidth(2.5);
        double scale = size / 80.0;
        drop.setScaleX(scale);
        drop.setScaleY(scale);
        getChildren().addAll(disc, drop);
        setMaxSize(size, size);
        setMinSize(size, size);
    }
}
