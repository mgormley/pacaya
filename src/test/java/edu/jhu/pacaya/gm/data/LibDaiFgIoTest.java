package edu.jhu.pacaya.gm.data;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.pacaya.gm.inf.BeliefPropagation;
import edu.jhu.pacaya.gm.inf.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.pacaya.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.pacaya.gm.inf.Beliefs;
import edu.jhu.pacaya.gm.model.Factor;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.FactorGraphsForTests;
import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.Var.VarType;
import edu.jhu.pacaya.gm.model.VarSet;
import edu.jhu.pacaya.util.semiring.RealAlgebra;
import edu.jhu.prim.util.Timer;


public class LibDaiFgIoTest {

    private static final Logger log = LoggerFactory.getLogger(LibDaiFgIoTest.class);

    String oneVarFg = "# Num factors = 1, Num variables = 1, Num edges = 2\n"
            + "1\n"
            + "\n"
            + "1\n"
            + "0\n"
            + "2\n"
            + "2\n"
            + "0 1.10000000000000000\n"
            + "1 1.90000000000000000\n"
            + "\n";
    
    private int[] array(int... ints) { return ints; }
    
    @Test
    public void testConfigIdToLibdaIx() {
        Var v1 = new Var(VarType.PREDICTED, 3, "v1", Arrays.asList("a", "b", "c"));
        Var v2 = new Var(VarType.PREDICTED, 2, "v2", Arrays.asList("e", "f"));
        VarSet vs = new VarSet(v1, v2);
        org.junit.Assert.assertArrayEquals(array(v1.getNumStates(), v2.getNumStates()), vs.getDims());

        Var[] vars = ArrayUtils.toArray(v2, v1);
        VarSet vs2 = new VarSet(vars);
        org.junit.Assert.assertArrayEquals(array(v1.getNumStates(), v2.getNumStates()), vs2.getDims());

        assertEquals(LibDaiFgIo.configIdToLibDaiIx(0, vs), 0);
        int[] dims = {  v2.getNumStates(), v1.getNumStates() };
        // because the order in vs2 is sorted and is the reverse of the order in vars, they should all match
        for (int i = 0; i < vs2.calcNumConfigs(); i++) {
            assertEquals(LibDaiFgIo.libDaiIxToConfigId(i, dims, vars, vs2), i);
        }
        // pacaya order:
        // v1 v2
        // 0 0 (becomes 0)
        // 0 1 (becomes 3)
        // 1 0 (becomes 1)
        // 1 1 (becomes 4)
        // 2 0 (becomes 2)
        // 2 1 (becomes 5)
        
        // reversed:
        // 0 0
        // 1 0
        // 2 0
        // 0 1
        // 1 1
        // 2 1
        
        // the conversion the other direction doesn't allow you to specify the order
        assertEquals(LibDaiFgIo.configIdToLibDaiIx(0, vs2), 0);
        assertEquals(LibDaiFgIo.configIdToLibDaiIx(1, vs2), 3);
        assertEquals(LibDaiFgIo.configIdToLibDaiIx(2, vs2), 1);
        assertEquals(LibDaiFgIo.configIdToLibDaiIx(3, vs2), 4);
        assertEquals(LibDaiFgIo.configIdToLibDaiIx(4, vs2), 2);
        assertEquals(LibDaiFgIo.configIdToLibDaiIx(5, vs2), 5);
        // since the pacaya var set is sorted, it doesn't matter which vs we use
        assertEquals(LibDaiFgIo.configIdToLibDaiIx(0, vs), 0);
        assertEquals(LibDaiFgIo.configIdToLibDaiIx(1, vs), 3);
        assertEquals(LibDaiFgIo.configIdToLibDaiIx(2, vs), 1);
        assertEquals(LibDaiFgIo.configIdToLibDaiIx(3, vs), 4);
        assertEquals(LibDaiFgIo.configIdToLibDaiIx(4, vs), 2);
        assertEquals(LibDaiFgIo.configIdToLibDaiIx(5, vs), 5);
}

