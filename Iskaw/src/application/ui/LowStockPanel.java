package application.ui;

import application.model.ProductSystem;
import application.model.Inventory;
import application.model.Product;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

// Reusable stock alerts card listing products that are currently low or out of stock
public class LowStockPanel extends VBox {
    private final ProductSystem system;
    private final VBox rows = new VBox(8);

    // Initializes the stock alerts card layout with a title and a scrollable list container
    public LowStockPanel(ProductSystem system) {
        this.system = system;
        getStyleClass().add("card");
        setSpacing(10);

        // Configure a vertical scroll pane to hold the dynamic list of low-stock product rows
        ScrollPane scroll = new ScrollPane(rows);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("plain-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Add the section header and scroll pane to the card container
        getChildren().addAll(Ui.sectionTitle("Stock alerts"), scroll);
    }

    // Rebuilds the alert rows from current low-stock products or displays an all-clear message
    public void refresh() {
        rows.getChildren().clear();
        Inventory inv = system.getInventory();

        // Populate a row with the product name and a status badge for each low or out-of-stock item
        for (Product p : system.getLowStockProducts()) {
            Label name = new Label(p.getName() + " (" + p.getUnit() + ")");
            name.setWrapText(true);
            name.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(name, Priority.ALWAYS);

            Label badge = Ui.badge(inv.isOut(p.getId()) ? "Out of stock" : inv.getStock(p.getId()) + " left",
                    inv.isOut(p.getId()) ? "badge-out" : "badge-low");

            HBox row = new HBox(8, name, badge);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(0, 4, 0, 0));
            rows.getChildren().add(row);
        }

        // Show a fallback message when no products are low or out of stock
        if (rows.getChildren().isEmpty()) rows.getChildren().add(Ui.subtle("All products are well stocked."));
    }
}