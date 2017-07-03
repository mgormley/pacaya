package edu.jhu.pacaya.gm.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.gm.model.ExplicitFactor;
import edu.jhu.pacaya.gm.model.Factor;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.Var.VarType;
import edu.jhu.pacaya.gm.model.VarConfig;
import edu.jhu.pacaya.gm.model.VarSet;
import edu.jhu.pacaya.gm.model.globalfac.GlobalFactor;
import edu.jhu.prim.map.IntObjectHashMap;

public class LibDaiFgIo {

    private static final Logger log = LoggerFactory.getLogger(LibDaiFgIo.class);
    private static final Pattern whitespace = Pattern.compile("\\s+");

    public static FactorGraph read(Path file) {
        try (BufferedReader in = Files.newBufferedReader(file, Charset.forName("UTF-8"))) {
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
            for (int i = 0; i < numFactors; i++) {
                // Read: Number of variables in the factor.
                int numVars = Integer.valueOf(readNextLineTrimmed(in, buffer));

                // Read: the var ids in libdai order
                int[] varIds = toIntArray(readNextLineTrimmed(in, buffer));
                if (numVars != varIds.length) {
                    String msg = String.format(
                            "Read incorrect number of variables for factor %d. Expected=%d Actual=%d", i, numVars,
                            varIds.length);
                    throw new IllegalStateException(msg);
                }

                // Read: The number of values each variable in the above row can
                // take on.
                int[] dims = toIntArray(readNextLineTrimmed(in, buffer));
                if (numVars != dims.length) {
                    String msg = String.format(
                            "Read incorrect number of dimensions for factor %d. Expected=%d Actual=%d", i, numVars,
                            dims.length);
                    throw new IllegalStateException(msg);
                }

                // Make the VarSet, creating any new variables along the way.
                VarSet vs = new VarSet();
                Var[] varArray = new Var[numVars];
                for (int v = 0; v < numVars; v++) {
                    if (!vars.containsKey(varIds[v])) {
                        Var newVar = new Var(VarType.PREDICTED, dims[v], "" + varIds[v], null);
                        vars.put(varIds[v], newVar);
                    }
                    Var thisVar = vars.get(varIds[v]);
                    // make sure that where there is redundancy, domain sizes
                    // match
                    if (thisVar.getNumStates() != dims[v]) {
                        String msg = String.format("Read inconsistent dimension for variable %s. Old=%d New=%d",
                                varIds[v], thisVar.getNumStates(), dims[v]);
                        throw new IllegalStateException(msg);
                    }
                    varArray[v] = thisVar;
                    vs.add(thisVar);
                }

                // Read: The number of nonzero entries in the factor.
                int numNonZeros = Integer.valueOf(readNextLineTrimmed(in, buffer));

                // Read: The values of the configurations of the variables.
                // This is the exponentiated score, not the log-score.
                ExplicitFactor dstFac = new ExplicitFactor(vs);
                dstFac.fill(Double.NEGATIVE_INFINITY);
                for (int j = 0; j < numNonZeros; j++) {
                    String[] pair = whitespace.split(readNextLineTrimmed(in, buffer));
                    int libdaiIx = Integer.valueOf(pair[0]);
                    int cix = libDaiIxToConfigId(libdaiIx, dims, varArray, vs);
                    double val = Math.log(Double.valueOf(pair[1]));
                    if (log.isTraceEnabled()) {
                        log.trace("here");
                        log.trace(ArrayUtils.toString(varIds));
                        log.trace(ArrayUtils.toString(Tensor.unravelIndexMatlab(libdaiIx, dims)));
                        log.trace("---");
                        log.trace(ArrayUtils.toString(vs.stream().map(v -> v.getName()).toArray()));
                        log.trace(ArrayUtils.toString(Tensor.unravelIndex(cix, vs.getDims())));
                        log.trace(String.format("cix: %s, val: %s", cix, val));
                    }
                    dstFac.setValue(cix, val);
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

    /**
     * converts the index to the index used in pacaya the difference is that the
     * vars in vs may be reordered compared to varArray and that pacaya
     * representation has the leftmost v in vs be the slowest changing while
     * libdai has the rightmost in varArray be the slowest changing; dims and
     * varArray are assumed to both correspond to the order of the variables
     * that is left to right (slowest changing last); the resulting index will
     * respect the order of var in vs which is vs.get(0) as the slowest
     * changing.
     * 
     */
    public static int libDaiIxToConfigId(int libDaiIx, int[] dims, Var[] varArray, VarSet vs) {
        int[] config = Tensor.unravelIndexMatlab(libDaiIx, dims);
        VarConfig vc = new VarConfig(varArray, config);
        return vc.getConfigIndexOfSubset(vs);
    }

    /**
     * converts the internal pacaya index of a config on the varset into the
     * libdai index corresponding to the same configuration
     */
    public static int configIdToLibDaiIx(int configId, VarSet vs) {
        int[] indices = vs.getVarConfigAsArray(configId);
        int[] dims = vs.getDims();
        return Tensor.ravelIndexMatlab(indices, dims);
    }

    private static int[] toIntArray(String line) {
        String[] strs = whitespace.split(line);
        int[] ints = new int[strs.length];
        for (int i = 0; i < ints.length; i++) {
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
                log.error("line {}: {}", l + 1, buffer.get(l));
            }
            throw new RuntimeException("Expected another line, but hit the end of the file.");
        }
        return line.trim();
    }

    public static void write(FactorGraph fg, Path file) {
        try (BufferedWriter out = Files.newBufferedWriter(file, Charset.forName("UTF-8"))) {
            write(fg, out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void write(FactorGraph fg, BufferedWriter out) {
        try {
            out.write(String.format("# Num factors = %d, Num variables = %d, Num edges = %d\n", fg.getNumFactors(),
                    fg.getNumVars(), fg.getNumEdges()));
            // Write: Number of factors.
            out.write(getNumNonGlobalFactors(fg) + "\n");
            // Write: Empty line.
            out.write("\n");
            for (Factor f : fg.getFactors()) {
                if (!(f instanceof GlobalFactor)) {
                    VarSet vars = f.getVars();
                    // Write: Number of variables in the factor.
                    out.write(vars.size() + "\n");

                    // Write: The ids of the vars
                    out.write(vars.stream()
                            .map(v -> Integer.toString(v.getId()))
                            .collect(Collectors.joining(" ")));
                    out.write("\n");

                    // Write: The number of values each variable in the above
                    // row can take on.
                    out.write(vars.stream()
                            .map(v -> Integer.toString(v.getNumStates()))
                            .collect(Collectors.joining(" ")));
                    out.write("\n");

                    // Write: The number of nonzero entries in the factor.
                    int n = vars.calcNumConfigs();
                    int nNonZero = 0;
                    for (int c = 0; c < n; c++) {
                        if (Math.exp(f.getLogUnormalizedScore(c)) > 0) {
                            nNonZero++;
                        }
                    }
                    out.write(nNonZero + "\n");

                    // Write: The values of the configurations of the variables.
                    // This is the exponentiated score, not the log-score.
                    for (int c = 0; c < n; c++) {
                        double prob = Math.exp(f.getLogUnormalizedScore(c));
                        if (prob > 0) {
                            out.write(String.format("%d %.17f\n", configIdToLibDaiIx(c, vars), prob));
                        }
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
