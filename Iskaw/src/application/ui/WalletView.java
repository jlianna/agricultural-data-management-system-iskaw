package application.ui;

import application.model.ProductSystem;
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

// Cash and transactions screen displaying wallet totals, donation entry forms, and voidable transaction history
public class WalletView extends View {
    private static final String ALL_TYPES = "All types";

    // Core system references, financial summary stat cards, and transaction table filter controls
    private final ProductSystem system;
    private final User user;
    private final StatCard balance = new StatCard("Wallet balance", "stat-green");
    private final StatCard salesIncome = new StatCard("Sales income", "stat-blue");
    private final StatCard cashDonations = new StatCard("Cash donations", "stat-butter");
    private final StatCard itemDonations = new StatCard("Item donations", "stat-blue");
    private final TransactionTable table;
    private final ComboBox<String> typeFilter = new ComboBox<>();
    private final TextField search = new TextField();

    // Initializes the cash and transactions screen with summary cards, donation/void actions, and a filterable ledger
    public WalletView(ProductSystem system, User user) {
        this.system = system;
        this.user = user;
        this.table = new TransactionTable(system, true, false);

        // Arrange the top summary stat cards evenly across the full width
        HBox cards = new HBox(16, balance, salesIncome, cashDonations, itemDonations);
        for (StatCard c : new StatCard[]{balance, salesIncome, cashDonations, itemDonations}) {
            HBox.setHgrow(c, Priority.ALWAYS);
            c.setMaxWidth(Double.MAX_VALUE);
        }

        // Configure transaction type dropdown and live search box listeners
        typeFilter.setItems(FXCollections.observableArrayList(ALL_TYPES, "Sale", "Cash Donation", "Item Donation"));
        typeFilter.setValue(ALL_TYPES);
        typeFilter.setOnAction(e -> reload());
        search.setPromptText("Search name, details or ID…");
        search.setPrefWidth(240);
        search.textProperty().addListener((o, a, b) -> reload());

        // Assemble action buttons and search/filter controls into the toolbar row
        HBox actions = new HBox(10,
                Ui.button("+ Cash donation", "btn-accent", this::recordCashDonation),
                Ui.button("+ Item donation", "btn-primary", this::recordItemDonation),
                Ui.button("Void selected", "btn-danger", this::voidSelected),
                Ui.spacer(), search, typeFilter);
        actions.setAlignment(Pos.CENTER_LEFT);

        // Combine header, stat cards, toolbar, and transaction table into the root container
        VBox.setVgrow(table, Priority.ALWAYS);
        HBox titleRow = new HBox(14, Ui.heading("Cash & transactions"),
                Ui.subtle("Wallet balance counts sales and cash donations; voided entries are excluded."));
        titleRow.setAlignment(Pos.BOTTOM_LEFT);
        getChildren().addAll(titleRow, cards, actions, table);
    }

    // Creates a standard padded GridPane layout for modal donation entry dialogs
    private GridPane form() {
        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(10);
        g.setPadding(new Insets(10));
        return g;
    }

    // Opens a modal dialog to record a monetary donation and refreshes the screen upon saving
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

    // Opens a modal dialog to record a custom donated item for charity without adding to business inventory
    private void recordItemDonation() {
        TextField donor = new TextField();
        donor.setPromptText("Anonymous");

        TextField itemName = new TextField();
        itemName.setPromptText("e.g. Canned Goods, Winter Jackets, Rice");
        itemName.setPrefWidth(260);

        TextField qty = new TextField();
        qty.setPromptText("e.g. 12");

        TextField details = new TextField();
        details.setPromptText("e.g. 5 kg sacks, unopened, for orphanage");

        GridPane g = form();
        g.addRow(0, Ui.fieldLabel("Donor name"), donor);
        g.addRow(1, Ui.fieldLabel("Item donated"), itemName);
        g.addRow(2, Ui.fieldLabel("Quantity"), qty);
        g.addRow(3, Ui.fieldLabel("Notes / details"), details);

        boolean saved = FormDialog.show("Record item donation", "Save donation", g, () ->
                Ui.attempt(() -> system.recordItemDonation(
                        user,
                        donor.getText(),
                        itemName.getText(),
                        Ui.parseWhole(qty.getText(), "Quantity"),
                        details.getText()
                )));
        if (saved) refresh();
    }

    // Validates selection, prompts for confirmation, and voids the chosen transaction
    private void voidSelected() {
        Transaction t = table.getSelectionModel().getSelectedItem();
        if (t == null) { Ui.info("Select a transaction", "Choose the transaction you want to void."); return; }
        if (t.isVoided()) { Ui.info("Already voided", "Transaction " + t.getId() + " has already been voided."); return; }
        if (!Ui.confirm("Void " + t.getId() + "?", t.getTypeLabel() + " – " + t.getDetails()
                + "\n\nInventory and the wallet balance will be adjusted. This cannot be undone.")) return;
        if (Ui.attempt(() -> system.voidTransaction(user, t))) refresh();
    }

    // Filters transactions by selected type and search query and updates the table data
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

    // Recalculates wallet and donation summary stat cards and reloads the filtered transaction list
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