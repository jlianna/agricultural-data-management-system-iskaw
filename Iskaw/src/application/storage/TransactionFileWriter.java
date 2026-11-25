package application.storage;

import application.model.CashDonation;
import application.model.ItemDonation;
import application.model.SaleTransaction;
import application.model.Transaction;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Saves every transaction (sales and donations, including voided ones) to transactions.txt
public class TransactionFileWriter {
    public static void write(Path path, List<Transaction> transactions) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Transaction t : transactions) {
        	// the 7 columns of every transaction has slot 0 as the type
            Object[] common = {null, t.getId(), t.getTimestamp(), t.getRecordedBy(), t.isVoided(), t.getVoidedBy(), t.getVoidedAt()};
            Object[] specific; // extra columns of object type
            if (t instanceof SaleTransaction) {
                SaleTransaction s = (SaleTransaction) t;
                common[0] = "SALE";
                specific = new Object[] {s.getParty(), s.getCustomerUsername(), s.getCashTendered()};
            } else if (t instanceof CashDonation) {
                common[0] = "CASH_DONATION";
                specific = new Object[] {t.getParty(), t.getCashAmount()};
            } else if (t instanceof ItemDonation) {
                ItemDonation d = (ItemDonation) t;
                common[0] = "ITEM_DONATION";
                specific = new Object[] {d.getParty(), d.getItemName(), d.getQuantity(), d.getNotes()};
            } else {
                throw new IllegalStateException("Unknown transaction class " + t.getClass().getName());
            }
            // joins the two arrays together into one array of columns
            Object[] all = new Object[common.length + specific.length];
            System.arraycopy(common, 0, all, 0, common.length);
            System.arraycopy(specific, 0, all, common.length, specific.length);
            lines.add(Records.row(all));
        }
        // the header line written at the top of the file
        Records.write(path, "Iskaw transactions: type|id|time|recorded by|voided|voided by|voided at|...", lines);
    }
}
