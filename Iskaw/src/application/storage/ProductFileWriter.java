package application.storage;

import application.model.Inventory;
import application.model.Product;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

//  Saves the catalog, with each product's stock level and photo file name, to products.txt
public class ProductFileWriter {
	// list of products objects and stock levels
    public static void write(Path path, List<Product> products, Inventory inventory) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Product p : products) {
        	// add the products
            lines.add(Records.row(p.getId(), p.getName(), p.getDescription(), p.getCategory(), p.getUnit(),
                    p.getPrice(), inventory.getStock(p.getId()), p.getImageFile())); 
        }
        // the header line written at the top of the file
        Records.write(path, "Iskaw products: id|name|description|category|unit|price|stock|photo file", lines);
    }
}
