package application.storage;

import application.model.Inventory;
import application.model.Product;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Loads the catalog from products.txt. Line: id|name|description|category|unit|price|stock|photo file 
public class ProductFileReader {
    // parameter inventory receives the saved stock level of every product 
    public static List<Product> read(Path path, Inventory inventory) throws IOException {
        // read into a fresh inventory first so a damaged file never leaves half of its stock behind
        return Records.load(path, records -> {
            List<Product> products = new ArrayList<>();
            Inventory stock = new Inventory(); // Temporary inventory
            for (String[] f : records) {
            	// f[5] is the price as text so we parse it to turn it into a number
                Product p = new Product(f[0], f[1], f[2], f[3], f[4], Double.parseDouble(f[5]));
                p.setImageFile(f[7]); // photo file name
                products.add(p); 
                // f[6] is the stock and math.max makes sure it is not negative
                stock.setStock(p.getId(), Math.max(0, Integer.parseInt(f[6]))); 
            }
            // copy the stock into the real inventory
            stock.snapshot().forEach(inventory::setStock);
            // return the products
            return products;
        });
    }
}
