package edu.jhu.hltcoe.parse.cky;

import edu.jhu.hltcoe.util.Pair;
import gnu.trove.TIntArrayList;

import java.util.Arrays;

/**
 * Parsing chart that stores every cell explicitly. This is suitable for
 * grammars with a very small number of non-terminals (e.g. the DMV).
 * 
 * @author mgormley
 * 
 */
public class Chart {

    private static class BackPointer {
        private Rule r;
        private int mid;

        public BackPointer(Rule r, int mid) {
            this.r = r;
            this.mid = mid;
        }

    }

    private static class ChartCell {
        private double[] maxScores;
        // private double[] insideScores;
        // private double[] outsideScores;
        private BackPointer[] bps;
        private TIntArrayList nts;

        public ChartCell(CnfGrammar grammar) {
            maxScores = new double[grammar.getNumNonTerminals()];
            // insideScores = new double[grammar.getNumNonTerminals()];
            // outsideScores = new double[grammar.getNumNonTerminals()];
            bps = new BackPointer[grammar.getNumNonTerminals()];
            nts = new TIntArrayList();

            // Initialize scores to negative infinity.
            Arrays.fill(maxScores, Double.NEGATIVE_INFINITY);
            // Arrays.fill(insideScores, Double.NEGATIVE_INFINITY);
            // Arrays.fill(outsideScores, Double.NEGATIVE_INFINITY);
        }
    }

    private ChartCell[][] chart;
    private CnfGrammar grammar;
    private int[] sent;

    public Chart(int[] sent, CnfGrammar grammar) {
        this.sent = sent;
        this.grammar = grammar;
        chart = new ChartCell[sent.length][sent.length+1];
        for (int i = 0; i < chart.length; i++) {
            for (int j = i+1; j < chart[i].length; j++) {
                chart[i][j] = new ChartCell(grammar);
            }
        }
    }

    public int[] getNonTerminals(int start, int end) {
        return chart[start][end].nts.toNativeArray();
    }

    public double getMaxScore(int start, int end, int nt) {
        return chart[start][end].maxScores[nt];
    }

    public void updateCell(int start, int end, Rule r, double score) {
        assert !r.isBinary();
        updateCell(start, end, end, r, score);
    }

    public void updateCell(int start, int mid, int end, Rule r, double score) {
        ChartCell cell = chart[start][end];
        int nt = r.getParent();
        if (cell.bps[nt] == null) {
            // If the non-terminal hasn't been added yet, include it in the set of non terminals.
            cell.nts.add(nt);
        }
        if (score > cell.maxScores[nt]) {
            cell.maxScores[nt] = score;
            cell.bps[nt] = new BackPointer(r, mid);
        }
    }

    public Pair<BinaryTreeNode,Double> getViterbiParse() {
        BinaryTreeNode root = getViterbiTree(0, sent.length, grammar.getRootSymbol());
        double rootScore = chart[0][sent.length].maxScores[grammar.getRootSymbol()];
        return new Pair<BinaryTreeNode, Double>(root, rootScore);
    }
    
    /**
     * Gets the highest probability tree with the span (start, end) and the root symbol rootSymbol.
     * 
     * @param start The start of the span of the requested tree.
     * @param end The end of the span of the requested tree.
     * @param rootSymbol The symbol of the root of the requested tree.
     * @return 
     */
    private BinaryTreeNode getViterbiTree(int start, int end, int rootSymbol) {
        ChartCell cell = chart[start][end];
        BackPointer bp = cell.bps[rootSymbol];
        if (bp == null) {
            return null;
        }
        
        BinaryTreeNode leftChild;
        BinaryTreeNode rightChild;
        if (bp.r.isLexical()) {
            leftChild = new BinaryTreeNode(bp.r.getLeftChild(), start, end, null, null, true, grammar.getLexAlphabet());
            rightChild = null;
        } else if (bp.r.isUnary()) {
            leftChild = getViterbiTree(start, bp.mid, bp.r.getLeftChild());
            rightChild = null;
        } else {
            leftChild = getViterbiTree(start, bp.mid, bp.r.getLeftChild());
            rightChild = getViterbiTree(bp.mid, end, bp.r.getRightChild());
        }
        
        return new BinaryTreeNode(rootSymbol, start, end, leftChild, rightChild, false, grammar.getNtAlphabet());
    }
}
