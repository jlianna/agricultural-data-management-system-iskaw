package application.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** A customer purchase paid in cash. */
public class SaleTransaction extends Transaction {
    private static final long serialVersionUID = 1L;

    private final String customer;
    private final List<SaleLine> lines;
    private final double total;
    private final double cashTendered;

    public SaleTransaction(String id, String recordedBy, String customer, List<SaleLine> lines, double cashTendered) {
        super(id, recordedBy);
        if (lines == null || lines.isEmpty()) throw new IllegalArgumentException("A sale needs at least one item.");
        this.lines = new ArrayList<>(lines);
        this.total = lines.stream().mapToDouble(SaleLine::getSubtotal).sum();
        if (cashTendered + 0.0001 < total)
            throw new IllegalArgumentException("Cash tendered is less than the total amount due.");
        this.cashTendered = cashTendered;
        this.customer = (customer == null || customer.trim().isEmpty()) ? "Walk-in customer" : customer.trim();
    }

    public List<SaleLine> getLines() { return Collections.unmodifiableList(lines); }
    public double getTotal() { return total; }
    public double getCashTendered() { return cashTendered; }
    public double getChange() { return cashTendered - total; }
    public int getItemCount() { return lines.stream().mapToInt(SaleLine::getQuantity).sum(); }

    @Override public String getTypeLabel() { return "Sale"; }
    @Override public String getParty() { return customer; }
    @Override public double getCashAmount() { return total; }

    @Override
    public String getDetails() {
        return lines.stream().map(l -> l.getQuantity() + "x " + l.getProductName()).collect(Collectors.joining(", "));
    }

    @Override
    public void applyTo(Inventory inventory) throws InsufficientStockException {
        for (SaleLine l : lines) { // check everything first so the sale is all-or-nothing
            int available = inventory.getStock(l.getProductId());
            if (available < l.getQuantity()) throw new InsufficientStockException(l.getProductName(), available);
        }
        for (SaleLine l : lines) inventory.deduct(l.getProductId(), l.getProductName(), l.getQuantity());
    }

    @Override
    protected void reverseFrom(Inventory inventory) {
        for (SaleLine l : lines) inventory.restock(l.getProductId(), l.getQuantity());
    }
}
