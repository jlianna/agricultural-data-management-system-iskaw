package application.model;

/**
 * A registered customer. Customers can browse the product catalogue and see their own
 * purchase history, but cannot record transactions or change any data.
 */
public class Customer extends User {
    private static final long serialVersionUID = 1L;

    public Customer(String username, String fullName, String password) {
        super(username, fullName, password);
    }

    @Override 
    public String getRole() { 
    	return "Customer"; 
    }
    @Override 
    public boolean isManager() { 
    	return false; 
    	}
    @Override public boolean isStaff() 
    { 
    	return false; 
    	}
}
