package edu.jhu.nlp.relations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.jhu.data.NerMention;
import edu.jhu.data.NerMentions;
import edu.jhu.data.Span;
import edu.jhu.data.simple.AnnoSentence;
import edu.jhu.featurize.TemplateLanguage.FeatTemplate;
import edu.jhu.gm.feat.ObsFeatureConjoiner;
import edu.jhu.gm.feat.ObsFeatureExtractor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarSet;
import edu.jhu.nlp.CorpusStatistics;
import edu.jhu.nlp.ObsFeTypedFactor;
import edu.jhu.prim.tuple.Pair;

public class RelationsFactorGraphBuilder {

    private static final Logger log = Logger.getLogger(RelationsFactorGraphBuilder.class);

    public static class RelationsFactorGraphBuilderPrm {
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
        
        public RelVar(VarType type, String name, NerMention arg1, NerMention arg2, List<String> stateNames) {
            super(type, stateNames.size(), name, stateNames);
            if (ment1.compareTo(ment2) >= 0) {
                log.warn("The first mention (ment1) should always preceed the second mention (ment2)");
            }
            this.ment1 = arg1;
            this.ment2 = arg2;
        }

        public static String getDefaultName(Span arg1, Span arg2) {
            return String.format("RelVar_[%d,%d]_[%d,%d]", arg1.start(), arg2.end(), arg2.start(), arg2.end());
        }
        
    }
    
    /**
     * Adds factors and variables to the given factor graph.
     */
    public void build(AnnoSentence sent, ObsFeatureConjoiner cj, FactorGraph fg, CorpusStatistics cs) {
        varMap = new HashMap<>();
        
        // Create relation variables.
        //
        // Iterate over all pairs of mentions, such that ne1 comes before ne2.
        // This code assumes that the mentions are already in sorted order.
        List<RelVar> rvs = new ArrayList<>();
        NerMentions nes = sent.getNamedEntities();
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
        ObsFeatureExtractor obsFe = new RelObsFe(prm, sent, cj.getTemplates());
        for (RelVar rv : rvs) {
            fg.addFactor(new ObsFeTypedFactor(new VarSet(rv), RelationFactorType.RELATION, cj, obsFe));
        }
    }
    
    public RelVar getVar(NerMention ne1, NerMention ne2) {
        return varMap.get(new Pair<NerMention,NerMention>(ne1, ne2));
    }
    
}
