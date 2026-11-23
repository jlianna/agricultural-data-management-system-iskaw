package application.storage;

import application.model.Order;
import application.model.SaleLine;
import application.model.SaleTransaction;
import application.model.Transaction;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Saves the items of every sale and order to lines.txt. */
public class SaleLineFileWriter {
    public static void write(Path path, List<Transaction> transactions, List<Order> orders) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Transaction t : transactions) {
            if (t instanceof SaleTransaction) add(lines, t.getId(), ((SaleTransaction) t).getLines());
        }
        for (Order o : orders) add(lines, o.getId(), o.getLines());
        Records.write(path, "Iskaw sale/order items: owner id|product id|product name|quantity|price paid", lines);
    }

    private static void add(List<String> out, String ownerId, List<SaleLine> items) {
        for (SaleLine l : items) {
            out.add(Records.row(ownerId, l.getProductId(), l.getProductName(), l.getQuantity(), l.getUnitPrice()));
        }
    }
}
