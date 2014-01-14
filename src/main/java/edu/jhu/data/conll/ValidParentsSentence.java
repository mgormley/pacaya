package edu.jhu.data.conll;

import java.util.Arrays;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.jhu.data.Label;
import edu.jhu.data.Sentence;
import edu.jhu.data.Word;
import edu.jhu.data.conll.SrlGraph.SrlArg;
import edu.jhu.data.conll.SrlGraph.SrlEdge;
import edu.jhu.induce.model.dmv.DmvModel;
import edu.jhu.induce.train.dmv.DmvTrainerFactory;
import edu.jhu.parse.cky.CnfGrammar;
import edu.jhu.parse.dmv.DmvCkyParser;
import edu.jhu.prim.arrays.BoolArrays;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.util.Alphabet;

/**
 * A sentence with side information indicating which child-parent dependency
 * arcs are valid.
 * 
 * @author mgormley
 * 
 */
public class ValidParentsSentence extends Sentence {
    
    public static final Logger log = Logger.getLogger(ValidParentsSentence.class);

    /**
     * Indicates whether a word position can be the root of the sentence (i.e. have the wall as a parent).
     */
    private boolean[] validRoot;
    /** 
     * Indicates whether a parent/child arc is valid. Indexed by child position, parent position.
     */
    private boolean[][] validParents;

    public ValidParentsSentence(Alphabet<Label> alphabet, Iterable<Label> labels, boolean[] validRoot, boolean[][] validParents) {
        super(alphabet, labels);
        // TODO: we actually use this constraint for DMV parsing which doubles the length of the sentence.
        //        if (validParents.length != size() || validParents[0].length != size()) {
        //            throw new IllegalStateException("validParents array has the wrong dimensions");
        //        }
        this.validRoot = validRoot;
        this.validParents = validParents;
    }
    
    private ValidParentsSentence(Alphabet<Label> alphabet, int[] labelIds, boolean[] validRoot, boolean[][] validParents) {
        super(alphabet, labelIds);
        this.validRoot = validRoot;
        this.validParents = validParents;
    }

    public ValidParentsSentence(Alphabet<Label> alphabet, Iterable<Label> labels, SrlGraph g) {
        super(alphabet, labels);
        
        final int n = this.size();
        validRoot = new boolean[n];
        BoolArrays.fill(validRoot, true);
        validParents = new boolean[n][n];
        BoolArrays.fill(validParents, true);

        // For each child position (c) which is an argument, invalidate all the
        // parent positions (p) except those for which there is an edge to a
        // predicate. Also invalid the root for any arguments.
        for (SrlArg arg : g.getArgs()) {
            int c = arg.getPosition();
            validRoot[c] = false;
            Arrays.fill(validParents[c], false);
            for (SrlEdge edge : arg.getEdges()) {
                int p = edge.getPred().getPosition();
                validParents[c][p] = true;
            }
        }
                
        // Create a fake sentence with just the word "UNK".
        Alphabet<Label> fakeAlph = new Alphabet<Label>();
        fakeAlph.lookupIndex(new Word("UNK"));
        Sentence sent = new ValidParentsSentence(fakeAlph, new int[n], validRoot, validParents);
        // Parse the sentence to see if these constraints are valid.
        DmvCkyParser parser = new DmvCkyParser(DmvTrainerFactory.getDmvCkyParserPrm(true));
        Level oldLevel = CnfGrammar.log.getLevel();
        CnfGrammar.log.setLevel(Level.WARN);
        Pair<int[], Double> pair = parser.parseForParents(sent, new DmvModel(fakeAlph));
        CnfGrammar.log.setLevel(oldLevel);
        int[] parents = pair.get1();
        // Check whether we successfully parsed.
        if (parents[0] == -2) {
            // We were unable to parse this sentence, so instead just set all parents to valid.
            BoolArrays.fill(validRoot, true);
            BoolArrays.fill(validParents, true);
            log.warn("Not applying constraint to sentence due incompatibility with tree constraints");
        }
        
        // TODO: handle case where all entries in validRoot are false;
    }
    
    @Deprecated
    public ValidParentsSentence(CoNLL09Sentence sent, Alphabet<Label> alphabet) {
        this(alphabet, new Sentence(sent, alphabet), sent.getSrlGraph());
    }

    public boolean[] getValidRoot() {
        return validRoot;
    }
    
    public boolean[][] getValidParents() {
        return validParents;
    }
    
    public boolean isValid(int child, int head) {
        if (head == -1) {
            return validRoot[child];
        } else {
            return validParents[child][head];
        }
    }
    
}
