package edu.jhu.hltcoe.gridsearch.dmv;

import java.util.HashSet;

import util.Alphabet;

import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.data.SentenceCollection;

// TODO: finish writing this class.
public class ShinyEdgesConstraint {

    private HashSet<EdgeType> shinyEdges;
    private double proportionShiny;
    boolean[][] isShinyEdge;
    private Alphabet<Label> alphabet;
    
    private static class EdgeType {
        private Label gov;
        private Label dep;
        
        public EdgeType(Label gov, Label dep) {
            this.gov = gov;
            this.dep = dep;
        }

        public Label getGov() {
            return gov;
        }
        
        public Label getDep() {
            return dep;
        }
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((dep == null) ? 0 : dep.hashCode());
            result = prime * result + ((gov == null) ? 0 : gov.hashCode());
            return result;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            EdgeType other = (EdgeType) obj;
            if (dep == null) {
                if (other.dep != null)
                    return false;
            } else if (!dep.equals(other.dep))
                return false;
            if (gov == null) {
                if (other.gov != null)
                    return false;
            } else if (!gov.equals(other.gov))
                return false;
            return true;
        }
    }
    
    public ShinyEdgesConstraint(double proportionShiny, SentenceCollection sentences) {
        this.proportionShiny = proportionShiny;
        shinyEdges = new HashSet<EdgeType>();
        this.alphabet = sentences.getLabelAlphabet();
        isShinyEdge = new boolean[alphabet.size()][alphabet.size()];
    }
    
    public boolean isShiny(Label gov, Label dep) {
        int govIdx = alphabet.lookupObject(gov);
        int depIdx = alphabet.lookupObject(dep);
        return isShinyEdge[govIdx][depIdx];
    }
}
