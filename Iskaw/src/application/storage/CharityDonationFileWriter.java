package application.storage;

import application.model.ItemDonation;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Saves in-kind charity item donations and distribution status to charity_donations.txt
public class CharityDonationFileWriter {
    public static void write(Path path, List<ItemDonation> donations) throws IOException {
        List<String> lines = new ArrayList<>();
        for (ItemDonation d : donations) {
            lines.add(Records.row(
                    d.getId(),
                    d.getTimestamp(),
                    d.getRecordedBy(),
                    d.isVoided(),
                    d.getVoidedBy(),
                    d.getVoidedAt(),
                    d.getParty(),
                    d.getItemName(),
                    d.getQuantity(),
                    d.getNotes(),
                    d.getDonatedQuantity(),
                    d.getCharityDistributionNotes()
            ));
        }
        // the header line written at the top of the file
        Records.write(path,
                "Iskaw charity donations: id|time|recorded by|voided|voided by|voided at|donor|item name|quantity|notes|donated qty|distribution notes",
                lines);
    }
}