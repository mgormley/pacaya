package edu.jhu.nlp.depparse;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.jhu.gm.data.UnlabeledFgExample;
import edu.jhu.gm.inf.FgInferencer;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.gm.model.globalfac.ProjDepTreeFactor.LinkVar;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.parse.dep.EdgeScores;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.collections.Maps;
import edu.jhu.util.semiring.Algebras;

public class DepParseDecoderTest {
    
    int n;
    List<VarTensor> margs;
    List<Var> vars;
    FgInferencer inf;
    FactorGraph fg;
    EdgeScores scores;
    int linkVarCount;

    @Before
    public void setUp() {
        boolean logDomain = true;
        n = 3;       
        margs = new ArrayList<VarTensor>();
        vars = new ArrayList<Var>();
        for (int p=-1; p<n; p++) {
            for (int c=0; c<n; c++) {
                if (p == c) { continue; }
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
        
        inf = new MockFgInf(vars, margs);
        fg = new FactorGraph();
        for (Var var : vars) {
            fg.addVar(var);
        }
        // Create the EdgeScores.
        Pair<EdgeScores, Integer> pair = DepParseDecoder.getEdgeScores(inf, fg, n);
        scores = pair.get1();
        linkVarCount = pair.get2();
    }
    
    @Test 
    public void testGetEdgeScores() {
        
        assertEquals(3*3, linkVarCount);
        assertEquals(0.7, scores.getScore(-1, 1), 1e-13);
        assertEquals(0.7, scores.getScore(1, 0), 1e-13);
        assertEquals(0.7, scores.getScore(1, 2), 1e-13);
        assertEquals(0.3, scores.getScore(-1, 2), 1e-13);
        assertEquals(0.3, scores.getScore(-1, 0), 1e-13);
        assertEquals(0.3, scores.getScore(2, 1), 1e-13);
        assertEquals(0.3, scores.getScore(2, 0), 1e-13);
    }
    
    @Test
    public void testGetParents() { 
        int[] parents = DepParseDecoder.getParents(scores);
        System.out.println(Arrays.toString(parents));
        Assert.assertArrayEquals(new int[]{1, -1, 1}, parents);
    }
    
    @Test
    public void testDecoder() {
        DepParseDecoder dp = new DepParseDecoder();
        AnnoSentence sent = new AnnoSentence();
        sent.setWords(Lists.getList("a", "b", "c"));
        int[] parents = dp.decode(inf, new UnlabeledFgExample(fg, new VarConfig()), sent);
        System.out.println(Arrays.toString(parents));
        Assert.assertArrayEquals(new int[]{1, -1, 1}, parents);
    }
    
    public static class MockFgInf implements FgInferencer {

        Map<Var,VarTensor> var2marg;
        List<VarTensor> margs;
        
        public MockFgInf(List<Var> vars, List<VarTensor> margs) {
            this.margs = margs;
            this.var2marg = Maps.zip(vars, margs);
        }

        @Override
        public VarTensor getMarginals(Var var) {
            return var2marg.get(var);
        }

        @Override
        public VarTensor getMarginalsForVarId(int varId) {
            return margs.get(varId);
        }
        
        @Override
        public void run() { throw new RuntimeException("Should not be called"); }

        @Override
        public VarTensor getMarginals(Factor factor) { throw new RuntimeException("Should not be called"); }

        @Override
        public VarTensor getMarginalsForFactorId(int factorId) { throw new RuntimeException("Should not be called"); }

        @Override
        public double getPartition() { throw new RuntimeException("Should not be called"); }

        @Override
        public VarTensor getLogMarginals(Var var) { throw new RuntimeException("Should not be called"); }

        @Override
        public VarTensor getLogMarginals(Factor factor) { throw new RuntimeException("Should not be called"); }

        @Override
        public VarTensor getLogMarginalsForVarId(int varId) { throw new RuntimeException("Should not be called"); }

        @Override
        public VarTensor getLogMarginalsForFactorId(int factorId) { throw new RuntimeException("Should not be called"); }

        @Override
        public double getLogPartition() { throw new RuntimeException("Should not be called"); }
        
    }

}
