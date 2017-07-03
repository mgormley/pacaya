package edu.jhu.pacaya.gm.run;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.pacaya.gm.data.LFgExample;
import edu.jhu.pacaya.gm.data.LibDaiFgIo;
import edu.jhu.pacaya.gm.inf.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.pacaya.gm.inf.BeliefPropagation.BpScheduleType;
import edu.jhu.pacaya.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.pacaya.gm.inf.BruteForceInferencer.BruteForceInferencerPrm;
import edu.jhu.pacaya.gm.inf.FgInferencer;
import edu.jhu.pacaya.gm.inf.FgInferencerFactory;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.util.Threads;
import edu.jhu.pacaya.util.cli.ArgParser;
import edu.jhu.pacaya.util.cli.Opt;
import edu.jhu.pacaya.util.report.Reporter;
import edu.jhu.pacaya.util.report.ReporterManager;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.LogSemiring;
import edu.jhu.pacaya.util.semiring.LogSignAlgebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;
import edu.jhu.pacaya.util.semiring.ShiftedRealAlgebra;
import edu.jhu.pacaya.util.semiring.SplitAlgebra;
import edu.jhu.prim.util.random.Prng;

public class LibDaiFgRunner {

    public enum Inference {
        BRUTE_FORCE, BP, DP
    };

    public enum AlgebraType {
        REAL(RealAlgebra.getInstance()), LOG(LogSemiring.getInstance()), LOG_SIGN(LogSignAlgebra.getInstance()),
        // SHIFTED_REAL and SPLIT algebras are for testing only.
        SHIFTED_REAL(ShiftedRealAlgebra.getInstance()), SPLIT(SplitAlgebra.getInstance());

        private Algebra s;

        private AlgebraType(Algebra s) {
            this.s = s;
        }

        public Algebra getAlgebra() {
            return s;
        }
    }

    @Opt(name = "seed", hasArg = true, description = "Pseudo random number generator seed for everything else.")
    public static long seed = Prng.DEFAULT_SEED;
    @Opt(hasArg = true, description = "Number of threads for computation.")
    public static int threads = 1;
    @Opt(hasArg = true, description = "input libdai .fg file", required = true)
    public static String input = null;
    @Opt(hasArg = true, description = "Type of inference method.")
    public static Inference inference = Inference.BP;
    @Opt(hasArg = true, description = "The algebra or semiring in which to run inference.")
    public static AlgebraType algebra = AlgebraType.LOG;
    @Opt(hasArg = true, description = "The BP schedule type.")
    public static BpScheduleType bpSchedule = BpScheduleType.TREE_LIKE;
    @Opt(hasArg = true, description = "The BP update order.")
    public static BpUpdateOrder bpUpdateOrder = BpUpdateOrder.SEQUENTIAL;
    @Opt(hasArg = true, description = "The max number of BP iterations.")
    public static int bpMaxIterations = 1;
    @Opt(hasArg = true, description = "Whether to normalize the messages.")
    public static boolean normalizeMessages = false;
    @Opt(hasArg = true, description = "The maximum message residual for convergence testing.")
    public static double bpConvergenceThreshold = 0.0;
    @Opt(hasArg = true, description = "Directory to dump debugging information for BP.")
    public static File bpDumpDir = null;
    @Opt(hasArg = true, description = "Whether a tape of BP actions should be maintained (for ERMA training).")
    public static boolean keepTape = false;
    @Opt(hasArg=true, description="Prefix path to where to print libda formated factor graphs for read data")
    public static String exportGraphsPath = null;

    private static final Logger log = LoggerFactory.getLogger(LibDaiFgRunner.class);
    private static final Reporter rep = Reporter.getReporter(LibDaiFgRunner.class);

    public static void main(String[] args) {
        int exitCode = 0;
        ArgParser parser = null;
        try {
            parser = new ArgParser(LibDaiFgRunner.class);
            parser.registerClass(LibDaiFgRunner.class);
            parser.registerClass(ReporterManager.class);
            parser.parseArgs(args);
            ReporterManager.init(ReporterManager.reportOut, true);
            Prng.seed(seed);
            Threads.initDefaultPool(threads);
            // read in the model
            FactorGraph fg = LibDaiFgIo.read(Paths.get(input));
            maybeWriteGraph(fg);
            // do inference
            FgInferencerFactory infFactory = getInfFactory();
            FgInferencer inf = infFactory.getInferencer(fg);
            inf.run();
            // report the results
            log.info("Testing");
            rep.report("logPartition", inf.getLogPartition());
        } catch (ParseException e1) {
            log.error(e1.getMessage());
            if (parser != null) {
                parser.printUsage();
            }
            exitCode = 1;
        } catch (Throwable t) {
            t.printStackTrace();
            exitCode = 1;
        } finally {
            Threads.shutdownDefaultPool();
            ReporterManager.close();
        }

        System.exit(exitCode);
    }

    private static FgInferencerFactory getInfFactory() throws ParseException {
        if (inference == Inference.BRUTE_FORCE) {
            BruteForceInferencerPrm prm = new BruteForceInferencerPrm(algebra.getAlgebra());
            return prm;
        } else if (inference == Inference.BP) {
            BeliefPropagationPrm bpPrm = new BeliefPropagationPrm();
            bpPrm.s = algebra.getAlgebra();
            bpPrm.schedule = bpSchedule;
            bpPrm.updateOrder = bpUpdateOrder;
            bpPrm.normalizeMessages = normalizeMessages;
            bpPrm.maxIterations = bpMaxIterations;
            bpPrm.convergenceThreshold = bpConvergenceThreshold;
            bpPrm.keepTape = keepTape;
            if (bpDumpDir != null) {
                bpPrm.dumpDir = Paths.get(bpDumpDir.getAbsolutePath());
            }
            return bpPrm;
        } else {
            throw new ParseException("Unsupported inference method: " + inference);
        }
    }
    
    private static void maybeWriteGraph(FactorGraph fg) {
        if (exportGraphsPath != null) {
            Path outDir = Paths.get(exportGraphsPath);
            // create directory if doesn't already exist
            if (!Files.exists(outDir)) {
                try {
                    Files.createDirectories(outDir);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
            // write out each fg
            log.info(String.format("Writing libdai formatted factor graph files to: %s",  outDir.toAbsolutePath().toString()));
            LibDaiFgIo.write(fg, Paths.get(outDir.toString(), String.format("%d.fg", 0)));
        }
    }
    

}
