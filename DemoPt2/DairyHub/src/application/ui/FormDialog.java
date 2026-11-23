package application.ui;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

import java.util.function.BooleanSupplier;

/** Reusable modal form: stays open until the supplied validation/save callback succeeds. */
public final class FormDialog {
    private FormDialog() { }

    /**
     * @param onOk called when the OK button is pressed; return false to keep the dialog open
     * @return true if the user confirmed and onOk succeeded
     */
    public static boolean show(String title, String okText, Node form, BooleanSupplier onOk) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(title);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getStylesheets().add(Ui.STYLESHEET);

        ButtonType ok = new ButtonType(okText, ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ok);
        okButton.getStyleClass().add("btn-primary");
        okButton.addEventFilter(ActionEvent.ACTION, ev -> {
            if (!onOk.getAsBoolean()) ev.consume();
        });
        return dialog.showAndWait().filter(b -> b == ok).isPresent();
    }
}
