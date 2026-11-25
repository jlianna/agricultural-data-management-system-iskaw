package application.model;
//A cash donation recorded in the wallet.
public class CashDonation extends Transaction {
	// Version ID used when saving or loading this object 
   private static final long serialVersionUID = 1L;
   private final String donor;
   private final double amount;


   //creates a new cash donation and checks the inputs
   public CashDonation(String id, String recordedBy, String donor, double amount) {
	   //pass the id and recorder info to the parent Transaction class
       super(id, recordedBy);
       // make sure the amount is a real number greater than zero
       if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0)
           throw new IllegalArgumentException("Donation amount must be greater than zero.");
	// Save the amount and clean up the donor name (use "Anonymous" if empty) 
       this.amount = amount;
       this.donor = (donor == null || donor.trim().isEmpty()) ? "Anonymous" : donor.trim();
   }
   // Return the label shown for this type of transaction 
	@Override 
	public String getTypeLabel() { 
		return "Cash Donation"; 
	}
	// Return who gave the donation 
	@Override 
	public String getParty() { 
		return donor; 
	}
	// Return a short text description of the donation 
	@Override public String getDetails() { 
		return "Cash donation"; 
	}
	
	
	// Return how much cash was donated 
	@Override 
	public double getCashAmount() { 
		return amount; 
	}
	// Do nothing to inventory because this is cash only 
	@Override public void applyTo(Inventory inventory) { }
	
	
	//Do nothing to inventory when undoing because this is cash only 
	@Override protected void reverseFrom(Inventory inventory) { }
}
