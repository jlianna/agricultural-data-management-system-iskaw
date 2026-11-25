package application.ui;

import application.model.Customer;
import application.model.Inventory;
import application.model.Order;
import application.model.Product;
import application.model.ProductSystem;
import application.model.SaleLine;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

// Customer storefront allowing users to browse product tiles, inspect details, manage a cart, and submit cash-on-pickup orders
public class ShopView extends View {
    private static final String ALL = "All categories";
    private static final String SORT_DEFAULT = "Sort: Category";
    private static final String SORT_LOW = "Price: low to high";
    private static final String SORT_HIGH = "Price: high to low";
    private static final String SORT_NAME = "Name: A to Z";
    private static final double TILE_WIDTH = 190;

    // Core system references, product grid controls, and active shopping cart state
    private final ProductSystem system;
    private final Customer customer;
    private final Navigator nav;

    private final List<Product> products = new ArrayList<>();
    private final FlowPane grid = new FlowPane(14, 14);
    private final Label resultCount = Ui.subtle("");
    private final TextField search = new TextField();
    private final ComboBox<String> category = new ComboBox<>();
    private final ComboBox<String> sort = new ComboBox<>();

    private final ObservableList<SaleLine> cart = FXCollections.observableArrayList();
    private final TableView<SaleLine> cartTable = new TableView<>();
    private final Label total = new Label();

    // Initializes the shop layout with a banner, filterable product tile grid on the left, and shopping cart on the right
    public ShopView(ProductSystem system, Customer customer, Navigator nav) {
        this.system = system;
        this.customer = customer;
        this.nav = nav;

        // ------------------------------------------------ left: product tiles
        // Configure search bar, category filter, and sort dropdown listeners to rebuild the grid dynamically
        search.setPromptText("Search products…  (click a product to see its details)");
        HBox.setHgrow(search, Priority.ALWAYS);
        category.setPrefWidth(170);
        sort.setPrefWidth(170);
        sort.setItems(FXCollections.observableArrayList(SORT_DEFAULT, SORT_LOW, SORT_HIGH, SORT_NAME));
        sort.setValue(SORT_DEFAULT);
        search.textProperty().addListener((o, a, b) -> rebuildGrid());
        category.setOnAction(e -> rebuildGrid());
        sort.setOnAction(e -> rebuildGrid());
        HBox tools = new HBox(10, search, category, sort);
        tools.setAlignment(Pos.CENTER_LEFT);

        // Configure the scrollable FlowPane container for displaying product cards
        grid.setPadding(new Insets(4, 4, 8, 4));
        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("plain-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox left = new VBox(10, Ui.sectionTitle("1. Pick your products"), tools, resultCount, scroll);
        left.getStyleClass().add("card");
        HBox.setHgrow(left, Priority.ALWAYS);

        // ------------------------------------------------ right: cart
        // Configure the cart table columns for item name, quantity, and line subtotal
        cartTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        cartTable.setPlaceholder(new Label("Your cart is empty."));
        cartTable.getColumns().add(Ui.column("Item", 160, SaleLine::getProductName));
        cartTable.getColumns().add(Ui.moneyColumn("Qty", 45, l -> String.valueOf(l.getQuantity())));
        cartTable.getColumns().add(Ui.moneyColumn("Subtotal", 90, l -> Ui.money(l.getSubtotal())));
        cartTable.setItems(cart);
        VBox.setVgrow(cartTable, Priority.ALWAYS);

        // Configure cart management buttons, order total readout, and order submission button
        total.getStyleClass().add("total-label");
        HBox cartButtons = new HBox(8,
                Ui.button("Remove item", "btn-danger", () -> {
                    SaleLine l = cartTable.getSelectionModel().getSelectedItem();
                    if (l != null) { cart.remove(l); updateTotal(); }
                }),
                Ui.button("Clear cart", "btn-secondary", this::clearCart));
        Button place = Ui.button("Place order", "btn-accent", this::placeOrder);
        place.setMaxWidth(Double.MAX_VALUE);
        place.setStyle("-fx-font-size: 15px; -fx-padding: 12 16 12 16;");

        VBox right = new VBox(10, Ui.sectionTitle("2. Your cart"), cartTable, cartButtons,
                Ui.fieldLabel("ORDER TOTAL"), total,
                Ui.subtle("You pay cash at the counter when you pick up your order."), place);
        right.getStyleClass().add("card");
        right.setPrefWidth(340);
        right.setMinWidth(340);

        // Assemble header, promotional banner, and two-column shop body into the root container
        HBox body = new HBox(16, left, right);
        VBox.setVgrow(body, Priority.ALWAYS);
        HBox titleRow = new HBox(14, Ui.heading("Iskaw Shop"),
                Ui.subtle("Dairy, beef, and cowhide products from Iskaw."));
        titleRow.setAlignment(Pos.BOTTOM_LEFT);

        ImageView productBanner = new ImageView(new Image(
        		getClass().getResource("iskaw-products-banner.png").toExternalForm()));
        productBanner.setFitHeight(110);
        productBanner.setFitWidth(900);
        productBanner.setPreserveRatio(false);
        productBanner.setSmooth(true);

        getChildren().addAll(titleRow, productBanner, body);
        updateTotal();
    }

    // ---------------------------------------------------------------- tiles

    // Filters and sorts the product catalogue according to current controls and repopulates the tile grid
    private void rebuildGrid() {
        String q = search.getText() == null ? "" : search.getText().trim().toLowerCase();
        String cat = category.getValue();

        // Select the comparator matching the active sort option
        Comparator<Product> order;
        String s = sort.getValue();
        if (SORT_LOW.equals(s)) order = Comparator.comparingDouble(Product::getPrice);
        else if (SORT_HIGH.equals(s)) order = Comparator.comparingDouble(Product::getPrice).reversed();
        else if (SORT_NAME.equals(s)) order = Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER);
        else order = Comparator.comparing(Product::getCategory).thenComparing(Product::getName);

        // Filter products by category and search text, then sort into the final display list
        List<Product> shown = products.stream()
                .filter(p -> cat == null || ALL.equals(cat) || p.getCategory().equals(cat))
                .filter(p -> q.isEmpty() || p.getName().toLowerCase().contains(q)
                        || p.getDescription().toLowerCase().contains(q)
                        || p.getCategory().toLowerCase().contains(q))
                .sorted(order)
                .collect(Collectors.toList());

        // Rebuild the FlowPane children and update the matching product count label
        grid.getChildren().clear();
        if (shown.isEmpty()) {
            grid.getChildren().add(new Label("No products match your search."));
        }
        for (Product p : shown) grid.getChildren().add(tile(p));
        resultCount.setText(shown.size() + (shown.size() == 1 ? " product" : " products"));
    }

