package edu.jhu.ilp.decomp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import edu.jhu.util.Files;

public class DummyBlockFileWriter implements BlockFileWriter {

    @Override
    public void writeBlockFile(File blockFile, File mpsFile, File tblFile) {
        try {
            FileWriter writer = new FileWriter(blockFile);
            BufferedReader reader = new BufferedReader(new FileReader(mpsFile));
            String line;
            
            // Read ROWS in order into an ArrayList
            Files.readUntil(reader, "ROWS");
            // By default the first "N" row defined in the ROWS section becomes a problem's objective.
            // This row is NOT counted when computing the row indices, so we read and skip it.
            line = reader.readLine();
            assert(line.contains("N  OBJECTIV"));
            int i=0;
            while ((line = reader.readLine()) != null) {
                if (line.equals("COLUMNS")) {
                    break;
                }
                writer.write(String.format("%d %d\n", 0, i));
                i++;
            }
            reader.close();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
