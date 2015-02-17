package edu.jhu.nlp.depparse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import edu.jhu.gm.data.UnlabeledFgExample;
import edu.jhu.gm.feat.FeatureExtractor;
import edu.jhu.gm.inf.BeliefPropagation;
import edu.jhu.gm.inf.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.gm.model.globalfac.LinkVar;
import edu.jhu.gm.train.CrfTrainerTest.SimpleVCFeatureExtractor;
import edu.jhu.nlp.data.DepEdgeMask;
import edu.jhu.nlp.depparse.DepParseFactorGraphBuilder.DepParseFactorGraphBuilderPrm;
import edu.jhu.prim.Primitives;
import edu.jhu.util.FeatureNames;
import edu.jhu.util.collections.Lists;


public class DepParseFactorGraphBuilderTest {

    @Test
    public void testFirstOrderDepParser() {
        DepParseFactorGraphBuilderPrm prm = getDefaultDepParseFactorGraphBuilderPrm();
        FactorGraph sfg = getJointNlpFg(prm);        
        assertEquals(9, sfg.getFactors().size());
        assertTrue(sfg.isUndirectedTree(sfg.getFactorNode(0)));
    }

    protected DepParseFactorGraphBuilderPrm getDefaultDepParseFactorGraphBuilderPrm() {
        DepParseFactorGraphBuilderPrm prm = new DepParseFactorGraphBuilderPrm();
        prm.linkVarType = VarType.PREDICTED;
        prm.unaryFactors = true;
        prm.pruneEdges = false;
        prm.grandparentFactors = false;
        prm.arbitrarySiblingFactors = false;
        prm.headBigramFactors = false;
        return prm;
    }

    @Test
    public void testAddGrandparentFactors() {
        // Grandparents only
        DepParseFactorGraphBuilderPrm prm = getDefaultDepParseFactorGraphBuilderPrm();
        prm.grandparentFactors = true;
        {
        prm.excludeNonprojectiveGrandparents = true;
        FactorGraph sfg = getJointNlpFg(prm);        
        assertEquals(9 + 10, sfg.getFactors().size());
        assertTrue(!sfg.isUndirectedTree(sfg.getFactorNode(0)));
        }{
        prm.excludeNonprojectiveGrandparents = false;
        FactorGraph sfg = getJointNlpFg(prm);        
        assertEquals(9 + 12, sfg.getFactors().size());
        assertTrue(!sfg.isUndirectedTree(sfg.getFactorNode(0)));
        }
    }
    
    @Test
    public void testAddArbitrarySiblingFactors() {
        // Arbitrary Siblings only 
        DepParseFactorGraphBuilderPrm prm = getDefaultDepParseFactorGraphBuilderPrm();
        prm.arbitrarySiblingFactors = true;
        FactorGraph sfg = getJointNlpFg(prm);        
        assertEquals(9 + 6, sfg.getFactors().size());
        assertTrue(!sfg.isUndirectedTree(sfg.getFactorNode(0)));
    }
    
    @Test
    public void testAddHeadBigramFactors() {
        // Head-bigrams only 
        DepParseFactorGraphBuilderPrm prm = getDefaultDepParseFactorGraphBuilderPrm();
        prm.headBigramFactors = true;
        FactorGraph sfg = getJointNlpFg(prm);        
        assertEquals(9 + 18, sfg.getFactors().size());
        assertTrue(!sfg.isUndirectedTree(sfg.getFactorNode(0)));
    }

    @Test
    public void testSecondOrderDepParser() {
        DepParseFactorGraphBuilderPrm prm = getDefaultDepParseFactorGraphBuilderPrm();
        FactorGraph sfg;
        // Siblings and Grandparents 
        prm.excludeNonprojectiveGrandparents = false;
        prm.grandparentFactors = true;
        prm.arbitrarySiblingFactors = true;
        sfg = getJointNlpFg(prm);     
        assertEquals(9 + 12 + 6, sfg.getFactors().size());
        assertTrue(!sfg.isUndirectedTree(sfg.getFactorNode(0)));
    }
    
    @Test
    public void testSecondOrderDepParserPruned() {
        DepParseFactorGraphBuilderPrm prm = getDefaultDepParseFactorGraphBuilderPrm();       
        FactorGraph sfg;
        
        // Siblings and Grandparents 
        prm.excludeNonprojectiveGrandparents = false;
        prm.grandparentFactors = true;
        prm.arbitrarySiblingFactors = true;
        prm.pruneEdges = true;
        sfg = getJointNlpFg(prm);     
        assertEquals(11, sfg.getFactors().size());
        System.out.println(sfg.getFactors());
        // This pruned version is a tree.
        assertTrue(sfg.isUndirectedTree(sfg.getFactorNode(0)));
        
        sfg.updateFromModel(new FgModel(1000));
        
        BeliefPropagationPrm bpPrm = new BeliefPropagationPrm();
        BeliefPropagation bp = new BeliefPropagation(sfg, bpPrm);
        bp.run();
        
        // Marginals should yield a left-branching tree.        
        System.out.println("\n\nVariable marginals:\n");
        for (Var v : sfg.getVars()) {
            VarTensor marg = bp.getMarginals(v);
            if (v instanceof LinkVar) {
                LinkVar link = (LinkVar) v;
                if (link.getParent() + 1 == link.getChild()) {
                    // Is left-branching edge.
                    assertTrue(!Primitives.equals(1.0, marg.getValue(LinkVar.FALSE), 1e-13)); 
                    assertTrue(!Primitives.equals(0.0, marg.getValue(LinkVar.TRUE), 1e-13)); 
                } else {
                    // Not left-branching edge.
                    assertEquals(1.0, marg.getValue(LinkVar.FALSE), 1e-13); 
                    assertEquals(0.0, marg.getValue(LinkVar.TRUE), 1e-13); 
                }
            }
            System.out.println(marg);
        }
    }


    public static FactorGraph getJointNlpFg(DepParseFactorGraphBuilderPrm prm) {
        // --- These won't even be used in these tests ---
        FeatureExtractor fe = new SimpleVCFeatureExtractor(new FeatureNames()); 
        // ---                                         ---
        List<String> words = Lists.getList("w1", "w2", "w3");
        // Prune all but a left branching tree.
        DepEdgeMask depEdgeMask = new DepEdgeMask(words.size(), false);
        for (int c=0; c<words.size(); c++) {
            depEdgeMask.setIsKept(c-1, c, true);
        }
        
        FactorGraph fg = new FactorGraph();        
        DepParseFactorGraphBuilder builder = new DepParseFactorGraphBuilder(prm);
        builder.build(words, depEdgeMask, fe, fg);
        
        fe.init(new UnlabeledFgExample(fg, new VarConfig()));
        return fg;
    }
    
}
