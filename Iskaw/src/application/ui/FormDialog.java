package application.ui;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

import java.util.function.BooleanSupplier;

// Reusable modal form that stays open until the supplied validation or save callback succeeds
public final class FormDialog {

    // Private constructor to prevent instantiation of this utility class
    private FormDialog() { }

    // Displays a modal dialog with the given form content and executes the callback on confirmation
    // Returns true if the user confirmed and the callback succeeded, false otherwise
    public static boolean show(String title, String okText, Node form, BooleanSupplier onOk) {
        // Initialize the dialog and configure its title, header, content, and stylesheet
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(title);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getStylesheets().add(Ui.STYLESHEET);

        // Create and add the custom confirmation button alongside a standard cancel button
        ButtonType ok = new ButtonType(okText, ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        
        // Retrieve the rendered confirmation button node and apply primary styling
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ok);
        okButton.getStyleClass().add("btn-primary");
        
        // Intercept the confirmation click to validate/save; consume the event to keep the dialog open if it fails
        okButton.addEventFilter(ActionEvent.ACTION, ev -> {    
        	if (!onOk.getAsBoolean()) 
        		ev.consume();
        });
        
        // Block until the user closes the dialog and return whether the confirmation button was selected
        return dialog.showAndWait().filter(b -> b == ok).isPresent();
    }
}