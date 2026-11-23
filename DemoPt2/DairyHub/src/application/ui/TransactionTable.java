package application.ui;

import application.model.DairySystem;
import application.model.Transaction;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

import java.util.List;

/** Reusable transaction grid used by the dashboards and the Cash & Transactions screen. */
public class TransactionTable extends TableView<Transaction> {

    /**
     * @param showStaff add a "Recorded by" column
     * @param compact   narrow layout (short date, no details column) for dashboards
     */
    public TransactionTable(DairySystem system, boolean showStaff, boolean compact) {
        setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY);
        setPlaceholder(new Label("No transactions to show."));

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

        TableColumn<Transaction, String> status = Ui.column("Status", 100, Transaction::getStatus);
        status.setCellFactory(col -> new TableCell<Transaction, String>() {
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

        setRowFactory(tv -> new TableRow<Transaction>() {
            @Override
            protected void updateItem(Transaction t, boolean empty) {
                super.updateItem(t, empty);
                getStyleClass().remove("voided-row");
                if (!empty && t != null && t.isVoided()) getStyleClass().add("voided-row");
            }
        });
    }

    public void setData(List<Transaction> data) {
        Transaction selected = getSelectionModel().getSelectedItem();
        getItems().setAll(data);
        if (selected != null && data.contains(selected)) getSelectionModel().select(selected);
    }
}
