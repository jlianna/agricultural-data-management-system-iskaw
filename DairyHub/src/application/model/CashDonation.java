package application.model;

/** A cash donation recorded in the wallet. */
public class CashDonation extends Transaction {
    private static final long serialVersionUID = 1L;

    private final String donor;
    private final double amount;

    public CashDonation(String id, String recordedBy, String donor, double amount) {
        super(id, recordedBy);
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0)
            throw new IllegalArgumentException("Donation amount must be greater than zero.");
        this.amount = amount;
        this.donor = (donor == null || donor.trim().isEmpty()) ? "Anonymous" : donor.trim();
    }

    @Override public String getTypeLabel() { return "Cash Donation"; }
    @Override public String getParty() { return donor; }
    @Override public String getDetails() { return "Cash donation"; }
    @Override public double getCashAmount() { return amount; }
    @Override public void applyTo(Inventory inventory) { /* cash only */ }
    @Override protected void reverseFrom(Inventory inventory) { /* cash only */ }
}
