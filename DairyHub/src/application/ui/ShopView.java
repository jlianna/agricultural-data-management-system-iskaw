package application.ui;

import application.model.Customer;
import application.model.DairySystem;
import application.model.Inventory;
import application.model.Order;
import application.model.Product;
import application.model.SaleLine;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The customer's shop: browse products, add them to a cart and place an order.
 * The customer pays cash when picking the order up; a clerk then fulfils it.
 */
public class ShopView extends View {
    private static final String ALL = "All categories";

    private final DairySystem system;
    private final Customer customer;
    private final Navigator nav;

    private final ObservableList<Product> items = FXCollections.observableArrayList();
    private final FilteredList<Product> filtered = new FilteredList<>(items, p -> true);
    private final TableView<Product> table = new TableView<>();
    private final TextField search = new TextField();
    private final ComboBox<String> category = new ComboBox<>();
    private final Spinner<Integer> quantity = new Spinner<>();

    private final ObservableList<SaleLine> cart = FXCollections.observableArrayList();
    private final TableView<SaleLine> cartTable = new TableView<>();
    private final Label total = new Label();

    public ShopView(DairySystem system, Customer customer, Navigator nav) {
        this.system = system;
        this.customer = customer;
        this.nav = nav;
        Inventory inv = system.getInventory();

        // ------------------------------------------------ left: catalogue
        search.setPromptText("Search products…  (double-click a product to add it)");
        HBox.setHgrow(search, Priority.ALWAYS);
        category.setPrefWidth(170);
        search.textProperty().addListener((o, a, b) -> applyFilter());
        category.setOnAction(e -> applyFilter());
        HBox tools = new HBox(10, search, category);
        tools.setAlignment(Pos.CENTER_LEFT);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No products match your search."));
        table.getColumns().add(Ui.column("Product", 170, Product::getName));
        table.getColumns().add(Ui.column("Size", 60, Product::getUnit));
        table.getColumns().add(Ui.column("Description", 190, Product::getDescription));
        TableColumn<Product, String> price = Ui.moneyColumn("Price", 80, p -> Ui.money(p.getPrice()));
        price.setComparator(Comparator.comparingDouble(s -> Double.parseDouble(s.replaceAll("[^0-9.]", ""))));
        table.getColumns().add(price);

        TableColumn<Product, String> availability = Ui.column("Availability", 105, p -> inv.statusOf(p.getId()));
        availability.setCellFactory(col -> new TableCell<Product, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null) setGraphic(null);
                else if (item.startsWith("Out")) setGraphic(Ui.badge("Sold out", "badge-out"));
                else if (item.startsWith("Low")) setGraphic(Ui.badge("Few left", "badge-low"));
                else setGraphic(Ui.badge("Available", "badge-ok"));
            }
        });
        availability.setSortable(false);
        table.getColumns().add(availability);

        SortedList<Product> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        table.setRowFactory(tv -> {
            TableRow<Product> row = new TableRow<Product>() {
                @Override
                protected void updateItem(Product p, boolean empty) {
                    super.updateItem(p, empty);
                    getStyleClass().remove("voided-row");
                    if (!empty && p != null && inv.isOut(p.getId())) getStyleClass().add("voided-row");
                }
            };
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) addToCart();
            });
            return row;
        });
        VBox.setVgrow(table, Priority.ALWAYS);

        quantity.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99, 1));
        quantity.setEditable(true);
        quantity.setPrefWidth(90);
        quantity.getEditor().focusedProperty().addListener((o, was, now) -> { if (!now) commitQuantity(); });
        HBox addRow = new HBox(10, Ui.fieldLabel("QTY"), quantity,
                Ui.button("Add to cart", "btn-primary", this::addToCart));
        addRow.setAlignment(Pos.CENTER_LEFT);

        VBox left = new VBox(10, Ui.sectionTitle("1. Pick your products"), tools, table, addRow);
        left.getStyleClass().add("card");
        HBox.setHgrow(left, Priority.ALWAYS);

        // ------------------------------------------------ right: cart
        cartTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        cartTable.setPlaceholder(new Label("Your cart is empty."));
        cartTable.getColumns().add(Ui.column("Item", 160, SaleLine::getProductName));
        cartTable.getColumns().add(Ui.moneyColumn("Qty", 45, l -> String.valueOf(l.getQuantity())));
        cartTable.getColumns().add(Ui.moneyColumn("Subtotal", 90, l -> Ui.money(l.getSubtotal())));
        cartTable.setItems(cart);
        VBox.setVgrow(cartTable, Priority.ALWAYS);

        total.getStyleClass().add("total-label");
        HBox cartButtons = new HBox(8,
                Ui.button("Remove item", "btn-danger", () -> {
                    SaleLine l = cartTable.getSelectionModel().getSelectedItem();
                    if (l != null) { cart.remove(l); updateTotal(); }
                }),
                Ui.button("Clear cart", "btn-secondary", this::clearCart));
        javafx.scene.control.Button place = Ui.button("Place order", "btn-accent", this::placeOrder);
        place.setMaxWidth(Double.MAX_VALUE);
        place.setStyle("-fx-font-size: 15px; -fx-padding: 12 16 12 16;");

        VBox right = new VBox(10, Ui.sectionTitle("2. Your cart"), cartTable, cartButtons,
                Ui.fieldLabel("ORDER TOTAL"), total,
                Ui.subtle("You pay cash at the counter when you pick up your order."), place);
        right.getStyleClass().add("card");
        right.setPrefWidth(340);
        right.setMinWidth(340);

        HBox body = new HBox(16, left, right);
        VBox.setVgrow(body, Priority.ALWAYS);
        HBox titleRow = new HBox(14, Ui.heading("Shop"),
                Ui.subtle("Add products to your cart and place an order for pick-up."));
        titleRow.setAlignment(Pos.BOTTOM_LEFT);
        getChildren().addAll(titleRow, body);
        updateTotal();
    }

    private void applyFilter() {
        String q = search.getText() == null ? "" : search.getText().trim().toLowerCase();
        String cat = category.getValue();
        filtered.setPredicate(p ->
                (cat == null || ALL.equals(cat) || p.getCategory().equals(cat))
                        && (q.isEmpty() || p.getName().toLowerCase().contains(q)
                        || p.getDescription().toLowerCase().contains(q)));
    }

    private void commitQuantity() {
        try {
            int v = Integer.parseInt(quantity.getEditor().getText().trim());
            quantity.getValueFactory().setValue(Math.max(1, Math.min(99, v)));
        } catch (NumberFormatException e) {
            quantity.getValueFactory().setValue(1);
        }
        quantity.getEditor().setText(String.valueOf(quantity.getValue()));
    }

    private void addToCart() {
        Product p = table.getSelectionModel().getSelectedItem();
        if (p == null) { Ui.info("Select a product", "Click a product in the list first."); return; }
        commitQuantity();
        int qty = quantity.getValue();
        int inCart = cart.stream().filter(l -> l.getProductId().equals(p.getId())).mapToInt(SaleLine::getQuantity).sum();
        int available = system.getInventory().getStock(p.getId());
        if (available == 0) { Ui.error(p.getName() + " is sold out right now."); return; }
        if (inCart + qty > available) {
            Ui.error("Only " + available + " of \"" + p.getName() + "\" available"
                    + (inCart > 0 ? " (" + inCart + " already in your cart)." : "."));
            return;
        }
        cart.removeIf(l -> l.getProductId().equals(p.getId()));
        cart.add(new SaleLine(p, inCart + qty));
        quantity.getValueFactory().setValue(1);
        updateTotal();
    }

    private void updateTotal() {
        total.setText(Ui.money(cart.stream().mapToDouble(SaleLine::getSubtotal).sum()));
    }

    private void clearCart() {
        cart.clear();
        updateTotal();
    }

    private void placeOrder() {
        if (cart.isEmpty()) { Ui.error("Your cart is empty. Add at least one product first."); return; }
        Ui.attempt(() -> {
            Order o = system.placeOrder(customer, new ArrayList<>(cart));
            clearCart();
            Ui.info("Order placed", "Order " + o.getId() + " (" + Ui.money(o.getTotal()) + ") has been sent to the cooperative.\n\n"
                    + "Pick it up at the counter and pay cash. A clerk will confirm it, and it will then appear in your purchases.");
            nav.go("orders");
        });
    }

    @Override
    public void refresh() {
        items.setAll(system.getProducts());
        String keep = category.getValue();
        List<String> cats = new ArrayList<>();
        cats.add(ALL);
        cats.addAll(system.getCategories());
        category.setItems(FXCollections.observableArrayList(cats));
        category.setValue(keep != null && cats.contains(keep) ? keep : ALL);
        // drop cart lines that can no longer be fulfilled from current stock
        cart.removeIf(l -> system.getInventory().getStock(l.getProductId()) < l.getQuantity());
        updateTotal();
        table.refresh();
    }
}
