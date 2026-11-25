package application.storage;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared helper of the file readers and writers: one record per line, fields separated by |.
 * A | or line break inside a value is escaped, and a missing (null) value is written as \N.
 */
final class Records {
    private static final String NULL = "\\N";

    private Records() { }

    // parser turns the raw lines (split into fileda) into objects
    // each reader class supplies its own Parser as a lambda
    interface Parser<T> {
        T parse(List<String[]> records);
    }

    //---------- READING ----------

    /**
     * Reads a file and parses it. If the file is unreadable or has a damaged record, the backup copy (.bak)
     * made by the last successful save is used instead. A file that does not exist yet counts as empty.
     */
    static <T> T load(Path path, Parser<T> parser) throws IOException {
        Path backup = backupOf(path); // users.txt.bak (backup)
        // first run and neither files exists yet, so there is nothing to load
        if (!Files.exists(path) && !Files.exists(backup)) return parser.parse(new ArrayList<>());
        try {
            return parser.parse(read(path)); // read the main file
        } catch (IOException | RuntimeException first) {
        	// main file is unreadable or has damaged line so try the previous copy
            if (!Files.exists(backup)) throw new IOException(path.getFileName() + " is unreadable: " + first, first);
            try {
                return parser.parse(read(backup)); // fall back to the backup file
            } catch (IOException | RuntimeException second) {
                throw new IOException(path.getFileName() + " and its backup are unreadable: " + second, second);
            }
        }
    }

    private static List<String[]> read(Path path) throws IOException {
        List<String[]> records = new ArrayList<>();
        for (String line : Files.readAllLines(path)) {
            if (line.isBlank() || line.startsWith("#")) continue;   // skip blank lines and the header comment
            // separate by |
            // -1 keeps empty last fields
            String[] parts = line.split("\\|", -1);
            for (int i = 0; i < parts.length; i++) parts[i] = unescape(parts[i]);
            records.add(parts);
        }
        return records;
    }

  //---------- WRITING ----------
    
    // join fields into one line
    static String row(Object... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) sb.append('|');
            sb.append(fields[i] == null ? NULL : escape(String.valueOf(fields[i])));
        }
        return sb.toString();
    }

    /**
     * Writes the records with Files.newBufferedWriter into a temporary file, keeps the old file as .bak and then
     * moves the new one into place, so a crash can never leave a half-written file.
     */
    static void write(Path path, String description, List<String> records) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent); // make data if it is missing
        Path tmp = path.resolveSibling(path.getFileName() + ".tmp"); //users.txt.tmp
        try (BufferedWriter writer = Files.newBufferedWriter(tmp)) {
            writer.write("# " + description); // header line explains the column
            writer.newLine();
            for (String record : records) {
                writer.write(record);
                writer.newLine();
            }
        }
        // keep the previous good version as users.txt.bak
        if (Files.exists(path)) Files.copy(path, backupOf(path), StandardCopyOption.REPLACE_EXISTING);
        try {
        	// swap the finished temp file into place
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
        	// fallback on filesystems 
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Path backupOf(Path path) {
        return path.resolveSibling(path.getFileName() + ".bak");
    }

    //---------- ESCAPING ----------

    // used when writing
    private static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break; // \
                case '|': sb.append("\\p"); break; // \p
                case '\n': sb.append("\\n"); break; // \n
                case '\r': sb.append("\\r"); break; 
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String unescape(String s) {
        if (s.equals(NULL)) return null;
        if (s.indexOf('\\') < 0) return s;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\\' || i == s.length() - 1) { sb.append(c); continue; }
            char n = s.charAt(++i);
            switch (n) {
                case 'p': sb.append('|'); break;
                case 'n': sb.append('\n'); break;
                case 'r': sb.append('\r'); break;
                default: sb.append(n);
            }
        }
        return sb.toString();
    }
}
