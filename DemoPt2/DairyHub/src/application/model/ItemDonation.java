package application.model;

/** A donation of dairy products; the donated items are added to inventory. */
public class ItemDonation extends Transaction {
    private static final long serialVersionUID = 1L;

    private final String donor;
    private final String productId;
    private final String productName;
    private final int quantity;

    public ItemDonation(String id, String recordedBy, String donor, Product product, int quantity) {
        super(id, recordedBy);
        if (product == null) throw new IllegalArgumentException("Select a product.");
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than zero.");
        this.productId = product.getId();
        this.productName = product.getName();
        this.quantity = quantity;
        this.donor = (donor == null || donor.trim().isEmpty()) ? "Anonymous" : donor.trim();
    }

    @Override public String getTypeLabel() { return "Item Donation"; }
    @Override public String getParty() { return donor; }
    @Override public String getDetails() { return quantity + "x " + productName; }
    @Override public double getCashAmount() { return 0; }
    @Override public boolean isInKind() { return true; }

    @Override
    public void applyTo(Inventory inventory) {
        inventory.restock(productId, quantity);
    }

    @Override
    protected void reverseFrom(Inventory inventory) throws InsufficientStockException {
        inventory.deduct(productId, productName, quantity);
    }
}
