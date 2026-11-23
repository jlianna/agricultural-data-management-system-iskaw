package application.model;

/** Full read/write access: products, inventory, transactions, clerk monitoring. */
public class Manager extends User {
    private static final long serialVersionUID = 1L;

    public Manager(String username, String fullName, String password) {
        super(username, fullName, password);
    }

    @Override public String getRole() { return "Manager"; }
    @Override public boolean isManager() { return true; }
}
