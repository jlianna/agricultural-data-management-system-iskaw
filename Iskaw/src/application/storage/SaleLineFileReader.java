package application.storage;

import application.model.SaleLine;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the items of every sale and order from lines.txt.
 * Line: owner id (e.g. T-00001 or O-00001)|product id|product name|quantity|price paid
 */
public class SaleLineFileReader {
    // returns the items of each sale / order, keyed by the id of the sale / order they belong to
    public static Map<String, List<SaleLine>> read(Path path) throws IOException {
        return Records.load(path, records -> {
            // declare a hash map for storage
            Map<String, List<SaleLine>> byOwner = new LinkedHashMap<>();
            for (String[] f : records) { 
            	// retrieve the existing item list for the owner id or create a new one if missing
            	// then append the newly created Saleline to it
                byOwner.computeIfAbsent(f[0], k -> new ArrayList<>())
                        .add(new SaleLine(f[1], f[2], Integer.parseInt(f[3]), Double.parseDouble(f[4])));
            }
            return byOwner;
        });
    }
}
