package application.model;

// Represents an in-kind item donation designated for charity (does not affect business inventory).
public class ItemDonation extends Transaction {
    private static final long serialVersionUID = 1L;

    private final String donor;
    private final String itemName;
    private final int quantity;
    private final String notes;
    private int donatedQuantity;
    private String charityDistributionNotes;

    public ItemDonation(String id, String recordedBy, String donor, String itemName, int quantity, String notes) {
        super(id, recordedBy);
        if (itemName == null || itemName.trim().isEmpty()) {
            throw new IllegalArgumentException("Please enter the name of the donated item.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }
        this.donor = (donor == null || donor.trim().isEmpty()) ? "Anonymous" : donor.trim();
        this.itemName = itemName.trim();
        this.quantity = quantity;
        this.notes = notes == null ? "" : notes.trim();
        this.donatedQuantity = 0;
        this.charityDistributionNotes = "";
    }

    public String getDonor() { return donor; }
    public String getItemName() { return itemName; }
    public int getQuantity() { return quantity; }
    public String getNotes() { return notes; }
    public int getDonatedQuantity() { return donatedQuantity; }
    public int getRemainingQuantity() { return Math.max(0, quantity - donatedQuantity); }
    public String getCharityDistributionNotes() { return charityDistributionNotes; }

    // Restores persisted charity dispatch progress when loading from file
    public void restoreDistribution(int donatedQuantity, String distributionNotes) {
        this.donatedQuantity = Math.max(0, Math.min(quantity, donatedQuantity));
        this.charityDistributionNotes = distributionNotes == null ? "" : distributionNotes.trim();
    }

    // Dispatches a specified quantity of this donated item to a charity
    public void donateToCharity(int qtyToDonate, String note) {
        if (isVoided()) {
            throw new IllegalStateException("Cannot donate a voided item entry (" + getId() + ").");
        }
        if (qtyToDonate <= 0) {
            throw new IllegalArgumentException("Quantity to donate for \"" + itemName + "\" must be greater than zero.");
        }
        if (qtyToDonate > getRemainingQuantity()) {
            throw new IllegalArgumentException("Only " + getRemainingQuantity() + " remaining for \"" + itemName + "\".");
        }
        this.donatedQuantity += qtyToDonate;
        if (note != null && !note.trim().isEmpty()) {
            this.charityDistributionNotes = this.charityDistributionNotes.isEmpty()
                    ? note.trim()
                    : this.charityDistributionNotes + "; " + note.trim();
        }
    }

    public String getCharityStatus() {
        if (isVoided()) return "Voided";
        if (donatedQuantity == 0) return "Ready for Charity";
        if (donatedQuantity < quantity) return "Partially Donated (" + donatedQuantity + "/" + quantity + ")";
        return "Donated to Charity";
    }

    @Override
    public String getTypeLabel() {
        return "Item Donation";
    }

    @Override
    public String getParty() {
        return donor;
    }

    @Override
    public String getDetails() {
        String base = quantity + " x " + itemName;
        return notes.isEmpty() ? base : base + " (" + notes + ")";
    }

    @Override
    public boolean isInKind() {
        return true;
    }

    @Override
    public double getCashAmount() {
        return 0.0;
    }

    @Override
    public void applyTo(Inventory inventory) {
        // Charity donations do not add to business inventory
    }

    @Override
    protected void reverseFrom(Inventory inventory) {
        if (donatedQuantity > 0) {
            throw new IllegalStateException("Cannot void " + getId() + " because items have already been donated to charity.");
        }
    }
}