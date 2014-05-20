package edu.jhu.srl;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.jhu.data.DepEdgeMask;
import edu.jhu.gm.model.DenseFactor;
import edu.jhu.gm.model.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarSet;
import edu.jhu.prim.util.math.FastMath;

public class DepParseDecoderTest {
    
    @Before
    public void setUp() {
        
    }
    
    @Test
    public void testGetParents() {
        boolean logDomain = true;
        int n = 3;
        List<DenseFactor> margs = new ArrayList<DenseFactor>();
        List<Var> vars = new ArrayList<Var>();
        for (int p=-1; p<n; p++) {
            for (int c=0; c<n; c++) {
                LinkVar v = new LinkVar(VarType.PREDICTED, LinkVar.getDefaultName(p, c), p, c);
                DenseFactor f = new DenseFactor(new VarSet(v));
                if ((p == -1 && c == 1) || 
                        (p == 1 && c == 0) || 
                        (p == 1 && c == 2)) {
                    f.setValue(LinkVar.TRUE, FastMath.log(0.7));
                    f.setValue(LinkVar.FALSE, FastMath.log(0.3));
                } else {
                    f.setValue(LinkVar.TRUE, FastMath.log(0.3));
                    f.setValue(LinkVar.FALSE, FastMath.log(0.7));
                }
                if (logDomain) { f.convertLogToReal(); }
                margs.add(f);
                vars.add(v);
            }
        }
        
        int[] parents = DepParseDecoder.getParents(margs, vars, n);
        System.out.println(Arrays.toString(parents));
        Assert.assertArrayEquals(new int[]{1, -1, 1}, parents);
    }
    
    @Test
    public void testGetDepEdgeMask() {
        boolean logDomain = true;
        int n = 3;
        List<DenseFactor> margs = new ArrayList<DenseFactor>();
        List<Var> vars = new ArrayList<Var>();
        for (int p=-1; p<n; p++) {
            for (int c=0; c<n; c++) {
                LinkVar v = new LinkVar(VarType.PREDICTED, LinkVar.getDefaultName(p, c), p, c);
                DenseFactor f = new DenseFactor(new VarSet(v));
                if ((p == -1 && c == 1) || 
                        (p == 1 && c == 0) || 
                        (p == 1 && c == 2)) {
                    f.setValue(LinkVar.TRUE, FastMath.log(0.7));
                    f.setValue(LinkVar.FALSE, FastMath.log(0.3));
                } else {
                    f.setValue(LinkVar.TRUE, FastMath.log(0.3));
                    f.setValue(LinkVar.FALSE, FastMath.log(0.7));
                }
                if (logDomain) { f.convertLogToReal(); }
                margs.add(f);
                vars.add(v);
            }
        }

        double propMaxMarg = 0.1;
        DepEdgeMask mask = DepParseDecoder.getDepEdgeMask(margs, vars, n, propMaxMarg);
        System.out.println(mask);
        assertEquals(n*n, mask.getCount());
        for (int p=-1; p<n; p++) {
            for (int c=0; c<n; c++) {
                if (p == c) { continue; }
                assertTrue("pc: "+p + " " + c, mask.isKept(p, c));
                assertTrue("pc: "+p + " " + c, !mask.isPruned(p, c));
            }
        }
        
        propMaxMarg = 0.5;
        mask = DepParseDecoder.getDepEdgeMask(margs, vars, n, propMaxMarg);
        System.out.println(mask);
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

}
