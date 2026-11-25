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

// Shared UI factory and helper methods for formatting, controls, tables, dialogs, and input parsing
public final class Ui {

    // URL of the shared stylesheet (also applied to dialogs, which live in their own windows)
    public static final String STYLESHEET = Ui.class.getResource("/application/style.css").toExternalForm();

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");
    private static final DateTimeFormatter SHORT_DATE_TIME = DateTimeFormatter.ofPattern("MMM d, h:mm a");

    // Private constructor to prevent instantiation of this utility class
    private Ui() { }

    // ---------------------------------------------------------------- formatting

    // Formats a numeric amount into Philippine Peso currency notation with two decimal places
    public static String money(double amount) {
        return String.format("₱%,.2f", amount);
    }

    // Formats a LocalDateTime using the full date and time pattern, or returns empty if null
    public static String dateTime(LocalDateTime t) {
        return t == null ? "" : DATE_TIME.format(t);
    }

    // Formats a LocalDateTime without the year for narrow table columns
    public static String shortDateTime(LocalDateTime t) {
        return t == null ? "" : SHORT_DATE_TIME.format(t);
    }

    // Builds a plain-text receipt summary for a completed sale transaction
    public static String receiptText(SaleTransaction t) {
        StringBuilder sb = new StringBuilder();
        sb.append("Receipt ").append(t.getId()).append("\n").append(dateTime(t.getTimestamp()))
                .append("\nCustomer: ").append(t.getParty()).append("\n\n");

        // Append each line item's quantity, product name, and subtotal
        for (SaleLine l : t.getLines())
            sb.append(String.format("%d x %s  %s%n", l.getQuantity(), l.getProductName(), money(l.getSubtotal())));

        // Append total, cash tendered, and change due
        sb.append("\nTotal:  ").append(money(t.getTotal()))
                .append("\nCash:   ").append(money(t.getCashTendered()))
                .append("\nChange: ").append(money(t.getChange()));
        return sb.toString();
    }

    // Builds a plain-text summary of a customer order including pickup instructions or receipt reference
    public static String orderText(Order o) {
        StringBuilder sb = new StringBuilder();
        sb.append("Order ").append(o.getId()).append("\nPlaced: ").append(dateTime(o.getTimestamp()))
                .append("\nCustomer: ").append(o.getCustomerName()).append("\n\n");

        // Append each ordered item and the overall order total
        for (SaleLine l : o.getLines())
            sb.append(String.format("%d x %s  %s%n", l.getQuantity(), l.getProductName(), money(l.getSubtotal())));
        sb.append("\nTotal: ").append(money(o.getTotal()));

        // Append status-specific pickup instructions or the linked sale receipt ID
        if (o.getStatus() == Order.Status.PENDING)
            sb.append("\n\nPick this up at the counter and pay cash. A clerk will confirm it.");
        else if (o.getSaleId() != null)
            sb.append("\n\nCompleted \u2013 receipt ").append(o.getSaleId());
        return sb.toString();
    }

    // Maps an order status label to its corresponding CSS badge style class
    public static String orderBadge(String statusLabel) {
        if ("Completed".equals(statusLabel)) 
        	return "badge-ok";
        if ("Cancelled".equals(statusLabel)) 
        	return "badge-void";
        return "badge-low";
    }

    // ---------------------------------------------------------------- controls

    // Creates a styled primary view heading label
    public static Label heading(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("heading");
        return l;
    }

    // Creates a styled card or section title label
    public static Label sectionTitle(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("section-title");
        return l;
    }

    // Creates a word-wrapped secondary helper text label
    public static Label subtle(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("subtle");
        l.setWrapText(true);
        return l;
    }

    // Creates a form field caption label that resists shrinking below its preferred width
    public static Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setMinWidth(Region.USE_PREF_SIZE);
        l.getStyleClass().add("field-label");
        return l;
    }

    // Creates a button with the given style class (btn-primary, btn-accent, btn-secondary, or btn-danger) and click handler
    public static Button button(String text, String style, Runnable action) {
        Button b = new Button(text);
        b.setMinWidth(Region.USE_PREF_SIZE); // never truncate button text
        b.getStyleClass().add(style);
        if (action != null) b.setOnAction(e -> action.run());
        return b;
    }

    // Creates a pill-shaped status badge label with the specified style class
    public static Label badge(String text, String style) {
        Label l = new Label(text);
        l.getStyleClass().addAll("badge", style);
        l.setMinWidth(Region.USE_PREF_SIZE);
        return l;
    }

    // Creates an expanding flexible region used to push sibling controls apart in HBox or VBox layouts
    public static Region spacer() {
        Region r = new Region();
        r.setMinSize(0, 0);
        javafx.scene.layout.HBox.setHgrow(r, Priority.ALWAYS);
        javafx.scene.layout.VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }

    // ---------------------------------------------------------------- tables

    // Creates a string-valued TableColumn with preferred and minimum widths
    public static <S> TableColumn<S, String> column(String title, double width, Function<S, String> value) {
        TableColumn<S, String> c = new TableColumn<>(title);
        c.setPrefWidth(width);
        c.setMinWidth(width * 0.85); // keeps text readable; table scrolls sideways instead of truncating
        c.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        return c;
    }

    // Creates a right-aligned TableColumn for monetary and numeric values
    public static <S> TableColumn<S, String> moneyColumn(String title, double width, Function<S, String> value) {
        TableColumn<S, String> c = column(title, width, value);
        c.setStyle("-fx-alignment: CENTER-RIGHT;");
        return c;
    }

    // Creates a preconfigured TableView with constrained column resizing and a custom empty-state placeholder
    public static <S> TableView<S> table(String emptyText) {
        TableView<S> t = new TableView<>();
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        t.setPlaceholder(new Label(emptyText));
        return t;
    }

    // ---------------------------------------------------------------- dialogs

    // Displays a modal error alert dialog styled with the shared application stylesheet
    public static void error(String message) {
        Alert a = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        a.setHeaderText("Something went wrong");
        a.getDialogPane().getStylesheets().add(STYLESHEET);
        a.showAndWait();
    }

    // Displays a modal informational alert dialog with a custom header title
    public static void info(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        a.setHeaderText(title);
        a.getDialogPane().getStylesheets().add(STYLESHEET);
        a.showAndWait();
    }

    // Displays a modal Yes/No confirmation dialog and returns true only if the user selects Yes
    public static boolean confirm(String title, String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        a.setHeaderText(title);
        a.getDialogPane().getStylesheets().add(STYLESHEET);
        return a.showAndWait().filter(b -> b == ButtonType.YES).isPresent();
    }

    // ---------------------------------------------------------------- input parsing

    // Parses a formatted currency string (stripping commas and peso signs) into a double, throwing a friendly error if invalid
    public static double parseMoney(String text, String fieldName) {
        try {
            if (text == null || text.trim().isEmpty()) throw new NumberFormatException();
            return Double.parseDouble(text.trim().replace(",", "").replace("₱", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a valid number.");
        }
    }

    // Parses a trimmed integer string, throwing a friendly IllegalArgumentException if invalid
    public static int parseWhole(String text, String fieldName) {
        try {
            return Integer.parseInt(text == null ? "" : text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a whole number.");
        }
    }

    // Executes an action safely, displaying any thrown exception in an error dialog and returning whether it succeeded
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

    // Functional interface representing an operation that may throw checked or unchecked exceptions
    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}