package application.ui;

import application.model.Customer;
import application.model.ProductSystem;
import application.model.Order;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/** "My Orders": a customer's orders with their status; pending orders can be cancelled. */
public class CustomerOrdersView extends View {
    private final ProductSystem system;
    private final Customer customer;
    private final TableView<Order> table = Ui.table("You have no orders yet. Go to the Shop to place one.");

    public CustomerOrdersView(ProductSystem system, Customer customer) {
        this.system = system;
        this.customer = customer;

        table.getColumns().add(Ui.column("Order", 75, Order::getId));
        table.getColumns().add(Ui.column("Placed", 135, o -> Ui.shortDateTime(o.getTimestamp())));
        table.getColumns().add(Ui.column("Items", 300, Order::getDetails));
        table.getColumns().add(Ui.moneyColumn("Total", 95, o -> Ui.money(o.getTotal())));
        TableColumn<Order, String> status = Ui.column("Status", 100, o -> o.getStatus().getLabel());
        
        status.setCellFactory(col -> new TableCell<Order, String>() {
            
        	@Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null) 
                	setGraphic(null);
                else 
                	setGraphic(Ui.badge(item, Ui.orderBadge(item)));
            }
        });
        
        table.getColumns().add(status);
        
        table.setRowFactory(tv -> {
            TableRow<Order> row = new TableRow<Order>() {
                @Override
                protected void updateItem(Order o, boolean empty) {
                    super.updateItem(o, empty);
                    getStyleClass().remove("voided-row");
                    if (!empty && o != null && o.getStatus() == Order.Status.CANCELLED) 
                    	getStyleClass().add("voided-row");
                }
            };
            
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) showDetails(row.getItem());
            });
            
            return row;
        });
        javafx.scene.layout.VBox.setVgrow(table, Priority.ALWAYS);

        HBox actions = new HBox(10,
                Ui.button("View details", "btn-secondary", () -> {
                    Order o = table.getSelectionModel().getSelectedItem();
                    
                    if (o == null) 
                    	Ui.info("Select an order", "Choose an order in the table first (or double-click it).");
                    else 
                    	showDetails(o);
                }),
                
                Ui.button("Cancel order", "btn-danger", this::cancel));
        actions.setAlignment(Pos.CENTER_LEFT);

        HBox titleRow = new HBox(14, Ui.heading("My orders"),
                Ui.subtle("Pick up pending orders at the counter and pay cash. Completed orders move to your purchases."));
        
        titleRow.setAlignment(Pos.BOTTOM_LEFT);
        getChildren().addAll(titleRow, actions, table);
    }

    private void showDetails(Order o) {
        Ui.info("Order " + o.getId() + " – " + o.getStatus().getLabel(), Ui.orderText(o));
    }

    private void cancel() {
        Order o = table.getSelectionModel().getSelectedItem();
        if (o == null) { 
        	Ui.info("Select an order", "Choose the order you want to cancel."); 
        	return; 
        	}
        
        if (o.getStatus() != Order.Status.PENDING) {
            Ui.info("Cannot cancel", "Order " + o.getId() + " is already " + o.getStatus().getLabel().toLowerCase() + ".");
            return;
        }
        if (!Ui.confirm("Cancel order " + o.getId() + "?", o.getDetails() + "\n\nThis cannot be undone.")) 
        	return;
        if (Ui.attempt(() -> system.cancelOrder(customer, o))) 
        	refresh();
    }

    @Override
    public void refresh() {
        Order keep = table.getSelectionModel().getSelectedItem();
        table.getItems().setAll(system.getOrdersOf(customer.getUsername()));
        if (keep != null && table.getItems().contains(keep)) table.getSelectionModel().select(keep);
    }
}
