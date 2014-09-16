package edu.jhu.nlp.relations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.jhu.gm.feat.ObsFeatureCache;
import edu.jhu.gm.feat.ObsFeatureConjoiner;
import edu.jhu.gm.feat.ObsFeatureExtractor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarSet;
import edu.jhu.nlp.CorpusStatistics;
import edu.jhu.nlp.ObsFeTypedFactor;
import edu.jhu.nlp.data.NerMention;
import edu.jhu.nlp.data.NerMentions;
import edu.jhu.nlp.data.Span;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.features.TemplateLanguage.FeatTemplate;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.util.Prm;

public class RelationsFactorGraphBuilder {

    private static final Logger log = Logger.getLogger(RelationsFactorGraphBuilder.class);

    public static class RelationsFactorGraphBuilderPrm extends Prm {
        private static final long serialVersionUID = 1L;
        public List<FeatTemplate> templates;
        public int featureHashMod = -1;
    }
    
    public enum RelationFactorType {
        RELATION
    }
    
    private RelationsFactorGraphBuilderPrm prm;
    private Map<Pair<NerMention, NerMention>, RelVar> varMap;
    
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
        varMap = new HashMap<>();
        
        // Create relation variables.
        //
        // Iterate over all pairs of mentions, such that ne1 comes before ne2.
        // This code assumes that the mentions are already in sorted order.
        List<RelVar> rvs = new ArrayList<>();
        NerMentions nes = sent.getNamedEntities();
        if (nes == null) {
            throw new IllegalArgumentException("Relation extraction requires named entities.");
        }
        for (int i = 0; i < nes.size(); i++) {
            NerMention ne1 = nes.get(i);
            for (int j=i+1; j < nes.size(); j++) {
                NerMention ne2 = nes.get(j);
                // Create relation variable.
                String name = RelVar.getDefaultName(ne1.getSpan(), ne2.getSpan());
                RelVar rv = new RelVar(VarType.PREDICTED, name, ne1, ne2, cs.relationStateNames);
                rvs.add(rv);
                varMap.put(new Pair<NerMention,NerMention>(ne1, ne2), rv);
            }
        }
        
        // Create a unary factor for each relation variable.
        for (RelVar rv : rvs) {
            fg.addFactor(new ObsFeTypedFactor(new VarSet(rv), RelationFactorType.RELATION, cj, obsFe));
        }
    }
    
    public RelVar getVar(NerMention ne1, NerMention ne2) {
        return varMap.get(new Pair<NerMention,NerMention>(ne1, ne2));
    }
    
}
