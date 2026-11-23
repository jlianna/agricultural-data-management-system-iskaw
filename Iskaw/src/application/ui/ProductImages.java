package application.ui;

import application.model.Product;
import application.model.ProductSystem;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds square product pictures for the shop and the product form.
 * Pictures are decoded at the size they are shown (not full resolution), cached, and cropped to fill the
 * square like the product tiles of an online shop. Products without a photo get a neutral placeholder.
 */
final class ProductImages {
    private static final Map<String, Image> CACHE = new HashMap<>();

    private ProductImages() { }

    /** A {@code size x size} square showing the product photo, or a placeholder when there is none. */
    static StackPane square(ProductSystem system, Product product, double size) {
        StackPane box = new StackPane();
        box.getStyleClass().add("photo-box");
        box.setMinSize(size, size);
        box.setPrefSize(size, size);
        box.setMaxSize(size, size);

        Label placeholder = new Label(size >= 120 ? initial(product) + "\nNo photo yet" : initial(product));
        placeholder.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        placeholder.getStyleClass().add("photo-placeholder");
        placeholder.setStyle("-fx-font-size: " + Math.max(11, Math.round(size / 4.5)) + "px;");
        box.getChildren().add(placeholder);

        Image img = load(system, product, size);
        if (img != null) {
            ImageView view = new ImageView(img);
            view.setFitWidth(size);
            view.setFitHeight(size);
            view.setPreserveRatio(false); // the viewport below is square, so nothing is stretched
            view.setSmooth(true);
            cropToSquare(img, view);
            box.getChildren().add(view);
        }
        Rectangle clip = new Rectangle(size, size);
        clip.setArcWidth(10);
        clip.setArcHeight(10);
        box.setClip(clip);
        return box;
    }

    /** A square preview of a picture file the manager just picked (not saved yet). */
    static StackPane squareFromFile(java.io.File file, double size) {
        StackPane box = new StackPane();
        box.getStyleClass().add("photo-box");
        box.setMinSize(size, size);
        box.setPrefSize(size, size);
        box.setMaxSize(size, size);
        Image img = new Image(file.toURI().toString(), size * 2, size * 2, true, true, true);
        ImageView view = new ImageView(img);
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(false);
        view.setSmooth(true);
        cropToSquare(img, view);
        box.getChildren().add(view);
        Rectangle clip = new Rectangle(size, size);
        clip.setArcWidth(10);
        clip.setArcHeight(10);
        box.setClip(clip);
        return box;
    }

    /** A cached image for the product, or null when it has no (readable) photo. */
    static Image load(ProductSystem system, Product product, double size) {
        Path file = system.getImagePath(product);
        if (file == null) return null;
        String key = file + "@" + (int) size;
        Image img = CACHE.get(key);
        if (img == null || img.isError()) {
            // decode at twice the displayed size so it stays sharp on high-DPI screens
            img = new Image(file.toUri().toString(), size * 2, size * 2, true, true, true);
            CACHE.put(key, img);
        }
        return img;
    }

    /** Drops cached pictures (called when a photo was replaced or removed). */
    static void forget() {
        CACHE.clear();
    }

    private static void cropToSquare(Image img, ImageView view) {
        Runnable crop = () -> {
            double w = img.getWidth();
            double h = img.getHeight();
            if (w <= 0 || h <= 0) return;
            double side = Math.min(w, h);
            view.setViewport(new Rectangle2D((w - side) / 2, (h - side) / 2, side, side));
        };
        if (img.getProgress() >= 1.0) crop.run();
        else img.progressProperty().addListener((o, was, now) -> {
            if (now.doubleValue() >= 1.0) crop.run();
        });
    }

    private static String initial(Product p) {
        String n = p == null ? "" : p.getName();
        return n.isEmpty() ? "?" : n.substring(0, 1).toUpperCase();
    }
}
