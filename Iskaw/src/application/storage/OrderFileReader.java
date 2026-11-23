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
    /** @param lines the items of each order, from {@link SaleLineFileReader} */
    public static List<Order> read(Path path, Map<String, List<SaleLine>> lines) throws IOException {
        return Records.load(path, records -> {
            List<Order> orders = new ArrayList<>();
            for (String[] f : records) {
                Order o = new Order(f[0], f[2], f[3], lines.getOrDefault(f[0], new ArrayList<>()));
                o.restoreState(time(f[1]), Order.Status.valueOf(f[4]), f[5], time(f[6]), f[7]);
                orders.add(o);
            }
            return orders;
        });
    }

    private static LocalDateTime time(String s) {
        return s == null ? null : LocalDateTime.parse(s);
    }
}
