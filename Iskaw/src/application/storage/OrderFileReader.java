package application.storage;

import application.model.Order;
import application.model.SaleLine;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads the customer orders from orders.txt.
 * Line: id|time|customer username|customer name|status|handled by|handled at|sale id
 */
public class OrderFileReader {
    // order items are already loaded from lines.txt and is grouped by order id
    public static List<Order> read(Path path, Map<String, List<SaleLine>> lines) throws IOException {
        return Records.load(path, records -> {
            List<Order> orders = new ArrayList<>();
            for (String[] f : records) {
            	// f[0] id, f[2] username, f[3] name, and the saved items
                Order o = new Order(f[0], f[2], f[3], lines.getOrDefault(f[0], new ArrayList<>()));
                // put back the original time, status and who handled it
                // oder.status.valueOf() turns the text into the enum value
                o.restoreState(time(f[1]), Order.Status.valueOf(f[4]), f[5], time(f[6]), f[7]);
                orders.add(o);
            }
            return orders;
        });
    }
    // date and time
    // missing value (null) stays null
    private static LocalDateTime time(String s) {
        return s == null ? null : LocalDateTime.parse(s);
    }
}
