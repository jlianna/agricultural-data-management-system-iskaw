package application.ui;

import application.model.ProductSystem;
import application.model.User;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

// Manager-only screen for viewing registered user accounts and creating or deleting accounts
public class StaffView extends View {
    private final ProductSystem system;
    private final User manager;
    private final TableView<User> table = Ui.table("No accounts.");
    private final TextField fullName = new TextField();
    private final TextField username = new TextField();
    private final PasswordField password = new PasswordField();

    // Initializes the accounts table on the left and the new clerk creation form on the right
    public StaffView(ProductSystem system, User manager) {
        this.system = system;
        this.manager = manager;

        // Configure account table columns for full name, username, and role
        table.getColumns().add(Ui.column("Name", 180, User::getFullName));
        table.getColumns().add(Ui.column("Username", 120, User::getUsername));
        table.getColumns().add(Ui.column("Role", 90, User::getRole));
        VBox.setVgrow(table, Priority.ALWAYS);

        // Configure delete button and disable it when no non-manager row is selected
        Button deleteBtn = Ui.button("Delete selected account", "btn-danger", this::deleteAccount);
        deleteBtn.setDisable(true);
        table.getSelectionModel().selectedItemProperty().addListener((o, oldVal, selected) ->
                deleteBtn.setDisable(selected == null || selected.isManager()));

        HBox tableActions = new HBox(10, deleteBtn);
        tableActions.setAlignment(Pos.CENTER_LEFT);

        VBox left = new VBox(10, table, tableActions);
        HBox.setHgrow(left, Priority.ALWAYS);

        // Configure input prompts and assemble the right-hand clerk registration form card
        fullName.setPromptText("e.g. Pedro Garcia");
        username.setPromptText("e.g. clerk3");
        password.setPromptText("At least 6 characters");

        VBox form = new VBox(8, Ui.sectionTitle("Add a clerk"),
                Ui.fieldLabel("FULL NAME"), fullName,
                Ui.fieldLabel("USERNAME"), username,
                Ui.fieldLabel("PASSWORD"), password,
                Ui.button("Create clerk account", "btn-accent", this::addClerk));
        form.getStyleClass().add("card");
        form.setPrefWidth(320);
        form.setMinWidth(320);

        // Combine the header, accounts table column, and registration form into the main layout
        HBox body = new HBox(16, left, form);
        VBox.setVgrow(body, Priority.ALWAYS);
        HBox titleRow = new HBox(14, Ui.heading("Accounts"), Ui.subtle("Managers create clerk accounts here. Customers sign up themselves on the login screen."));
        titleRow.setAlignment(Pos.BOTTOM_LEFT);
        getChildren().addAll(titleRow, body);
    }

    // Validates input fields, registers a new clerk account, clears the form, and refreshes the table
    private void addClerk() {
        Ui.attempt(() -> {
            system.addClerk(manager, username.getText(), fullName.getText(), password.getText());
            fullName.clear();
            username.clear();
            password.clear();
            refresh();
        });
    }

    // Prompts for confirmation and deletes the selected clerk or customer account
    private void deleteAccount() {
        User selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        if (!Ui.confirm("Delete " + selected.getFullName() + "?",
                "Remove account \"" + selected.getUsername() + "\" (" + selected.getRole() + ")? Past sales and order records will remain.")) return;
        if (Ui.attempt(() -> system.deleteUser(manager, selected))) {
            table.getSelectionModel().clearSelection();
            refresh();
        }
    }

    // Reloads all user accounts into the table
    @Override
    public void refresh() {
        table.getItems().setAll(system.getUsers());
    }
}