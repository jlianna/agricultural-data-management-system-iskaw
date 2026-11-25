package application.ui;

import application.model.Clerk;
import application.model.ProductSystem;
import application.model.Dashboard;
import application.model.SaleTransaction;
import application.model.User;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Manager dashboard displaying business totals, clerk performance, sales history, best sellers, and stock alerts
public class ManagerDashboardView extends View {
    private final ProductSystem system;
    private final User manager;
    private final Navigator nav;

    // Summary stat cards for key cooperative financial and inventory metrics
    private final StatCard wallet = new StatCard("Wallet balance", "stat-green");
    private final StatCard sales = new StatCard("Total sales", "stat-blue");
    private final StatCard donations = new StatCard("Cash donations", "stat-butter");
    private final StatCard lowStock = new StatCard("Items low / out of stock", "stat-red");

    // UI tables, filters, charts, and alert panels for monitoring operations
    private final TableView<Dashboard.ClerkSummary> clerkTable = Ui.table("No clerks yet.");
    private final TableView<SaleTransaction> historyTable = Ui.table("No sales recorded yet.");
    private final ComboBox<String> staffFilter = new ComboBox<>();
    private final BarChart<Number, String> chart = new BarChart<>(new NumberAxis(), new CategoryAxis());
    private final LowStockPanel alerts;
    private List<User> filterUsers = new ArrayList<>();

