package edu.jhu.srl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import edu.jhu.gm.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.Var;
import edu.jhu.gm.Var.VarType;
import edu.jhu.gm.VarSet;
import edu.jhu.srl.SrlFactorGraph.RoleStructure;
import edu.jhu.srl.SrlFactorGraph.RoleVar;
import edu.jhu.srl.SrlFactorGraph.SrlFactorGraphPrm;
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
        HashSet<Integer> knownPreds = new HashSet<Integer>(Utilities.getList(0, 2));
        SrlFactorGraph sfg = new SrlFactorGraph(prm, 3, knownPreds, Utilities.getList("A1", "A2", "A3"));
        
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
        HashSet<Integer> knownPreds = new HashSet<Integer>(Utilities.getList(0, 2));
        SrlFactorGraph sfg = new SrlFactorGraph(prm, 3, knownPreds, Utilities.getList("A1", "A2", "A3"));
        
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
        HashSet<Integer> knownPreds = new HashSet<Integer>(Utilities.getList(0, 2));
        SrlFactorGraph sfg = new SrlFactorGraph(prm, 3, knownPreds, Utilities.getList("A1", "A2", "A3"));

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
        HashSet<Integer> knownPreds = new HashSet<Integer>(Utilities.getList(0, 2));
        SrlFactorGraph sfg = new SrlFactorGraph(prm, 3, knownPreds, Utilities.getList("A1", "A2", "A3"));
        
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
        HashSet<Integer> knownPreds = new HashSet<Integer>(Utilities.getList(0, 2));
        SrlFactorGraph sfg = new SrlFactorGraph(prm, 3, knownPreds, Utilities.getList("A1", "A2", "A3"));
        
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
}
