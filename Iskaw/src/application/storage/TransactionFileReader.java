package application.storage;

import application.model.CashDonation;
import application.model.ItemDonation;
import application.model.SaleLine;
import application.model.SaleTransaction;
import application.model.Transaction;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads sales, cash donations and item donations (voided ones too) from transactions.txt.
 * Common start: type|id|time|recorded by|voided|voided by|voided at
 * then SALE: customer|customer username|cash tendered; CASH_DONATION: donor|amount;
 * ITEM_DONATION: donor|product id|product name|quantity
 */
public class TransactionFileReader {
    /** @param lines the items of each sale, from {@link SaleLineFileReader} */
    public static List<Transaction> read(Path path, Map<String, List<SaleLine>> lines) throws IOException {
        return Records.load(path, records -> {
            List<Transaction> list = new ArrayList<>();
            for (String[] f : records) {
                Transaction t;
                switch (f[0]) {
                    case "SALE":
                        t = new SaleTransaction(f[1], f[3], f[7], f[8],
                                lines.getOrDefault(f[1], new ArrayList<>()), Double.parseDouble(f[9]));
                        break;
                    case "CASH_DONATION":
                        t = new CashDonation(f[1], f[3], f[7], Double.parseDouble(f[8]));
                        break;
                    case "ITEM_DONATION":
                        t = new ItemDonation(f[1], f[3], f[7], f[8], f[9], Integer.parseInt(f[10]));
                        break;
                    default:
                        throw new IllegalArgumentException("unknown transaction type " + f[0]);
                }
                t.restoreState(time(f[2]), Boolean.parseBoolean(f[4]), f[5], time(f[6]));
                list.add(t);
            }
            return list;
        });
    }

    private static LocalDateTime time(String s) {
        return s == null ? null : LocalDateTime.parse(s);
    }
}
