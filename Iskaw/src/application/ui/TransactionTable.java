package application.ui;

import application.model.ProductSystem;
import application.model.Transaction;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

import java.util.List;

// Reusable transaction table component used across dashboards and the Cash & Transactions screen
public class TransactionTable extends TableView<Transaction> {

    // Configures table columns, status badge rendering, and voided-row styling based on staff visibility and compact mode
    public TransactionTable(ProductSystem system, boolean showStaff, boolean compact) {
        setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY);
        setPlaceholder(new Label("No transactions to show."));

        // Add core transaction columns, adjusting date format and optional details/staff columns for compact layouts
        getColumns().add(Ui.column("ID", 70, Transaction::getId));
        if (compact) getColumns().add(Ui.column("Date", 135, t -> Ui.shortDateTime(t.getTimestamp())));
        else getColumns().add(Ui.column("Date & time", 155, t -> Ui.dateTime(t.getTimestamp())));
        getColumns().add(Ui.column("Type", 105, Transaction::getTypeLabel));
        getColumns().add(Ui.column("Customer / donor", compact ? 120 : 130, Transaction::getParty));
        if (!compact) getColumns().add(Ui.column("Details", 190, Transaction::getDetails));
        getColumns().add(Ui.moneyColumn("Amount", 95,
                t -> t.isInKind() ? "In kind" : Ui.money(t.getCashAmount())));
        if (showStaff)
            getColumns().add(Ui.column("Recorded by", 120, t -> system.displayName(t.getRecordedBy())));

        // Configure the status column to render styled badges distinguishing active vs. voided transactions
        TableColumn<Transaction, String> status = Ui.column("Status", 100, Transaction::getStatus);
        status.setCellFactory(col -> new TableCell<Transaction, String>() {
            // Renders a status badge inside each non-empty status cell
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(Ui.badge(item, "Voided".equals(item) ? "badge-void" : "badge-ok"));
                }
                setText(null);
            }
        });
        getColumns().add(status);

        // Apply muted row styling to voided transactions
        setRowFactory(tv -> new TableRow<Transaction>() {
            // Adds or clears the voided-row CSS class depending on the transaction's voided state
            @Override
            protected void updateItem(Transaction t, boolean empty) {
                super.updateItem(t, empty);
                getStyleClass().remove("voided-row");
                if (!empty && t != null && t.isVoided()) getStyleClass().add("voided-row");
            }
        });
    }

    // Replaces the table's transaction list while preserving the currently selected row if still present
    public void setData(List<Transaction> data) {
        Transaction selected = getSelectionModel().getSelectedItem();
        getItems().setAll(data);
        if (selected != null && data.contains(selected)) getSelectionModel().select(selected);
    }
}