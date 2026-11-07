package application.ui;

import application.model.DairySystem;
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

/**
 * Main application frame: a sidebar (role-aware navigation) plus a content area that
 * swaps between {@link View}s. Managers see more menu items than clerks.
 */
public class Shell extends BorderPane implements Navigator {
    private final SceneManager scenes;
    private final User user;
    private final StackPane content = new StackPane();
    private final Map<String, View> views = new HashMap<>();
    private final Map<String, Button> navButtons = new LinkedHashMap<>();

    public Shell(SceneManager scenes, DairySystem system, User user) {
        this.scenes = scenes;
        this.user = user;

        // Screens available to everyone
        views.put("dashboard", user.isManager()
                ? new ManagerDashboardView(system, user, this)
                : new ClerkDashboardView(system, user, user, this, false));
        views.put("sales", new SalesView(system, user, this));
        views.put("inventory", new InventoryView(system, user));
        views.put("wallet", new WalletView(system, user));
        // Manager-only screens
        if (user.isManager()) {
            views.put("products", new ProductsView(system, user));
            views.put("staff", new StaffView(system, user));
        }

        VBox sidebar = new VBox(6);
        sidebar.getStyleClass().add("sidebar");

        Label brand = new Label("DairyHub");
        brand.getStyleClass().add("brand");
        Label brandSub = new Label("Bukid Dairy Cooperative");
        brandSub.getStyleClass().add("brand-sub");
        HBox brandRow = new HBox(10, new Logo(38), new VBox(brand, brandSub));
        brandRow.setAlignment(Pos.CENTER_LEFT);
        brandRow.setPadding(new Insets(0, 0, 18, 4));
        sidebar.getChildren().add(brandRow);

        addNav(sidebar, "dashboard", "Dashboard");
        addNav(sidebar, "sales", user.isManager() ? "Record Sale" : "New Sale");
        addNav(sidebar, "inventory", "Inventory");
        if (user.isManager()) addNav(sidebar, "products", "Products");
        addNav(sidebar, "wallet", "Cash & Transactions");
        if (user.isManager()) addNav(sidebar, "staff", "Staff");

        Label name = new Label(user.getFullName());
        name.getStyleClass().add("user-name");
        Label role = new Label(user.getRole());
        role.getStyleClass().add("user-role");
        VBox chip = new VBox(name, role);
        chip.getStyleClass().add("user-chip");
        Button logout = Ui.button("Log out", "btn-secondary", scenes::showLogin);
        logout.setMaxWidth(Double.MAX_VALUE);

        sidebar.getChildren().addAll(Ui.spacer(), chip, logout);

        setLeft(sidebar);
        setCenter(content);
        go("dashboard");
    }

    private void addNav(VBox sidebar, String key, String text) {
        Button b = new Button(text);
        b.getStyleClass().add("nav-button");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> go(key));
        navButtons.put(key, b);
        sidebar.getChildren().add(b);
    }

    @Override
    public void go(String key) {
        View v = views.get(key);
        if (v != null) open(v, key);
    }

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
