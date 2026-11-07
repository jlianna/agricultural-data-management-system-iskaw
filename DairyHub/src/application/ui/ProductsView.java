package application.ui;

import application.model.DairySystem;
import application.model.Product;
import application.model.User;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Manager-only product catalogue: add, edit and delete products. */
public class ProductsView extends View {
    private final DairySystem system;
    private final User user;
    private final TableView<Product> table = new TableView<>();

    private final TextField name = new TextField();
    private final TextArea description = new TextArea();
    private final ComboBox<String> category = new ComboBox<>();
    private final TextField unit = new TextField();
    private final TextField price = new TextField();
    private final TextField initialStock = new TextField();
    private final Label formTitle = Ui.sectionTitle("Add a product");
    private final Button save;
    private final Button delete;

    public ProductsView(DairySystem system, User user) {
        this.system = system;
        this.user = user;

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No products yet. Add your first one on the right."));
        table.getColumns().add(Ui.column("Product", 190, Product::getName));
        table.getColumns().add(Ui.column("Category", 110, Product::getCategory));
        table.getColumns().add(Ui.column("Size", 70, Product::getUnit));
        table.getColumns().add(Ui.moneyColumn("Price", 80, p -> Ui.money(p.getPrice())));
        table.getColumns().add(Ui.moneyColumn("In stock", 70,
                p -> String.valueOf(system.getInventory().getStock(p.getId()))));
        table.getSelectionModel().selectedItemProperty().addListener((o, a, p) -> fill(p));
        VBox.setVgrow(table, Priority.ALWAYS);

        description.setPrefRowCount(3);
        description.setWrapText(true);
        category.setEditable(true);
        category.setMaxWidth(Double.MAX_VALUE);
        category.setPromptText("Choose or type a category");
        unit.setPromptText("e.g. 1 L, 200 g");
        price.setPromptText("0.00");
        initialStock.setPromptText("0");

        Button add = Ui.button("Add as new product", "btn-accent", this::addProduct);
        save = Ui.button("Save changes", "btn-primary", this::saveChanges);
        delete = Ui.button("Delete", "btn-danger", this::deleteProduct);
        Button clear = Ui.button("Clear", "btn-secondary", () -> table.getSelectionModel().clearSelection());
        add.setMaxWidth(Double.MAX_VALUE);
        save.setMaxWidth(Double.MAX_VALUE);
        HBox row2 = new HBox(8, delete, clear);

        VBox form = new VBox(8, formTitle,
                Ui.fieldLabel("NAME"), name,
                Ui.fieldLabel("DESCRIPTION"), description,
                Ui.fieldLabel("CATEGORY"), category,
                Ui.fieldLabel("SIZE / UNIT"), unit,
                Ui.fieldLabel("PRICE (₱)"), price,
                Ui.fieldLabel("OPENING STOCK (NEW PRODUCTS ONLY)"), initialStock,
                add, save, row2);
        form.getStyleClass().add("card");
        form.setPrefWidth(320);
        form.setMinWidth(320);

        HBox body = new HBox(16, table, form);
        HBox.setHgrow(table, Priority.ALWAYS);
        VBox.setVgrow(body, Priority.ALWAYS);
        HBox titleRow = new HBox(14, Ui.heading("Products"),
                Ui.subtle("Select a product to edit it, or fill the form to add a new one."));
        titleRow.setAlignment(Pos.BOTTOM_LEFT);
        getChildren().addAll(titleRow, body);
        fill(null);
    }

    private void fill(Product p) {
        boolean editing = p != null;
        formTitle.setText(editing ? "Edit product" : "Add a product");
        save.setDisable(!editing);
        delete.setDisable(!editing);
        initialStock.setDisable(editing);
        if (editing) {
            name.setText(p.getName());
            description.setText(p.getDescription());
            category.setValue(p.getCategory());
            unit.setText(p.getUnit());
            price.setText(String.format("%.2f", p.getPrice()));
            initialStock.clear();
        } else {
            name.clear();
            description.clear();
            category.setValue(null);
            category.getEditor().clear();
            unit.clear();
            price.clear();
            initialStock.clear();
        }
    }

    private String categoryText() {
        // an editable ComboBox keeps typed text in its editor until committed
        String typed = category.getEditor().getText();
        return typed == null || typed.trim().isEmpty() ? category.getValue() : typed;
    }

    private void addProduct() {
        Ui.attempt(() -> {
            int stock = initialStock.getText().trim().isEmpty() ? 0 : Ui.parseWhole(initialStock.getText(), "Opening stock");
            system.addProduct(user, name.getText(), description.getText(), categoryText(), unit.getText(),
                    Ui.parseMoney(price.getText(), "Price"), stock);
            table.getSelectionModel().clearSelection();
            refresh();
            fill(null);
        });
    }

    private void saveChanges() {
        Product p = table.getSelectionModel().getSelectedItem();
        if (p == null) return;
        Ui.attempt(() -> {
            system.updateProduct(user, p, name.getText(), description.getText(), categoryText(), unit.getText(),
                    Ui.parseMoney(price.getText(), "Price"));
            refresh();
            table.getSelectionModel().select(p);
        });
    }

    private void deleteProduct() {
        Product p = table.getSelectionModel().getSelectedItem();
        if (p == null) return;
        if (!Ui.confirm("Delete " + p.getName() + "?",
                "The product and its stock count will be removed. Past transactions keep their records.")) return;
        if (Ui.attempt(() -> system.deleteProduct(user, p))) {
            table.getSelectionModel().clearSelection();
            refresh();
        }
    }

    @Override
    public void refresh() {
        String keep = categoryText();
        table.getItems().setAll(system.getProducts());
        category.setItems(FXCollections.observableArrayList(system.getCategories()));
        if (table.getSelectionModel().getSelectedItem() == null && keep != null) category.getEditor().setText(keep);
        table.refresh();
    }
}
