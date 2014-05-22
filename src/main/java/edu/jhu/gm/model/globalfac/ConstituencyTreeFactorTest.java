package edu.jhu.gm.model.globalfac;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

import edu.jhu.gm.inf.BeliefPropagation;
import edu.jhu.gm.inf.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.gm.inf.BruteForceInferencer;
import edu.jhu.gm.model.DenseFactor;
import edu.jhu.gm.model.ExplicitExpFamFactor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.globalfac.ConstituencyTreeFactor.SpanVar;

public class ConstituencyTreeFactorTest {
    
    @Test
    public void testBpVsBruteForce() {
        
        for(int n : Arrays.asList(2, 3, 4)) {
            for(boolean logDomain : Arrays.asList(false, true)) {

                ConstituencyTreeFactor ctFact = new ConstituencyTreeFactor(n, VarType.PREDICTED);
                FactorGraph fg = new FactorGraph();
                fg.addFactor(ctFact);

                BeliefPropagationPrm prm = new BeliefPropagationPrm();
                prm.logDomain = logDomain;
                BeliefPropagation bp = new BeliefPropagation(fg, prm);
                bp.run();

                BruteForceInferencer bf = new BruteForceInferencer(fg, logDomain);
                bf.run();

                for(int i=0; i<n; i++)  {
                    for(int j=i+1; j<=n; j++) {
                        SpanVar var = ctFact.getSpanVar(i, j);
                        DenseFactor bpMarg = bp.getMarginals(var);
                        DenseFactor bfMarg = bf.getMarginals(var);
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
        DenseFactor doesntLikeSpan = new DenseFactor(new VarSet(johnLoves));
        doesntLikeSpan.setValue(SpanVar.TRUE, -1d);
        doesntLikeSpan.setValue(SpanVar.FALSE, 1d);
        fg.addFactor(new ExplicitExpFamFactor(doesntLikeSpan));

        SpanVar lovesMary = ctFact.getSpanVar(1, 3);
        DenseFactor likesSpan = new DenseFactor(new VarSet(lovesMary));
        likesSpan.setValue(SpanVar.TRUE, 1d);
        likesSpan.setValue(SpanVar.FALSE, -1d);
        fg.addFactor(new ExplicitExpFamFactor(likesSpan));
        
        BeliefPropagationPrm prm = new BeliefPropagationPrm();
        prm.logDomain = true;
        BeliefPropagation bp = new BeliefPropagation(fg, prm);
        bp.run();
        
        DenseFactor johnLovesMarg = bp.getMarginals(johnLoves);
        DenseFactor lovesMaryMarg = bp.getMarginals(lovesMary);
        assertTrue(johnLovesMarg.getValue(SpanVar.TRUE) < lovesMaryMarg.getValue(SpanVar.TRUE));
        
        // TODO do inside by hand and put in some exact numbers
    }

}
