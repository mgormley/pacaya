package edu.jhu.hltcoe.gridsearch.dmv;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.util.Alphabet;
import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.data.Tag;
import edu.jhu.hltcoe.data.WallDepTreeNode;

public class ShinyEdges {
    
    private static final Logger log = Logger.getLogger(ShinyEdges.class);

    boolean[][] isShinyEdge;
    private Alphabet<Label> alphabet;
    private int wallIdx;
    private int numShinyEdges;

    public ShinyEdges(Alphabet<Label> alphabet) {
        alphabet.stopGrowth();
        this.alphabet = alphabet;
        this.wallIdx = alphabet.size();
        this.isShinyEdge = new boolean[alphabet.size() + 1][alphabet.size() + 1];
        this.numShinyEdges = 0;
    }

    /**
     * Arcs from Table 1 of Naseem et al. (2010).
     * 
     * Assumes that the tagset is given by the coarse universal linguistic
     * constraint plus the auxiliary tag.
     * 
     * From that paper: "These rules are defined over coarse part-of-speech tags: 
     * Noun, Verb, Adjective, Adverb, Pronoun, Article, Auxiliary, Preposition, 
     * Numeral and Conjunction."
     */
    public static ShinyEdges getUniversalSet(Alphabet<Label> alphabet) {
        ShinyEdges edges = new ShinyEdges(alphabet);
        
        Label root = WallDepTreeNode.WALL_LABEL;
        Label aux = new Tag("AUX"); // This is a custom tag not originally included in Das & Petrov (2010).
        Label verb = new Tag("VERB");
        Label noun = new Tag("NOUN");
        Label pron = new Tag("PRON");
        Label adj = new Tag("ADJ");
        Label adv = new Tag("ADV");
        Label adp = new Tag("ADP");
        Label conj = new Tag("CONJ");
        Label det = new Tag("DET");
        Label num = new Tag("NUM");
        Label prt = new Tag("PRT");
        Label other = new Tag("X");
        Label punc = new Tag(".");
        
        // Root
        edges.addShinyEdge(root, aux);
        edges.addShinyEdge(root, verb);
        // Verb
        edges.addShinyEdge(verb, noun);
        edges.addShinyEdge(verb, pron);
        edges.addShinyEdge(verb, adv);
        edges.addShinyEdge(verb, verb);
        // Aux
        edges.addShinyEdge(aux, verb);
        // Noun
        edges.addShinyEdge(noun, adj);
        edges.addShinyEdge(noun, det);
        edges.addShinyEdge(noun, noun);
        edges.addShinyEdge(noun, num);
        // Adp
        edges.addShinyEdge(adp, noun);
        // Adj
        edges.addShinyEdge(adj, adv);
        
        log.info("Num shiny edges: " + edges.getNumShinyEdges());
        return edges;
    }

    public void addShinyEdge(Label gov, Label dep) {
        int govIdx = getIdx(gov);
        int depIdx = getIdx(dep);
        if (govIdx == -1) {
            log.warn(String.format("Skipping edge: %s --> %s since %s is not in the alphabet", gov, dep, gov));
            return;
        } else if (depIdx == -1) {
            log.warn(String.format("Skipping edge: %s --> %s since %s is not in the alphabet", gov, dep, dep));
            return;
        }
        if (isShinyEdge[govIdx][depIdx]) {
            log.warn(String.format("Edge is already shiny: %s --> %s", gov, dep));
            return;
        }
        isShinyEdge[govIdx][depIdx] = true;
        numShinyEdges++;
    }

    public boolean isShiny(Label gov, Label dep) {
        int govIdx = getIdx(gov);
        int depIdx = getIdx(dep);
        return isShinyEdge[govIdx][depIdx];
    }

    private int getIdx(Label label) {
        if (label == WallDepTreeNode.WALL_LABEL) {
            return wallIdx;
        }
        return alphabet.lookupIndex(label);
    }
    
    private int getNumShinyEdges() {
        return numShinyEdges;
    }
}
