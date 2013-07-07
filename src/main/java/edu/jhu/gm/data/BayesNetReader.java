package edu.jhu.gm.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;

import edu.jhu.gm.Factor;
import edu.jhu.gm.FactorGraph;
import edu.jhu.gm.Var;
import edu.jhu.gm.Var.VarType;
import edu.jhu.gm.VarConfig;
import edu.jhu.gm.VarSet;

public class BayesNetReader {

    private static final Pattern whitespace = Pattern.compile("\\s+");
    private static final Pattern arrow = Pattern.compile("\\s*->\\s*");
    private static final Pattern comma = Pattern.compile("\\s*,\\s*");
    private static final Pattern equals = Pattern.compile("\\s*=\\s*");
    private static final Pattern whitespaceOrComma = Pattern.compile("[,\\s]+");
    
    private HashMap<String,Var> varMap;
    private HashMap<VarSet, Factor> factorMap;

    public BayesNetReader() {
        
    }
    
    /**
     * Reads a Bayesian Network from a network file and a CPD file, and returns
     * a factor graph representation of it.
     */
    public FactorGraph readBnAsFg(File networkFile, File cpdFile) throws IOException {      
        return readBnAsFg(new FileInputStream(networkFile), new FileInputStream(cpdFile));
    }
    
    /**
     * Reads a Bayesian Network from a network InputStream and a CPD InputStream, and returns
     * a factor graph representation of it.
     */
    public FactorGraph readBnAsFg(InputStream networkIs, InputStream cpdIs) throws IOException {
        // Read network file.
        BufferedReader networkReader = new BufferedReader(new InputStreamReader(networkIs));
        // -- read the number of variables.
        int numVars = Integer.parseInt(networkReader.readLine().trim());
        varMap = new HashMap<String,Var>();
        VarSet allVars = new VarSet();
        for (int i = 0; i < numVars; i++) {
            Var var = parseVar(networkReader.readLine());
            allVars.add(var);
            varMap.put(var.getName(), var);
        }
        assert (allVars.size() == numVars);    
        // -- read the dependencies between variables.
        // ....or not...
        
        networkReader.close();
                
        // Read CPD file.
        BufferedReader cpdReader = new BufferedReader(new InputStreamReader(cpdIs));
        
        factorMap = new LinkedHashMap<VarSet, Factor>();
        String line;
        while ((line = cpdReader.readLine()) != null) {
            // Parse out the variable configuration.           
            VarConfig config = new VarConfig();
            String[] assns = whitespaceOrComma.split(line);
            for (int i=0; i<assns.length-1; i++) {
                String assn = assns[i];
                String[] va = equals.split(assn);
                assert(va.length == 2);
                String varName = va[0];
                String stateName = va[1];
                config.put(varMap.get(varName), stateName);
            }
            
            // The double is the last value on the line.
            double value = Double.parseDouble(assns[assns.length-1]);
                        
            // Get the factor for this configuration, creating a new one if necessary.
            VarSet vars = config.getVars();
            Factor f = factorMap.get(vars);
            if (f == null) { f = new Factor(vars); }
            
            // Set the value in the factor.
            f.setValue(config.getConfigIndex(), value);
            factorMap.put(vars, f);
        }
        
        cpdReader.close();
        
        
        // Create the factor graph.
        FactorGraph fg = new FactorGraph();
        for (Factor f : factorMap.values()) {
            fg.addFactor(f);
        }        
        return fg;
    }
    
    /**
     * Reads a variable from a line containing the variable name, a space, then
     * a comma-separated list of the values it can take.
     */
    private static Var parseVar(String varLine) {
        String[] ws = whitespace.split(varLine);
        String name = ws[0];
        List<String> stateNames = Arrays.asList(comma.split(ws[1]));
        int numStates = stateNames.size();
        return new Var(VarType.PREDICTED, numStates, name, stateNames);
    }

}
