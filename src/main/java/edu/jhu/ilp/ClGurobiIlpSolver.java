package edu.jhu.ilp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.jhu.util.files.Files;
import edu.jhu.util.sys.System;

public class ClGurobiIlpSolver implements IlpSolver {

    private static final String gurobiBinary = "gurobi_cl";
    private final Pattern spaceRegex = Pattern.compile(" ");
    
    private File tempDir;
    private Map<String, Double> result;
    private int numThreads;
    private double workMemMegs;
    private double objective;

    public ClGurobiIlpSolver(File tempDir, int numThreads, double workMemMegs) {
        this.tempDir = tempDir;
        this.numThreads = numThreads;
        this.workMemMegs = workMemMegs;
    }

    @Override
    /**
     * TODO: currently returns only optimal solution, but should also return feasible solutions.
     */
    public boolean solve(File lpFile) {
        result = null;
        
        // Run Gurobi
        File solFile = new File(tempDir, lpFile.getName().replace(".lp", ".sol"));
        String[] cmdArray = new String[] {
                gurobiBinary,
                "ResultFile="+solFile.getAbsolutePath(),
                "Threads="+numThreads,
                "NodefileDir="+tempDir.getAbsolutePath(),
                "NodefileStart="+workMemMegs/1024.0,
                lpFile.getAbsolutePath() };
        //TODO: handle infeasible case
        File gurobiLog = new File(tempDir, "gurobi.log");
        System.runCommand(cmdArray, gurobiLog, tempDir);

        // Throw exception if optimal solution not found
        if (!Files.fileContains(gurobiLog, "Optimal solution found")) {
            return false;
        }
        
        // Read objective from log file
        Matcher objValMatcher = Files.getFirstMatch(gurobiLog, Pattern.compile("Best objective (.*?),"));
        objective = Double.parseDouble(objValMatcher.group(1));
        
        // Read .sol file into result map
        try {
            result = readGurobiSol(solFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        return true;
    }

    @Override
    public Map<String, Double> getResult() {
        return result;
    }

    @Override
    public double getObjective() {
        return objective;
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
        reader.close();
        return solMap;
    }

    public String getType() {
        return "lp";
    }

}
