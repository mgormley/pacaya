package edu.jhu.nlp.srl;

import edu.jhu.data.conll.SrlGraph;
import edu.jhu.data.conll.SrlGraph.SrlEdge;
import edu.jhu.data.simple.AnnoSentence;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.nlp.CorpusStatistics;
import edu.jhu.nlp.joint.JointNlpFactorGraph;
import edu.jhu.nlp.srl.SrlFactorGraphBuilder.RoleVar;
import edu.jhu.nlp.srl.SrlFactorGraphBuilder.SenseVar;

public class SrlEncoder {

    public static void getSrlTrainAssignment(AnnoSentence sent, JointNlpFactorGraph sfg, VarConfig vc, boolean predictSense, boolean predictPredPos) {
        SrlGraph srlGraph = sent.getSrlGraph();
        if (srlGraph == null) {
            return;
        }
        
        // ROLE VARS
        // Add all the training data assignments to the role variables, if they are not latent.
        // First, just set all the role names to "_".
        for (int i=0; i<sent.size(); i++) {
            for (int j=0; j<sent.size(); j++) {
                RoleVar roleVar = sfg.getRoleVar(i, j);
                if (roleVar != null && roleVar.getType() != VarType.LATENT) {
                    vc.put(roleVar, "_");
                }
            }
        }
        // Then set the ones which are observed.
        for (SrlEdge edge : srlGraph.getEdges()) {
            int parent = edge.getPred().getPosition();
            int child = edge.getArg().getPosition();
            String roleName = edge.getLabel();
            
            RoleVar roleVar = sfg.getRoleVar(parent, child);
            if (roleVar != null && roleVar.getType() != VarType.LATENT) {
                int roleNameIdx = roleVar.getState(roleName);
                // TODO: This isn't quite right...we should really store the actual role name here.
                if (roleNameIdx == -1) {
                    vc.put(roleVar, CorpusStatistics.UNKNOWN_ROLE);
                } else {
                    vc.put(roleVar, roleNameIdx);
                }
            }
        }
        
        // Add the training data assignments to the predicate senses.
        for (int i=0; i<sent.size(); i++) {
            SenseVar senseVar = sfg.getSenseVar(i);
            if (senseVar != null) {
                if (predictSense) {
                    if (predictPredPos && !sent.isKnownPred(i)) {
                        vc.put(senseVar, "_");
                    } else {
                        // Tries to map the sense variable to its label (e.g. argM-TMP).
                        // If the variable state space does not include that label, we
                        // fall back on the UNKNOWN_SENSE constant. If for some reason
                        // the UNKNOWN_SENSE constant isn't present, we just predict the
                        // first possible sense.
                        if (!tryPut(vc, senseVar, srlGraph.getPredAt(i).getLabel())) {
                            if (!tryPut(vc, senseVar, CorpusStatistics.UNKNOWN_SENSE)) {
                                // This is a hack to ensure that something is added at test time.
                                vc.put(senseVar, 0);
                            }
                        }
                    }
                } else if (predictPredPos) {
                    if (sent.isKnownPred(i)) {
                        // We use CorpusStatistics.UNKNOWN_SENSE to indicate that
                        // there exists a predicate at this position.
                        vc.put(senseVar, CorpusStatistics.UNKNOWN_SENSE);
                    } else {
                        // The "_" indicates that there is no predicate at this
                        // position.
                        vc.put(senseVar, "_");
                    }
                } else {
                    throw new IllegalStateException("Neither predictSense nor predictPredPos is set. So there shouldn't be any SenseVars.");
                }
            }
        }
    }

    /**
     * Trys to put the entry (var, stateName) in vc.
     * @return True iff the entry (var, stateName) was added to vc.
     */
    private static boolean tryPut(VarConfig vc, Var var, String stateName) {
        int stateNameIdx = var.getState(stateName);
        if (stateNameIdx == -1) {
            return false;
        } else {
            vc.put(var, stateName);
            return true;
        }
    }
    
}
