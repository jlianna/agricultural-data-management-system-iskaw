package application.storage;

import application.model.Clerk;
import application.model.Customer;
import application.model.Manager;
import application.model.User;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Loads the accounts from users.txt. Line: role|username|full name|password hash
public class UserFileReader {
	// list of user objects
    public static List<User> read(Path path) throws IOException {
        // records.load reads the file ( with the back up file as fallback)
    	// and runs this lamda on the lines
    	return Records.load(path, records -> {
            // declare empty storage list
            List<User> users = new ArrayList<>();
            for (String[] f : records) { // fields of one line
                switch (f[0]) { // role which decides which class to create
                    case "Manager": users.add(new Manager(f[1], f[2], f[3], true)); break;
                    case "Clerk": users.add(new Clerk(f[1], f[2], f[3], true)); break;
                    case "Customer": users.add(new Customer(f[1], f[2], f[3], true)); break;
                    default: throw new IllegalArgumentException("unknown account type " + f[0]);
                }
            }
            // return the list of users
            return users;
        });
    }
}
