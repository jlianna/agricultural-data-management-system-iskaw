package application.model;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Base class of all system users. Passwords are stored as SHA-256 hashes. */
public abstract class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String username;
    private final String fullName;
    private final String passwordHash;

    protected User(String username, String fullName, String password) {
        if (username == null || username.trim().isEmpty()) throw new IllegalArgumentException("Username is required.");
        if (fullName == null || fullName.trim().isEmpty()) throw new IllegalArgumentException("Full name is required.");
        if (password == null || password.length() < 6)
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        this.username = username.trim().toLowerCase();
        this.fullName = fullName.trim();
        this.passwordHash = hash(password);
    }

    public String getUsername() { return username; }
    public String getFullName() { return fullName; }

    public boolean checkPassword(String password) {
        return password != null && passwordHash.equals(hash(password));
    }

    /** Human readable role name, e.g. "Manager". */
    public abstract String getRole();

    /** Managers have full read/write access; clerks have restricted access. */
    public abstract boolean isManager();

    private static String hash(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest(s.getBytes(StandardCharsets.UTF_8))) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String toString() {
        return fullName + " (" + getRole() + ")";
    }
}
