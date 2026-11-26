package application.storage;

import application.model.ItemDonation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Loads in-kind charity item donations and distribution status from charity_donations.txt
public class CharityDonationFileReader {
	// reads all charity dispatch records 
    public static List<ItemDonation> read(Path path) throws IOException {
        // returns empty list if file does not exist
    	if (!Files.exists(path)) {
            return new ArrayList<>();
        }
    	// reads the file
        return Records.load(path, records -> {
            List<ItemDonation> list = new ArrayList<>();
            for (String[] f : records) {
            	// extract optional notes from f[9], and have empty as default if omitted
                String notes = f.length > 9 ? f[9] : "";
                // construct donation object
                ItemDonation d = new ItemDonation(f[0], f[2], f[6], f[7], Integer.parseInt(f[8]), notes);
                // restore historical creation timestamp
                d.restoreState(time(f[1]), Boolean.parseBoolean(f[3]), f[4], time(f[5]));
                // extract optional distribution quantity
                int donatedQty = f.length > 10 ? Integer.parseInt(f[10]) : 0;
                String distNotes = f.length > 11 ? f[11] : "";
                // apply saved distribution progress to donation record
                d.restoreDistribution(donatedQty, distNotes);
                // add fully reconstructed donation
                list.add(d);
            }
            return list;
        });
    }
    
    // date and time
    // missing value (null) stays null
    private static LocalDateTime time(String s) {
        return s == null ? null : LocalDateTime.parse(s);
    }
}