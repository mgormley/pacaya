package edu.jhu.srl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import edu.jhu.data.DepEdgeMask;
import edu.jhu.gm.feat.FactorTemplateList;
import edu.jhu.gm.feat.Feature;
import edu.jhu.gm.feat.FeatureExtractor;
import edu.jhu.gm.feat.ObsFeatureConjoiner;
import edu.jhu.gm.feat.ObsFeatureConjoiner.ObsFeatureConjoinerPrm;
import edu.jhu.gm.feat.ObsFeatureExtractor;
import edu.jhu.gm.inf.BeliefPropagation;
import edu.jhu.gm.inf.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.train.CrfTrainerTest.SimpleVCFeatureExtractor;
import edu.jhu.gm.train.CrfTrainerTest.SimpleVCFeatureExtractor2;
import edu.jhu.prim.set.IntHashSet;
import edu.jhu.prim.set.IntSet;
import edu.jhu.srl.JointNlpFactorGraph.JointFactorGraphPrm;
import edu.jhu.srl.SrlFactorGraph.RoleStructure;
import edu.jhu.srl.SrlFactorGraph.RoleVar;
import edu.jhu.srl.SrlFactorGraph.SenseVar;
import edu.jhu.srl.SrlFactorGraph.SrlFactorTemplate;
import edu.jhu.util.Alphabet;
import edu.jhu.util.collections.Lists;

/**
 * Unit tests for {@link JointNlpFactorGraph}.
 * @author mgormley
 */
public class JointNlpFactorGraphTest {

    @Test
    public void testNSquaredModel() {
        JointFactorGraphPrm prm = new JointFactorGraphPrm();
        prm.srlPrm.roleStructure = RoleStructure.ALL_PAIRS;
        prm.dpPrm.linkVarType = VarType.LATENT;
        prm.srlPrm.makeUnknownPredRolesLatent = false;
        prm.srlPrm.allowPredArgSelfLoops = false;
        prm.dpPrm.useProjDepTreeFactor = false;
        JointNlpFactorGraph sfg = getSrlFg(prm);
        
        LinkVar link = sfg.getLinkVar(1, 2);
        assertNotNull(link);
        assertEquals(1, link.getParent());
        assertEquals(2, link.getChild());

        assertNull(sfg.getLinkVar(1, 1));

        RoleVar role = sfg.getRoleVar(1, 2);
        assertNotNull(role);
        assertEquals(1, role.getParent());
        assertEquals(2, role.getChild());
        
        assertNull(sfg.getRoleVar(1, 1));
        
        List<Var> vars = sfg.getVars();
        assertEquals(9, VarSet.getVarsOfType(vars, VarType.LATENT).size());
        assertEquals(6, VarSet.getVarsOfType(vars, VarType.PREDICTED).size());
        // 6 unary Role, 6 binary Role Link, 9 unary Link.
        assertEquals(6 + 6 + 9, sfg.getNumFactors());
    }

    @Test
    public void testPredsGiven() {
        JointFactorGraphPrm prm = new JointFactorGraphPrm();
        prm.srlPrm.roleStructure = RoleStructure.PREDS_GIVEN;
        prm.dpPrm.linkVarType = VarType.LATENT;
        prm.srlPrm.makeUnknownPredRolesLatent = false;
        prm.srlPrm.allowPredArgSelfLoops = false;
        prm.dpPrm.useProjDepTreeFactor = false;
        JointNlpFactorGraph sfg = getSrlFg(prm);
        
        LinkVar link = sfg.getLinkVar(-1, 2);
        assertNotNull(link);
        assertEquals(-1, link.getParent());
        assertEquals(2, link.getChild());

        assertNull(sfg.getLinkVar(1, 1));

        RoleVar role = sfg.getRoleVar(1, 2);
        assertNull(role);
        
        assertNull(sfg.getRoleVar(1, 1));
        
        List<Var> vars = sfg.getVars();
        assertEquals(9, VarSet.getVarsOfType(vars, VarType.LATENT).size());
        assertEquals(4, VarSet.getVarsOfType(vars, VarType.PREDICTED).size());
        // 4 unary Role, 4 binary Role Link, 9 unary Link.
        assertEquals(4 + 4 + 9, sfg.getNumFactors());
    }
    
