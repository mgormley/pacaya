package edu.jhu.pacaya.gm.data.bayesnet;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import edu.jhu.pacaya.gm.inf.BruteForceInferencer;
import edu.jhu.pacaya.gm.model.Factor;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.VarConfig;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.util.semiring.RealAlgebra;

public class BayesNetReaderTest {

    public static final String cpdSimpleResource = "/edu/jhu/gm/data/cpd-simple.txt";
    public static final String networkSimpleResource = "/edu/jhu/gm/data/network-simple.txt";
    
    @Test
    public void testReadSimple() throws IOException {
        FactorGraph fg = readSimpleFg();
        
        assertEquals(2, fg.getNumFactors());
        assertEquals(3, fg.getNumVars());
        
        System.out.println(fg);
        for (Factor f : fg.getFactors()) {
            VarTensor vt = BruteForceInferencer.safeNewVarTensor(RealAlgebra.getInstance(), f);
            System.out.println(vt);
        }
        VarTensor vt0 = BruteForceInferencer.safeNewVarTensor(RealAlgebra.getInstance(), fg.getFactors().get(0));
        VarTensor vt1 = BruteForceInferencer.safeNewVarTensor(RealAlgebra.getInstance(), fg.getFactors().get(1));
        
        // Check that the variables are named correctly.
        assertEquals("A", vt0.getVars().get(0).getName());
        assertEquals("B", vt0.getVars().get(1).getName());
        assertEquals("C", vt0.getVars().get(2).getName());
        
        // Check that we read the values in correctly.
        VarConfig vc0 = new VarConfig();
        vc0.put(vt0.getVars().get(0), "YY");
        vc0.put(vt0.getVars().get(1), "Yes");
        vc0.put(vt0.getVars().get(2), "Yes");
        assertEquals(0.8, vt0.getValue(vc0.getConfigIndex()), 1e-13);
        
        // Check that we read the values in correctly.
        VarConfig vc1 = new VarConfig();
        vc1.put(vt1.getVars().get(0), "YY");
        vc1.put(vt1.getVars().get(1), "Yes");
        assertEquals(0.6, vt1.getValue(vc1.getConfigIndex()), 1e-13);
    }

    public static FactorGraph readSimpleFg() throws IOException {
        InputStream cpdIs = BayesNetReaderTest.class.getResourceAsStream(cpdSimpleResource);
        InputStream networkIs = BayesNetReaderTest.class.getResourceAsStream(networkSimpleResource);
        
        BayesNetReader bnr = new BayesNetReader();
        FactorGraph fg = bnr.readBnAsFg(networkIs, cpdIs);
        return fg;
    }

}
