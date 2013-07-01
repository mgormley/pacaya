package edu.jhu.parse.cky;

import java.util.ArrayList;
import java.util.Collections;

import org.apache.log4j.Logger;

import edu.jhu.PipelineRunner;
import edu.jhu.data.Label;
import edu.jhu.data.Word;
import edu.jhu.util.Alphabet;

/**
 * Grammar in Chomsky normal form.
 * 
 * @author mgormley
 *
 */
public class CnfGrammar {
    
    private static final Logger log = Logger.getLogger(CnfGrammar.class);
    
    private int rootSymbol;
    private ArrayList<Rule> allRules;

    private ArrayList<Rule>[] lexRulesForChild;
    private ArrayList<Rule>[] unaryRulesForChild;
    private ArrayList<Rule>[] unaryRulesForParent; // Used by the outside algorithm.
    private ArrayList<Rule>[][] binaryRulesForChildren;
    private ArrayList<Rule>[] binaryRulesWithLeftChild;
    private ArrayList<Rule>[] binaryRulesWithRightChild;
    
    // Arrays corresponding to binaryRulesForChildren, binaryRulesWithLeftChild, binaryRulesWithRightChild.
    private Rule[][][] brfc;
    private Rule[][] brwlc;
    private Rule[][] brwrc;
    
    private Alphabet<Label> lexAlphabet;
    private Alphabet<Label> ntAlphabet;
    
    @SuppressWarnings("unchecked")
    public CnfGrammar(ArrayList<Rule> allRules, int rootSymbol, Alphabet<Label> lexAlphabet, Alphabet<Label> ntAlphabet) {
        this.rootSymbol = rootSymbol;
        this.lexAlphabet = lexAlphabet;
        this.ntAlphabet = ntAlphabet;
        this.allRules = allRules;
        lexRulesForChild = new ArrayList[lexAlphabet.size()];
        unaryRulesForChild = new ArrayList[ntAlphabet.size()];
        unaryRulesForParent = new ArrayList[ntAlphabet.size()];
        binaryRulesForChildren = new ArrayList[ntAlphabet.size()][ntAlphabet.size()];
        binaryRulesWithLeftChild = new ArrayList[ntAlphabet.size()];
        binaryRulesWithRightChild = new ArrayList[ntAlphabet.size()];

        log.info("Num lexical types: " + lexAlphabet.size());
        log.info("Num nonterminals: " + ntAlphabet.size());
        
        fill(lexRulesForChild);
        fill(unaryRulesForChild);
        fill(unaryRulesForParent);
        for (int i=0; i<binaryRulesForChildren.length; i++) {
            for (int j=0; j<binaryRulesForChildren[i].length; j++) {
                binaryRulesForChildren[i][j] = new ArrayList<Rule>(0);
            }
        }
        fill(binaryRulesWithLeftChild);
        fill(binaryRulesWithRightChild);
        
        for (Rule r : allRules) {
            if (r.isLexical()) {
                lexRulesForChild[r.getLeftChild()].add(r);
            } else if (r.isUnary()) {
                unaryRulesForChild[r.getLeftChild()].add(r);
                unaryRulesForParent[r.getParent()].add(r);
            } else {
                binaryRulesForChildren[r.getLeftChild()][r.getRightChild()]
                        .add(r);
                binaryRulesWithLeftChild[r.getLeftChild()].add(r);
                binaryRulesWithRightChild[r.getRightChild()].add(r);
            }
        }
        
        brfc = getAsArrays(binaryRulesForChildren);
        brwlc = getAsArrays(binaryRulesWithLeftChild);
        brwrc = getAsArrays(binaryRulesWithRightChild);
    }
    
    private static Rule[][] getAsArrays(ArrayList<Rule>[] a) {
        Rule[][] b = new Rule[a.length][];
        for (int i=0; i<a.length; i++) {
            b[i] = a[i].toArray(new Rule[]{});
        }
        return b;
    }

    private static Rule[][][] getAsArrays(ArrayList<Rule>[][] a) {
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
    
    public Alphabet<Label> getLexAlphabet() {
        return lexAlphabet;
    }

    public Alphabet<Label> getNtAlphabet() {
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

    public boolean isUnknownWord(Word word) {
        if (lexAlphabet.lookupIndex(word) < 0) {
            return true;
        }
        return false;
    }
    
}