    // Constructs the manager dashboard layout and configures all tables, charts, and interactive controls
    public ManagerDashboardView(ProductSystem system, User manager, Navigator nav) {
        this.system = system;
        this.manager = manager;
        this.nav = nav;
        this.alerts = new LowStockPanel(system);

        // Build the page header row
        Label title = Ui.heading("Manager dashboard");
        Label sub = Ui.subtle("Overview of the whole cooperative");
        HBox titleRow = new HBox(14, title, sub);
        titleRow.setAlignment(Pos.BOTTOM_LEFT);

        // Arrange the top summary stat cards evenly across the full width
        HBox cards = new HBox(16, wallet, sales, donations, lowStock);
        for (StatCard c : new StatCard[]{wallet, sales, donations, lowStock}) {
            HBox.setHgrow(c, Priority.ALWAYS);
            c.setMaxWidth(Double.MAX_VALUE);
        }

        // Configure clerk performance columns and enable double-click or button navigation into a clerk's dashboard
        clerkTable.getColumns().add(Ui.column("Clerk", 150, s -> s.getClerk().getFullName()));
        clerkTable.getColumns().add(Ui.moneyColumn("Sales", 55, s -> String.valueOf(s.getSalesCount())));
        clerkTable.getColumns().add(Ui.moneyColumn("Sales total", 100, s -> Ui.money(s.getSalesTotal())));
        clerkTable.getColumns().add(Ui.moneyColumn("Donations", 100, s -> Ui.money(s.getDonationsTotal())));
        clerkTable.getColumns().add(Ui.moneyColumn("Voids", 55, s -> String.valueOf(s.getVoidedCount())));
        clerkTable.setPrefHeight(150);
        clerkTable.setRowFactory(tv -> {
            javafx.scene.control.TableRow<Dashboard.ClerkSummary> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) openClerk(row.getItem().getClerk());
            });
            return row;
        });
        Button openClerk = Ui.button("Open selected clerk’s dashboard", "btn-primary", () -> {
            Dashboard.ClerkSummary s = clerkTable.getSelectionModel().getSelectedItem();
            if (s == null) Ui.info("Select a clerk", "Choose a clerk in the table first (or double-click a row).");
            else openClerk(s.getClerk());
        });
        VBox clerkCard = new VBox(10, Ui.sectionTitle("Clerk performance"), clerkTable, openClerk);
        clerkCard.getStyleClass().add("card");

        // Configure the horizontal bar chart displaying top-selling products
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(190);
        chart.setMinHeight(150);
        VBox chartCard = new VBox(8, Ui.sectionTitle("Best sellers (units sold)"), chart);
        chartCard.getStyleClass().add("card");
        VBox.setVgrow(chart, Priority.ALWAYS);

        // Combine the clerk performance card and best sellers chart into the upper split row
        VBox leftTop = new VBox(16, clerkCard);
        HBox.setHgrow(leftTop, Priority.ALWAYS);
        HBox.setHgrow(clerkCard, Priority.ALWAYS);
        HBox top = new HBox(16, leftTop, chartCard);
        HBox.setHgrow(chartCard, Priority.ALWAYS);
        leftTop.setPrefWidth(520);
        chartCard.setPrefWidth(400);

        // Configure the sale history table columns and staff filter dropdown
        historyTable.getColumns().add(Ui.column("Date", 135, t -> Ui.shortDateTime(t.getTimestamp())));
        historyTable.getColumns().add(Ui.column("Clerk", 110, t -> system.displayName(t.getRecordedBy())));
        historyTable.getColumns().add(Ui.column("Customer", 100, SaleTransaction::getParty));
        historyTable.getColumns().add(Ui.column("Items", 180, SaleTransaction::getDetails));
        historyTable.getColumns().add(Ui.moneyColumn("Total", 90, t -> Ui.money(t.getTotal())));
        historyTable.getColumns().add(Ui.column("Status", 85, SaleTransaction::getStatus));
        staffFilter.setOnAction(e -> loadHistory());
        HBox historyHeader = new HBox(12, Ui.sectionTitle("Clerk sale history"), Ui.spacer(),
                Ui.subtle("Show:"), staffFilter);
        historyHeader.setAlignment(Pos.CENTER_LEFT);
        VBox historyCard = new VBox(10, historyHeader, historyTable);
        historyCard.getStyleClass().add("card");
        VBox.setVgrow(historyTable, Priority.ALWAYS);
        HBox.setHgrow(historyCard, Priority.ALWAYS);

        // Place the sale history card alongside the fixed-width low stock alerts panel in the bottom row
        alerts.setPrefWidth(250);
        alerts.setMinWidth(250);
        HBox bottom = new HBox(16, historyCard, alerts);
        VBox.setVgrow(bottom, Priority.ALWAYS);

        // Add all dashboard sections into the root container
        getChildren().addAll(titleRow, cards, top, bottom);
    }

    // Navigates to the individual dashboard view for the selected clerk
    private void openClerk(Clerk clerk) {
        ClerkDashboardView v = new ClerkDashboardView(system, manager, clerk, nav, true);
        nav.open(v, "dashboard");
    }

    // Populates the sale history table filtered by the currently selected staff member
    private void loadHistory() {
        int i = staffFilter.getSelectionModel().getSelectedIndex();
        Dashboard d = new Dashboard(system, i <= 0 || i > filterUsers.size() ? null : filterUsers.get(i - 1));
        historyTable.getItems().setAll(d.getSalesHistory());
    }

    // Reloads all business metrics, clerk summaries, best sellers chart, staff filter options, and stock alerts
    @Override
    public void refresh() {
        Dashboard all = new Dashboard(system, null);
        wallet.setValue(Ui.money(all.getWalletBalance()));
        sales.setValue(Ui.money(all.getSalesTotal()) + "  ·  " + all.getSalesCount());
        donations.setValue(Ui.money(all.getCashDonationsTotal()));
        lowStock.setValue(String.valueOf(all.getLowStockCount()));

        clerkTable.getItems().setAll(all.getClerkSummaries());

        XYChart.Series<Number, String> series = new XYChart.Series<>();
        Map<String, Integer> top = all.getTopProducts(5);
        top.forEach((name, units) -> series.getData().add(new XYChart.Data<>(units, name)));
        chart.getData().setAll(java.util.Collections.singletonList(series));
        if (top.isEmpty()) chart.setTitle("No sales yet");
        else chart.setTitle(null);

        int keep = Math.max(0, staffFilter.getSelectionModel().getSelectedIndex());
        filterUsers = new ArrayList<>();
        for (User u : system.getUsers()) if (u.isStaff()) filterUsers.add(u);
        List<String> names = new ArrayList<>();
        names.add("All staff");
        for (User u : filterUsers) names.add(u.getFullName());
        staffFilter.setOnAction(null);
        staffFilter.setItems(FXCollections.observableArrayList(names));
        staffFilter.getSelectionModel().select(Math.min(keep, names.size() - 1));
        staffFilter.setOnAction(e -> loadHistory());
        loadHistory();

        alerts.refresh();
    }
}