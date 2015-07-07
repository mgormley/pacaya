package edu.jhu.pacaya.gm.data;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.pacaya.gm.inf.Beliefs;
import edu.jhu.pacaya.gm.inf.ErmaBp;
import edu.jhu.pacaya.gm.inf.ErmaBp.BpUpdateOrder;
import edu.jhu.pacaya.gm.inf.ErmaBp.ErmaBpPrm;
import edu.jhu.pacaya.gm.model.Factor;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.FactorGraphsForTests;
import edu.jhu.pacaya.util.semiring.RealAlgebra;
import edu.jhu.prim.util.Timer;
import edu.stanford.nlp.io.StringOutputStream;


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
            + "1 0\n"
            + "2 2\n"
            + "4\n"
            + "0 0.20000000000000000\n"
            + "1 0.40000000000000000\n"
            + "2 0.30000000000000000\n"
            + "3 0.50000000000000000\n"
            + "\n"
            + "2\n"
            + "2 1\n"
            + "2 2\n"
            + "4\n"
            + "0 1.20000000000000000\n"
            + "1 1.40000000000000000\n"
            + "2 1.30000000000000000\n"
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
        StringOutputStream sos = new StringOutputStream();
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sos));
        LibDaiFgIo.write(fg, out);
        String fgfile = sos.toString();
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
            ErmaBpPrm prm = new ErmaBpPrm();
            prm.updateOrder = BpUpdateOrder.PARALLEL;
            prm.maxIterations = 10;
            prm.keepTape = false;
            prm.s = RealAlgebra.getInstance();
            ErmaBp bp = new ErmaBp(fg, prm);
            Beliefs b = bp.forward();
            t.stop();
            log.info("Completed trial {} in {} secs", trial, t.totSec());
        }
    }

}
