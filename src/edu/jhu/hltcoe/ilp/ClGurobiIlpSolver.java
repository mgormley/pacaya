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

public class ClGurobiIlpSolver implements IlpSolver {

    private static final String gurobiBinary = "gurobi_cl";
    private final Pattern spaceRegex = Pattern.compile(" ");
    
    private File tempDir;
    private Map<String, Double> result;
    private int numThreads;
    
    public ClGurobiIlpSolver(File tempDir, int numThreads) {
        this.tempDir = tempDir;
        this.numThreads = numThreads;
    }

    @Override
    public Map<String, Double> getResult() {
        return result;
    }

    @Override
    public void solve(File lpFile) {
        result = null;
        
        // Run Gurobi
        File solFile = new File(tempDir, lpFile.getName().replace(".lp", ".sol"));
        String[] cmdArray = new String[] {
                gurobiBinary,
                "ResultFile="+solFile.getAbsolutePath(),
                "Threads="+numThreads,
                lpFile.getAbsolutePath() };
        //TODO: handle infeasible case
        File gurobiLog = new File(tempDir, "gurobi.log");
        Command.runCommand(cmdArray, gurobiLog, tempDir);

        // Read .sol file into result map
        try {
            result = readGurobiSol(solFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private Map<String,Double> readGurobiSol(File solFile) throws IOException {
        Map<String,Double> solMap = new HashMap<String,Double>();
        
        BufferedReader reader = new BufferedReader(new FileReader(solFile));
        String line;
        while((line = reader.readLine()) != null) {
            String[] splits = spaceRegex.split(line);
            String gurobiVar = splits[0];
            Double value = Double.valueOf(splits[1]);
            solMap.put(gurobiVar, value);
        }
        return solMap;
    }

}
