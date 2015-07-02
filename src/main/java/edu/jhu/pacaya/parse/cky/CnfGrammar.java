package edu.jhu.pacaya.parse.cky;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.pacaya.parse.cky.CkyPcfgParser.LoopOrder;
import edu.jhu.prim.bimap.IntObjectBimap;

/**
 * Grammar in Chomsky normal form.
 * 
 * @author mgormley
 *
 */
public class CnfGrammar {

    public static final Logger log = LoggerFactory.getLogger(CnfGrammar.class);
    
    private int rootSymbol;
    private ArrayList<Rule> allRules;

    private ArrayList<Rule>[] lexRulesForChild;
    private ArrayList<Rule>[] unaryRulesForChild;
    private ArrayList<Rule>[] unaryRulesForParent; // Used by the outside algorithm.
    
    // Arrays corresponding to binaryRulesForChildren, binaryRulesWithLeftChild, binaryRulesWithRightChild.
    private Rule[][][] brfc;
    private Rule[][] brwlc;
    private Rule[][] brwrc;
    
    private IntObjectBimap<String> lexAlphabet;
    private IntObjectBimap<String> ntAlphabet;

    private LoopOrder loopOrder;
    
    @SuppressWarnings("unchecked")
    public CnfGrammar(ArrayList<Rule> allRules, int rootSymbol, IntObjectBimap<String> lexAlphabet, IntObjectBimap<String> ntAlphabet, LoopOrder loopOrder) {
        this.rootSymbol = rootSymbol;
        this.lexAlphabet = lexAlphabet;
        this.ntAlphabet = ntAlphabet;
        this.allRules = allRules;
        this.loopOrder = loopOrder;
        
        lexRulesForChild = new ArrayList[lexAlphabet.size()];
        unaryRulesForChild = new ArrayList[ntAlphabet.size()];
        unaryRulesForParent = new ArrayList[ntAlphabet.size()];

        log.info("Num lexical types: " + lexAlphabet.size());
        log.info("Num nonterminals: " + ntAlphabet.size());
        
        fill(lexRulesForChild);
        fill(unaryRulesForChild);
        fill(unaryRulesForParent);

        int numLexicalRules = 0;
        int numUnaryRules = 0;
        for (Rule r : allRules) {
            if (r.isLexical()) {
                lexRulesForChild[r.getLeftChild()].add(r);
                numLexicalRules++;
            } else if (r.isUnary()) {
                unaryRulesForChild[r.getLeftChild()].add(r);
                unaryRulesForParent[r.getParent()].add(r);
                numUnaryRules++;
            }
        }
        int numBinaryRules = allRules.size() - numLexicalRules - numUnaryRules;
        log.info(String.format("Rules: total=%d binary=%d unary=%d lexical=%d", allRules.size(), numBinaryRules, numUnaryRules, numLexicalRules));
        
        if (loopOrder == LoopOrder.CARTESIAN_PRODUCT) {
            List<Rule>[][] binaryRulesForChildren = new List[ntAlphabet.size()][ntAlphabet.size()];
            for (int i = 0; i < binaryRulesForChildren.length; i++) {
                for (int j = 0; j < binaryRulesForChildren[i].length; j++) {
                    binaryRulesForChildren[i][j] = Collections.emptyList();
                }
            }        
            for (Rule r : allRules) {
                if (r.isBinary()) {
                    if (binaryRulesForChildren[r.getLeftChild()][r.getRightChild()].size() == 0) {
                        binaryRulesForChildren[r.getLeftChild()][r.getRightChild()] = new ArrayList<Rule>(0);
                    }
                    binaryRulesForChildren[r.getLeftChild()][r.getRightChild()].add(r);
                }
            }
            brfc = getAsArrays(binaryRulesForChildren);
        } else if (loopOrder == LoopOrder.LEFT_CHILD) {
            ArrayList<Rule>[] binaryRulesWithLeftChild = new ArrayList[ntAlphabet.size()];
            fill(binaryRulesWithLeftChild);
            for (Rule r : allRules) {
                if (r.isBinary()) {
                    binaryRulesWithLeftChild[r.getLeftChild()].add(r);
                }
            }
            brwlc = getAsArrays(binaryRulesWithLeftChild);
        } else if (loopOrder == LoopOrder.RIGHT_CHILD) {
            ArrayList<Rule>[] binaryRulesWithRightChild = new ArrayList[ntAlphabet.size()];
            fill(binaryRulesWithRightChild);
            for (Rule r : allRules) {
                if (r.isBinary()) {
                    binaryRulesWithRightChild[r.getRightChild()].add(r);
                }
            }
            brwrc = getAsArrays(binaryRulesWithRightChild);
        }
    }
    
    private static Rule[][] getAsArrays(ArrayList<Rule>[] a) {
        Rule[][] b = new Rule[a.length][];
        for (int i=0; i<a.length; i++) {
            b[i] = a[i].toArray(new Rule[]{});
        }
        return b;
    }

    private static Rule[][][] getAsArrays(List<Rule>[][] a) {
        Rule[][][] b = new Rule[a.length][a[0].length][];
        for (int i=0; i<a.length; i++) {
            for (int j=0; j<a[i].length; j++) {
                b[i][j] = a[i][j].toArray(new Rule[]{});
            }
        }
        return b;
    }

    private static void fill(ArrayList<Rule>[] array) {
        for (int i=0; i<array.length; i++) {
            array[i] = new ArrayList<Rule>();
        }
    }

    /** Gets all lexical rules with the given child. */
    public final ArrayList<Rule> getLexicalRulesWithChild(int child) {
        return lexRulesForChild[child];
    }

    public final ArrayList<Rule> getUnaryRulesWithChild(int child) {
        return unaryRulesForChild[child];
    }

    public ArrayList<Rule> getUnaryRulesWithParent(int parent) {
        return unaryRulesForParent[parent];
    }

    public final Rule[] getBinaryRulesWithChildren(int leftChildNt, int rightChildNt) {
        return brfc[leftChildNt][rightChildNt];
    }

    public final Rule[] getBinaryRulesWithLeftChild(int leftChildNt) {
        return brwlc[leftChildNt];
    }

    public final Rule[] getBinaryRulesWithRightChild(int rightChildNt) {
        return brwrc[rightChildNt];
    }
    
    public IntObjectBimap<String> getLexAlphabet() {
        return lexAlphabet;
    }

    public IntObjectBimap<String> getNtAlphabet() {
        return ntAlphabet;
    }

    public int getNumLexicalTypes() {
        return lexAlphabet.size();
    }
    
    public int getNumNonTerminals() {
        return ntAlphabet.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CnfGrammar [rootSymbol=");
        sb.append(ntAlphabet.lookupObject(rootSymbol));
        sb.append(", allRules=\n");
        for (int i=0; i<allRules.size(); i++) {
            sb.append("\t");
            sb.append(allRules.get(i));
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    public int getRootSymbol() {
        return rootSymbol;
    }

    public boolean isUnknownWord(String word) {
        if (lexAlphabet.lookupIndex(word) < 0) {
            return true;
        }
        return false;
    }
    
    public LoopOrder getLoopOrder() {
        return loopOrder;
    }
    
}
