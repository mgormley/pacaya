package edu.jhu.nlp.srl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.Test;

import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarSet;
import edu.jhu.nlp.joint.JointNlpFactorGraph;
import edu.jhu.nlp.joint.JointNlpFactorGraph.JointFactorGraphPrm;
import edu.jhu.nlp.joint.JointNlpFactorGraphTest;
import edu.jhu.nlp.srl.SrlFactorGraphBuilder.RoleStructure;

public class SrlFactorGraphBuilderTest {

    @Test
    public void testPredictPredPos() {
        JointFactorGraphPrm prm = new JointFactorGraphPrm();
        prm.includeDp = false;
        prm.srlPrm.roleStructure = RoleStructure.ALL_PAIRS;
        prm.srlPrm.makeUnknownPredRolesLatent = false;
        prm.srlPrm.allowPredArgSelfLoops = false;
        prm.srlPrm.predictPredPos = true;
        prm.srlPrm.binarySenseRoleFactors = true;
        JointNlpFactorGraph sfg = JointNlpFactorGraphTest.getJointNlpFg(prm);
                
        System.out.print("Vars: ");
        for (Var var : sfg.getVars()) {
            System.out.print(var.getName() + " ");
        }
        System.out.println();
        
        System.out.print("Factors: ");
        for (Factor f : sfg.getFactors()) {
            System.out.print(f.getVars() + " ");
        }
        System.out.println();
        
        assertNotNull(sfg.getRoleVar(1, 2));
        assertNull(sfg.getRoleVar(1, 1));
        
        List<Var> vars = sfg.getVars();
        assertEquals(0, VarSet.getVarsOfType(vars, VarType.LATENT).size());
        assertEquals(9, VarSet.getVarsOfType(vars, VarType.PREDICTED).size());
        // 6 unary Role, 6 binary Role Sense, 3 sense unary.
        assertEquals(6 + 6 + 3, sfg.getNumFactors());
    }
    
    @Test
    public void testPredictPredSenseAndPredPos() {
        JointFactorGraphPrm prm = new JointFactorGraphPrm();
        prm.includeDp = false;
        prm.srlPrm.roleStructure = RoleStructure.ALL_PAIRS;
        prm.srlPrm.makeUnknownPredRolesLatent = false;
        prm.srlPrm.allowPredArgSelfLoops = false;
        prm.srlPrm.predictSense = true;
        prm.srlPrm.predictPredPos = true;
        prm.srlPrm.binarySenseRoleFactors = true;
        JointNlpFactorGraph sfg = JointNlpFactorGraphTest.getJointNlpFg(prm);
                
        System.out.print("Vars: ");
        for (Var var : sfg.getVars()) {
            System.out.print(var.getName() + " ");
        }
        System.out.println();
        
        System.out.print("Factors: ");
        for (Factor f : sfg.getFactors()) {
            System.out.print(f.getVars() + " ");
        }
        System.out.println();
        
        assertNotNull(sfg.getRoleVar(1, 2));
        assertNull(sfg.getRoleVar(1, 1));
        
        List<Var> vars = sfg.getVars();
        assertEquals(0, VarSet.getVarsOfType(vars, VarType.LATENT).size());
        assertEquals(9, VarSet.getVarsOfType(vars, VarType.PREDICTED).size());
        // 6 unary Role, 6 binary Role Sense, 3 sense unary.
        assertEquals(6 + 6 + 3, sfg.getNumFactors());
    }
    
}
