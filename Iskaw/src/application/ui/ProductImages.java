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

// Utility class for building, caching, and center-cropping square product image tiles and placeholders
final class ProductImages {
    private static final Map<String, Image> CACHE = new HashMap<>();

    // Private constructor to prevent instantiation of this utility class
    private ProductImages() { }

    // Builds a square container displaying the product's photo or a styled initial placeholder
    static StackPane square(ProductSystem system, Product product, double size) {
        // Initialize a fixed-size square container for the product thumbnail
        StackPane box = new StackPane();
        box.getStyleClass().add("photo-box");
        box.setMinSize(size, size);
        box.setPrefSize(size, size);
        box.setMaxSize(size, size);

        // Create a fallback placeholder label showing the product's initial (and text hint for larger tiles)
        Label placeholder = new Label(size >= 120 ? initial(product) + "\nNo photo yet" : initial(product));
        placeholder.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        placeholder.getStyleClass().add("photo-placeholder");
        placeholder.setStyle("-fx-font-size: " + Math.max(11, Math.round(size / 4.5)) + "px;");
        box.getChildren().add(placeholder);

        // Load and overlay the actual product image if available, cropping it cleanly to a square
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

        // Apply rounded corners to the image tile using a clipped rectangle
        Rectangle clip = new Rectangle(size, size);
        clip.setArcWidth(10);
        clip.setArcHeight(10);
        box.setClip(clip);
        return box;
    }

    // Builds a square preview tile directly from an unsaved local image file selected by the manager
    static StackPane squareFromFile(java.io.File file, double size) {
        // Initialize a fixed-size square container for the image preview
        StackPane box = new StackPane();
        box.getStyleClass().add("photo-box");
        box.setMinSize(size, size);
        box.setPrefSize(size, size);
        box.setMaxSize(size, size);

        // Decode the selected file at twice the target display resolution and crop it into a square
        Image img = new Image(file.toURI().toString(), size * 2, size * 2, true, true, true);
        ImageView view = new ImageView(img);
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(false);
        view.setSmooth(true);
        cropToSquare(img, view);
        box.getChildren().add(view);

        // Apply rounded corner clipping to the preview tile
        Rectangle clip = new Rectangle(size, size);
        clip.setArcWidth(10);
        clip.setArcHeight(10);
        box.setClip(clip);
        return box;
    }

    // Loads and caches a scaled image for the given product, or returns null if no valid photo exists
    static Image load(ProductSystem system, Product product, double size) {
        Path file = system.getImagePath(product);
        if (file == null) return null;

        // Look up the image in cache by file path and target size, decoding at 2x resolution if missing
        String key = file + "@" + (int) size;
        Image img = CACHE.get(key);
        if (img == null || img.isError()) {
            // decode at twice the displayed size so it stays sharp on high-DPI screens
            img = new Image(file.toUri().toString(), size * 2, size * 2, true, true, true);
            CACHE.put(key, img);
        }
        return img;
    }

    // Clears all cached product images so updated or removed photos reload cleanly
    static void forget() {
        CACHE.clear();
    }

    // Computes a centered square viewport on the ImageView once the image finishes loading
    private static void cropToSquare(Image img, ImageView view) {
        Runnable crop = () -> {
            double w = img.getWidth();
            double h = img.getHeight();
            if (w <= 0 || h <= 0) return;
            double side = Math.min(w, h);
            view.setViewport(new Rectangle2D((w - side) / 2, (h - side) / 2, side, side));
        };

        // Apply the crop immediately if already loaded, or wait for asynchronous loading to finish
        if (img.getProgress() >= 1.0) crop.run();
        else img.progressProperty().addListener((o, was, now) -> {
            if (now.doubleValue() >= 1.0) crop.run();
        });
    }

    // Extracts the uppercase first letter of the product's name for placeholder tiles
    private static String initial(Product p) {
        String n = p == null ? "" : p.getName();
        return n.isEmpty() ? "?" : n.substring(0, 1).toUpperCase();
    }
}