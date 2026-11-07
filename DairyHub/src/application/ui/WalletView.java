package application.ui;

import application.model.DairySystem;
import application.model.Dashboard;
import application.model.Product;
import application.model.Transaction;
import application.model.User;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Balance / cash / transaction-flow screen: the wallet balance, cash and item donations,
 * and the full transaction history with the ability to void a transaction.
 */
public class WalletView extends View {
    private static final String ALL_TYPES = "All types";

    private final DairySystem system;
    private final User user;
    private final StatCard balance = new StatCard("Wallet balance", "stat-green");
    private final StatCard salesIncome = new StatCard("Sales income", "stat-blue");
    private final StatCard cashDonations = new StatCard("Cash donations", "stat-butter");
    private final StatCard itemDonations = new StatCard("Item donations", "stat-blue");
    private final TransactionTable table;
    private final ComboBox<String> typeFilter = new ComboBox<>();
    private final TextField search = new TextField();

    public WalletView(DairySystem system, User user) {
        this.system = system;
        this.user = user;
        this.table = new TransactionTable(system, true, false);

        HBox cards = new HBox(16, balance, salesIncome, cashDonations, itemDonations);
        for (StatCard c : new StatCard[]{balance, salesIncome, cashDonations, itemDonations}) {
            HBox.setHgrow(c, Priority.ALWAYS);
            c.setMaxWidth(Double.MAX_VALUE);
        }

        typeFilter.setItems(FXCollections.observableArrayList(ALL_TYPES, "Sale", "Cash Donation", "Item Donation"));
        typeFilter.setValue(ALL_TYPES);
        typeFilter.setOnAction(e -> reload());
        search.setPromptText("Search name, details or ID…");
        search.setPrefWidth(240);
        search.textProperty().addListener((o, a, b) -> reload());

        HBox actions = new HBox(10,
                Ui.button("+ Cash donation", "btn-accent", this::recordCashDonation),
                Ui.button("+ Item donation", "btn-primary", this::recordItemDonation),
                Ui.button("Void selected", "btn-danger", this::voidSelected),
                Ui.spacer(), search, typeFilter);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox.setVgrow(table, Priority.ALWAYS);
        HBox titleRow = new HBox(14, Ui.heading("Cash & transactions"),
                Ui.subtle("Wallet balance counts sales and cash donations; voided entries are excluded."));
        titleRow.setAlignment(Pos.BOTTOM_LEFT);
        getChildren().addAll(titleRow, cards, actions, table);
    }

    private GridPane form() {
        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(10);
        g.setPadding(new Insets(10));
        return g;
    }

    private void recordCashDonation() {
        TextField donor = new TextField();
        donor.setPromptText("Anonymous");
        TextField amount = new TextField();
        amount.setPromptText("0.00");
        GridPane g = form();
        g.addRow(0, Ui.fieldLabel("Donor name"), donor);
        g.addRow(1, Ui.fieldLabel("Amount (₱)"), amount);
        boolean saved = FormDialog.show("Record cash donation", "Save donation", g, () ->
                Ui.attempt(() -> system.recordCashDonation(user, donor.getText(),
                        Ui.parseMoney(amount.getText(), "Amount"))));
        if (saved) refresh();
    }

    private void recordItemDonation() {
        TextField donor = new TextField();
        donor.setPromptText("Anonymous");
        ComboBox<Product> product = new ComboBox<>(FXCollections.observableArrayList(system.getProducts()));
        product.setPromptText("Choose a product");
        product.setPrefWidth(260);
        TextField qty = new TextField();
        qty.setPromptText("e.g. 12");
        GridPane g = form();
        g.addRow(0, Ui.fieldLabel("Donor name"), donor);
        g.addRow(1, Ui.fieldLabel("Product"), product);
        g.addRow(2, Ui.fieldLabel("Quantity"), qty);
        boolean saved = FormDialog.show("Record item donation", "Save donation", g, () ->
                Ui.attempt(() -> system.recordItemDonation(user, donor.getText(), product.getValue(),
                        Ui.parseWhole(qty.getText(), "Quantity"))));
        if (saved) refresh();
    }

    private void voidSelected() {
        Transaction t = table.getSelectionModel().getSelectedItem();
        if (t == null) { Ui.info("Select a transaction", "Choose the transaction you want to void."); return; }
        if (t.isVoided()) { Ui.info("Already voided", "Transaction " + t.getId() + " has already been voided."); return; }
        if (!Ui.confirm("Void " + t.getId() + "?", t.getTypeLabel() + " – " + t.getDetails()
                + "\n\nInventory and the wallet balance will be adjusted. This cannot be undone.")) return;
        if (Ui.attempt(() -> system.voidTransaction(user, t))) refresh();
    }

    private void reload() {
        String type = typeFilter.getValue();
        String q = search.getText() == null ? "" : search.getText().trim().toLowerCase();
        List<Transaction> rows = system.getTransactions().stream()
                .filter(t -> type == null || ALL_TYPES.equals(type) || t.getTypeLabel().equals(type))
                .filter(t -> q.isEmpty() || t.getId().toLowerCase().contains(q)
                        || t.getParty().toLowerCase().contains(q) || t.getDetails().toLowerCase().contains(q))
                .collect(Collectors.toList());
        table.setData(rows);
    }

    @Override
    public void refresh() {
        Dashboard d = new Dashboard(system, null);
        balance.setValue(Ui.money(d.getWalletBalance()));
        salesIncome.setValue(Ui.money(d.getSalesTotal()));
        cashDonations.setValue(Ui.money(d.getCashDonationsTotal()));
        itemDonations.setValue(d.getItemDonationCount() + " donation" + (d.getItemDonationCount() == 1 ? "" : "s"));
        reload();
    }
}
