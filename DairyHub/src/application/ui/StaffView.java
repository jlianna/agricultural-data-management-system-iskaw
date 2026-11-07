package application.ui;

import application.model.DairySystem;
import application.model.User;
import javafx.geometry.Pos;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Manager-only screen for viewing staff and creating new clerk accounts. */
public class StaffView extends View {
    private final DairySystem system;
    private final User manager;
    private final TableView<User> table = Ui.table("No staff accounts.");
    private final TextField fullName = new TextField();
    private final TextField username = new TextField();
    private final PasswordField password = new PasswordField();

    public StaffView(DairySystem system, User manager) {
        this.system = system;
        this.manager = manager;

        table.getColumns().add(Ui.column("Name", 180, User::getFullName));
        table.getColumns().add(Ui.column("Username", 120, User::getUsername));
        table.getColumns().add(Ui.column("Role", 90, User::getRole));
        VBox.setVgrow(table, Priority.ALWAYS);

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

        HBox body = new HBox(16, table, form);
        HBox.setHgrow(table, Priority.ALWAYS);
        VBox.setVgrow(body, Priority.ALWAYS);
        HBox titleRow = new HBox(14, Ui.heading("Staff"), Ui.subtle("Clerks can sell, record donations and void transactions."));
        titleRow.setAlignment(Pos.BOTTOM_LEFT);
        getChildren().addAll(titleRow, body);
    }

    private void addClerk() {
        Ui.attempt(() -> {
            system.addClerk(manager, username.getText(), fullName.getText(), password.getText());
            fullName.clear();
            username.clear();
            password.clear();
            refresh();
        });
    }

    @Override
    public void refresh() {
        table.getItems().setAll(system.getUsers());
    }
}
