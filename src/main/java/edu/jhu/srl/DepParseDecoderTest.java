package edu.jhu.srl;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import edu.jhu.data.conll.SrlGraph;
import edu.jhu.gm.model.DenseFactor;
import edu.jhu.gm.model.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarSet;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.srl.SrlFactorGraph.RoleVar;
import edu.jhu.srl.SrlFactorGraph.SenseVar;
import edu.jhu.util.collections.Lists;

public class DepParseDecoderTest {
    
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
                    f.setValue(LinkVar.TRUE, FastMath.log(0.7));
                    f.setValue(LinkVar.FALSE, FastMath.log(0.3));
                } else {
                    f.setValue(LinkVar.TRUE, FastMath.log(0.3));
                    f.setValue(LinkVar.FALSE, FastMath.log(0.7));
                }
                margs.add(f);
                vars.add(v);
            }
        }
        
        int[] parents = DepParseDecoder.getParents(margs, vars, n);
        System.out.println(Arrays.toString(parents));
        Assert.assertArrayEquals(new int[]{1, -1, 1}, parents);
    }

}
