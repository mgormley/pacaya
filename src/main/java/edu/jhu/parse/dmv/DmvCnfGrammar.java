package edu.jhu.parse.dmv;

import java.util.ArrayList;

import edu.jhu.data.Label;
import edu.jhu.data.Sentence;
import edu.jhu.data.Tag;
import edu.jhu.data.Word;
import edu.jhu.induce.model.dmv.DmvModel;
import edu.jhu.parse.cky.CkyPcfgParser.LoopOrder;
import edu.jhu.parse.cky.CnfGrammar;
import edu.jhu.parse.cky.Rule;
import edu.jhu.parse.dmv.DmvRule.DmvRuleType;
import edu.jhu.util.Alphabet;
import edu.jhu.util.collections.Lists;

public class DmvCnfGrammar {
    
    private static final String rootSymbolStr = "S";

    // Rules that correspond to probabilities in the model.
    private final Rule[] root; // Indexed by child.
    private final Rule[][][] child; // Indexed by child, parent, and direction.
    private final Rule[][][][] decision; // Indexed by parent, direction, valence (0 or 1), and STOP/CONT.    
    // Structural rules (with probability 1.0).
    private final Rule[][][] structural; // Indexed by child, parent, and direction.
    
    private final CnfGrammar cnfGrammar;
    private final Alphabet<Label> lexAlphabet;
    private final Alphabet<Label> ntAlphabet;

    private final int numTags;
    private final int rootSymbol;
    
    private int[] annoToUnanno; // Mapping of annotated tags to unannotated tags.
    private int[][] unannoToAnno; // Mapping of annotated tags to annotated tags.
    
    public DmvCnfGrammar(DmvModel dmv, Alphabet<Label> labelAlphabet, LoopOrder loopOrder) {
        numTags = labelAlphabet.size();
        this.lexAlphabet = new Alphabet<Label>();
        // Cache mapping of unannoated tags to annotated tags.
        annoToUnanno = new int[numTags*2];
        unannoToAnno = new int[numTags][2];
        for (int unanno=0; unanno<labelAlphabet.size(); unanno++) {
            for (int dir=0; dir<2; dir++) {
                int anno;
                if (dir == DmvModel.LEFT) {
                    anno = lexAlphabet.lookupIndex(new Word(String.format("%d_{l}", unanno))); 
                } else {
                    anno = lexAlphabet.lookupIndex(new Word(String.format("%d_{r}", unanno)));
                }
                annoToUnanno[anno] = unanno;
                unannoToAnno[unanno][dir] = anno;
            }
        }
        this.ntAlphabet = new Alphabet<Label>();
        this.rootSymbol = ntAlphabet.lookupIndex(new Tag(rootSymbolStr));
        
        this.root = new Rule[numTags];
        this.child = new Rule[numTags][numTags][2];
        this.decision = new Rule[numTags][2][2][2];
        this.structural = new Rule[numTags][numTags][2];
        
        for (int c=0; c<numTags; c++) {
            this.root[c] = getRootRule(c);
            for (int p=0; p<numTags; p++) {
                for (int dir=0; dir<2; dir++) {
                    this.child[c][p][dir] = getChildRule(c, p, dir);
                    this.structural[c][p][dir] = getStructuralRule(c, p, dir); 
                }
            }
            for (int dir=0; dir<2; dir++) {
                for (int val=0; val<2; val++) {
                    for (int sc=0; sc<2; sc++) {
                        this.decision[c][dir][val][sc] = getDecisionRule(c, dir, val, sc);
                    }
                }
            }
        }
        
        updateLogProbs(dmv);
        
        this.cnfGrammar = new CnfGrammar(getAllRules(), rootSymbol, lexAlphabet, ntAlphabet, loopOrder);
    }

    public int getAnnotatedTagRight(int tag) {
        return unannoToAnno[tag][DmvModel.Lr.RIGHT.getAsInt()];
    }
    
    public int getAnnotatedTagLeft(int tag) {
        return unannoToAnno[tag][DmvModel.Lr.LEFT.getAsInt()];
    }
    
    public int getUnannotatedTag(int annotatedTag) {
        return annoToUnanno[annotatedTag];
    }

    /**
     * Updates the log-probabilities on the rules using those from the input DMV model.
     * @param dmv The input DMV model.
     */
    public void updateLogProbs(DmvModel dmv) {
        for (int c=0; c<numTags; c++) {
            this.root[c].setScore(dmv.root[c]);
            for (int p=0; p<numTags; p++) {
                for (int dir=0; dir<2; dir++) {
                    this.child[c][p][dir].setScore(dmv.child[c][p][dir]);
                    this.structural[c][p][dir].setScore(0.0);
                }
            }
            for (int dir=0; dir<2; dir++) {
                for (int val=0; val<2; val++) {
                    for (int sc=0; sc<2; sc++) {
                        this.decision[c][dir][val][sc].setScore(dmv.decision[c][dir][val][sc]);
                    }
                }
            }
        }
    }

