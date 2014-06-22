package edu.jhu.tag;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FileMapTagReducer extends AbstractTagReducer {

    Map<String, String> fileMap;

    public FileMapTagReducer(File file) {
        fileMap = readFileMap(file);
    }
    
    private static Map<String, String> readFileMap(File file) {
        Map<String, String> fileMap = new HashMap<String, String>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] splits = line.split("\t");
                assert(splits.length == 0 || splits.length == 2);
                if (splits.length == 2) {
                    fileMap.put(splits[0], splits[1]);
                }
            }
            reader.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return fileMap;
    }

    @Override
    public String reduceTag(String tag) {
        return fileMap.get(tag);
    }

}
