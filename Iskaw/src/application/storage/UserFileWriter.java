package application.storage;

import application.model.User;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Saves the accounts to users.txt (passwords only as SHA-256 hashes)
public class UserFileWriter {
	// list of user objects
    public static void write(Path path, List<User> users) throws IOException {
        List<String> lines = new ArrayList<>();
        for (User u : users) {
        	// getRole() gets the role and records.row joins them with |
            lines.add(Records.row(u.getRole(), u.getUsername(), u.getFullName(), u.getPasswordHash()));
        }
        // the header line written at the top of the file
        Records.write(path, "Iskaw accounts: role|username|full name|password hash", lines);
    }
}
