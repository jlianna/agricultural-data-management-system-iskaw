package application.ui;

import application.model.Customer;
import application.model.ProductSystem;
import application.model.Dashboard;
import application.model.Order;
import application.model.SaleTransaction;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Map;

// Customer dashboard view displaying spending summary, purchase history, and top-bought items.
public class CustomerDashboardView extends View {
    private final ProductSystem system;
    private final Customer customer;

    private final StatCard spent = new StatCard("Total spent", "stat-blue");
    private final StatCard purchases = new StatCard("Purchases", "stat-butter");
    private final StatCard items = new StatCard("Items bought", "stat-green");
    private final StatCard pending = new StatCard("Pending orders", "stat-red");
    private final TableView<SaleTransaction> table = Ui.table("No purchases yet. Place an order in the Shop.");
    private final VBox favourites = new VBox(10);

    // Initializes the customer dashboard header, summary cards, purchase table, and favorites sidebar.
    public CustomerDashboardView(ProductSystem system, Customer customer, Navigator nav) {
        this.system = system;
        this.customer = customer;

        HBox titleRow = new HBox(12, Ui.heading("Hello, " + firstName(customer.getFullName())),
                Ui.subtle("Welcome to Iskaw"), Ui.spacer(),
                Ui.button("Shop now", "btn-accent", () -> nav.go("shop")));
        titleRow.setAlignment(Pos.CENTER_LEFT);

        // Arrange the four customer summary KPI cards evenly across the top row.
        HBox cards = new HBox(16, spent, purchases, items, pending);
        for (StatCard c : new StatCard[]{spent, purchases, items, pending}) {
            HBox.setHgrow(c, Priority.ALWAYS);
            c.setMaxWidth(Double.MAX_VALUE);
        }

        // Configure purchase history table columns and row click/styling behavior.
        table.getColumns().add(Ui.column("Receipt", 70, SaleTransaction::getId));
        table.getColumns().add(Ui.column("Date", 135, t -> Ui.shortDateTime(t.getTimestamp())));
        table.getColumns().add(Ui.column("Items", 220, SaleTransaction::getDetails));
        table.getColumns().add(Ui.moneyColumn("Total", 90, t -> Ui.money(t.getTotal())));
        table.getColumns().add(Ui.column("Status", 90, SaleTransaction::getStatus));
        VBox.setVgrow(table, Priority.ALWAYS);
        
        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<SaleTransaction> row = new javafx.scene.control.TableRow<SaleTransaction>() {
                // Overridden method to apply custom styling to voided transaction rows.
                @Override
                protected void updateItem(SaleTransaction t, boolean empty) {
                    super.updateItem(t, empty);
                    getStyleClass().remove("voided-row");
                    if (!empty && t != null && t.isVoided()) getStyleClass().add("voided-row");
                }
            };
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) showReceipt(row.getItem());
            });
            return row;
        });

        HBox historyHeader = new HBox(10, Ui.sectionTitle("My purchases"), Ui.spacer(),
                Ui.button("View receipt", "btn-secondary", () -> {
                    SaleTransaction t = table.getSelectionModel().getSelectedItem();
                    if (t == null) Ui.info("Select a purchase", "Choose a purchase in the table first (or double-click it).");
                    else showReceipt(t);
                }));
        historyHeader.setAlignment(Pos.CENTER_LEFT);
        VBox historyCard = new VBox(10, historyHeader, table);
        historyCard.getStyleClass().add("card");
        HBox.setHgrow(historyCard, Priority.ALWAYS);

        // Configure the fixed-width sidebar displaying top-purchased products.
        VBox favCard = new VBox(12, Ui.sectionTitle("My favourites"), favourites,
                Ui.spacer(),
                Ui.subtle("Order from the Shop, then pick it up and pay cash at the counter. "
                        + "Once a clerk confirms it, the purchase appears here."));
        favCard.getStyleClass().add("card");
        favCard.setPrefWidth(300);
        favCard.setMinWidth(300);

        HBox lower = new HBox(16, historyCard, favCard);
        VBox.setVgrow(lower, Priority.ALWAYS);

        getChildren().addAll(titleRow, cards, lower);
        setPadding(new Insets(26, 30, 26, 30));
    }

    // Extracts the first name from a customer's full name for the greeting header.
    private static String firstName(String full) {
        int i = full.indexOf(' ');
        return i > 0 ? full.substring(0, i) : full;
    }

    // Displays a modal dialog containing the itemized receipt for a selected sale.
    private void showReceipt(SaleTransaction t) {
        Ui.info("Receipt " + t.getId() + (t.isVoided() ? " (voided)" : ""), Ui.receiptText(t));
    }

    // Overridden method to reload customer spending metrics, purchase history, and top products.
    @Override
    public void refresh() {
        Dashboard d = Dashboard.forCustomer(system, customer);
        spent.setValue(Ui.money(d.getSalesTotal()));
        purchases.setValue(String.valueOf(d.getSalesCount()));
        
        int units = 0;
        for (SaleTransaction t : d.getSalesHistory()) 
            if (!t.isVoided()) 
                units += t.getItemCount();
        
        items.setValue(String.valueOf(units));
        
        pending.setValue(String.valueOf(system.getOrdersOf(customer.getUsername()).stream()
                .filter(o -> o.getStatus() == Order.Status.PENDING).count()));

        table.getItems().setAll(d.getSalesHistory());

        favourites.getChildren().clear();
        Map<String, Integer> top = d.getTopProducts(5);
        for (Map.Entry<String, Integer> e : top.entrySet()) {
            Label name = new Label(e.getKey());
            name.setWrapText(true);
            name.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(name, Priority.ALWAYS);
            HBox row = new HBox(8, name, Ui.badge(e.getValue() + " bought", "badge-ok"));
            row.setAlignment(Pos.CENTER_LEFT);
            favourites.getChildren().add(row);
        }
        if (top.isEmpty()) favourites.getChildren().add(Ui.subtle("Your most-bought products will appear here after your first purchase."));
    }
}