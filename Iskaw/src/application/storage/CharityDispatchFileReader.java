package application.storage;

import application.model.CharityDispatch;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Loads charity dispatch history from charity_dispatches.txt
public class CharityDispatchFileReader {
	// reads all saved charity dispatch
    public static List<CharityDispatch> read(Path path) throws IOException {
        if (!Files.exists(path)) {
        	// returns empty list if file does not exist
            return new ArrayList<>();
        }
        // read the file
        return Records.load(path, records -> {
            List<CharityDispatch> list = new ArrayList<>();
            for (String[] f : records) {
            	// preserve item insertion order
                Map<String, Integer> items = new LinkedHashMap<>();
                if (f.length > 5 && f[5] != null && !f[5].isEmpty()) {
                    for (String pair : f[5].split(";")) {
                    	// to handle items names containing '=' safely
                        int eq = pair.lastIndexOf('=');
                        if (eq > 0) {
                            items.put(pair.substring(0, eq), Integer.parseInt(pair.substring(eq + 1)));
                        }
                    }
                }
                // build the object and have the default optional notes as empty string
                CharityDispatch d = new CharityDispatch(f[0], f[2], f[3], f.length > 4 ? f[4] : "", items);
                // overwrite constructor's default timestamp
                if (f[1] != null) d.restoreTimestamp(LocalDateTime.parse(f[1]));
                // add the reconstructed dispatch record to the result list
                list.add(d);
            }
            return list;
        });
    }
}