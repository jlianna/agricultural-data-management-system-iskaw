package application.ui;

import application.model.ProductSystem;
import application.model.Product;
import application.model.User;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import java.io.File;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

// Manager-only product catalogue screen for adding, editing, and deleting products
public class ProductsView extends View {
    private final ProductSystem system;
    private final User user;
    private final TableView<Product> table = new TableView<>();

    // Form input fields, action buttons, and temporary photo state trackers
    private final TextField name = new TextField();
    private final TextArea description = new TextArea();
    private final ComboBox<String> category = new ComboBox<>();
    private final TextField unit = new TextField();
    private final TextField price = new TextField();
    private final TextField initialStock = new TextField();
    private final Label formTitle = Ui.sectionTitle("Add a product");
    private final Button save;
    private final Button delete;
    private final VBox photoHolder = new VBox();
    private final Button removePhotoButton;
    private File pendingPhoto;          // picture chosen but not saved yet
    private boolean removePhoto;        // "Remove photo" pressed but not saved yet
    private File lastFolder;

    // Initializes the split layout with the product catalogue table on the left and the editor form on the right
    public ProductsView(ProductSystem system, User user) {
        this.system = system;
        this.user = user;

        // Configure table sizing, empty placeholder, and custom square photo thumbnail column
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No products yet. Add your first one on the right."));
        table.setFixedCellSize(46);
        TableColumn<Product, Product> photoCol = new TableColumn<>("Photo");
        photoCol.setMinWidth(60);
        photoCol.setMaxWidth(60);
        photoCol.setSortable(false);
        photoCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        photoCol.setCellFactory(col -> new TableCell<Product, Product>() {
            // Renders a 36x36 square thumbnail or placeholder inside each product row
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : ProductImages.square(system, item, 36));
                setAlignment(Pos.CENTER);
            }
        });

        // Add product detail columns and bind row selection to populate the editor form
        table.getColumns().add(photoCol);
        table.getColumns().add(Ui.column("Product", 190, Product::getName));
        table.getColumns().add(Ui.column("Category", 110, Product::getCategory));
        table.getColumns().add(Ui.column("Size", 70, Product::getUnit));
        table.getColumns().add(Ui.moneyColumn("Price", 80, p -> Ui.money(p.getPrice())));
        table.getColumns().add(Ui.moneyColumn("In stock", 70,
                p -> String.valueOf(system.getInventory().getStock(p.getId()))));
        table.getSelectionModel().selectedItemProperty().addListener((o, a, p) -> fill(p));
        VBox.setVgrow(table, Priority.ALWAYS);

        // Configure text inputs and editable category dropdown prompts
        description.setPrefRowCount(3);
        description.setWrapText(true);
        category.setEditable(true);
        category.setMaxWidth(Double.MAX_VALUE);
        category.setPromptText("Choose or type a category");
        unit.setPromptText("e.g. 1 L, 200 g");
        price.setPromptText("0.00");
        initialStock.setPromptText("0");

        // Initialize CRUD action buttons for adding, updating, deleting, and clearing form selections
        Button add = Ui.button("Add as new product", "btn-accent", this::addProduct);
        save = Ui.button("Save changes", "btn-primary", this::saveChanges);
        delete = Ui.button("Delete", "btn-danger", this::deleteProduct);
        Button clear = Ui.button("Clear", "btn-secondary", () -> table.getSelectionModel().clearSelection());
        add.setMaxWidth(Double.MAX_VALUE);
        save.setMaxWidth(Double.MAX_VALUE);
        HBox row2 = new HBox(8, delete, clear);

        // Configure the product photo preview box and choose/remove image buttons
        Button choosePhoto = Ui.button("Choose photo…", "btn-secondary", this::choosePhoto);
        removePhotoButton = Ui.button("Remove", "btn-secondary", this::clearPhoto);
        VBox photoButtons = new VBox(8, choosePhoto, removePhotoButton,
                Ui.subtle("PNG, JPG, GIF or BMP, up to 8 MB."));
        photoHolder.getStyleClass().add("photo-frame");
        photoHolder.setMinSize(110, 110);
        photoHolder.setMaxSize(110, 110);
        HBox photoRow = new HBox(12, photoHolder, photoButtons);
        photoRow.setAlignment(Pos.CENTER_LEFT);

        // Assemble all form fields and buttons into the right-hand card container
        VBox form = new VBox(8, formTitle,
                Ui.fieldLabel("PHOTO"), photoRow,
                Ui.fieldLabel("NAME"), name,
                Ui.fieldLabel("DESCRIPTION"), description,
                Ui.fieldLabel("CATEGORY"), category,
                Ui.fieldLabel("SIZE / UNIT"), unit,
                Ui.fieldLabel("PRICE (₱)"), price,
                Ui.fieldLabel("OPENING STOCK (NEW PRODUCTS ONLY)"), initialStock,
                add, save, row2);
        form.getStyleClass().add("card");
        form.setPrefWidth(300);

        // the form can be taller than the window, so it scrolls instead of being cut off
        ScrollPane formScroll = new ScrollPane(form);
        formScroll.setFitToWidth(true);
        formScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        formScroll.getStyleClass().add("plain-scroll");
        formScroll.setPrefWidth(335);
        formScroll.setMinWidth(335);

        // Combine the header, product table, and scrollable form into the main view
        HBox body = new HBox(16, table, formScroll);
        HBox.setHgrow(table, Priority.ALWAYS);
        VBox.setVgrow(body, Priority.ALWAYS);
        HBox titleRow = new HBox(14, Ui.heading("Products"),
                Ui.subtle("Select a product to edit it, or fill the form to add a new one."));
        titleRow.setAlignment(Pos.BOTTOM_LEFT);
        getChildren().addAll(titleRow, body);
        fill(null);
    }

    // Populates the form with the selected product's details or resets it for creating a new product
    private void fill(Product p) {
        boolean editing = p != null;
        formTitle.setText(editing ? "Edit product" : "Add a product");
        save.setDisable(!editing);
        delete.setDisable(!editing);
        initialStock.setDisable(editing);
        pendingPhoto = null;
        removePhoto = false;
        showPhoto(p);

        // Load existing values when editing, or clear all inputs when adding a new product
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

    // ------------------------------------------------------------ photo

    // Updates the photo preview box based on pending file selection, removal state, or saved product photo
    private void showPhoto(Product p) {
        photoHolder.getChildren().clear();
        if (pendingPhoto != null) {
            photoHolder.getChildren().add(ProductImages.squareFromFile(pendingPhoto, 110));
        } else if (removePhoto) {
            photoHolder.getChildren().add(ProductImages.square(system, null, 110));
        } else {
            photoHolder.getChildren().add(ProductImages.square(system, p, 110));
        }
        removePhotoButton.setDisable(pendingPhoto == null && (removePhoto || p == null || !p.hasImage()));
    }

    // Opens a file chooser dialog for selecting a local image file to stage as the product photo
    private void choosePhoto() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose a product photo");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Pictures", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
        if (lastFolder != null && lastFolder.isDirectory()) fc.setInitialDirectory(lastFolder);
        File f = fc.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (f == null) return;
        lastFolder = f.getParentFile();
        pendingPhoto = f;
        removePhoto = false;
        showPhoto(table.getSelectionModel().getSelectedItem());
    }

    // Clears any staged photo and flags the existing product photo for removal upon saving
    private void clearPhoto() {
        pendingPhoto = null;
        removePhoto = true;
        showPhoto(table.getSelectionModel().getSelectedItem());
    }

    // Retrieves the typed category text from the editable ComboBox editor or falls back to its selected value
    private String categoryText() {
        // an editable ComboBox keeps typed text in its editor until committed
        String typed = category.getEditor().getText();
        return typed == null || typed.trim().isEmpty() ? category.getValue() : typed;
    }

    // Validates form inputs, creates a new product with optional opening stock and photo, and resets the form
    private void addProduct() {
        Ui.attempt(() -> {
            int stock = initialStock.getText().trim().isEmpty() ? 0 : Ui.parseWhole(initialStock.getText(), "Opening stock");
            Product added = system.addProduct(user, name.getText(), description.getText(), categoryText(),
                    unit.getText(), Ui.parseMoney(price.getText(), "Price"), stock);

            // Attach the pending photo if one was selected, catching image errors without rolling back product creation
            String photoProblem = null;
            if (pendingPhoto != null) {
                try {
                    system.setProductImage(user, added, pendingPhoto.toPath());
                } catch (RuntimeException ex) {
                    photoProblem = ex.getMessage();
                }
            }

            // Reset form selection, reload table data, and report any non-fatal image upload warning
            table.getSelectionModel().clearSelection();
            refresh();
            fill(null);
            if (photoProblem != null)
                Ui.error("The product was added, but its photo could not be saved: " + photoProblem);
        });
    }

    // Updates the currently selected product's metadata and applies any pending photo addition or removal
    private void saveChanges() {
        Product p = table.getSelectionModel().getSelectedItem();
        if (p == null) return;
        Ui.attempt(() -> {
            system.updateProduct(user, p, name.getText(), description.getText(), categoryText(), unit.getText(),
                    Ui.parseMoney(price.getText(), "Price"));
            if (pendingPhoto != null) system.setProductImage(user, p, pendingPhoto.toPath());
            else if (removePhoto) system.removeProductImage(user, p);
            ProductImages.forget();
            refresh();
            table.getSelectionModel().select(p);
        });
    }

    // Prompts for confirmation and deletes the selected product from the catalogue
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

    // Reloads the product list and available categories while preserving unsaved category input
    @Override
    public void refresh() {
        String keep = categoryText();
        table.getItems().setAll(system.getProducts());
        category.setItems(FXCollections.observableArrayList(system.getCategories()));
        if (table.getSelectionModel().getSelectedItem() == null && keep != null) category.getEditor().setText(keep);
        table.refresh();
    }
}