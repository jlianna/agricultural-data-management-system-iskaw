package application.storage;

import application.model.Order;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Saves the customer orders and their status to orders.txt. */
public class OrderFileWriter {
    public static void write(Path path, List<Order> orders) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Order o : orders) {
            lines.add(Records.row(o.getId(), o.getTimestamp(), o.getCustomerUsername(), o.getCustomerName(),
                    o.getStatus().name(), o.getHandledBy(), o.getHandledAt(), o.getSaleId()));
        }
        Records.write(path, "Iskaw orders: id|time|customer username|customer name|status|handled by|handled at|sale id", lines);
    }
}
