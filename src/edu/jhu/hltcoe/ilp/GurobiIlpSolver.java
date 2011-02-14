package edu.jhu.hltcoe.ilp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import edu.jhu.hltcoe.util.Command;

public class GurobiIlpSolver implements IlpSolver {

    private static final String gurobiBinary = "gurobi_cl";
    private final Pattern spaceRegex = Pattern.compile(" ");
    private final Pattern tabRegex = Pattern.compile("\t");
    
    private String zimplFile;
    private Map<String, String> result;

    public GurobiIlpSolver(String zimplFile) {
        this.zimplFile = zimplFile;
    }

    @Override
    public Map<String, String> getResult() {
        return result;
    }

    @Override
    public void solve() {
        // Run Zimpl
        ZimplRunner zimplRunner = new ZimplRunner(zimplFile);
        zimplRunner.runZimpl();
        String lpFile = zimplRunner.getLpFile();
        String tblFile = zimplRunner.getTblFile();
        
        // Run Gurobi
        String solFile = zimplFile.replaceAll(".zpl$", ".sol");
        String[] cmdArray = new String[] {
                gurobiBinary,
                "ResultFile="+solFile,
                lpFile };
        //TODO: handle infeasible case
        String zimplFileParent = new File(zimplFile).getParent();
        String gurobiLog = zimplFileParent + "/gurobi.log";
        Command.runCommand(cmdArray, gurobiLog, new File(zimplFileParent));

        // Read tbl file and map variable values to original names
        try {
            Map<String,String> solMap = readGurobiSol(solFile);
            Map<String,String> tblMap = readTblMap(solFile);
            
            for (Entry<String, String> entry : solMap.entrySet()) {
                String gurobiVar = entry.getKey();
                String value = entry.getValue();
                String zimplVar = tblMap.get(gurobiVar);
                // TODO: speedup: filter which vars are included in the result with an optional regex 
                result.put(zimplVar, value);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private Map<String,String> readGurobiSol(String solFile) throws IOException {
        Map<String,String> solMap = new HashMap<String,String>();
        
        BufferedReader reader = new BufferedReader(new FileReader(solFile));
        String line;
        while((line = reader.readLine()) != null) {
            String[] splits = spaceRegex.split(line);
            String gurobiVar = splits[0];
            String value = splits[1];
            solMap.put(gurobiVar, value);
        }
        return solMap;
    }
    
    private Map<String,String> readTblMap(String solFile) throws IOException {
        Map<String,String> tblMap = new HashMap<String,String>();
        
        BufferedReader reader = new BufferedReader(new FileReader(solFile));
        String line;
        while((line = reader.readLine()) != null) {
            String[] splits = tabRegex.split(line);
            String gurobiVar = splits[3];
            String zimplVar = splits[4];
            tblMap.put(gurobiVar, zimplVar);
        }
        return tblMap;
    }

}
