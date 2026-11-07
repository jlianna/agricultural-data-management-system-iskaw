package application.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Read-only summary of activity. Built for one user (a clerk's own dashboard)
 * or for everyone (the manager's overview, when {@code owner} is null).
 */
public class Dashboard {

    /** One row of the manager's clerk-performance table. */
    public static class ClerkSummary {
        private final Clerk clerk;
        private final int salesCount;
        private final double salesTotal;
        private final double donationsTotal;
        private final int voidedCount;

        ClerkSummary(Clerk clerk, int salesCount, double salesTotal, double donationsTotal, int voidedCount) {
            this.clerk = clerk;
            this.salesCount = salesCount;
            this.salesTotal = salesTotal;
            this.donationsTotal = donationsTotal;
            this.voidedCount = voidedCount;
        }

        public Clerk getClerk() { return clerk; }
        public int getSalesCount() { return salesCount; }
        public double getSalesTotal() { return salesTotal; }
        public double getDonationsTotal() { return donationsTotal; }
        public int getVoidedCount() { return voidedCount; }
    }

    private final DairySystem system;
    private final User owner;
    private final List<Transaction> scope; // the transactions this dashboard covers

    public Dashboard(DairySystem system, User owner) {
        this.system = system;
        this.owner = owner;
        this.scope = owner == null ? system.getTransactions() : system.getTransactionsBy(owner.getUsername());
    }

    private Active active() { return new Active(); }

    /** Tiny helper so the totals below read clearly. */
    private class Active {
        List<Transaction> sales() {
            return scope.stream().filter(t -> t instanceof SaleTransaction && !t.isVoided()).collect(Collectors.toList());
        }
        List<Transaction> cashDonations() {
            return scope.stream().filter(t -> t instanceof CashDonation && !t.isVoided()).collect(Collectors.toList());
        }
        List<Transaction> itemDonations() {
            return scope.stream().filter(t -> t instanceof ItemDonation && !t.isVoided()).collect(Collectors.toList());
        }
    }

    public User getOwner() { return owner; }

    public double getSalesTotal() { return active().sales().stream().mapToDouble(Transaction::getCashAmount).sum(); }
    public int getSalesCount() { return active().sales().size(); }
    public double getCashDonationsTotal() {
        return active().cashDonations().stream().mapToDouble(Transaction::getCashAmount).sum();
    }
    public int getItemDonationCount() { return active().itemDonations().size(); }
    public int getVoidedCount() { return (int) scope.stream().filter(Transaction::isVoided).count(); }
    public double getWalletBalance() { return system.getWalletBalance(); }
    public int getLowStockCount() { return system.getLowStockProducts().size(); }

    /** Most recent transactions in scope, newest first. */
    public List<Transaction> getRecent(int limit) {
        return scope.stream().limit(limit).collect(Collectors.toList());
    }

    /** All sales in scope (including voided), newest first. */
    public List<SaleTransaction> getSalesHistory() {
        return scope.stream().filter(t -> t instanceof SaleTransaction)
                .map(t -> (SaleTransaction) t).collect(Collectors.toList());
    }

    /** Best-selling products by units sold (voided sales ignored). */
    public Map<String, Integer> getTopProducts(int limit) {
        Map<String, Integer> totals = new LinkedHashMap<>();
        for (Transaction t : active().sales())
            for (SaleLine l : ((SaleTransaction) t).getLines())
                totals.merge(l.getProductName(), l.getQuantity(), Integer::sum);
        return totals.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    /** Per-clerk figures for the manager's dashboard. */
    public List<ClerkSummary> getClerkSummaries() {
        return system.getClerks().stream().map(c -> {
            Dashboard d = new Dashboard(system, c);
            return new ClerkSummary(c, d.getSalesCount(), d.getSalesTotal(), d.getCashDonationsTotal(), d.getVoidedCount());
        }).collect(Collectors.toList());
    }
}
