package application.ui;

import application.model.ProductSystem;
import application.model.Order;
import application.model.SaleTransaction;
import application.model.User;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Staff view of customer orders. When a customer collects and pays for an order, staff
 * fulfil it: that records a sale linked to the customer and updates stock and the wallet.
 */
public class StaffOrdersView extends View {
    private static final String PENDING = "Pending only";
    private static final String ALL = "All orders";

    private final ProductSystem system;
    private final User user;
    private final TableView<Order> table = Ui.table("No orders to show.");
    private final ComboBox<String> filter = new ComboBox<>(FXCollections.observableArrayList(PENDING, ALL));
    private final StatCard pending = new StatCard("Pending orders", "stat-butter");

    public StaffOrdersView(ProductSystem system, User user) {
        this.system = system;
        this.user = user;

        table.getColumns().add(Ui.column("Order", 75, Order::getId));
        table.getColumns().add(Ui.column("Placed", 135, o -> Ui.shortDateTime(o.getTimestamp())));
        table.getColumns().add(Ui.column("Customer", 130, Order::getCustomerName));
        table.getColumns().add(Ui.column("Items", 260, Order::getDetails));
        table.getColumns().add(Ui.moneyColumn("Total", 95, o -> Ui.money(o.getTotal())));
        TableColumn<Order, String> status = Ui.column("Status", 100, o -> o.getStatus().getLabel());
        status.setCellFactory(col -> new TableCell<Order, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null) setGraphic(null);
                else setGraphic(Ui.badge(item, Ui.orderBadge(item)));
            }
        });
        table.getColumns().add(status);
        table.getColumns().add(Ui.column("Handled by", 120,
                o -> o.getHandledBy() == null ? "" : system.displayName(o.getHandledBy())));
        table.setRowFactory(tv -> new TableRow<Order>() {
            @Override
            protected void updateItem(Order o, boolean empty) {
                super.updateItem(o, empty);
                getStyleClass().remove("voided-row");
                if (!empty && o != null && o.getStatus() == Order.Status.CANCELLED) getStyleClass().add("voided-row");
            }
        });
        VBox.setVgrow(table, Priority.ALWAYS);

        filter.setValue(PENDING);
        filter.setOnAction(e -> reload());

        HBox actions = new HBox(10,
                Ui.button("Fulfil selected order", "btn-accent", this::fulfil),
                Ui.button("Cancel selected order", "btn-danger", this::cancel),
                Ui.spacer(), Ui.subtle("Show:"), filter);
        actions.setAlignment(Pos.CENTER_LEFT);

        pending.setMaxWidth(260);
        HBox titleRow = new HBox(14, Ui.heading("Customer orders"),
                Ui.subtle("Fulfil an order when the customer picks it up and pays cash."));
        titleRow.setAlignment(Pos.BOTTOM_LEFT);
        getChildren().addAll(titleRow, pending, actions, table);
    }

    private Order selected() {
        Order o = table.getSelectionModel().getSelectedItem();
        if (o == null) Ui.info("Select an order", "Choose an order in the table first.");
        return o;
    }

    private void fulfil() {
        Order o = selected();
        if (o == null) return;
        if (o.getStatus() != Order.Status.PENDING) {
            Ui.info("Already " + o.getStatus().getLabel().toLowerCase(), "Order " + o.getId() + " has already been "
                    + o.getStatus().getLabel().toLowerCase() + ".");
            return;
        }
        TextField cash = new TextField(String.format("%.2f", o.getTotal()));
        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(10);
        g.setPadding(new Insets(10));
        g.addRow(0, Ui.fieldLabel("Customer"), new Label(o.getCustomerName()));
        g.addRow(1, Ui.fieldLabel("Items"), wrapped(o.getDetails()));
        g.addRow(2, Ui.fieldLabel("Total due"), new Label(Ui.money(o.getTotal())));
        g.addRow(3, Ui.fieldLabel("Cash received (₱)"), cash);

        SaleTransaction[] result = new SaleTransaction[1];
        boolean done = FormDialog.show("Fulfil order " + o.getId(), "Record sale", g, () ->
                Ui.attempt(() -> result[0] = system.fulfillOrder(user, o, Ui.parseMoney(cash.getText(), "Cash received"))));
        if (done) {
            Ui.info("Order fulfilled", Ui.receiptText(result[0]));
            refresh();
        }
    }

    private Label wrapped(String text) {
        Label l = new Label(text);
        l.setWrapText(true);
        l.setMaxWidth(280);
        return l;
    }

    private void cancel() {
        Order o = selected();
        if (o == null) return;
        if (o.getStatus() != Order.Status.PENDING) {
            Ui.info("Cannot cancel", "Order " + o.getId() + " is already " + o.getStatus().getLabel().toLowerCase() + ".");
            return;
        }
        if (!Ui.confirm("Cancel order " + o.getId() + "?", o.getCustomerName() + ": " + o.getDetails()
                + "\n\nStock and the wallet are not affected.")) return;
        if (Ui.attempt(() -> system.cancelOrder(user, o))) refresh();
    }

    private void reload() {
        boolean onlyPending = !ALL.equals(filter.getValue());
        List<Order> rows = system.getOrders().stream()
                .filter(o -> !onlyPending || o.getStatus() == Order.Status.PENDING).collect(Collectors.toList());
        Order keep = table.getSelectionModel().getSelectedItem();
        table.getItems().setAll(rows);
        if (keep != null && rows.contains(keep)) table.getSelectionModel().select(keep);
    }

    @Override
    public void refresh() {
        pending.setValue(String.valueOf(system.getPendingOrderCount()));
        reload();
    }
}
