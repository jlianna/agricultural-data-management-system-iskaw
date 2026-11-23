package application.ui;

import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/** Temporary Iskaw logo until the actual business logo is added. */
public class Logo extends StackPane {
    public Logo(double size) {
        // Use a simple cow symbol for now because Iskaw sells products from cattle
    	Circle disc = new Circle(size/2, Color.web("white"));
    	
    	
//    	Image iskawlogin = new Image(getClass().getResource("iskawlogin.jpg").toExternalForm());
//    	ImageView bgView = new ImageView(iskawlogin);

    	Image iskawlogo = new Image(getClass().getResource("iskawlogo.png").toExternalForm());
    	ImageView logoView = new ImageView(iskawlogo);

//        bgView.setFitWidth(size);
//        bgView.setFitHeight(size);
        
        logoView.setFitWidth(size); // 60% of the container size
        logoView.setFitHeight(size);
        logoView.setPreserveRatio(true);
        
    
        //iskaw.setStyle("-fx-font-size: " + (size * 0.52) + "px;");
        getChildren().addAll(disc, logoView);
        setMaxSize(size, size);
        setMinSize(size, size);
    }
}