    @Test
    public void testRoleSelfLoops() {
        JointFactorGraphPrm prm = new JointFactorGraphPrm();
        prm.srlPrm.roleStructure = RoleStructure.ALL_PAIRS;
        prm.dpPrm.linkVarType = VarType.LATENT;
        prm.srlPrm.makeUnknownPredRolesLatent = false;
        prm.srlPrm.allowPredArgSelfLoops = true;
        prm.dpPrm.useProjDepTreeFactor = false;
        JointNlpFactorGraph sfg = getSrlFg(prm);

        assertNotNull(sfg.getRoleVar(1, 1));
        assertNotNull(sfg.getRoleVar(2, 2));
        
        List<Var> vars = sfg.getVars();
        assertEquals(9, VarSet.getVarsOfType(vars, VarType.LATENT).size());
        assertEquals(9, VarSet.getVarsOfType(vars, VarType.PREDICTED).size());
        // 9 unary Role, 6 binary Role Link, 9 unary Link.
        assertEquals(9 + 6 + 9, sfg.getNumFactors());
    }
    
    @Test
    public void testLinksPredictedRolesLatent() {
        JointFactorGraphPrm prm = new JointFactorGraphPrm();
        prm.srlPrm.roleStructure = RoleStructure.ALL_PAIRS;
        prm.dpPrm.linkVarType = VarType.PREDICTED;
        prm.srlPrm.makeUnknownPredRolesLatent = true;
        prm.srlPrm.allowPredArgSelfLoops = false;
        prm.dpPrm.useProjDepTreeFactor = false;
        JointNlpFactorGraph sfg = getSrlFg(prm);
        
        List<Var> vars = sfg.getVars();
        assertEquals(2, VarSet.getVarsOfType(vars, VarType.LATENT).size());
        assertEquals(9 + 4, VarSet.getVarsOfType(vars, VarType.PREDICTED).size());
        // 6 unary Role, 6 binary Role Link, 9 unary Link.
        assertEquals(6 + 6 + 9, sfg.getNumFactors());
    }

    @Test
    public void testUseProjDepTreeFactor() {
        JointFactorGraphPrm prm = new JointFactorGraphPrm();
        prm.srlPrm.roleStructure = RoleStructure.ALL_PAIRS;
        prm.dpPrm.linkVarType = VarType.LATENT;
        prm.srlPrm.makeUnknownPredRolesLatent = false;
        prm.srlPrm.allowPredArgSelfLoops = false;
        prm.dpPrm.useProjDepTreeFactor = true;
        JointNlpFactorGraph sfg = getSrlFg(prm);
        
        LinkVar link = sfg.getLinkVar(1, 2);
        assertNotNull(link);
        assertEquals(1, link.getParent());
        assertEquals(2, link.getChild());

        assertNull(sfg.getLinkVar(1, 1));

        RoleVar role = sfg.getRoleVar(1, 2);
        assertNotNull(role);
        assertEquals(1, role.getParent());
        assertEquals(2, role.getChild());
        
        assertNull(sfg.getRoleVar(1, 1));
        
        List<Var> vars = sfg.getVars();
        assertEquals(9, VarSet.getVarsOfType(vars, VarType.LATENT).size());
        assertEquals(6, VarSet.getVarsOfType(vars, VarType.PREDICTED).size());
        // 6 unary Role, 6 binary Role Link, 9 unary Link, 1 global.
        assertEquals(6 + 6 + 9 + 1, sfg.getNumFactors());
    }

    @Test
    public void testPredictSense() {
        JointFactorGraphPrm prm = new JointFactorGraphPrm();
        prm.srlPrm.roleStructure = RoleStructure.PREDS_GIVEN;
        prm.srlPrm.predictSense = true;
        JointNlpFactorGraph sfg = getSrlFg(prm);
        
        // Assertions about the Sense variables.
        assertNotNull(sfg.getSenseVar(0));
        assertNull(sfg.getSenseVar(1));
        assertNotNull(sfg.getSenseVar(2));
        
        assertEquals(VarType.PREDICTED, sfg.getSenseVar(0).getType());
        assertEquals(VarType.PREDICTED, sfg.getSenseVar(2).getType());
        
        assertEquals(0, sfg.getSenseVar(0).getParent());
        assertEquals(2, sfg.getSenseVar(2).getParent());
        
        assertEquals(Lists.getList("w1.01", "w1.02"), sfg.getSenseVar(0).getStateNames());
        assertEquals(Lists.getList("w3.01", "w3.02"), sfg.getSenseVar(2).getStateNames());
        
        // Assertions about the Sense factors.
        int numSenseFactors = 0;
        for (Factor f : sfg.getFactors()) {
            if (f instanceof ObsFeTypedFactor) {
                ObsFeTypedFactor srlf = (ObsFeTypedFactor) f;
                if (srlf.getFactorType() == SrlFactorTemplate.SENSE_UNARY) {
                    assertEquals(1, srlf.getVars().size());
                    assertTrue(srlf.getVars().iterator().next() instanceof SenseVar);
                    numSenseFactors++;
                }
            }
        }        
        assertEquals(2, numSenseFactors);
    }
    
