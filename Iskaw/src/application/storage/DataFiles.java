package application.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Where the data files live: the names of the files inside the data folder. */
public class DataFiles {
    private final Path dir;

    public DataFiles(Path dir) { this.dir = dir; }

    public Path folder() { return dir; }
    public Path users() { return dir.resolve("users.txt"); }
    public Path products() { return dir.resolve("products.txt"); }
    public Path transactions() { return dir.resolve("transactions.txt"); }
    public Path orders() { return dir.resolve("orders.txt"); }
    /** The items of every sale and order. */
    public Path lines() { return dir.resolve("lines.txt"); }
    public Path images() { return dir.resolve("images"); }
    /** The old single-file binary save of earlier versions. */
    public Path legacySave() { return dir.resolve("dairyhub.dat"); }

    /** True when a saved copy already exists in the folder. */
    public boolean exists() {
        return Files.exists(users()) || Files.exists(products());
    }

    /** Moves damaged data files into data/corrupt-&lt;time&gt;/ so nothing is silently lost. */
    public void setAsideDamagedFiles() throws IOException {
        Path box = dir.resolve("corrupt-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
        Files.createDirectories(box);
        for (Path file : new Path[] {users(), products(), transactions(), orders(), lines()}) {
            for (String suffix : new String[] {"", ".bak"}) {
                Path f = file.resolveSibling(file.getFileName() + suffix);
                if (Files.exists(f)) Files.move(f, box.resolve(f.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
