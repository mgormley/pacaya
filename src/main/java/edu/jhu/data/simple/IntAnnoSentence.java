package edu.jhu.data.simple;

import java.util.ArrayList;

import edu.jhu.data.DepEdgeMask;
import edu.jhu.data.LabelSequence;
import edu.jhu.data.conll.SrlGraph;
import edu.jhu.parse.cky.intdata.IntBinaryTree;
import edu.jhu.prim.set.IntHashSet;

public class IntAnnoSentence {

    private LabelSequence<String> words;
    private LabelSequence<String> lemmas;
    private LabelSequence<String> posTags;
    private LabelSequence<String> cposTags;
    private LabelSequence<String> clusters;
    private ArrayList<LabelSequence<String>> feats;
    private LabelSequence<String> deprels;
    /**
     * Internal representation of a dependency parse: parents[i] gives the index
     * of the parent of the word at index i. The Wall node has index -1. If a
     * word has no parent, it has index -2 (e.g. if punctuation was not marked
     * with a head).
     */
    private int[] parents;
    private DepEdgeMask depEdgeMask;
    //private SrlGraph srlGraph;
    private IntHashSet knownPreds; 
    /** Constituency parse. */
    private IntBinaryTree binaryTree;

}
