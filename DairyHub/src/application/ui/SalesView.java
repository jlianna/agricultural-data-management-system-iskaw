package application.ui;

import application.model.DairySystem;
import application.model.Product;
import application.model.SaleLine;
import application.model.SaleTransaction;
import application.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Point-of-sale screen: pick products, build a cart, take cash and record the sale. */
public class SalesView extends View {
    private final DairySystem system;
    private final User user;

    private final ObservableList<Product> catalog = FXCollections.observableArrayList();
    private final FilteredList<Product> visible = new FilteredList<>(catalog, p -> true);
    private final TableView<Product> productTable = new TableView<>();
    private final Spinner<Integer> quantity = new Spinner<>();
    private final TextField search = new TextField();

    private final ObservableList<SaleLine> cart = FXCollections.observableArrayList();
    private final TableView<SaleLine> cartTable = new TableView<>();
    private final TextField customer = new TextField();
    private final TextField cash = new TextField();
    private final Label total = new Label();
    private final Label change = new Label();

    public SalesView(DairySystem system, User user, Navigator nav) {
        this.system = system;
        this.user = user;

        // ------------------------------------------------ left: product picker
        search.setPromptText("Search products…  (double-click a product to add it)");
        search.textProperty().addListener((o, a, b) -> {
            String q = b == null ? "" : b.trim().toLowerCase();
            visible.setPredicate(p -> q.isEmpty() || p.getName().toLowerCase().contains(q)
                    || p.getCategory().toLowerCase().contains(q));
        });

        productTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        productTable.setPlaceholder(new Label("No products found."));
        productTable.getColumns().add(Ui.column("Product", 210, Product::getName));
        productTable.getColumns().add(Ui.column("Size", 70, Product::getUnit));
        productTable.getColumns().add(Ui.moneyColumn("Price", 80, p -> Ui.money(p.getPrice())));
        productTable.getColumns().add(Ui.moneyColumn("In stock", 70, p -> String.valueOf(stockOf(p))));
        productTable.setItems(visible);
        productTable.setRowFactory(tv -> {
            TableRow<Product> row = new TableRow<Product>() {
                @Override
                protected void updateItem(Product p, boolean empty) {
                    super.updateItem(p, empty);
                    getStyleClass().remove("voided-row");
                    if (!empty && p != null && stockOf(p) == 0) getStyleClass().add("voided-row");
                }
            };
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) addToCart();
            });
            return row;
        });
        VBox.setVgrow(productTable, Priority.ALWAYS);

        quantity.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1));
        quantity.setEditable(true);
        quantity.setPrefWidth(90);
        quantity.getEditor().focusedProperty().addListener((o, was, now) -> { if (!now) commitQuantity(); });

        HBox addRow = new HBox(10, Ui.fieldLabel("QTY"), quantity,
                Ui.button("Add to cart", "btn-primary", this::addToCart));
        addRow.setAlignment(Pos.CENTER_LEFT);

        VBox left = new VBox(10, Ui.sectionTitle("1. Choose products"), search, productTable, addRow);
        left.getStyleClass().add("card");
        HBox.setHgrow(left, Priority.ALWAYS);

        // ------------------------------------------------ right: cart & payment
        cartTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        cartTable.setPlaceholder(new Label("The cart is empty."));
        cartTable.getColumns().add(Ui.column("Item", 170, SaleLine::getProductName));
        cartTable.getColumns().add(Ui.moneyColumn("Qty", 45, l -> String.valueOf(l.getQuantity())));
        cartTable.getColumns().add(Ui.moneyColumn("Subtotal", 90, l -> Ui.money(l.getSubtotal())));
        cartTable.setItems(cart);
        cartTable.setPrefHeight(200);
        VBox.setVgrow(cartTable, Priority.ALWAYS);

        customer.setPromptText("Walk-in customer");
        cash.setPromptText("0.00");
        cash.textProperty().addListener((o, a, b) -> updateTotals());
        total.getStyleClass().add("total-label");
        change.getStyleClass().add("change-label");

        Button exact = Ui.button("Exact", "btn-secondary", () -> cash.setText(String.format("%.2f", cartTotal())));
        HBox cashRow = new HBox(8, cash, exact);
        HBox.setHgrow(cash, Priority.ALWAYS);

        Button remove = Ui.button("Remove item", "btn-danger", () -> {
            SaleLine l = cartTable.getSelectionModel().getSelectedItem();
            if (l != null) { cart.remove(l); updateTotals(); }
        });
        Button clear = Ui.button("Clear cart", "btn-secondary", this::clearCart);
        HBox cartButtons = new HBox(8, remove, clear);

        Button complete = Ui.button("Complete sale", "btn-accent", this::completeSale);
        complete.setMaxWidth(Double.MAX_VALUE);
        complete.setStyle("-fx-font-size: 15px; -fx-padding: 12 16 12 16;");

        VBox right = new VBox(10, Ui.sectionTitle("2. Cart & payment"), cartTable, cartButtons,
                Ui.fieldLabel("CUSTOMER (OPTIONAL)"), customer,
                Ui.fieldLabel("TOTAL DUE"), total,
                Ui.fieldLabel("CASH RECEIVED"), cashRow, change, complete);
        right.getStyleClass().add("card");
        right.setPrefWidth(380);
        right.setMinWidth(380);

        HBox body = new HBox(16, left, right);
        VBox.setVgrow(body, Priority.ALWAYS);
        HBox titleRow = new HBox(14, Ui.heading(user.isManager() ? "Record sale" : "New sale"),
                Ui.subtle("Cash purchases only. Stock is deducted automatically."));
        titleRow.setAlignment(Pos.BOTTOM_LEFT);
        getChildren().addAll(titleRow, body);
        setPadding(new Insets(26, 30, 26, 30));
        updateTotals();
    }

    private int stockOf(Product p) {
        return system.getInventory().getStock(p.getId());
    }

    private double cartTotal() {
        return cart.stream().mapToDouble(SaleLine::getSubtotal).sum();
    }

    /** Makes sure a typed-in quantity is applied (and valid) before it is read. */
    private void commitQuantity() {
        try {
            int v = Integer.parseInt(quantity.getEditor().getText().trim());
            quantity.getValueFactory().setValue(Math.max(1, Math.min(999, v)));
        } catch (NumberFormatException e) {
            quantity.getValueFactory().setValue(1);
        }
        quantity.getEditor().setText(String.valueOf(quantity.getValue()));
    }

    private void addToCart() {
        Product p = productTable.getSelectionModel().getSelectedItem();
        if (p == null) { Ui.info("Select a product", "Click a product in the list first."); return; }
        commitQuantity();
        int qty = quantity.getValue();
        int inCart = cart.stream().filter(l -> l.getProductId().equals(p.getId())).mapToInt(SaleLine::getQuantity).sum();
        int available = stockOf(p);
        if (available == 0) { Ui.error(p.getName() + " is out of stock."); return; }
        if (inCart + qty > available) {
            Ui.error("Only " + available + " of \"" + p.getName() + "\" in stock"
                    + (inCart > 0 ? " (" + inCart + " already in the cart)." : "."));
            return;
        }
        cart.removeIf(l -> l.getProductId().equals(p.getId()));
        cart.add(new SaleLine(p, inCart + qty));
        quantity.getValueFactory().setValue(1);
        updateTotals();
    }

    private void updateTotals() {
        double t = cartTotal();
        total.setText(Ui.money(t));
        try {
            double given = Ui.parseMoney(cash.getText(), "Cash");
            if (cart.isEmpty()) change.setText("");
            else if (given + 0.0001 < t) change.setText("Short by " + Ui.money(t - given));
            else change.setText("Change: " + Ui.money(given - t));
            change.setStyle(given + 0.0001 < t ? "-fx-text-fill: #c0392b;" : "");
        } catch (IllegalArgumentException e) {
            change.setText("");
        }
    }

    private void clearCart() {
        cart.clear();
        cash.clear();
        customer.clear();
        updateTotals();
    }

    private void completeSale() {
        if (cart.isEmpty()) { Ui.error("Add at least one product to the cart first."); return; }
        Ui.attempt(() -> {
            double given = Ui.parseMoney(cash.getText(), "Cash received");
            SaleTransaction t = system.recordSale(user, customer.getText(), cart, given);
            Ui.info("Sale recorded", receipt(t));
            clearCart();
            refresh();
        });
    }

    private String receipt(SaleTransaction t) {
        StringBuilder sb = new StringBuilder();
        sb.append("Receipt ").append(t.getId()).append("\n").append(Ui.dateTime(t.getTimestamp()))
                .append("\nCustomer: ").append(t.getParty()).append("\n\n");
        for (SaleLine l : t.getLines())
            sb.append(String.format("%d x %s  %s%n", l.getQuantity(), l.getProductName(), Ui.money(l.getSubtotal())));
        sb.append("\nTotal:  ").append(Ui.money(t.getTotal()))
                .append("\nCash:   ").append(Ui.money(t.getCashTendered()))
                .append("\nChange: ").append(Ui.money(t.getChange()));
        return sb.toString();
    }

    @Override
    public void refresh() {
        catalog.setAll(system.getProducts());
        productTable.refresh();
        // drop cart lines whose stock changed underneath us
        cart.removeIf(l -> system.findProduct(l.getProductId()).map(p -> stockOf(p) < l.getQuantity()).orElse(true));
        updateTotals();
    }
}
