package application.model;
//Records sales and donations, checks inventory, and may void own transactions.
public class Clerk extends User {
   private static final long serialVersionUID = 1L;
   public Clerk(String username, String fullName, String password) {
       super(username, fullName, password);
   }
   //Used when loading saved data. 
   public Clerk(String username, String fullName, String passwordHash, boolean alreadyHashed) {
       super(username, fullName, passwordHash, alreadyHashed);
   }
   
   //return the role as Clerk
   @Override 
   public String getRole() { 
	   return "Clerk"; 
   }
   
   //return false
   @Override 
   public boolean isManager() { 
	   return false; 
   }
}
