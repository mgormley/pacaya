package edu.jhu.srl;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import edu.jhu.data.conll.SrlGraph;
import edu.jhu.gm.model.DenseFactor;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.model.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.srl.SrlFactorGraph.RoleVar;
import edu.jhu.srl.SrlFactorGraph.SenseVar;
import edu.jhu.util.Utilities;

public class SrlDecoderTest {

    @Test
    public void testGetSrlGraph() {
        int n = 3;
        VarConfig vc = new VarConfig();
        vc.put(new SenseVar(VarType.PREDICTED, 2, "s-1", Utilities.getList("false","true"), 1), 1);
        vc.put(new RoleVar(VarType.PREDICTED, 2, "r-1_0", Utilities.getList("false","true"), 1, 0), 1);
        vc.put(new RoleVar(VarType.PREDICTED, 2, "r-1_2", Utilities.getList("false","true"), 1, 2), 0);
        // Self-loop
        vc.put(new SenseVar(VarType.PREDICTED, 2, "s-2", Utilities.getList("false","true"), 2), 1);
        vc.put(new RoleVar(VarType.PREDICTED, 2, "r-2_2", Utilities.getList("false","true"), 2, 2), 1);
        SrlGraph g = SrlDecoder.getSrlGraphFromVarConfig(vc, n);
        
        System.out.println(g);
        assertEquals(2, g.getNumPreds());
        assertEquals(2, g.getNumArgs());
        
        assertEquals("true", g.getPredAt(1).getLabel());
        assertEquals("true", g.getArgAt(0).getEdges().get(0).getLabel());
        assertEquals("false", g.getArgAt(2).getEdges().get(0).getLabel());
        assertEquals("true", g.getPredAt(2).getLabel());
        assertEquals("true", g.getArgAt(2).getEdges().get(1).getLabel());
    }
    
    @Test
    public void testGetParents() {
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
                    f.setValue(LinkVar.TRUE, Utilities.log(0.7));
                    f.setValue(LinkVar.FALSE, Utilities.log(0.3));
                } else {
                    f.setValue(LinkVar.TRUE, Utilities.log(0.3));
                    f.setValue(LinkVar.FALSE, Utilities.log(0.7));
                }
                margs.add(f);
                vars.add(v);
            }
        }
        
        int[] parents = SrlDecoder.getParents(margs, vars, n);
        System.out.println(Arrays.toString(parents));
        Assert.assertArrayEquals(new int[]{1, -1, 1}, parents);
    }

}
