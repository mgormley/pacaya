package edu.jhu.pacaya.gm.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.pacaya.gm.model.ExplicitFactor;
import edu.jhu.pacaya.gm.model.Factor;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.VarSet;


public class LibDaiFgIo {

    private static final Logger log = LoggerFactory.getLogger(LibDaiFgIo.class);

//    public static FactorGraph read(Path file) throws IOException {
//        BufferedReader reader = Files.newBufferedReader(file);
//        // Read: Number of factors.
//        String line;
//        while ((line = reader.readLine()) != null) {
//            
//        }
//        List<Factor> factors = new ArrayList<Factor>();
//        
//        FactorGraph fg = new FactorGraph();
//        reader.close();
//        throw new RuntimeException("Not implemented");
//    }
    
    // TODO: Test this.
    public static void write(FactorGraph fg, Path file) throws IOException {
        try (BufferedWriter out = Files.newBufferedWriter(file)) {
            write(fg, out);    
        }
    }
    
    public static void write(FactorGraph fg, BufferedWriter out) throws IOException {
        out.write(String.format("# Num factors = %d, Num variables = %d, Num edges = %d\n", fg.getNumFactors(), fg.getNumVars(), fg.getNumEdges()));
        // Write: Number of factors.
        out.write(fg.getNumFactors() + "\n");
        // Write: Empty line.
        out.write("\n");        
        for (Factor f : fg.getFactors()) {
            if (f instanceof ExplicitFactor) {
                ExplicitFactor ef = (ExplicitFactor) f;
                VarSet vars = ef.getVars();
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
                out.write(ef.size() + "\n");
                // Write: The values of the configurations of the variables.
                for (int c=0; c<ef.size(); c++) {
                    out.write(c + " " + ef.getValue(c) + "\n");
                }
                // Write: Empty line.
                out.write("\n");
            } else {
                log.warn("Skipping factor of type: {}", f.getClass());
            }
        }
        out.close();
    }
    
    
}
