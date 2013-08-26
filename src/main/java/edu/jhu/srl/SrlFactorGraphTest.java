package edu.jhu.srl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import edu.jhu.gm.Factor;
import edu.jhu.gm.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.Var;
import edu.jhu.gm.Var.VarType;
import edu.jhu.gm.VarSet;
import edu.jhu.srl.SrlFactorGraph.RoleStructure;
import edu.jhu.srl.SrlFactorGraph.RoleVar;
import edu.jhu.srl.SrlFactorGraph.SenseVar;
import edu.jhu.srl.SrlFactorGraph.SrlFactor;
import edu.jhu.srl.SrlFactorGraph.SrlFactorGraphPrm;
import edu.jhu.srl.SrlFactorGraph.SrlFactorTemplate;
import edu.jhu.util.Utilities;

/**
 * Unit tests for {@link SrlFactorGraph}.
 * @author mgormley
 */
public class SrlFactorGraphTest {

    @Test
    public void testNSquaredModel() {
        SrlFactorGraphPrm prm = new SrlFactorGraphPrm();
        prm.roleStructure = RoleStructure.ALL_PAIRS;
        prm.linkVarType = VarType.LATENT;
        prm.makeUnknownPredRolesLatent = false;
        prm.allowPredArgSelfLoops = false;
        prm.useProjDepTreeFactor = false;
        prm.alwaysIncludeLinkVars = true;
        SrlFactorGraph sfg = getSrlFg(prm);
        
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
        SrlFactorGraphPrm prm = new SrlFactorGraphPrm();
        prm.roleStructure = RoleStructure.PREDS_GIVEN;
        prm.linkVarType = VarType.LATENT;
        prm.makeUnknownPredRolesLatent = false;
        prm.allowPredArgSelfLoops = false;
        prm.useProjDepTreeFactor = false;
        prm.alwaysIncludeLinkVars = true;
        SrlFactorGraph sfg = getSrlFg(prm);
        
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
        SrlFactorGraphPrm prm = new SrlFactorGraphPrm();
        prm.roleStructure = RoleStructure.ALL_PAIRS;
        prm.linkVarType = VarType.LATENT;
        prm.makeUnknownPredRolesLatent = false;
        prm.allowPredArgSelfLoops = true;
        prm.useProjDepTreeFactor = false;
        prm.alwaysIncludeLinkVars = true;
        SrlFactorGraph sfg = getSrlFg(prm);

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
        SrlFactorGraphPrm prm = new SrlFactorGraphPrm();
        prm.roleStructure = RoleStructure.ALL_PAIRS;
        prm.linkVarType = VarType.PREDICTED;
        prm.makeUnknownPredRolesLatent = true;
        prm.allowPredArgSelfLoops = false;
        prm.useProjDepTreeFactor = false;
        prm.alwaysIncludeLinkVars = true;
        SrlFactorGraph sfg = getSrlFg(prm);
        
        List<Var> vars = sfg.getVars();
        assertEquals(2, VarSet.getVarsOfType(vars, VarType.LATENT).size());
        assertEquals(9 + 4, VarSet.getVarsOfType(vars, VarType.PREDICTED).size());
        // 6 unary Role, 6 binary Role Link, 9 unary Link.
        assertEquals(6 + 6 + 9, sfg.getNumFactors());
    }

    @Test
    public void testUseProjDepTreeFactor() {
        SrlFactorGraphPrm prm = new SrlFactorGraphPrm();
        prm.roleStructure = RoleStructure.ALL_PAIRS;
        prm.linkVarType = VarType.LATENT;
        prm.makeUnknownPredRolesLatent = false;
        prm.allowPredArgSelfLoops = false;
        prm.useProjDepTreeFactor = true;
        SrlFactorGraph sfg = getSrlFg(prm);
        
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
        SrlFactorGraphPrm prm = new SrlFactorGraphPrm();
        prm.roleStructure = RoleStructure.PREDS_GIVEN;
        prm.predictSense = true;
        SrlFactorGraph sfg = getSrlFg(prm);
        
        // Assertions about the Sense variables.
        assertNotNull(sfg.getSenseVar(0));
        assertNull(sfg.getSenseVar(1));
        assertNotNull(sfg.getSenseVar(2));
        
        assertEquals(VarType.PREDICTED, sfg.getSenseVar(0).getType());
        assertEquals(VarType.PREDICTED, sfg.getSenseVar(2).getType());
        
        assertEquals(0, sfg.getSenseVar(0).getParent());
        assertEquals(2, sfg.getSenseVar(2).getParent());
        
        assertEquals(Utilities.getList("w1.01", "w1.02"), sfg.getSenseVar(0).getStateNames());
        assertEquals(Utilities.getList("w3.01", "w3.02"), sfg.getSenseVar(2).getStateNames());
        
        // Assertions about the Sense factors.
        int numSenseFactors = 0;
        for (Factor f : sfg.getFactors()) {
            SrlFactor srlf = (SrlFactor) f;
            if (srlf.getFactorType() == SrlFactorTemplate.SENSE_UNARY) {
                assertEquals(1, srlf.getVars().size());
                assertTrue(srlf.getVars().iterator().next() instanceof SenseVar);
                numSenseFactors++;
            }
        }        
        assertEquals(2, numSenseFactors);
    }

    private static SrlFactorGraph getSrlFg(SrlFactorGraphPrm prm) {
        PredSenseMap psMap = new PredSenseMap() {

            @Override
            public List<String> getSenseStateNames(String predicate) {
                return Utilities.getList(predicate + ".01", predicate + ".02");
            }
            
        };
        HashSet<Integer> knownPreds = new HashSet<Integer>(Utilities.getList(0, 2));
        return new SrlFactorGraph(prm, Utilities.getList("w1", "w2", "w3"), knownPreds, Utilities.getList("A1", "A2", "A3"), psMap);
    }
    
}
