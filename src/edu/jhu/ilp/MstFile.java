package edu.jhu.hltcoe.ilp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class MstFile {

    private static Pattern whitespace = Pattern.compile("\\s+");

    public static Map<String, Double> read(File mstFile) {
        try {
            Map<String, Double> mipStart = new HashMap<String, Double>();
            BufferedReader reader = new BufferedReader(new FileReader(mstFile));
            String line;
            boolean reading = false;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("ENDATA")) {
                    reading = false;
                    break;
                } else if (reading) {
                    String[] splits = whitespace.split(line);
                    mipStart.put(splits[1], Double.valueOf(splits[2]));
                } else if (line.startsWith("NAME")) {
                    reading = true;
                    continue;
                } 
            }
            reader.close();
            return mipStart;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void write(Map<String, Double> mipStart, File mstFile) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(mstFile));
            writer.write("* Created by MstFile.java\n");
            writer.write("NAME\n");
            for (Map.Entry<String, Double> entry : mipStart.entrySet()) {
                String line = String.format(" %s %e\n", entry.getKey(), entry.getValue());
                writer.write(line);
            }
            writer.write("ENDATA\n");
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
