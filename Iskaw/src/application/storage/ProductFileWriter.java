package application.storage;

import application.model.Inventory;
import application.model.Product;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Saves the catalogue, with each product's stock level and photo file name, to products.txt. */
public class ProductFileWriter {
    public static void write(Path path, List<Product> products, Inventory inventory) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Product p : products) {
            lines.add(Records.row(p.getId(), p.getName(), p.getDescription(), p.getCategory(), p.getUnit(),
                    p.getPrice(), inventory.getStock(p.getId()), p.getImageFile()));
        }
        Records.write(path, "Iskaw products: id|name|description|category|unit|price|stock|photo file", lines);
    }
}
