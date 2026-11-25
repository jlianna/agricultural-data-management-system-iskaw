package application.ui;

import application.model.ProductSystem;
import application.model.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

// Welcome and login screen that handles user authentication and new customer registration
public class LoginView extends StackPane {

    // Builds the split login interface with branding on the left and the sign-in form on the right
    public LoginView(ProductSystem system, Consumer<User> onLogin) {
        getStyleClass().add("login-bg");

        // Configure the left branding panel with the application logo, tagline, and summary
        Label tagline = new Label("Cow Made Better");
        tagline.getStyleClass().add("tagline-text");
        Label sub = new Label("Iskaw helps manage dairy, beef, cowhide products, stock,\nsales and customer orders in one place.");
        sub.getStyleClass().add("hero-sub");
        
        Image iskawlogo = new Image(getClass().getResource("iskawlogo.png").toExternalForm());
        ImageView iskaw = new ImageView(iskawlogo);
        VBox welcome = new VBox(14, iskaw, tagline, sub);
        welcome.setAlignment(Pos.CENTER_LEFT);
        welcome.setPadding(new Insets(0, 40, 0, 0));

        // Initialize the right login card headers and credential input fields
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

        // Define the login submission action to authenticate credentials or display validation errors
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

        // Configure the primary sign-in button and secondary customer registration button
        Button signIn = Ui.button("Sign in", "btn-primary", submit);
        signIn.setMaxWidth(Double.MAX_VALUE);
        signIn.setDefaultButton(true);

        Button signUp = Ui.button("New customer? Create an account", "btn-secondary",
                () -> showSignUp(system, username));
        signUp.setMaxWidth(Double.MAX_VALUE);

        // Create the demo credentials reference box for testing different user roles
        Label hint = new Label("Demo accounts\nManager:    manager / manager123\nClerk:        clerk1 / clerk123\nCustomer:  customer1 / customer123");
        hint.getStyleClass().add("subtle");
        VBox hintBox = new VBox(hint);
        hintBox.getStyleClass().add("hint-box");

        // Assemble all form controls into the login card container
        VBox card = new VBox(12, title, prompt, 
        		Ui.fieldLabel("USERNAME"), username,
                Ui.fieldLabel("PASSWORD"), password, 
                error, signIn, signUp, hintBox);
        card.getStyleClass().add("login-card");

        // Display any startup warning message if one was flagged by the system
        if (system.getStartupWarning() != null) 
        	error.setText(system.getStartupWarning());

        // Center the branding panel and login card side-by-side in the root StackPane
        HBox layout = new HBox(30, welcome, card);
        layout.setAlignment(Pos.CENTER);
        layout.setFillHeight(false);
        getChildren().add(layout);
    }

    // Opens the customer self sign-up modal dialog and pre-fills the username on successful registration
    private void showSignUp(ProductSystem system, TextField loginUsername) {
        // Initialize registration input fields with helpful prompts
        TextField fullName = new TextField();
        fullName.setPromptText("e.g. Aaron Cale");
        
        TextField user = new TextField();
        user.setPromptText("Choose a username");
        
        PasswordField pass = new PasswordField();
        pass.setPromptText("At least 6 characters");
        
        PasswordField confirm = new PasswordField();
        confirm.setPromptText("Type it again");

        // Arrange registration labels and inputs into a structured grid layout
        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(10);
        g.setPadding(new Insets(10));
        g.addRow(0, Ui.fieldLabel("Full name"), fullName);
        g.addRow(1, Ui.fieldLabel("Username"), user);
        g.addRow(2, Ui.fieldLabel("Password"), pass);
        g.addRow(3, Ui.fieldLabel("Confirm password"), confirm);

        // Display the dialog and validate password matching before registering the new customer
        boolean created = FormDialog.show("Create a customer account", "Create account", g, () ->
                Ui.attempt(() -> {
                    if (!pass.getText().equals(confirm.getText()))
                        throw new IllegalArgumentException("The two passwords do not match.");
                    system.registerCustomer(user.getText(), fullName.getText(), pass.getText());
                }));

        // Populate the login username field and notify the user if registration succeeded
        if (created) {
            loginUsername.setText(user.getText().trim().toLowerCase());
            Ui.info("Account created", "You can now sign in with your new username and password.");
        }
    }
}