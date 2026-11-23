package application.ui;

import application.model.DairySystem;
import application.model.Inventory;
import application.model.Product;
import application.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Inventory screen: everyone can check stock levels; only managers can restock. */
public class InventoryView extends View {
    private static final String ALL = "All categories";

    private final DairySystem system;
    private final User user;
    private final ObservableList<Product> items = FXCollections.observableArrayList();
    private final FilteredList<Product> filtered = new FilteredList<>(items, p -> true);
    private final TableView<Product> table = new TableView<>();
    private final TextField search = new TextField();
    private final ComboBox<String> category = new ComboBox<>();
    private final StatCard total = new StatCard("Products", "stat-blue");
    private final StatCard healthy = new StatCard("In stock", "stat-green");
    private final StatCard low = new StatCard("Low stock", "stat-butter");
    private final StatCard out = new StatCard("Out of stock", "stat-red");

    public InventoryView(DairySystem system, User user) {
        this.system = system;
        this.user = user;
        Inventory inv = system.getInventory();

        HBox cards = new HBox(16, total, healthy, low, out);
        for (StatCard c : new StatCard[]{total, healthy, low, out}) {
            HBox.setHgrow(c, Priority.ALWAYS);
            c.setMaxWidth(Double.MAX_VALUE);
        }

        search.setPromptText("Search products…");
        search.setPrefWidth(240);
        category.setPrefWidth(180);
        search.textProperty().addListener((o, a, b) -> applyFilter());
        category.setOnAction(e -> applyFilter());

        HBox tools = new HBox(12, search, category, Ui.spacer());
        tools.setAlignment(Pos.CENTER_LEFT);
        if (user.isManager()) tools.getChildren().add(Ui.button("Restock selected", "btn-accent", this::restock));

        // ---- table
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new javafx.scene.control.Label("No products match your search."));
        table.getColumns().add(Ui.column("Product", 190, Product::getName));
        table.getColumns().add(Ui.column("Category", 110, Product::getCategory));
        table.getColumns().add(Ui.column("Size", 70, Product::getUnit));
        table.getColumns().add(Ui.column("Description", 240, Product::getDescription));
        TableColumn<Product, String> price = Ui.moneyColumn("Price", 80, p -> Ui.money(p.getPrice()));
        price.setComparator(Comparator.comparingDouble(s -> Double.parseDouble(s.replaceAll("[^0-9.]", ""))));
        table.getColumns().add(price);
        TableColumn<Product, String> stock = Ui.moneyColumn("In stock", 70, p -> String.valueOf(inv.getStock(p.getId())));
        stock.setComparator(Comparator.comparingInt(Integer::parseInt));
        table.getColumns().add(stock);

        TableColumn<Product, String> status = Ui.column("Status", 100, p -> inv.statusOf(p.getId()));
        status.setCellFactory(col -> new TableCell<Product, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    String style = item.startsWith("Out") ? "badge-out" : item.startsWith("Low") ? "badge-low" : "badge-ok";
                    setGraphic(Ui.badge(item, style));
                }
            }
        });
        table.getColumns().add(status);

        SortedList<Product> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        VBox.setVgrow(table, Priority.ALWAYS);

        String hint = user.isManager()
                ? "Check stock levels and restock products."
                : "Check stock levels before selling. Ask a manager to restock.";
        HBox titleRow = new HBox(14, Ui.heading("Inventory"), Ui.subtle(hint));
        titleRow.setAlignment(Pos.BOTTOM_LEFT);

        getChildren().addAll(titleRow, cards, tools, table);
    }

    private void applyFilter() {
        String q = search.getText() == null ? "" : search.getText().trim().toLowerCase();
        String cat = category.getValue();
        filtered.setPredicate(p ->
                (cat == null || ALL.equals(cat) || p.getCategory().equals(cat))
                        && (q.isEmpty() || p.getName().toLowerCase().contains(q)
                        || p.getDescription().toLowerCase().contains(q)));
    }

    private void restock() {
        Product p = table.getSelectionModel().getSelectedItem();
        if (p == null) {
            Ui.info("Select a product", "Choose the product you want to restock first.");
            return;
        }
        TextField qty = new TextField();
        qty.setPromptText("e.g. 24");
        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.setPadding(new Insets(10));
        form.addRow(0, Ui.fieldLabel("Product"), new javafx.scene.control.Label(p.toString()));
        form.addRow(1, Ui.fieldLabel("Current stock"),
                new javafx.scene.control.Label(String.valueOf(system.getInventory().getStock(p.getId()))));
        form.addRow(2, Ui.fieldLabel("Add quantity"), qty);
        boolean done = FormDialog.show("Restock " + p.getName(), "Restock", form,
                () -> Ui.attempt(() -> system.restock(user, p, Ui.parseWhole(qty.getText(), "Quantity"))));
        if (done) refresh();
    }

    @Override
    public void refresh() {
        Inventory inv = system.getInventory();
        List<Product> all = system.getProducts();
        items.setAll(all);

        String keep = category.getValue();
        List<String> cats = new ArrayList<>();
        cats.add(ALL);
        cats.addAll(system.getCategories());
        category.setItems(FXCollections.observableArrayList(cats));
        category.setValue(keep != null && cats.contains(keep) ? keep : ALL);

        int lowCount = 0, outCount = 0;
        for (Product p : all) {
            if (inv.isOut(p.getId())) outCount++;
            else if (inv.isLow(p.getId())) lowCount++;
        }
        total.setValue(String.valueOf(all.size()));
        healthy.setValue(String.valueOf(all.size() - lowCount - outCount));
        low.setValue(String.valueOf(lowCount));
        out.setValue(String.valueOf(outCount));
        table.refresh();
    }
}
