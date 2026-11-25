package application.ui;

import application.model.ProductSystem;
import application.model.Dashboard;
import application.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

// Dashboard view for clerks, supporting both personal clerk mode and read-only manager inspection mode.
public class ClerkDashboardView extends View {
    private final ProductSystem system;
    private final User subject;
    private final StatCard sales = new StatCard("My sales", "stat-blue");
    private final StatCard count = new StatCard("Sales recorded", "stat-butter");
    private final StatCard donations = new StatCard("Cash donations collected", "stat-green");
    private final StatCard voids = new StatCard("Voided transactions", "stat-red");
    private final TransactionTable recent;
    private final LowStockPanel lowStock;

    // Initializes the clerk dashboard layout, header actions, stat cards, and activity panels.
    public ClerkDashboardView(ProductSystem system, User viewer, User subject, Navigator nav, boolean readOnlyForManager) {
        this.system = system;
        this.subject = subject;
        this.recent = new TransactionTable(system, false, true);
        this.lowStock = new LowStockPanel(system);

        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);
       
        // Configure header controls depending on whether a manager or the clerk is viewing.
        if (readOnlyForManager) {
            titleRow.getChildren().addAll(
                    Ui.button("← Back", "btn-secondary", () -> nav.go("dashboard")),
                    Ui.heading(subject.getFullName() + "’s dashboard"),
                    Ui.subtle("Clerk view (managed by " + viewer.getFullName() + ")"));
        } 
        else {
            titleRow.getChildren().addAll(Ui.heading("Hello, " + firstName(subject.getFullName()) + " 👋"));
            titleRow.getChildren().addAll(Ui.spacer(),
                    Ui.button("New sale", "btn-accent", () -> nav.go("sales")),
                    Ui.button("Record donation", "btn-primary", () -> nav.go("wallet")));
        }

        // Arrange the four summary KPI cards evenly across the top row.
        HBox cards = new HBox(16, sales, count, donations, voids);
        for (StatCard c : new StatCard[]{sales, count, donations, voids}) HBox.setHgrow(c, Priority.ALWAYS);
        for (StatCard c : new StatCard[]{sales, count, donations, voids}) c.setMaxWidth(Double.MAX_VALUE);

        // Set up the main recent transactions card on the left.
        VBox recentCard = new VBox(10, Ui.sectionTitle(readOnlyForManager ? "Recent activity" : "My recent transactions"), recent);
        recentCard.getStyleClass().add("card");
        
        VBox.setVgrow(recent, Priority.ALWAYS);
        HBox.setHgrow(recentCard, Priority.ALWAYS);

        // Set up the fixed-width low-stock alert panel on the right.
        lowStock.setPrefWidth(300);
        lowStock.setMinWidth(300);
        
        HBox lower = new HBox(16, recentCard, lowStock);
        VBox.setVgrow(lower, Priority.ALWAYS);

        getChildren().addAll(titleRow, cards, lower);
        setPadding(new Insets(26, 30, 26, 30));
    }

    // Extracts the first name from a user's full name for the welcome greeting.
    private static String firstName(String full) {
        int i = full.indexOf(' ');
        return i > 0 ? full.substring(0, i) : full;
    }

    // Overridden method to reload clerk metrics, recent transactions, and low-stock items.
    @Override
    public void refresh() {
        Dashboard d = new Dashboard(system, subject);
        sales.setValue(Ui.money(d.getSalesTotal()));
        count.setValue(String.valueOf(d.getSalesCount()));
        donations.setValue(Ui.money(d.getCashDonationsTotal()));
        voids.setValue(String.valueOf(d.getVoidedCount()));
        recent.setData(d.getRecent(15));
        lowStock.refresh();
    }
}