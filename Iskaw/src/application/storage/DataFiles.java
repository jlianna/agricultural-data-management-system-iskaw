package application.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

//Where the data files live: the names of the files inside the data folder.
public class DataFiles {
	// data folder is set once object is created
    private final Path dir;

    public DataFiles(Path dir) { this.dir = dir; }

    public Path folder() { 
    	return dir; 
    }
    
    // dir.resolve builds the path so it works on windows, mac, and linux
    public Path users() { 
    	return dir.resolve("users.txt"); 
    }
    
    // dir.resolve builds the path so it works on windows, mac, and linux
    public Path products() { 
    	return dir.resolve("products.txt"); 
    }
    // dir.resolve builds the path so it works on windows, mac, and linux
    public Path transactions() { 
    	return dir.resolve("transactions.txt"); 
    }
    
    // dir.resolve builds the path so it works on windows, mac, and linux
    public Path orders() { 
    	return dir.resolve("orders.txt"); 
    }
    
    //The items of every sale and order.
    public Path lines() { 
    	return dir.resolve("lines.txt"); 
    }
    
    // product photos
    public Path images() { 
    	return dir.resolve("images"); 
    }
    
    // charity donations
    public Path charityDonations() {
        return dir.resolve("charity_donations.txt");
    }
    
    // charity dispatches
    public Path charityDispatches() {
        return dir.resolve("charity_dispatches.txt");
    }
    
    //True when a saved copy already exists in the folder. 
    public boolean exists() {
        return Files.exists(users()) || Files.exists(products());
    }

    //Moves damaged data files into data/corrupt (time) ;/ so nothing is silently lost.
    public void setAsideDamagedFiles() throws IOException {
    	// folder name carries the date and time
        Path box = dir.resolve("corrupt-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
        Files.createDirectories(box);
        
        for (Path file : new Path[] {users(), products(), transactions(), orders(), lines()}) {
            // handles the file and its .bak backup
        	for (String suffix : new String[] {"", ".bak"}) {
                Path f = file.resolveSibling(file.getFileName() + suffix);
                if (Files.exists(f)) Files.move(f, box.resolve(f.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
