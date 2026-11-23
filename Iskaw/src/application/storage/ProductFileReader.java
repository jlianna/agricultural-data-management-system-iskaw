package application.storage;

import application.model.Inventory;
import application.model.Product;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Loads the catalogue from products.txt. Line: id|name|description|category|unit|price|stock|photo file */
public class ProductFileReader {
    /** @param inventory receives the saved stock level of every product */
    public static List<Product> read(Path path, Inventory inventory) throws IOException {
        // read into a fresh inventory first so a damaged file never leaves half of its stock behind
        return Records.load(path, records -> {
            List<Product> products = new ArrayList<>();
            Inventory stock = new Inventory();
            for (String[] f : records) {
                Product p = new Product(f[0], f[1], f[2], f[3], f[4], Double.parseDouble(f[5]));
                p.setImageFile(f[7]);
                products.add(p);
                stock.setStock(p.getId(), Math.max(0, Integer.parseInt(f[6])));
            }
            stock.snapshot().forEach(inventory::setStock);
            return products;
        });
    }
}
