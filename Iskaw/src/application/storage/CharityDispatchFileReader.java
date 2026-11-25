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
    public static List<CharityDispatch> read(Path path) throws IOException {
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }
        return Records.load(path, records -> {
            List<CharityDispatch> list = new ArrayList<>();
            for (String[] f : records) {
                Map<String, Integer> items = new LinkedHashMap<>();
                if (f.length > 5 && f[5] != null && !f[5].isEmpty()) {
                    for (String pair : f[5].split(";")) {
                        int eq = pair.lastIndexOf('=');
                        if (eq > 0) {
                            items.put(pair.substring(0, eq), Integer.parseInt(pair.substring(eq + 1)));
                        }
                    }
                }
                CharityDispatch d = new CharityDispatch(f[0], f[2], f[3], f.length > 4 ? f[4] : "", items);
                if (f[1] != null) d.restoreTimestamp(LocalDateTime.parse(f[1]));
                list.add(d);
            }
            return list;
        });
    }
}