    @Test
    public void testWriteOneVar() throws Exception {
        FactorGraph fg = FactorGraphsForTests.getOneVarFg();        
        String fgfile = writeToString(fg);
        assertEquals(oneVarFg, fgfile);        
    }

    @Test
    public void testReadWriteOneVar() throws IOException {
        FactorGraph fg = readFromString(oneVarFg);
        String fgfile = writeToString(fg);
        System.out.println(fgfile);
        assertEquals(oneVarFg, fgfile);
    }
    
    String linearChainFg = "# Num factors = 5, Num variables = 3, Num edges = 14\n"
            + "5\n"
            + "\n"
            + "1\n"
            + "0\n"
            + "2\n"
            + "2\n"
            + "0 0.10000000000000002\n"
            + "1 0.90000000000000000\n"
            + "\n"
            + "1\n"
            + "1\n"
            + "2\n"
            + "2\n"
            + "0 0.30000000000000000\n"
            + "1 0.70000000000000000\n"
            + "\n"
            + "1\n"
            + "2\n"
            + "2\n"
            + "2\n"
            + "0 0.50000000000000000\n"
            + "1 0.50000000000000000\n"
            + "\n"
            + "2\n"
            + "0 1\n"
            + "2 2\n"
            + "4\n"
            + "0 0.20000000000000000\n"
            + "2 0.40000000000000000\n"
            + "1 0.30000000000000000\n"
            + "3 0.50000000000000000\n"
            + "\n"
            + "2\n"
            + "1 2\n"
            + "2 2\n"
            + "4\n"
            + "0 1.20000000000000000\n"
            + "2 1.40000000000000000\n"
            + "1 1.30000000000000000\n"
            + "3 1.50000000000000000\n"
            + "\n";

    @Test
    public void testWriteLinearChain() throws Exception {
        FactorGraph fg = FactorGraphsForTests.getLinearChainGraph();        
        String fgfile = writeToString(fg);
        assertEquals(linearChainFg, fgfile);        
    }

    @Test
    public void testReadWriteLinearChain() throws IOException {
        FactorGraph fg = readFromString(linearChainFg);
        String fgfile = writeToString(fg);
        assertEquals(linearChainFg, fgfile);
    }

    private static FactorGraph readFromString(String input) {
        ByteArrayInputStream bais = new ByteArrayInputStream(input.getBytes());
        FactorGraph fg = LibDaiFgIo.read(new BufferedReader(new InputStreamReader(bais)));
        System.out.println(fg);
        for (Factor f : fg.getFactors()) {
            System.out.println(f);
        }
        return fg;
    }

    private static String writeToString(FactorGraph fg) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(bos));
        LibDaiFgIo.write(fg, out);
        String fgfile = bos.toString();
        System.out.println(fgfile);
        return fgfile;
    }
    
    /**
     * Output: July 1, 2015
     * Reading factor graph file: /Users/mgormley/research/easy-pacaya/temp/dp14.fg
     * Completed trial 1 in 9525.0ms
     */
    public static void main(String[] args) {
        Path file = Paths.get(args[0]);
        log.info("Reading factor graph file: " + file);
        FactorGraph fg = LibDaiFgIo.read(file);
        log.info("Running inference");
        // Run BP
        for (int trial=0; trial < 3; trial++) {
            Timer t = new Timer();
            t.start();
            BeliefPropagationPrm prm = new BeliefPropagationPrm();
            prm.updateOrder = BpUpdateOrder.PARALLEL;
            prm.maxIterations = 10;
            prm.keepTape = false;
            prm.s = RealAlgebra.getInstance();
            BeliefPropagation bp = new BeliefPropagation(fg, prm);
            Beliefs b = bp.forward();
            t.stop();
            log.info("Completed trial {} in {} secs", trial, t.totSec());
        }
    }

}
