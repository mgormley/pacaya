package edu.jhu.hltcoe.ilp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.jhu.hltcoe.util.Command;


public class ZimplRunner {
    
    private static final String zimplBinary;
    static {
        File zb = new File("/Users/mgormley/Documents/JHU4_S10/dep_parse/bin/zimpl-3.1.0.darwin.x86.gnu.opt");
        if (zb.exists()) {
            zimplBinary = zb.getAbsolutePath();
        } else {
            zimplBinary = "zimpl";
        }
    }
    private String outputPrefix;
    private File zimplFile;
    private File tempDir;
    private static final Pattern varStartvalPattern = Pattern.compile("var\\s+([^\\[ \\t]+).*\\sstartval\\s.*");
    
    public ZimplRunner(File zimplFile, File tempDir) {
        this.zimplFile = zimplFile;
        this.tempDir = tempDir;
        this.outputPrefix = new File(tempDir, zimplFile.getName().replace(".zpl", "")).getAbsolutePath();
    }

    public File getLpFile() {
        return new File(outputPrefix + ".lp");
    }
    
    public File getTblFile() {
        return new File(outputPrefix + ".tbl");
    }
    
    public File getMstFile() {
        return new File(outputPrefix + ".mst");
    }
    
    public File getOrdFile() {
        return new File(outputPrefix + ".ord");
    }
    
    public void runZimpl() {
        runZimpl("lp", 1);
        fixMstFile();
    }

    private void runZimpl(String type, int verbosity) {
        String[] cmdArray = new String[] { 
                zimplBinary, 
                "-r", // Write ORD file
                "-m", // Write MST file
                "-o", outputPrefix,
                "-t", type, 
                "-v"+String.valueOf(verbosity), 
                zimplFile.getAbsolutePath() };
        File zimplLog = new File(tempDir, "zimpl.log");
        Command.runCommand(cmdArray, zimplLog, tempDir);
    }
    
    private void fixMstFile() {
        try {
            BufferedReader reader;
            String line;
            
            StringBuilder varListSb = null;
            reader = new BufferedReader(new FileReader(zimplFile));
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
            
            File mstFile = getMstFile();
            File fixedMstFile = new File(getMstFile().getPath()+".fixed");
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
