package application.ui;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

// Temporary Iskaw logo badge until the actual business logo is finalized
public class Logo extends StackPane {

    // Constructs a circular logo badge scaled to the specified pixel dimensions
    public Logo(double size) {
        // Create a white circular backdrop matching the requested diameter
        Circle disc = new Circle(size/2, Color.web("white"));
        
        
//        Image iskawlogin = new Image(getClass().getResource("iskawlogin.jpg").toExternalForm());
//        ImageView bgView = new ImageView(iskawlogin);

        // Load the Iskaw logo asset and wrap it in an ImageView
        Image iskawlogo = new Image(getClass().getResource("iskawlogo.png").toExternalForm());
        ImageView logoView = new ImageView(iskawlogo);

//        bgView.setFitWidth(size);
//        bgView.setFitHeight(size);
        
        // Scale the logo image to fit within the container while preserving its aspect ratio
        logoView.setFitWidth(size); // 60% of the container size
        logoView.setFitHeight(size);
        logoView.setPreserveRatio(true);
        
    
        //iskaw.setStyle("-fx-font-size: " + (size * 0.52) + "px;");
        // Layer the logo over the white disc and lock the container's min and max dimensions
        getChildren().addAll(disc, logoView);
        setMaxSize(size, size);
        setMinSize(size, size);
    }
}