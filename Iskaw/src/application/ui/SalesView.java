package application.ui;

import application.model.Customer;
import application.model.ProductSystem;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

// Point-of-sale screen for selecting products, building a cart, collecting cash, and recording sales
public class SalesView extends View {
    private final ProductSystem system;
    private final User user;

    // Product catalogue list, search filter, and left-hand selection controls
    private final ObservableList<Product> catalog = FXCollections.observableArrayList();
    private final FilteredList<Product> visible = new FilteredList<>(catalog, p -> true);
    private final TableView<Product> productTable = new TableView<>();
    private final Spinner<Integer> quantity = new Spinner<>();
    private final TextField search = new TextField();

    // Active shopping cart state and right-hand checkout controls
    private final ObservableList<SaleLine> cart = FXCollections.observableArrayList();
    private final TableView<SaleLine> cartTable = new TableView<>();
    private final TextField customer = new TextField();
    private final ComboBox<Customer> account = new ComboBox<>();
    private final TextField cash = new TextField();
    private final Label total = new Label();
    private final Label change = new Label();

    // Initializes the two-column point-of-sale layout with product picker on the left and checkout cart on the right
    public SalesView(ProductSystem system, User user, Navigator nav) {
        this.system = system;
        this.user = user;

        // ------------------------------------------------ left: product picker
        // Filter the visible product table dynamically as the user types in the search box
        search.setPromptText("Search products…  (double-click a product to add it)");
        search.textProperty().addListener((o, a, b) -> {
            String q = b == null ? "" : b.trim().toLowerCase();
            visible.setPredicate(p -> q.isEmpty() || p.getName().toLowerCase().contains(q)
                    || p.getCategory().toLowerCase().contains(q));
        });

        // Configure product table columns, out-of-stock row styling, and double-click to add behavior
        productTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        productTable.setPlaceholder(new Label("No products found."));
        productTable.getColumns().add(Ui.column("Product", 210, Product::getName));
        productTable.getColumns().add(Ui.column("Size", 70, Product::getUnit));
        productTable.getColumns().add(Ui.moneyColumn("Price", 80, p -> Ui.money(p.getPrice())));
        productTable.getColumns().add(Ui.moneyColumn("In stock", 70, p -> String.valueOf(stockOf(p))));
        productTable.setItems(visible);
        productTable.setRowFactory(tv -> {
            TableRow<Product> row = new TableRow<Product>() {
                // Highlights out-of-stock product rows using the voided-row style class
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

        // Configure the editable quantity spinner and commit manual input when focus leaves the editor
        quantity.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1));
        quantity.setEditable(true);
        quantity.setPrefWidth(90);
        quantity.getEditor().focusedProperty().addListener((o, was, now) -> { if (!now) commitQuantity(); });

        // Assemble the quantity selector and product catalogue into the left card
        HBox addRow = new HBox(10, Ui.fieldLabel("QTY"), quantity,
                Ui.button("Add to cart", "btn-primary", this::addToCart));
        addRow.setAlignment(Pos.CENTER_LEFT);

        VBox left = new VBox(10, Ui.sectionTitle("1. Choose products"), search, productTable, addRow);
        left.getStyleClass().add("card");
        HBox.setHgrow(left, Priority.ALWAYS);

        // ------------------------------------------------ right: cart & payment
        // Configure cart table columns to display selected items, quantities, and line subtotals
        cartTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        cartTable.setPlaceholder(new Label("The cart is empty."));
        cartTable.getColumns().add(Ui.column("Item", 170, SaleLine::getProductName));
        cartTable.getColumns().add(Ui.moneyColumn("Qty", 45, l -> String.valueOf(l.getQuantity())));
        cartTable.getColumns().add(Ui.moneyColumn("Subtotal", 90, l -> Ui.money(l.getSubtotal())));
        cartTable.setItems(cart);
        cartTable.setPrefHeight(200);
        VBox.setVgrow(cartTable, Priority.ALWAYS);

        // Configure registered customer dropdown and disable walk-in name field when an account is chosen
        customer.setPromptText("Walk-in customer");
        account.setPromptText("Registered customer (optional)");
        account.setMaxWidth(Double.MAX_VALUE);
        account.setConverter(new StringConverter<Customer>() {
            // Converts Customer objects to readable display strings and leaves reverse conversion unused
            @Override public String toString(Customer c) { return c == null ? "" : c.getFullName() + " (" + c.getUsername() + ")"; }
            @Override public Customer fromString(String s) { return null; }
        });
        account.valueProperty().addListener((o, a, b) -> {
            customer.setDisable(b != null);
            if (b != null) customer.clear();
        });
        HBox accountRow = new HBox(6, account, Ui.button("Clear", "btn-secondary", () -> account.setValue(null)));
        HBox.setHgrow(account, Priority.ALWAYS);

        // Configure cash input, live total/change readouts, and an exact-amount shortcut button
        cash.setPromptText("0.00");
        cash.textProperty().addListener((o, a, b) -> updateTotals());
        total.getStyleClass().add("total-label");
        change.getStyleClass().add("change-label");

        Button exact = Ui.button("Exact", "btn-secondary", () -> cash.setText(String.format("%.2f", cartTotal())));
        HBox cashRow = new HBox(8, cash, exact);
        HBox.setHgrow(cash, Priority.ALWAYS);

        // Configure cart management buttons and the primary sale completion button
        Button remove = Ui.button("Remove item", "btn-danger", () -> {
            SaleLine l = cartTable.getSelectionModel().getSelectedItem();
            if (l != null) { cart.remove(l); updateTotals(); }
        });
        Button clear = Ui.button("Clear cart", "btn-secondary", this::clearCart);
        HBox cartButtons = new HBox(8, remove, clear);

        Button complete = Ui.button("Complete sale", "btn-accent", this::completeSale);
        complete.setMaxWidth(Double.MAX_VALUE);
        complete.setStyle("-fx-font-size: 15px; -fx-padding: 12 16 12 16;");

        // Assemble cart, customer, and payment controls into the fixed-width right card
        VBox right = new VBox(10, Ui.sectionTitle("2. Cart & payment"), cartTable, cartButtons,
                Ui.fieldLabel("CUSTOMER (OPTIONAL)"), accountRow, customer,
                Ui.fieldLabel("TOTAL DUE"), total,
                Ui.fieldLabel("CASH RECEIVED"), cashRow, change, complete);
        right.getStyleClass().add("card");
        right.setPrefWidth(380);
        right.setMinWidth(380);

        // Combine header and split panels into the root container and initialize totals
        HBox body = new HBox(16, left, right);
        VBox.setVgrow(body, Priority.ALWAYS);
        HBox titleRow = new HBox(14, Ui.heading(user.isManager() ? "Record sale" : "New sale"),
                Ui.subtle("Cash purchases only. Stock is deducted automatically."));
        titleRow.setAlignment(Pos.BOTTOM_LEFT);
        getChildren().addAll(titleRow, body);
        setPadding(new Insets(26, 30, 26, 30));
        updateTotals();
    }

    // Returns the current available inventory count for the given product
    private int stockOf(Product p) {
        return system.getInventory().getStock(p.getId());
    }

    // Sums the subtotals of all line items currently in the cart
    private double cartTotal() {
        return cart.stream().mapToDouble(SaleLine::getSubtotal).sum();
    }

    // Validates and clamps any manually typed spinner text into the 1–999 range before reading it
    private void commitQuantity() {
        try {
            int v = Integer.parseInt(quantity.getEditor().getText().trim());
            quantity.getValueFactory().setValue(Math.max(1, Math.min(999, v)));
        } catch (NumberFormatException e) {
            quantity.getValueFactory().setValue(1);
        }
        quantity.getEditor().setText(String.valueOf(quantity.getValue()));
    }

    // Validates available stock and adds or merges the selected product and quantity into the cart
    private void addToCart() {
        Product p = productTable.getSelectionModel().getSelectedItem();
        if (p == null) { Ui.info("Select a product", "Click a product in the list first."); return; }
        commitQuantity();
        int qty = quantity.getValue();

        // Check existing cart quantity against available stock to prevent overselling
        int inCart = cart.stream().filter(l -> l.getProductId().equals(p.getId())).mapToInt(SaleLine::getQuantity).sum();
        int available = stockOf(p);
        if (available == 0) { Ui.error(p.getName() + " is out of stock."); return; }
        if (inCart + qty > available) {
            Ui.error("Only " + available + " of \"" + p.getName() + "\" in stock"
                    + (inCart > 0 ? " (" + inCart + " already in the cart)." : "."));
            return;
        }

        // Replace any existing line for this product with the combined quantity and reset spinner
        cart.removeIf(l -> l.getProductId().equals(p.getId()));
        cart.add(new SaleLine(p, inCart + qty));
        quantity.getValueFactory().setValue(1);
        updateTotals();
    }

    // Recalculates the total due and updates the change or shortfall message based on cash entered
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

    // Empties the cart and resets customer and payment inputs for the next transaction
    private void clearCart() {
        cart.clear();
        cash.clear();
        customer.clear();
        account.setValue(null);
        updateTotals();
    }

    // Validates cash payment, records the sale transaction, displays the receipt, and resets the screen
    private void completeSale() {
        if (cart.isEmpty()) { Ui.error("Add at least one product to the cart first."); return; }
        Ui.attempt(() -> {
            double given = Ui.parseMoney(cash.getText(), "Cash received");
            Customer acct = account.getValue();
            SaleTransaction t = system.recordSale(user, acct == null ? customer.getText() : acct.getFullName(),
                    acct == null ? null : acct.getUsername(), cart, given);
            Ui.info("Sale recorded", receipt(t));
            clearCart();
            refresh();
        });
    }

    // Formats the completed sale transaction into a printable receipt string
    private String receipt(SaleTransaction t) {
        return Ui.receiptText(t);
    }

    // Reloads products and registered customers, prunes cart items exceeding updated stock, and recalculates totals
    @Override
    public void refresh() {
        catalog.setAll(system.getProducts());
        account.setItems(javafx.collections.FXCollections.observableArrayList(system.getCustomers()));
        productTable.refresh();
        // drop cart lines whose stock changed underneath us
        cart.removeIf(l -> system.findProduct(l.getProductId()).map(p -> stockOf(p) < l.getQuantity()).orElse(true));
        updateTotals();
    }
}