    // Builds an interactive product card with square photo, category/stock badges, price, and quick-add button
    private VBox tile(Product p) {
        Inventory inv = system.getInventory();
        int stock = inv.getStock(p.getId());

        // Layer the category chip and optional low-stock or sold-out badge over the square product image
        StackPane picture = ProductImages.square(system, p, TILE_WIDTH - 2);
        StackPane overlay = new StackPane(picture);
        Label chip = new Label(p.getCategory());
        chip.getStyleClass().add("tile-chip");
        StackPane.setAlignment(chip, Pos.BOTTOM_LEFT);
        overlay.getChildren().add(chip);
        if (stock == 0) {
            Label b = new Label("Sold out");
            b.getStyleClass().addAll("tile-flag", "tile-flag-out");
            StackPane.setAlignment(b, Pos.TOP_RIGHT);
            overlay.getChildren().add(b);
        } else if (inv.isLow(p.getId())) {
            Label b = new Label("Only " + stock + " left");
            b.getStyleClass().addAll("tile-flag", "tile-flag-low");
            StackPane.setAlignment(b, Pos.TOP_RIGHT);
            overlay.getChildren().add(b);
        }

        // Configure fixed-height product title, unit size, price label, and quick-add button
        Label name = new Label(p.getName());
        name.getStyleClass().add("tile-name");
        name.setWrapText(true);
        name.setMinHeight(44);
        name.setPrefHeight(44);
        name.setMaxHeight(44);
        name.setMaxWidth(Double.MAX_VALUE);

        Label unit = new Label(p.getUnit());
        unit.getStyleClass().add("tile-unit");

        Label price = new Label(Ui.money(p.getPrice()));
        price.getStyleClass().add("tile-price");
        Button add = new Button("+ Add");
        add.getStyleClass().add("tile-add");
        add.setMinWidth(Button.USE_PREF_SIZE);
        add.setDisable(stock == 0);
        add.setOnAction(e -> addToCart(p, 1));
        HBox priceRow = new HBox(6, price, Ui.spacer(), add);
        priceRow.setAlignment(Pos.CENTER_LEFT);
        priceRow.setPadding(new Insets(0, 10, 0, 10));

        // Assemble all card elements and bind click handlers to open the product detail modal
        VBox tile = new VBox(6, overlay, name, unit, priceRow);
        tile.getStyleClass().add("product-tile");
        tile.setPrefWidth(TILE_WIDTH);
        tile.setMinWidth(TILE_WIDTH);
        tile.setMaxWidth(TILE_WIDTH);
        if (stock == 0) tile.getStyleClass().add("product-tile-soldout");
        // clicking the picture or the text opens the details; the Add button handles its own click
        overlay.setOnMouseClicked(e -> showDetails(p));
        name.setOnMouseClicked(e -> showDetails(p));
        unit.setOnMouseClicked(e -> showDetails(p));
        return tile;
    }

