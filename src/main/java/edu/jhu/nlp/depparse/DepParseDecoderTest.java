package edu.jhu.nlp.depparse;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.jhu.data.DepEdgeMask;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.globalfac.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.model.VarSet;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.util.semiring.Algebras;

public class DepParseDecoderTest {
    
    private int n;
    private List<VarTensor> margs;
    private List<Var> vars;

    @Before
    public void setUp() {
        boolean logDomain = true;
        n = 3;       
        margs = new ArrayList<VarTensor>();
        vars = new ArrayList<Var>();
        for (int p=-1; p<n; p++) {
            for (int c=0; c<n; c++) {
                LinkVar v = new LinkVar(VarType.PREDICTED, LinkVar.getDefaultName(p, c), p, c);
                VarTensor f = new VarTensor(Algebras.REAL_ALGEBRA, new VarSet(v));
                if ((p == -1 && c == 1) || 
                        (p == 1 && c == 0) || 
                        (p == 1 && c == 2)) {
                    f.setValue(LinkVar.TRUE, 0.7);
                    f.setValue(LinkVar.FALSE, 0.3);
                } else {
                    f.setValue(LinkVar.TRUE, 0.3);
                    f.setValue(LinkVar.FALSE, 0.7);
                }
                margs.add(f);
                vars.add(v);
            }
        }
    }
    
    @Test
    public void testGetParents() {       
        int[] parents = DepParseDecoder.getParents(margs, vars, n);
        System.out.println(Arrays.toString(parents));
        Assert.assertArrayEquals(new int[]{1, -1, 1}, parents);
    }
    
    @Test
    public void testGetDepEdgeMaskPropMaxMarg() {
        double propMaxMarg = 0.1;
        DepEdgeMask mask = DepParseDecoder.getDepEdgeMask(margs, vars, n, propMaxMarg, 99);
        System.out.println(mask);
        assertNoneArePruned(mask);
        
        propMaxMarg = 0.5;
        mask = DepParseDecoder.getDepEdgeMask(margs, vars, n, propMaxMarg, 99);
        System.out.println(mask);
        assertOnlyMostLikelyAreKept(mask);
    }
    
    @Test
    public void testGetDepEdgeMaskCount() {
        DepEdgeMask mask = DepParseDecoder.getDepEdgeMask(margs, vars, n, 0, 99);
        System.out.println(mask);
        assertNoneArePruned(mask);
        
        mask = DepParseDecoder.getDepEdgeMask(margs, vars, n, 0, 1);
        System.out.println(mask);
        assertOnlyMostLikelyAreKept(mask);
    }

    private void assertOnlyMostLikelyAreKept(DepEdgeMask mask) {
        assertEquals(3, mask.getCount());

        for (int p=-1; p<n; p++) {
            for (int c=0; c<n; c++) {
                if (p == c) { continue; }
                if ((p == -1 && c == 1) || 
                        (p == 1 && c == 0) || 
                        (p == 1 && c == 2)) {
                    assertTrue("pc: "+p + " " + c, mask.isKept(p, c));
                    assertTrue("pc: "+p + " " + c, !mask.isPruned(p, c));
                } else {
                    assertTrue("pc: "+p + " " + c, !mask.isKept(p, c));
                    assertTrue("pc: "+p + " " + c, mask.isPruned(p, c));
                }
            }
        }
    }

    private void assertNoneArePruned(DepEdgeMask mask) {
        assertEquals(n*n, mask.getCount());

        for (int p=-1; p<n; p++) {
            for (int c=0; c<n; c++) {
                if (p == c) { continue; }
                assertTrue("pc: "+p + " " + c, mask.isKept(p, c));
                assertTrue("pc: "+p + " " + c, !mask.isPruned(p, c));
            }
        }
    }

}
