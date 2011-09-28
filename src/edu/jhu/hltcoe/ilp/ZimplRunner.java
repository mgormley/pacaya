package edu.jhu.hltcoe.ilp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import edu.jhu.hltcoe.util.Command;


public class ZimplRunner {
    
    private static final Pattern tabRegex = Pattern.compile("\\s+");
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
    
    public ZimplRunner(File zimplFile, File tempDir) {
        this.zimplFile = zimplFile;
        this.tempDir = tempDir;
        this.outputPrefix = new File(tempDir, zimplFile.getName().replace(".zpl", "")).getAbsolutePath();
    }

    public File getZimplFile() {
        return zimplFile;
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
    }

    private void runZimpl(String type, int verbosity) {
        String[] cmdArray = new String[] { 
                zimplBinary, 
                "-r", // Write ORD file
                "-m", // Write MST file
                "-o", outputPrefix,
                "-t", type, 
                "-l","14", // Maximum length of names in output file. 
                "-v"+String.valueOf(verbosity), 
                zimplFile.getAbsolutePath() };
        File zimplLog = new File(tempDir, "zimpl.log");
        Command.runCommand(cmdArray, zimplLog, tempDir);
    }

    /**
     * Reads in the tbl file as a map from the Zimpl var names to Solver var names.
     */
    public static Map<String,String> readTblMapFromZimpl(File tblFile) throws IOException {
        Map<String,String> tblMap = new HashMap<String,String>();
        
        BufferedReader reader = new BufferedReader(new FileReader(tblFile));
        String line;
        while((line = reader.readLine()) != null) {
            String[] splits = tabRegex.split(line);
            String gurobiVar = splits[3];
            String zimplVar = splits[4];
            // Remove double quotes
            zimplVar = zimplVar.substring(1,zimplVar.length()-1);
            tblMap.put(zimplVar, gurobiVar);
        }
        return tblMap;
    }

    /**
     * Reads in the tbl file as a map from the Solver var names to Zimpl var names.
     */
    public static Map<String,String> readTblMapToZimpl(File tblFile) throws IOException {
        Map<String,String> tblMap = new HashMap<String,String>();
        
        BufferedReader reader = new BufferedReader(new FileReader(tblFile));
        String line;
        while((line = reader.readLine()) != null) {
            String[] splits = tabRegex.split(line);
            String gurobiVar = splits[3];
            String zimplVar = splits[4];
            // Remove double quotes
            zimplVar = zimplVar.substring(1,zimplVar.length()-1);
            tblMap.put(gurobiVar, zimplVar);
        }
        return tblMap;
    }

}
