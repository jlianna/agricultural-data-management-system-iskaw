package application.ui;

import application.model.DairySystem;
import application.model.Inventory;
import application.model.Product;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Reusable "stock alerts" card listing products that are low or out of stock. */
public class LowStockPanel extends VBox {
    private final DairySystem system;
    private final VBox rows = new VBox(8);

    public LowStockPanel(DairySystem system) {
        this.system = system;
        getStyleClass().add("card");
        setSpacing(10);
        ScrollPane scroll = new ScrollPane(rows);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("plain-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().addAll(Ui.sectionTitle("Stock alerts"), scroll);
    }

    public void refresh() {
        rows.getChildren().clear();
        Inventory inv = system.getInventory();
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
        if (rows.getChildren().isEmpty()) rows.getChildren().add(Ui.subtle("All products are well stocked."));
    }
}
