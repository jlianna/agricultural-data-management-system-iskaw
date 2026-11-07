package application.ui;

import application.model.DairySystem;
import application.model.Dashboard;
import application.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * A clerk's dashboard. Clerks see their own; a manager can open any clerk's dashboard
 * (in which case {@code readOnlyForManager} is true and a Back button is shown).
 */
public class ClerkDashboardView extends View {
    private final DairySystem system;
    private final User subject;
    private final StatCard sales = new StatCard("My sales", "stat-blue");
    private final StatCard count = new StatCard("Sales recorded", "stat-butter");
    private final StatCard donations = new StatCard("Cash donations collected", "stat-green");
    private final StatCard voids = new StatCard("Voided transactions", "stat-red");
    private final TransactionTable recent;
    private final LowStockPanel lowStock;

    public ClerkDashboardView(DairySystem system, User viewer, User subject, Navigator nav, boolean readOnlyForManager) {
        this.system = system;
        this.subject = subject;
        this.recent = new TransactionTable(system, false, true);
        this.lowStock = new LowStockPanel(system);

        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        if (readOnlyForManager) {
            titleRow.getChildren().addAll(
                    Ui.button("← Back", "btn-secondary", () -> nav.go("dashboard")),
                    Ui.heading(subject.getFullName() + "’s dashboard"),
                    Ui.subtle("Clerk view (managed by " + viewer.getFullName() + ")"));
        } else {
            titleRow.getChildren().addAll(Ui.heading("Hello, " + firstName(subject.getFullName()) + " 👋"));
            titleRow.getChildren().addAll(Ui.spacer(),
                    Ui.button("New sale", "btn-accent", () -> nav.go("sales")),
                    Ui.button("Record donation", "btn-primary", () -> nav.go("wallet")));
        }

        HBox cards = new HBox(16, sales, count, donations, voids);
        for (StatCard c : new StatCard[]{sales, count, donations, voids}) HBox.setHgrow(c, Priority.ALWAYS);
        for (StatCard c : new StatCard[]{sales, count, donations, voids}) c.setMaxWidth(Double.MAX_VALUE);

        VBox recentCard = new VBox(10, Ui.sectionTitle(readOnlyForManager ? "Recent activity" : "My recent transactions"), recent);
        recentCard.getStyleClass().add("card");
        VBox.setVgrow(recent, Priority.ALWAYS);
        HBox.setHgrow(recentCard, Priority.ALWAYS);

        lowStock.setPrefWidth(300);
        lowStock.setMinWidth(300);
        HBox lower = new HBox(16, recentCard, lowStock);
        VBox.setVgrow(lower, Priority.ALWAYS);

        getChildren().addAll(titleRow, cards, lower);
        setPadding(new Insets(26, 30, 26, 30));
    }

    private static String firstName(String full) {
        int i = full.indexOf(' ');
        return i > 0 ? full.substring(0, i) : full;
    }

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
