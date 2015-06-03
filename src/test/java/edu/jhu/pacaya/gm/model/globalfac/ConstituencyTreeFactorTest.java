package edu.jhu.pacaya.gm.model.globalfac;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Ignore;
import org.junit.Test;

import edu.jhu.pacaya.gm.inf.BeliefPropagation;
import edu.jhu.pacaya.gm.inf.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.pacaya.gm.inf.BruteForceInferencer;
import edu.jhu.pacaya.gm.model.ExplicitExpFamFactor;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.Var.VarType;
import edu.jhu.pacaya.gm.model.VarSet;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.gm.model.globalfac.ConstituencyTreeFactor.SpanVar;
import edu.jhu.pacaya.util.collections.Lists;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.LogSemiring;
import edu.jhu.pacaya.util.semiring.RealAlgebra;

@Ignore("Needs to be updated with Travis' latest version")
public class ConstituencyTreeFactorTest {
    
    @Test
    public void testBpVsBruteForce() {
        
        for(int n : Arrays.asList(2, 3, 4)) {
            for(Algebra s : Lists.getList(RealAlgebra.REAL_ALGEBRA, LogSemiring.LOG_SEMIRING)) {

                ConstituencyTreeFactor ctFact = new ConstituencyTreeFactor(n, VarType.PREDICTED);
                FactorGraph fg = new FactorGraph();
                fg.addFactor(ctFact);

                BeliefPropagationPrm prm = new BeliefPropagationPrm();
                prm.s = s;
                BeliefPropagation bp = new BeliefPropagation(fg, prm);
                bp.run();

                BruteForceInferencer bf = new BruteForceInferencer(fg, s);
                bf.run();

                for(int i=0; i<n; i++)  {
                    for(int j=i+1; j<=n; j++) {
                        SpanVar var = ctFact.getSpanVar(i, j);
                        VarTensor bpMarg = bp.getMarginals(var);
                        VarTensor bfMarg = bf.getMarginals(var);
                        assertTrue(bpMarg.equals(bfMarg, 1e-6));
                    }
                }

            }
        }
        
    }
    
    @Test
    public void testByHand() {
        // "John loves Mary"
        ConstituencyTreeFactor ctFact = new ConstituencyTreeFactor(3, VarType.PREDICTED);
        FactorGraph fg = new FactorGraph();
        fg.addFactor(ctFact);
        
        SpanVar johnLoves = ctFact.getSpanVar(0, 2);
        ExplicitExpFamFactor doesntLikeSpan = new ExplicitExpFamFactor(new VarSet(johnLoves));
        doesntLikeSpan.setValue(SpanVar.TRUE, -1d);
        doesntLikeSpan.setValue(SpanVar.FALSE, 1d);
        fg.addFactor(doesntLikeSpan);

        SpanVar lovesMary = ctFact.getSpanVar(1, 3);
        ExplicitExpFamFactor likesSpan = new ExplicitExpFamFactor(new VarSet(lovesMary));
        likesSpan.setValue(SpanVar.TRUE, 1d);
        likesSpan.setValue(SpanVar.FALSE, -1d);
        fg.addFactor(likesSpan);
        
        BeliefPropagationPrm prm = new BeliefPropagationPrm();
        prm.s = LogSemiring.LOG_SEMIRING;
        BeliefPropagation bp = new BeliefPropagation(fg, prm);
        bp.run();
        
        VarTensor johnLovesMarg = bp.getMarginals(johnLoves);
        VarTensor lovesMaryMarg = bp.getMarginals(lovesMary);
        assertTrue(johnLovesMarg.getValue(SpanVar.TRUE) < lovesMaryMarg.getValue(SpanVar.TRUE));
        
        // TODO do inside by hand and put in some exact numbers
    }

}
