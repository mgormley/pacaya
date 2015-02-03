package edu.jhu.nlp.relations;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.gm.feat.ObsFeatureConjoiner;
import edu.jhu.gm.feat.ObsFeatureExtractor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarSet;
import edu.jhu.nlp.CorpusStatistics;
import edu.jhu.nlp.ObsFeTypedFactor;
import edu.jhu.nlp.data.NerMention;
import edu.jhu.nlp.data.Span;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.features.TemplateLanguage.FeatTemplate;
import edu.jhu.nlp.relations.RelObsFe.RelObsFePrm;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.util.Prm;

public class RelationsFactorGraphBuilder {

    private static final Logger log = LoggerFactory.getLogger(RelationsFactorGraphBuilder.class);

    public static class RelationsFactorGraphBuilderPrm extends Prm {
        private static final long serialVersionUID = 1L;
        public RelObsFePrm fePrm = new RelObsFePrm();
    }
    
    public enum RelationFactorType {
        RELATION
    }
    
    private RelationsFactorGraphBuilderPrm prm;
    private List<RelVar> relVars;
    
    public RelationsFactorGraphBuilder(RelationsFactorGraphBuilderPrm prm) {
        this.prm = prm;
    }
    
    public static class RelVar extends Var {

        private static final long serialVersionUID = 1L;

        NerMention ment1;
        NerMention ment2;     
        
        public RelVar(VarType type, String name, NerMention ment1, NerMention ment2, List<String> stateNames) {
            super(type, stateNames.size(), name, stateNames);
            if (ment1.compareTo(ment2) >= 0) {
                log.warn("The first mention (ment1) should always preceed the second mention (ment2)");
            }
            this.ment1 = ment1;
            this.ment2 = ment2;
        }

        public static String getDefaultName(Span arg1, Span arg2) {
            return String.format("RelVar_[%d,%d]_[%d,%d]", arg1.start(), arg2.end(), arg2.start(), arg2.end());
        }
        
    }
    
    /**
     * Adds factors and variables to the given factor graph.
     */
    public void build(AnnoSentence sent, ObsFeatureConjoiner cj, FactorGraph fg, CorpusStatistics cs, ObsFeatureExtractor obsFe) {
        relVars = new ArrayList<>();
        
        // Create relation variables.
        //
        // Iterate over all pairs of mentions, such that ne1 comes before ne2.
        // This code assumes that the mentions are already in sorted order.
        List<RelVar> rvs = new ArrayList<>();
        if (sent.getNePairs() == null) {
            throw new IllegalArgumentException("Relation extraction requires named entity pairs.");
        }
    	for (Pair<NerMention,NerMention> pair : sent.getNePairs()) {
    		NerMention ne1 = pair.get1();
    		NerMention ne2 = pair.get2();
            // Create relation variable.
            String name = RelVar.getDefaultName(ne1.getSpan(), ne2.getSpan());
            RelVar rv = new RelVar(VarType.PREDICTED, name, ne1, ne2, cs.relationStateNames);
            rvs.add(rv);
            relVars.add(rv);
        }
        
        // Create a unary factor for each relation variable.
        for (RelVar rv : rvs) {
            fg.addFactor(new ObsFeTypedFactor(new VarSet(rv), RelationFactorType.RELATION, cj, obsFe));
        }
    }
    
    public List<RelVar> getRelVars() {
        return relVars;
    }
    
}
