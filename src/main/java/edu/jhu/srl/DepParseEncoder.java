package edu.jhu.srl;

import edu.jhu.data.simple.AnnoSentence;
import edu.jhu.gm.model.ProjDepTreeFactor.LinkVar;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;

public class DepParseEncoder {

    public static void getDepParseTrainAssignment(AnnoSentence sent, JointNlpFactorGraph sfg, VarConfig vc) {
        int[] parents = sent.getParents();
        if (parents == null) {
            return;
        }
        
        // LINK VARS
        // Add all the training data assignments to the link variables, if they are not latent.
        // IMPORTANT NOTE: We include the case where the parent is the Wall node (position -1).
        for (int i=-1; i<sent.size(); i++) {
            for (int j=0; j<sent.size(); j++) {
                if (j != i && sfg.getLinkVar(i, j) != null) {
                    LinkVar linkVar = sfg.getLinkVar(i, j);
                    if (linkVar.getType() != VarType.LATENT) {
                        // Syntactic head, from dependency parse.
                        int state;
                        if (parents[j] != i) {
                            state = LinkVar.FALSE;
                        } else {
                            state = LinkVar.TRUE;
                        }
                        vc.put(linkVar, state);
                    }
                }
            }
        }
    }

}
