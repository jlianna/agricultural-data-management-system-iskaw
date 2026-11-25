package application.ui;

import application.model.Customer;
import application.model.ProductSystem;
import application.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

// Main application frame featuring a role-aware navigation sidebar and a swappable view content area
public class Shell extends BorderPane implements Navigator {
    private final SceneManager scenes;
    private final User user;
    private final StackPane content = new StackPane();
    private final Map<String, View> views = new HashMap<>();
    private final Map<String, Button> navButtons = new LinkedHashMap<>();

    // Initializes role-specific views, builds the navigation sidebar, and opens the default dashboard
    public Shell(SceneManager scenes, ProductSystem system, User user) {
        this.scenes = scenes;
        this.user = user;

        // Register available screens according to whether the user is a customer, clerk, or manager
        if (user instanceof Customer) {
            // Customers only get their own dashboard and the read-only shop
            views.put("dashboard", new CustomerDashboardView(system, (Customer) user, this));
            views.put("shop", new ShopView(system, (Customer) user, this));
            views.put("orders", new CustomerOrdersView(system, (Customer) user));
        } else {
            // Staff screens
            views.put("dashboard", user.isManager()
                    ? new ManagerDashboardView(system, user, this)
                    : new ClerkDashboardView(system, user, user, this, false));
            views.put("sales", new SalesView(system, user, this));
            views.put("orders", new StaffOrdersView(system, user));
            views.put("inventory", new InventoryView(system, user));
            views.put("wallet", new WalletView(system, user));
            // Manager-only screens
            if (user.isManager()) {
                views.put("products", new ProductsView(system, user));
                views.put("charity", new CharityView(system, user));
                views.put("staff", new StaffView(system, user));
            }
        }

        // Configure the sidebar container and top application brand header
        VBox sidebar = new VBox(6);
        sidebar.getStyleClass().add("sidebar");

        Label brand = new Label("Iskaw");
        brand.getStyleClass().add("brand");
        Label brandSub = new Label("Dairy • Beef • Cow Hide");
        brandSub.getStyleClass().add("brand-sub");
        HBox brandRow = new HBox(10, new Logo(38), new VBox(brand, brandSub));
        brandRow.setAlignment(Pos.CENTER_LEFT);
        brandRow.setPadding(new Insets(0, 0, 18, 4));
        sidebar.getChildren().add(brandRow);

        // Populate sidebar navigation links matching the user's role permissions
        addNav(sidebar, "dashboard", "Dashboard");
        if (user instanceof Customer) {
            addNav(sidebar, "shop", "Shop");
            addNav(sidebar, "orders", "My Orders");
        } else {
            addNav(sidebar, "sales", user.isManager() ? "Record Sale" : "New Sale");
            addNav(sidebar, "orders", "Customer Orders");
            addNav(sidebar, "inventory", "Inventory");
            if (user.isManager()) addNav(sidebar, "products", "Products");
            addNav(sidebar, "wallet", "Cash & Transactions");
            if (user.isManager()) addNav(sidebar, "charity", "Charity Donations");
            if (user.isManager()) addNav(sidebar, "staff", "Accounts");
        }

        // Create the bottom user profile badge and log-out button
        Label name = new Label(user.getFullName());
        name.getStyleClass().add("user-name");
        Label role = new Label(user.getRole());
        role.getStyleClass().add("user-role");
        VBox chip = new VBox(name, role);
        chip.getStyleClass().add("user-chip");
        Button logout = Ui.button("Log out", "btn-secondary", scenes::showLogin);
        logout.setMaxWidth(Double.MAX_VALUE);

        sidebar.getChildren().addAll(Ui.spacer(), chip, logout);

        // Assemble the sidebar and main content pane, then navigate to the dashboard
        setLeft(sidebar);
        setCenter(content);
        go("dashboard");
    }

    // Creates a styled navigation button, registers it by key, and appends it to the sidebar
    private void addNav(VBox sidebar, String key, String text) {
        Button b = new Button(text);
        b.getStyleClass().add("nav-button");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> go(key));
        navButtons.put(key, b);
        sidebar.getChildren().add(b);
    }

    // Switches the active view to the registered screen matching the given navigation key
    @Override
    public void go(String key) {
        View v = views.get(key);
        if (v != null) open(v, key);
    }

    // Refreshes and displays the given view in the main content area and highlights its navigation button
    @Override
    public void open(View view, String navKey) {
        view.refresh();
        content.getChildren().setAll(view);
        navButtons.forEach((k, b) -> {
            b.getStyleClass().remove("nav-button-active");
            if (k.equals(navKey)) b.getStyleClass().add("nav-button-active");
        });
    }
}