    // Displays a modal dialog showing full product details, stock status, and a quantity selector to add to cart
    private void showDetails(Product p) {
        Inventory inv = system.getInventory();
        int stock = inv.getStock(p.getId());

        // Configure product title, price, description, and stock availability badge
        Label name = new Label(p.getName());
        name.getStyleClass().add("heading");
        name.setWrapText(true);
        Label price = new Label(Ui.money(p.getPrice()) + "  ·  " + p.getUnit());
        price.getStyleClass().add("tile-price");
        price.setStyle("-fx-font-size: 20px;");
        Label description = new Label(p.getDescription().isEmpty() ? "No description yet." : p.getDescription());
        description.setWrapText(true);
        description.setMaxWidth(300);

        Label availability;
        if (stock == 0) availability = Ui.badge("Sold out", "badge-out");
        else if (inv.isLow(p.getId())) availability = Ui.badge("Only " + stock + " left", "badge-low");
        else availability = Ui.badge("Available (" + stock + " in stock)", "badge-ok");

        // Configure the quantity spinner and disable it if the item is out of stock
        Spinner<Integer> qty = new Spinner<>();
        qty.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99, 1));
        qty.setEditable(true);
        qty.setPrefWidth(90);
        HBox qtyRow = new HBox(10, Ui.fieldLabel("QTY"), qty);
        qtyRow.setAlignment(Pos.CENTER_LEFT);
        qtyRow.setDisable(stock == 0);

        // Combine enlarged product photo and detail column into the dialog pane
        VBox info = new VBox(10, name, Ui.badge(p.getCategory(), "badge-void"), price, description, availability, qtyRow);
        info.setPrefWidth(300);

        HBox content = new HBox(20, ProductImages.square(system, p, 260), info);
        content.setPadding(new Insets(6));

        // Present the dialog and add the chosen quantity to the cart when confirmed
        ButtonType addType = new ButtonType("Add to cart", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(p.getName());
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getStylesheets().add(Ui.STYLESHEET);
        dialog.getDialogPane().getButtonTypes().addAll(addType, ButtonType.CLOSE);
        dialog.getDialogPane().lookupButton(addType).setDisable(stock == 0);
        dialog.setResultConverter(b -> b);
        dialog.showAndWait().filter(b -> b == addType).ifPresent(b -> addToCart(p, readQuantity(qty)));
    }

    // Parses and clamps the spinner's text input into a valid quantity between 1 and 99
    private static int readQuantity(Spinner<Integer> spinner) {
        try {
            int v = Integer.parseInt(spinner.getEditor().getText().trim());
            return Math.max(1, Math.min(99, v));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    // ---------------------------------------------------------------- cart

    // Validates available inventory and adds or merges the specified product quantity into the customer's cart
    private void addToCart(Product p, int qty) {
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
        updateTotal();
    }

    // Sums all cart line subtotals and updates the displayed order total
    private void updateTotal() {
        total.setText(Ui.money(cart.stream().mapToDouble(SaleLine::getSubtotal).sum()));
    }

    // Removes all items from the cart and resets the displayed total
    private void clearCart() {
        cart.clear();
        updateTotal();
    }

    // Submits the cart as a new customer order, displays pickup instructions, and navigates to the orders screen
    private void placeOrder() {
        if (cart.isEmpty()) { Ui.error("Your cart is empty. Add at least one product first."); 
        	return; 
        }
        
        Ui.attempt(() -> {
            Order o = system.placeOrder(customer, new ArrayList<>(cart));
            clearCart();
            Ui.info("Order placed", "Order " + o.getId() + " (" + Ui.money(o.getTotal()) + ") has been sent to the cooperative.\n\n"
                    + "Pick it up at the counter and pay cash. A clerk will confirm it, and it will then appear in your purchases.");
            nav.go("orders");
        });
    }

    // Reloads products and categories, prunes cart items exceeding current stock, and rebuilds the product grid
    @Override
    public void refresh() {
        products.clear();
        products.addAll(system.getProducts());
        String keep = category.getValue();
        List<String> cats = new ArrayList<>();
        cats.add(ALL);
        cats.addAll(system.getCategories());
        category.setOnAction(null); // avoid rebuilding twice while the list is replaced
        category.setItems(FXCollections.observableArrayList(cats));
        category.setValue(keep != null && cats.contains(keep) ? keep : ALL);
        category.setOnAction(e -> rebuildGrid());
        // drop cart lines that can no longer be fulfilled from current stock
        cart.removeIf(l -> system.getInventory().getStock(l.getProductId()) < l.getQuantity());
        updateTotal();
        rebuildGrid();
    }
}