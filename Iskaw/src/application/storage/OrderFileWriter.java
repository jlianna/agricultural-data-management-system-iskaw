package application.storage;

import application.model.Order;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Saves the customer orders and their status to orders.txt. 
public class OrderFileWriter {
    public static void write(Path path, List<Order> orders) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Order o : orders) {
        	// records the orders
        	// getstatus().name() gets the enum text
        	// null while still pending
            lines.add(Records.row(o.getId(), o.getTimestamp(), o.getCustomerUsername(), o.getCustomerName(),
                    o.getStatus().name(), o.getHandledBy(), o.getHandledAt(), o.getSaleId()));
        }
        // the header line written at the top of the file
        Records.write(path, "Iskaw orders: id|time|customer username|customer name|status|handled by|handled at|sale id", lines);
    }
}
