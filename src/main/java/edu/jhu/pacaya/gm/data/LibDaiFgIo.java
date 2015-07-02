package edu.jhu.pacaya.gm.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.pacaya.autodiff.DimIter;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.gm.model.ExplicitFactor;
import edu.jhu.pacaya.gm.model.Factor;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.Var.VarType;
import edu.jhu.pacaya.gm.model.VarSet;
import edu.jhu.pacaya.gm.model.globalfac.GlobalFactor;
import edu.jhu.pacaya.util.semiring.RealAlgebra;
import edu.jhu.prim.map.IntObjectHashMap;


public class LibDaiFgIo {

    private static final Logger log = LoggerFactory.getLogger(LibDaiFgIo.class);
    private static final Pattern whitespace = Pattern.compile("\\s+");
    
    public static FactorGraph read(Path file) {
        try (BufferedReader in = Files.newBufferedReader(file)) { 
            return read(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static FactorGraph read(BufferedReader in) {
        try {
            log.debug("Reading factor graph file.");
            IntObjectHashMap<Var> vars = new IntObjectHashMap<Var>();
            List<Factor> factors = new ArrayList<>();
            List<String> buffer = new ArrayList<String>();
            
            // Read: Number of factors.
            int numFactors = Integer.valueOf(readNextLineTrimmed(in, buffer));
            // Read: Empty line.
            readNextLineTrimmed(in, buffer);
            for (int i=0; i<numFactors; i++) {                
                // Read: Number of variables in the factor.
                int numVars = Integer.valueOf(readNextLineTrimmed(in, buffer));
                
                // Read: The ids of the vars in reverse order, so that the one with the fastest
                // changing index is the leftmost variable.  
                int[] varIds = toIntArray(readNextLineTrimmed(in, buffer));
                if (numVars != varIds.length) {
                    String msg = String.format("Read incorrect number of variables for factor %d. Expected=%d Actual=%d", i, numVars, varIds.length);
                    throw new IllegalStateException(msg);
                }
                ArrayUtils.reverse(varIds);
                
                // Read: The number of values each variable in the above row can take on.   
                int[] dims = toIntArray(readNextLineTrimmed(in, buffer));
                if (numVars != dims.length) {
                    String msg = String.format("Read incorrect number of dimensions for factor %d. Expected=%d Actual=%d", i, numVars, dims.length);
                    throw new IllegalStateException(msg);
                }
                ArrayUtils.reverse(dims);
                
                // Make the VarSet, creating any new variables along the way.
                VarSet vs = new VarSet();
                for (int v=0; v<numVars; v++) {
                    if (!vars.containsKey(varIds[v])) {
                        vars.put(varIds[v], new Var(VarType.PREDICTED, dims[v], ""+varIds[v], null));
                    }
                    vs.add(vars.get(varIds[v]));
                }
                
                // Get the mapping from input variable to its position in the above VarSet.
                int[] iv2vs = new int[numVars];
                Arrays.fill(iv2vs, -1);
                for (int v=0; v<vs.size(); v++) {
                    iv2vs[v] = vs.indexOf(vars.get(varIds[v]));
                }
                
                // Read: The number of nonzero entries in the factor.
                int numNonZeros = Integer.valueOf(readNextLineTrimmed(in, buffer));
                
                // Read: The values of the configurations of the variables.
                // This is the exponentiated score, not the log-score.
                
                // 1. Read the values into an unlabeled Tensor.
                Tensor srcFac = new Tensor(RealAlgebra.getInstance(), dims);
                for (int j=0; j < numNonZeros; j++) {
                    String[] pair = whitespace.split(readNextLineTrimmed(in, buffer));
                    int idx = Integer.valueOf(pair[0]);
                    double val = Double.valueOf(pair[1]);
                    srcFac.setValue(idx, val);
                }
                
                // 2. Create the VarTensor mapping the values from the Tensor into the VarTensor.
                ExplicitFactor dstFac = new ExplicitFactor(vs);             
                DimIter iter = new DimIter(dims);
                while (iter.hasNext()) {
                    int[] srcIdx = iter.next();
                    int[] dstIdx = new int[srcIdx.length];
                    for (int v=0; v<srcIdx.length; v++) {
                        dstIdx[v] = srcIdx[iv2vs[v]];
                    }
                    double val = srcFac.get(srcIdx);                    
                    // Convert to log score.
                    dstFac.set(Math.log(val), dstIdx);
                }
                factors.add(dstFac);
                
                // Read: Empty line.
                readNextLineTrimmed(in, buffer);
            }
            in.close();
            
            log.debug("Creating FactorGraph object.");
            FactorGraph fg = new FactorGraph();
            for (Factor f : factors) {
                fg.addFactor(f);
            }
            return fg;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static int[] toIntArray(String line) {
        String[] strs = whitespace.split(line);
        int[] ints = new int[strs.length];
        for (int i=0; i<ints.length; i++) {
            ints[i] = Integer.valueOf(strs[i]);
        }
        return ints;
    }

    private static String readNextLineTrimmed(BufferedReader in, List<String> buffer) throws IOException {
        String line;
        while ((line = in.readLine()) != null) {
            if (line != null) {
                buffer.add(line);
            }
            if (!line.startsWith("#")) {
                break;
            }
        }
        if (line == null) {
            log.error("Last ten lines read: ");
            for (int l = Math.max(0, buffer.size() - 10); l < buffer.size(); l++) {                
                log.error("line {}: {}", l+1, buffer.get(l));
            }
            throw new RuntimeException("Expected another line, but hit the end of the file.");
        }
        return line.trim();
    }

    public static void write(FactorGraph fg, Path file) {
        try (BufferedWriter out = Files.newBufferedWriter(file)) {
            write(fg, out);    
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static void write(FactorGraph fg, BufferedWriter out) {
        try {
            out.write(String.format("# Num factors = %d, Num variables = %d, Num edges = %d\n", fg.getNumFactors(), fg.getNumVars(), fg.getNumEdges()));
            // Write: Number of factors.
            out.write(getNumNonGlobalFactors(fg) + "\n");
            // Write: Empty line.
            out.write("\n");
            for (Factor f : fg.getFactors()) {
                if (!(f instanceof GlobalFactor)) {
                    VarSet vars = f.getVars();
                    // Write: Number of variables in the factor.
                    out.write(vars.size() + "\n");
                    // Write: The ids of the vars in reverse order, so that the one with the fastest
                    // changing index is the leftmost variable.                
                    for (int v=vars.size()-1; v>=0; v--) {
                        out.write("" + vars.get(v).getId());
                        if (v > 0) { out.write(" "); }
                    }
                    out.write("\n");
                    // Write: The number of values each variable in the above row can take on.               
                    for (int v=vars.size()-1; v>=0; v--) {
                        out.write("" + vars.get(v).getNumStates());
                        if (v > 0) { out.write(" "); }
                    }
                    out.write("\n");
                    // Write: The number of nonzero entries in the factor.
                    int n = vars.calcNumConfigs();
                    out.write(n + "\n");
                    // Write: The values of the configurations of the variables.
                    // This is the exponentiated score, not the log-score.
                    for (int c=0; c < n; c++) {
                        out.write(String.format("%d %.17f\n", c, Math.exp(f.getLogUnormalizedScore(c))));
                    }
                    // Write: Empty line.
                    out.write("\n");
                } else {
                    log.warn("Skipping factor of type: {}", f.getClass());
                }
            }
            out.close();
        } catch (IOException e) { 
            throw new RuntimeException(e);
        }
    }

    private static int getNumNonGlobalFactors(FactorGraph fg) {
        int count = 0;
        for (Factor f : fg.getFactors()) {
            if (!(f instanceof GlobalFactor)) {
                count++;
            }
        }
        return count;
    }
    
    
}
