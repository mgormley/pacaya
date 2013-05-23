package edu.jhu.hltcoe.parse.cky;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import edu.jhu.hltcoe.util.Alphabet;
import edu.jhu.hltcoe.util.Utilities;

/**
 * Grammar in Chomsky normal form.
 * 
 * @author mgormley
 *
 */
public class CnfGrammar {

    private int rootSymbol;
    private ArrayList<Rule> allRules;

    private ArrayList<Rule>[] lexRulesForChild;
    private ArrayList<Rule>[] unaryRulesForChild;
    private ArrayList<Rule>[][] binaryRulesForChildren;
    private ArrayList<Rule>[] binaryRulesWithLeftChild;
    private ArrayList<Rule>[] binaryRulesWithRightChild;

    private Alphabet<String> lexAlphabet;
    private Alphabet<String> ntAlphabet;
    
    @SuppressWarnings("unchecked")
    public CnfGrammar(ArrayList<Rule> allRules, int rootSymbol, Alphabet<String> lexAlphabet, Alphabet<String> ntAlphabet) {
        this.rootSymbol = rootSymbol;
        this.lexAlphabet = lexAlphabet;
        this.ntAlphabet = ntAlphabet;
        this.allRules = allRules;
        lexRulesForChild = new ArrayList[lexAlphabet.size()];
        unaryRulesForChild = new ArrayList[ntAlphabet.size()];
        binaryRulesForChildren = new ArrayList[ntAlphabet.size()][ntAlphabet.size()];
        binaryRulesWithLeftChild = new ArrayList[ntAlphabet.size()];
        binaryRulesWithRightChild = new ArrayList[ntAlphabet.size()];

        fill(lexRulesForChild);
        fill(unaryRulesForChild);
        for (int i=0; i<binaryRulesForChildren.length; i++) {
            for (int j=0; j<binaryRulesForChildren[i].length; j++) {
                binaryRulesForChildren[i][j] = new ArrayList<Rule>();
            }
        }
        fill(binaryRulesWithLeftChild);
        fill(binaryRulesWithRightChild);
        
        for (Rule r : allRules) {
            if (r.isLexical()) {
                lexRulesForChild[r.getLeftChild()].add(r);
            } else if (r.isUnary()) {
                unaryRulesForChild[r.getLeftChild()].add(r);
            } else {
                binaryRulesForChildren[r.getLeftChild()][r.getRightChild()]
                        .add(r);
                binaryRulesWithLeftChild[r.getLeftChild()].add(r);
                binaryRulesWithRightChild[r.getRightChild()].add(r);
            }
        }
    }

    private static void fill(ArrayList<Rule>[] array) {
        for (int i=0; i<array.length; i++) {
            array[i] = new ArrayList<Rule>();
        }
    }

    public ArrayList<Rule> getLexicalRulesWithChild(int child) {
        return lexRulesForChild[child];
    }

    public ArrayList<Rule> getUnaryRulesWithChild(int child) {
        return unaryRulesForChild[child];
    }

    public ArrayList<Rule> getBinaryRulesWithChildren(int leftChildNt, int rightChildNt) {
        return binaryRulesForChildren[leftChildNt][rightChildNt];
    }

    public ArrayList<Rule> getBinaryRulesWithLeftChild(int leftChildNt) {
        return binaryRulesWithLeftChild[leftChildNt];
    }
    
    public ArrayList<Rule> getBinaryRulesWithRightChild(int rightChildNt) {
        return binaryRulesWithRightChild[rightChildNt];
    }

    public Alphabet<String> getLexAlphabet() {
        return lexAlphabet;
    }

    public Alphabet<String> getNtAlphabet() {
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
    
}
