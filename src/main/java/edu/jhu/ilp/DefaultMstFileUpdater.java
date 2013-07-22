package edu.jhu.ilp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Removes start vals for variables not mentioned in the zimpl file.
 */
public class DefaultMstFileUpdater implements MstFileUpdater {
    
    private static final Pattern varStartvalPattern = Pattern.compile("var\\s+([^\\[ \\t]+).*\\sstartval\\s.*");

    public void updateMstFile(ZimplRunner zimplRunner) {
        try {
            BufferedReader reader;
            String line;
            
            StringBuilder varListSb = null;
            reader = new BufferedReader(new FileReader(zimplRunner.getZimplFile()));
            while((line = reader.readLine()) != null) {
                Matcher matcher = varStartvalPattern.matcher(line);
                if (matcher.find()) {
                    String varPrefix = matcher.group(1);
                    if (varListSb == null) {
                        varListSb = new StringBuilder("^    ("+varPrefix);
                    } else {
                        varListSb.append("|"+varPrefix);
                    }
                }
            }
            reader.close();
            if (varListSb == null) {
                return;
            }
            varListSb.append(")");
            Pattern varListPattern = Pattern.compile(varListSb.toString());
            
            File mstFile = zimplRunner.getMstFile();
            File fixedMstFile = new File(mstFile.getPath()+".fixed");
            reader = new BufferedReader(new FileReader(mstFile));
            BufferedWriter writer = new BufferedWriter(new FileWriter(fixedMstFile));
            while((line = reader.readLine()) != null) {
                if (line.startsWith("    ")) {
                    Matcher matcher = varListPattern.matcher(line);
                    if (!matcher.find()) {
                        continue;
                    }                    
                }
                writer.write(line);
                writer.write("\n");
            }
            reader.close();
            writer.close();
            
            mstFile.delete();
            fixedMstFile.renameTo(mstFile);            
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
}