package application.ui;

import application.model.DairySystem;
import application.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/** Welcome / login screen. */
public class LoginView extends StackPane {

    public LoginView(DairySystem system, Consumer<User> onLogin) {
        getStyleClass().add("login-bg");

        // ---- left: welcome message
        Label hero = new Label("Fresh from the farm,\nmanaged with care.");
        hero.getStyleClass().add("hero-text");
        Label sub = new Label("DairyHub helps the Bukid Dairy Cooperative track products, stock,\nsales and donations in one place.");
        sub.getStyleClass().add("hero-sub");
        VBox welcome = new VBox(14, new Logo(84), hero, sub);
        welcome.setAlignment(Pos.CENTER_LEFT);
        welcome.setPadding(new Insets(0, 40, 0, 0));

        // ---- right: login card
        Label title = new Label("Welcome back");
        title.getStyleClass().add("login-title");
        Label prompt = Ui.subtle("Sign in to continue");

        TextField username = new TextField();
        username.setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");

        Label error = new Label();
        error.getStyleClass().add("error-text");
        error.setWrapText(true);

        Runnable submit = () -> {
            try {
                User u = system.login(username.getText(), password.getText());
                password.clear();
                error.setText("");
                onLogin.accept(u);
            } catch (IllegalArgumentException ex) {
                error.setText(ex.getMessage());
                password.clear();
                password.requestFocus();
            }
        };
        Button signIn = Ui.button("Sign in", "btn-primary", submit);
        signIn.setMaxWidth(Double.MAX_VALUE);
        signIn.setDefaultButton(true);

        Label hint = new Label("Demo accounts\nManager:  manager / manager123\nClerk:      clerk1 / clerk123");
        hint.getStyleClass().add("subtle");
        VBox hintBox = new VBox(hint);
        hintBox.getStyleClass().add("hint-box");

        VBox card = new VBox(12, title, prompt, Ui.fieldLabel("USERNAME"), username,
                Ui.fieldLabel("PASSWORD"), password, error, signIn, hintBox);
        card.getStyleClass().add("login-card");

        if (system.getStartupWarning() != null) error.setText(system.getStartupWarning());

        HBox layout = new HBox(30, welcome, card);
        layout.setAlignment(Pos.CENTER);
        layout.setFillHeight(false);
        getChildren().add(layout);
    }
}
