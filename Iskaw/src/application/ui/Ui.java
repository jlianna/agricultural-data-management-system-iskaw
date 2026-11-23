package application.ui;

import application.model.Order;
import application.model.SaleLine;
import application.model.SaleTransaction;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

//Small factory/helper methods shared by every screen
public final class Ui {

    //URL of the shared stylesheet (also applied to dialogs, which live in their own windows).
    public static final String STYLESHEET = Ui.class.getResource("/application/style.css").toExternalForm();

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");
    private static final DateTimeFormatter SHORT_DATE_TIME = DateTimeFormatter.ofPattern("MMM d, h:mm a");

    private Ui() { }

    // ---------------------------------------------------------------- formatting

    public static String money(double amount) {
        return String.format("₱%,.2f", amount);
    }

    public static String dateTime(LocalDateTime t) {
        return t == null ? "" : DATE_TIME.format(t);
    }

    // Compact form for narrow tables (no year). 
    public static String shortDateTime(LocalDateTime t) {
        return t == null ? "" : SHORT_DATE_TIME.format(t);
    }

    //Plain-text receipt for a sale (used by the sale screen and the customer dashboard).
    public static String receiptText(SaleTransaction t) {
        StringBuilder sb = new StringBuilder();
        sb.append("Receipt ").append(t.getId()).append("\n").append(dateTime(t.getTimestamp()))
                .append("\nCustomer: ").append(t.getParty()).append("\n\n");
        for (SaleLine l : t.getLines())
            sb.append(String.format("%d x %s  %s%n", l.getQuantity(), l.getProductName(), money(l.getSubtotal())));
        sb.append("\nTotal:  ").append(money(t.getTotal()))
                .append("\nCash:   ").append(money(t.getCashTendered()))
                .append("\nChange: ").append(money(t.getChange()));
        return sb.toString();
    }

    // Plain-text summary of an order (used by the customer and staff order screens).
    public static String orderText(Order o) {
        StringBuilder sb = new StringBuilder();
        sb.append("Order ").append(o.getId()).append("\nPlaced: ").append(dateTime(o.getTimestamp()))
                .append("\nCustomer: ").append(o.getCustomerName()).append("\n\n");
        for (SaleLine l : o.getLines())
            sb.append(String.format("%d x %s  %s%n", l.getQuantity(), l.getProductName(), money(l.getSubtotal())));
        sb.append("\nTotal: ").append(money(o.getTotal()));
        if (o.getStatus() == Order.Status.PENDING)
            sb.append("\n\nPick this up at the counter and pay cash. A clerk will confirm it.");
        else if (o.getSaleId() != null)
            sb.append("\n\nCompleted \u2013 receipt ").append(o.getSaleId());
        return sb.toString();
    }

    //Badge style for an order status label. 
    public static String orderBadge(String statusLabel) {
        if ("Completed".equals(statusLabel)) 
        	return "badge-ok";
        if ("Cancelled".equals(statusLabel)) 
        	return "badge-void";
        return "badge-low";
    }

    // ---------------------------------------------------------------- controls

    public static Label heading(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("heading");
        return l;
    }

    public static Label sectionTitle(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("section-title");
        return l;
    }

    public static Label subtle(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("subtle");
        l.setWrapText(true);
        return l;
    }

    public static Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setMinWidth(Region.USE_PREF_SIZE);
        l.getStyleClass().add("field-label");
        return l;
    }

    /** @param style one of btn-primary, btn-accent, btn-secondary, btn-danger */
    public static Button button(String text, String style, Runnable action) {
        Button b = new Button(text);
        b.setMinWidth(Region.USE_PREF_SIZE); // never truncate button text
        b.getStyleClass().add(style);
        if (action != null) b.setOnAction(e -> action.run());
        return b;
    }

    public static Label badge(String text, String style) {
        Label l = new Label(text);
        l.getStyleClass().addAll("badge", style);
        l.setMinWidth(Region.USE_PREF_SIZE);
        return l;
    }

    public static Region spacer() {
        Region r = new Region();
        r.setMinSize(0, 0);
        javafx.scene.layout.HBox.setHgrow(r, Priority.ALWAYS);
        javafx.scene.layout.VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }

    // ---------------------------------------------------------------- tables

    public static <S> TableColumn<S, String> column(String title, double width, Function<S, String> value) {
        TableColumn<S, String> c = new TableColumn<>(title);
        c.setPrefWidth(width);
        c.setMinWidth(width * 0.85); // keeps text readable; table scrolls sideways instead of truncating
        c.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return c;
    }

    public static <S> TableColumn<S, String> moneyColumn(String title, double width, Function<S, String> value) {
        TableColumn<S, String> c = column(title, width, value);
        c.setStyle("-fx-alignment: CENTER-RIGHT;");
        return c;
    }

    public static <S> TableView<S> table(String emptyText) {
        TableView<S> t = new TableView<>();
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        t.setPlaceholder(new Label(emptyText));
        return t;
    }

    // ---------------------------------------------------------------- dialogs

    public static void error(String message) {
        Alert a = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        a.setHeaderText("Something went wrong");
        a.getDialogPane().getStylesheets().add(STYLESHEET);
        a.showAndWait();
    }

    public static void info(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        a.setHeaderText(title);
        a.getDialogPane().getStylesheets().add(STYLESHEET);
        a.showAndWait();
    }

    public static boolean confirm(String title, String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        a.setHeaderText(title);
        a.getDialogPane().getStylesheets().add(STYLESHEET);
        return a.showAndWait().filter(b -> b == ButtonType.YES).isPresent();
    }

    // ---------------------------------------------------------------- input parsing

    /** Parses a money amount such as "1,250.50". @throws IllegalArgumentException with a friendly message */
    public static double parseMoney(String text, String fieldName) {
        try {
            if (text == null || text.trim().isEmpty()) throw new NumberFormatException();
            return Double.parseDouble(text.trim().replace(",", "").replace("₱", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a valid number.");
        }
    }

    /** Parses a whole number. @throws IllegalArgumentException with a friendly message */
    public static int parseWhole(String text, String fieldName) {
        try {
            return Integer.parseInt(text == null ? "" : text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a whole number.");
        }
    }

    /** Runs an action and shows any failure in a dialog instead of crashing. Returns true on success. */
    public static boolean attempt(ThrowingRunnable action) {
        try {
            action.run();
            return true;
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            error(e.getMessage());
        } catch (Exception e) {
            error(e.getMessage() == null ? e.toString() : e.getMessage());
        }
        return false;
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}
