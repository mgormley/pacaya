package edu.jhu.nlp.data.simple;

import java.util.ArrayList;
import java.util.List;

import edu.jhu.nlp.data.DepEdgeMask;
import edu.jhu.nlp.data.NerMentions;
import edu.jhu.nlp.data.RelationMentions;
import edu.jhu.nlp.data.conll.SrlGraph;
import edu.jhu.parse.cky.intdata.IntNaryTree;
import edu.jhu.prim.list.IntArrayList;
import edu.jhu.prim.set.IntHashSet;

public class IntAnnoSentence {

    private IntArrayList words;
    private IntArrayList lemmas;
    private IntArrayList posTags;
    private IntArrayList cposTags;
    private IntArrayList clusters;
    private List<double[]> embeds;
    private ArrayList<IntArrayList> feats;
    private IntArrayList deprels;
    /**
     * Internal representation of a dependency parse: parents[i] gives the index
     * of the parent of the word at index i. The Wall node has index -1. If a
     * word has no parent, it has index -2 (e.g. if punctuation was not marked
     * with a head).
     */
    private int[] parents;
    private DepEdgeMask depEdgeMask;
    private IntHashSet knownPreds;
    // TODO: This should be broken into semantic-roles and word senses.
    private SrlGraph srlGraph;
    /** Constituency parse. */
    private IntNaryTree naryTree;
    private NerMentions namedEntities;
    private RelationMentions relations;
    
    

}
