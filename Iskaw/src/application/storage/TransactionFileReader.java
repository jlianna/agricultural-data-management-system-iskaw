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
	// sale items already loaded from lines.txt and grouped by sale id
    public static List<Transaction> read(Path path, Map<String, List<SaleLine>> lines) throws IOException {
        return Records.load(path, records -> {
            List<Transaction> list = new ArrayList<>();
            for (String[] f : records) {
                Transaction t;
                switch (f[0]) { // f[0] is the type word
                    case "SALE":
                    	// f[1] id, f[3] recorded by, f[7] customer, f[8] username, f[9] cash given
                        t = new SaleTransaction(f[1], f[3], f[7], f[8],
                                lines.getOrDefault(f[1], new ArrayList<>()), Double.parseDouble(f[9]));
                        break;
                    case "CASH_DONATION":
                        t = new CashDonation(f[1], f[3], f[7], Double.parseDouble(f[8]));
                        break;
                    case "ITEM_DONATION":
                        if (f.length >= 11 && f[10].matches("\\d+")) {
                            // Legacy format: donor|productId|productName|quantity
                            t = new ItemDonation(f[1], f[3], f[7], f[9], Integer.parseInt(f[10]), "");
                        } else {
                            // Current format: donor|itemName|quantity|notes
                            String notes = f.length > 10 ? f[10] : "";
                            t = new ItemDonation(f[1], f[3], f[7], f[8], Integer.parseInt(f[9]), notes);
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("unknown transaction type " + f[0]);
                }
                // the constructors stamp 'now' as the time and not 'voided'
                // restoreState puts back the original time and the void details
                t.restoreState(time(f[2]), Boolean.parseBoolean(f[4]), f[5], time(f[6]));
                list.add(t);
            }
            return list;
        });
    }
    
    // date and time
    // missing value (null) stays null
    private static LocalDateTime time(String s) {
        return s == null ? null : LocalDateTime.parse(s);
    }
}