    /**
     * Shorthand for String.format().
     */
    private String f(String format, Object... args) {
        return String.format(format, args);
    }

    private Rule getRootRule(int p) {
        return getBinaryRule(rootSymbolStr, 
                             f("L_{%d}", p), 
                             f("R_{%d}", p), 
                             0.0,
                             true, // The left and right heads will always be the same.
                             DmvRuleType.ROOT);
    }
    
    private Rule getChildRule(int c, int p, int dir) {
        if (dir == DmvModel.LEFT) {
            return getBinaryRule(f("L_{%d}^{1}", p), 
                                 f("L_{%d}", c), 
                                 f("M_{%d,%d*}", c, p), 
                                 0.0,
                                 false, // The left is the child.
                                 DmvRuleType.CHILD);
        } else {
            return getBinaryRule(f("R_{%d}^{1}", p),
                                 f("M_{%d*,%d}", p, c),
                                 f("R_{%d}", c),
                                 0.0,
                                 true, // The left is the head.
                                 DmvRuleType.CHILD);
        }
    }

    private Rule getStructuralRule(int c, int p, int dir) {
        if (dir == DmvModel.LEFT) { 
            return getBinaryRule(f("M_{%d,%d*}", c, p),
                                 f("R_{%d}", c),
                                 f("L_{%d}^{*}", p),
                                 0.0,
                                 false,
                                 DmvRuleType.STRUCTURAL); // The left is the child.
        } else {
            return getBinaryRule(f("M_{%d*,%d}", p, c),
                                 f("R_{%d}^{*}", p),
                                 f("L_{%d}", c),
                                 0.0,
                                 true,
                                 DmvRuleType.STRUCTURAL); // The left is the head.
        }
    }

    private Rule getDecisionRule(int c, int dir, int val, int sc) {
        String cap = (dir == DmvModel.LEFT) ? "L" : "R";
        String parent_ss = (val == 0) ? "" : "^{*}"; // parent superscript
        
        int parent = ntAlphabet.lookupIndex(new Tag(f("%s_{%d}%s", cap, c, parent_ss)));
        if (sc == DmvModel.END){
            int leftChild = (dir == DmvModel.LEFT) ? getAnnotatedTagLeft(c) : getAnnotatedTagRight(c);
            int rightChild = Rule.LEXICAL_RULE;
            double score = 0.0;
            return new DmvRule(parent, leftChild, rightChild, score, ntAlphabet, lexAlphabet, true, DmvRuleType.DECISION);
        } else {
            int leftChild = ntAlphabet.lookupIndex(new Tag(f("%s_{%d}^{1}", cap, c)));
            int rightChild = Rule.UNARY_RULE;
            double score = 0.0;
            return new DmvRule(parent, leftChild, rightChild, score, ntAlphabet, lexAlphabet, true, DmvRuleType.DECISION);
        }
    }
    
    private Rule getBinaryRule(String parentStr, String leftChildStr, String rightChildStr, double logProb, boolean isLeftHead, DmvRuleType type) {
        int parent = ntAlphabet.lookupIndex(new Tag(parentStr));
        int leftChild = ntAlphabet.lookupIndex(new Tag(leftChildStr));
        int rightChild = ntAlphabet.lookupIndex(new Tag(rightChildStr));
        return new DmvRule(parent, leftChild, rightChild, logProb, ntAlphabet, lexAlphabet, isLeftHead, type);
    }

    /**
     * Gets a representation of the sentence that can be parsed by this grammar.
     */
    public int[] getAnnotatedSent(Sentence sentence) {
        int[] labelIds = sentence.getLabelIds();
        int[] sent = new int[2 * labelIds.length];
        for (int i=0; i<labelIds.length; i++) {
            sent[2*i] = this.getAnnotatedTagLeft(labelIds[i]);
            sent[2*i+1] = this.getAnnotatedTagRight(labelIds[i]);
        }
        return sent;
    }

    private ArrayList<Rule> getAllRules() {
        ArrayList<Rule> allRules = new ArrayList<Rule>();
        Lists.addAll(allRules, root);
        Lists.addAll(allRules, child);
        Lists.addAll(allRules, decision);
        Lists.addAll(allRules, structural);
        return allRules;
    }
    
    public CnfGrammar getCnfGrammar() {
        return cnfGrammar;
    }
}
