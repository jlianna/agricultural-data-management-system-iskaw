package application.ui;

import application.model.ItemDonation;
import application.model.ProductSystem;
import application.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Manager-only screen for viewing donated charity items and dispatching them to charities
public class CharityView extends View {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy h:mm a");

    private final ProductSystem system;
    private final User manager;
    private final StatCard availableItems = new StatCard("Items awaiting charity", "stat-green");
    private final StatCard totalUnitsAvailable = new StatCard("Available units", "stat-blue");
    private final StatCard totalUnitsDonated = new StatCard("Units sent to charity", "stat-butter");
    private final TableView<ItemDonation> table = Ui.table("No donated items recorded yet.");
    private final TextField search = new TextField();

    public CharityView(ProductSystem system, User manager) {
        this.system = system;
        this.manager = manager;

        HBox cards = new HBox(16, availableItems, totalUnitsAvailable, totalUnitsDonated);
        for (StatCard c : new StatCard[]{availableItems, totalUnitsAvailable, totalUnitsDonated}) {
            HBox.setHgrow(c, Priority.ALWAYS);
            c.setMaxWidth(Double.MAX_VALUE);
        }

        table.getColumns().add(Ui.column("ID", 90, ItemDonation::getId));
        table.getColumns().add(Ui.column("Date", 155, d -> d.getTimestamp().format(DATE_FMT)));
        table.getColumns().add(Ui.column("Donor", 130, ItemDonation::getParty));
        table.getColumns().add(Ui.column("Donated Item", 170, ItemDonation::getItemName));
        table.getColumns().add(Ui.column("Remaining / Total", 130,
                d -> d.getRemainingQuantity() + " / " + d.getQuantity()));
        table.getColumns().add(Ui.column("Item Notes", 180, ItemDonation::getNotes));
        table.getColumns().add(Ui.column("Charity Status", 170, ItemDonation::getCharityStatus));
        table.getColumns().add(Ui.column("Distribution Log", 210, ItemDonation::getCharityDistributionNotes));
        VBox.setVgrow(table, Priority.ALWAYS);

        search.setPromptText("Search donor, item, or notes…");
        search.setPrefWidth(250);
        search.textProperty().addListener((o, a, b) -> reload());

        HBox actions = new HBox(10,
                Ui.button("Donate items to charity", "btn-accent", this::openCharityDispatchDialog),
                Ui.spacer(),
                search);
        actions.setAlignment(Pos.CENTER_LEFT);

        HBox titleRow = new HBox(14,
                Ui.heading("Charity donations"),
                Ui.subtle("In-kind items donated separately from store inventory. Select items to send to a charity."));
        titleRow.setAlignment(Pos.BOTTOM_LEFT);

        getChildren().addAll(titleRow, cards, actions, table);
    }

    // Opens a modal dialog allowing the manager to check multiple donated items, set quantities, and add charity notes
    private void openCharityDispatchDialog() {
        List<ItemDonation> available = system.getCharityDonations().stream()
                .filter(d -> !d.isVoided() && d.getRemainingQuantity() > 0)
                .collect(Collectors.toList());

        if (available.isEmpty()) {
            Ui.info("No items available", "There are currently no remaining donated items to send to charity.");
            return;
        }

        TextField charityName = new TextField();
        charityName.setPromptText("e.g. St. Jude Orphanage / Community Pantry");
        charityName.setPrefWidth(280);

        TextField notes = new TextField();
        notes.setPromptText("e.g. Picked up by coordinator, batch #1");
        notes.setPrefWidth(280);

        // Build checklist rows for each available donated item with a quantity input
        GridPane itemGrid = new GridPane();
        itemGrid.setHgap(12);
        itemGrid.setVgap(8);
        itemGrid.setPadding(new Insets(6));
        itemGrid.addRow(0,
                Ui.fieldLabel("SELECT ITEM"),
                Ui.fieldLabel("AVAILABLE"),
                Ui.fieldLabel("QTY TO DONATE"));

        List<SelectionRow> rows = new ArrayList<>();
        int r = 1;
        for (ItemDonation item : available) {
            CheckBox check = new CheckBox(item.getItemName() + " (" + item.getId() + ")");
            Label availLbl = new Label(String.valueOf(item.getRemainingQuantity()));
            TextField qtyField = new TextField(String.valueOf(item.getRemainingQuantity()));
            qtyField.setPrefWidth(80);
            qtyField.setDisable(true);

            check.selectedProperty().addListener((obs, wasSelected, isSelected) ->
                    qtyField.setDisable(!isSelected));

            itemGrid.addRow(r++, check, availLbl, qtyField);
            rows.add(new SelectionRow(item, check, qtyField));
        }

        ScrollPane scroll = new ScrollPane(itemGrid);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(210);
        scroll.setMaxHeight(240);

        GridPane headerForm = new GridPane();
        headerForm.setHgap(12);
        headerForm.setVgap(10);
        headerForm.addRow(0, Ui.fieldLabel("Charity / Recipient"), charityName);
        headerForm.addRow(1, Ui.fieldLabel("Dispatch Notes"), notes);

        VBox content = new VBox(12, headerForm, Ui.fieldLabel("CHOOSE DONATED ITEMS & QUANTITIES"), scroll);
        content.setPadding(new Insets(10));

        boolean saved = FormDialog.show("Donate items to charity", "Confirm donation", content, () ->
                Ui.attempt(() -> {
                    Map<ItemDonation, Integer> selectedMap = new LinkedHashMap<>();
                    for (SelectionRow row : rows) {
                        if (row.check.isSelected()) {
                            int qty = Ui.parseWhole(row.qtyField.getText(),
                                    "Quantity for " + row.item.getItemName());
                            selectedMap.put(row.item, qty);
                        }
                    }
                    system.donateItemsToCharity(manager, selectedMap, charityName.getText(), notes.getText());
                }));

        if (saved) refresh();
    }

    private void reload() {
        String q = search.getText() == null ? "" : search.getText().trim().toLowerCase();
        List<ItemDonation> filtered = system.getCharityDonations().stream()
                .filter(d -> q.isEmpty()
                        || d.getId().toLowerCase().contains(q)
                        || d.getParty().toLowerCase().contains(q)
                        || d.getItemName().toLowerCase().contains(q)
                        || d.getNotes().toLowerCase().contains(q)
                        || d.getCharityDistributionNotes().toLowerCase().contains(q))
                .collect(Collectors.toList());
        table.getItems().setAll(filtered);
    }

    @Override
    public void refresh() {
        List<ItemDonation> active = system.getCharityDonations().stream()
                .filter(d -> !d.isVoided())
                .collect(Collectors.toList());

        long awaitingCount = active.stream().filter(d -> d.getRemainingQuantity() > 0).count();
        int unitsAvail = active.stream().mapToInt(ItemDonation::getRemainingQuantity).sum();
        int unitsSent = active.stream().mapToInt(ItemDonation::getDonatedQuantity).sum();

        availableItems.setValue(String.valueOf(awaitingCount));
        totalUnitsAvailable.setValue(String.valueOf(unitsAvail));
        totalUnitsDonated.setValue(String.valueOf(unitsSent));
        reload();
    }

    private static class SelectionRow {
        final ItemDonation item;
        final CheckBox check;
        final TextField qtyField;

        SelectionRow(ItemDonation item, CheckBox check, TextField qtyField) {
            this.item = item;
            this.check = check;
            this.qtyField = qtyField;
        }
    }
}