package edu.jhu.nlp.relations;

import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.data.Span;
import edu.jhu.data.simple.AnnoSentence;
import edu.jhu.gm.feat.FeatureExtractor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.Var;

public class RelationsFactorGraphBuilder {

    private static final Logger log = Logger.getLogger(RelationsFactorGraphBuilder.class);

    public static class RelVar extends Var {

        private static final long serialVersionUID = 1L;

        private Span ment1;
        private Span ment2;     
        
        public RelVar(VarType type, String name, Span arg1, Span arg2, List<String> stateNames) {
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
    public void build(AnnoSentence sent, FeatureExtractor fe, FactorGraph fg) {
        // Create relation variables.
        
        
        // Create a unary factor for each relation variable.
        
    }
    
}