    @Test
    public void testFirstOrderDepParser() {
        JointFactorGraphPrm prm = new JointFactorGraphPrm();
        prm.includeSrl = false;
        prm.dpPrm.linkVarType = VarType.PREDICTED;
        prm.dpPrm.unaryFactors = true;
        JointNlpFactorGraph sfg = getSrlFg(prm);
        
        assertEquals(9, sfg.getFactors().size());
        assertTrue(sfg.isUndirectedTree(sfg.getFactorNode(0)));
    }

    @Test
    public void testSecondOrderDepParser() {
        JointFactorGraphPrm prm = new JointFactorGraphPrm();
        prm.includeSrl = false;
        prm.dpPrm.linkVarType = VarType.PREDICTED;
        prm.dpPrm.unaryFactors = true;       
        JointNlpFactorGraph sfg;
        
        // Grandparents only
        prm.dpPrm.grandparentFactors = true;
        prm.dpPrm.siblingFactors = false;
        sfg = getSrlFg(prm);        
        assertEquals(9 + 10, sfg.getFactors().size());
        assertTrue(!sfg.isUndirectedTree(sfg.getFactorNode(0)));

        // Siblings only 
        prm.dpPrm.grandparentFactors = false;
        prm.dpPrm.siblingFactors = true;
        sfg = getSrlFg(prm);        
        assertEquals(9 + 6, sfg.getFactors().size());
        assertTrue(!sfg.isUndirectedTree(sfg.getFactorNode(0)));
        
        // Siblings and Grandparents 
        prm.dpPrm.excludeNonprojectiveGrandparents = false;
        prm.dpPrm.grandparentFactors = true;
        prm.dpPrm.siblingFactors = true;
        sfg = getSrlFg(prm);     
        assertEquals(9 + 12 + 6, sfg.getFactors().size());
        assertTrue(!sfg.isUndirectedTree(sfg.getFactorNode(0)));
    }
    
    @Test
    public void testSecondOrderDepParserPruned() {
        JointFactorGraphPrm prm = new JointFactorGraphPrm();
        prm.includeSrl = false;
        prm.dpPrm.linkVarType = VarType.PREDICTED;
        prm.dpPrm.unaryFactors = true;       
        JointNlpFactorGraph sfg;
        
        // Siblings and Grandparents 
        prm.dpPrm.excludeNonprojectiveGrandparents = false;
        prm.dpPrm.grandparentFactors = true;
        prm.dpPrm.siblingFactors = true;
        prm.dpPrm.pruneEdges = true;
        sfg = getSrlFg(prm);     
        assertEquals(11, sfg.getFactors().size());
        System.out.println(sfg.getFactors());
        // This pruned version is a tree.
        assertTrue(sfg.isUndirectedTree(sfg.getFactorNode(0)));
        
        BeliefPropagationPrm bpPrm = new BeliefPropagationPrm();
        BeliefPropagation bp = new BeliefPropagation(sfg, bpPrm);
        bp.run();
        System.out.println("\n\nVariable marginals:\n");
        for (Var v : sfg.getVars()) {
            System.out.println(bp.getMarginals(v));
        }
    }
    
    public static JointNlpFactorGraph getSrlFg(JointFactorGraphPrm prm) {
        // --- These won't even be used in these tests ---
        FactorTemplateList fts = new FactorTemplateList();
        FeatureExtractor fe = new SimpleVCFeatureExtractor2(new Alphabet<Feature>()); 
        ObsFeatureExtractor obsFe = new SimpleVCFeatureExtractor(fts);
        ObsFeatureConjoiner ofc = new ObsFeatureConjoiner(new ObsFeatureConjoinerPrm(), fts);
        // ---                                         ---
        Map<String,List<String>> psMap = new HashMap<String,List<String>>() {

            @Override
            public List<String> get(Object predicate) {
                return Lists.getList(predicate + ".01", predicate + ".02");
            }
            
        };
        IntSet knownPreds = IntHashSet.fromArray(0, 2);
        List<String> words = Lists.getList("w1", "w2", "w3");        
        // Prune all but a left branching tree.
        DepEdgeMask depEdgeMask = new DepEdgeMask(words.size(), false);
        for (int c=0; c<words.size(); c++) {
            depEdgeMask.setIsKept(c-1, c, true);
        }
        return new JointNlpFactorGraph(prm, words, words, depEdgeMask, knownPreds, Lists.getList("A1", "A2", "A3"), psMap, obsFe, ofc, fe);
    }
    
}
