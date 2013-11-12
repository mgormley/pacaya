package edu.jhu.ilp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import edu.jhu.util.sys.System;


public class ZimplRunner {
    
    private static final Pattern tabRegex = Pattern.compile("\\s+");
    private static final String zimplBinary;
    static {
        File zb = new File("/Users/mgormley/research/parsing/bin/zimpl-3.1.0.darwin.x86.gnu.opt");
        if (zb.exists()) {
            zimplBinary = zb.getAbsolutePath();
        } else {
            zimplBinary = "zimpl";
        }
    }
    private String outputPrefix;
    private File zimplFile;
    private File tempDir;
    private String type;
    
    public ZimplRunner(File zimplFile, File tempDir, String type) {
        this.zimplFile = zimplFile;
        this.tempDir = tempDir;
        this.outputPrefix = new File(tempDir, zimplFile.getName().replace(".zpl", "")).getAbsolutePath();
        this.type = type;
    }

    public File getZimplFile() {
        return zimplFile;
    }
    
    public File getIlpFile() {
        return new File(outputPrefix + "." + type);
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
        runZimpl(1);
    }

    private void runZimpl(int verbosity) {
        int maxLength;
        if (type.equals("mps")) {
            maxLength = 8;
        } else {
            maxLength = 14;
        }
        
        String[] cmdArray = new String[] { 
                zimplBinary, 
                "-r", // Write ORD file
                "-m", // Write MST file
                "-o", outputPrefix,
                "-t", type, // e.g. lp, mps
                "-l", String.valueOf(maxLength), // Maximum length of names in output file. 
                "-v"+String.valueOf(verbosity), 
                zimplFile.getAbsolutePath() };
        File zimplLog = new File(tempDir, "zimpl.log");
        System.runCommand(cmdArray, zimplLog, tempDir);
    }

    /**
     * Tries to map to the zimpl variable name, but returns the original if it can't be found.
     */
    public static String safeMap(Map<String, String> tblMap, String key) {
        String value = tblMap.get(key);
        if (value != null) {
            return value;
        } else {
            return key;
        }
    }

    /**
     * Reads in the tbl file as a map from the Zimpl var names to Solver var names.
     */

    public static Map<String,String> readTblMapFromZimpl(File tblFile) throws IOException {
        return readTblMapFromZimpl(tblFile, "v");
    }
    
    /**
     * @param mapType v for variables and c for constraints
     */
    public static Map<String,String> readTblMapFromZimpl(File tblFile, String mapType) throws IOException {
        Map<String,String> tblMap = new HashMap<String,String>();
        
        BufferedReader reader = new BufferedReader(new FileReader(tblFile));
        String line;
        while((line = reader.readLine()) != null) {
            String[] splits = tabRegex.split(line);
            String type = splits[1];
            if (!type.equals(mapType)) {
                continue;
            }
            String gurobiVar = splits[3];
            String zimplVar = splits[4];
            // Remove double quotes
            zimplVar = zimplVar.substring(1,zimplVar.length()-1);
            tblMap.put(zimplVar, gurobiVar);
        }
        reader.close();
        return tblMap;
    }

    /**
     * Reads in the tbl file as a map from the Solver var names to Zimpl var names.
     */
    public static Map<String,String> readTblMapToZimpl(File tblFile) throws IOException {
        return readTblMapToZimpl(tblFile, "v");
    }
    
    /**
     * @param mapType v for variables and c for constraints
     */
    public static Map<String,String> readTblMapToZimpl(File tblFile, String mapType) throws IOException {
        Map<String,String> tblMap = new HashMap<String,String>();
        
        BufferedReader reader = new BufferedReader(new FileReader(tblFile));
        String line;
        while((line = reader.readLine()) != null) {
            String[] splits = tabRegex.split(line);
            String type = splits[1];
            if (!type.equals(mapType)) {
                continue;
            }
            String gurobiVar = splits[3];
            String zimplVar = splits[4];
            // Remove double quotes
            zimplVar = zimplVar.substring(1,zimplVar.length()-1);
            tblMap.put(gurobiVar, zimplVar);
        }
        reader.close();
        return tblMap;
    }

}
