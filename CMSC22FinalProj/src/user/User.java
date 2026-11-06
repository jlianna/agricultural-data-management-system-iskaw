package user;

public abstract class User {
	// attributes
	private String userId;
    private String username;
    private String password;
    private Role role;
    
    // constructor
    public User(String userId, String username, String password, Role role) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    // authentication check
    public boolean authenticate(String inputPassword) {
        return this.password.equals(inputPassword);
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public Role getRole() { return role; }

}
