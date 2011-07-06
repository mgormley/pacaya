package edu.jhu.hltcoe.ilp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

public class ZimplSolver  {

    private static final Pattern tabRegex = Pattern.compile("\\s+");
    
    private File tempDir;
    private Map<String,Double> result;
    private IlpSolver ilpSolver;

    private double objective;

    public ZimplSolver(File tempDir, IlpSolver ilpSolver) {
        this.tempDir = tempDir;
        this.ilpSolver = ilpSolver;
    }

    public void solve(File zimplFile) {
        result = new HashMap<String,Double>();
        
        // Run Zimpl
        ZimplRunner zimplRunner = new ZimplRunner(zimplFile, tempDir);
        zimplRunner.runZimpl();
        File lpFile = zimplRunner.getLpFile();
        File tblFile = zimplRunner.getTblFile();
        
        // Run ILP Solver
        ilpSolver.solve(lpFile);
        objective = ilpSolver.getObjective();
        Map<String,Double> solMap = ilpSolver.getResult();

        // Read tbl file and map variable values to original names
        try {
            Map<String,String> tblMap = readTblMap(tblFile);
            
            for (Entry<String, Double> entry : solMap.entrySet()) {
                String gurobiVar = entry.getKey();
                Double value = entry.getValue();
                
                // Change negative zero to zero just for canonicalization
                value = value == -0.0 ? 0.0 : value;
                
                String zimplVar = tblMap.get(gurobiVar);
                // TODO: speedup: filter which vars are included in the result with an optional regex 
                result.put(zimplVar, value);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String,Double> getResult() {
        return result;
    }
    
    public double getObjective() {
        return objective;
    }
        
    private static Map<String,String> readTblMap(File tblFile) throws IOException {
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
