package application.storage;

import application.model.CharityDispatch;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// Saves charity dispatch history to charity_dispatches.txt
public class CharityDispatchFileWriter {
    public static void write(Path path, List<CharityDispatch> dispatches) throws IOException {
        List<String> lines = new ArrayList<>();
        for (CharityDispatch d : dispatches) {
        	// store items into single field
            String packedItems = d.getItems().entrySet().stream()
                    .map(e -> e.getKey().replace(";", ",").replace("=", "-") + "=" + e.getValue())
                    .collect(Collectors.joining(";"));
            // join the 6 fields with |
            lines.add(Records.row(
                    d.getId(),
                    d.getTimestamp(),
                    d.getDispatchedBy(),
                    d.getCharityName(),
                    d.getNotes(),
                    packedItems
            ));
        }
        // the header line written at the top of the file
        Records.write(path,
                "Iskaw charity dispatches: id|time|dispatched by|charity|notes|items",
                lines);
    }
}