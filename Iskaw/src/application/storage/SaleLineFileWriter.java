package application.storage;

import application.model.Order;
import application.model.SaleLine;
import application.model.SaleTransaction;
import application.model.Transaction;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Saves the items of every sale and order to lines.txt
public class SaleLineFileWriter {
    public static void write(Path path, List<Transaction> transactions, List<Order> orders) throws IOException {
        // storage for the formatted sales
    	List<String> lines = new ArrayList<>();
    	// extract items only from the actual sales
        for (Transaction t : transactions) {
            if (t instanceof SaleTransaction) add(lines, t.getId(), ((SaleTransaction) t).getLines());
        }
        // extract items
        for (Order o : orders) add(lines, o.getId(), o.getLines()); 
         // the header line written at the top of the file
        Records.write(path, "Iskaw sale/order items: owner id|product id|product name|quantity|price paid", lines);
    }
    
    // formats each saleline tagged with its order id
    private static void add(List<String> out, String ownerId, List<SaleLine> items) {
        for (SaleLine l : items) {
        	// joins the details into a single line
            out.add(Records.row(ownerId, l.getProductId(), l.getProductName(), l.getQuantity(), l.getUnitPrice()));
        }
    }
}
