package edu.jhu.hltcoe.parse.cky;

import edu.jhu.hltcoe.util.Pair;


/**
 * Parsing chart that stores every cell explicitly.
 * 
 * @author mgormley
 * 
 */
public class Chart {
    
    public static class BackPointer {
        private Rule r;
        private int mid;

        public BackPointer(Rule r, int mid) {
            this.r = r;
            this.mid = mid;
        }

        /**
         * Get the midpoint of the rule application.
         */
        public int getMid() {
            return mid;
        }
        
        /**
         * Get the rule.
         */
        public Rule getRule() {
            return r;
        }
    }

    public enum ChartCellType { FULL, HASH };

    private final ChartCellType cellType;

    private final ChartCell[][] chart;
    private final CnfGrammar grammar;
    private final int[] sent;

    public Chart(int[] sent, CnfGrammar grammar, ChartCellType cellType) {
        this.cellType = cellType;
        this.sent = sent;
        this.grammar = grammar;
        chart = new ChartCell[sent.length][sent.length+1];
        for (int i = 0; i < chart.length; i++) {
            for (int j = i+1; j < chart[i].length; j++) {
                switch(this.cellType) {
                case HASH:
                    chart[i][j] = new SingleHashChartCell(grammar);
                    break;
                case FULL:
                    chart[i][j] = new FullChartCell(grammar);
                    break;
                default:
                    throw new RuntimeException("not implemented for " + cellType);
                }
                
            }
        }
    }

    public Pair<BinaryTree,Double> getViterbiParse() {
        BinaryTree root = getViterbiTree(0, sent.length, grammar.getRootSymbol());
        double rootScore = chart[0][sent.length].getMaxScore(grammar.getRootSymbol());
        return new Pair<BinaryTree, Double>(root, rootScore);
    }
    
    /**
     * Gets the highest probability tree with the span (start, end) and the root symbol rootSymbol.
     * 
     * @param start The start of the span of the requested tree.
     * @param end The end of the span of the requested tree.
     * @param rootSymbol The symbol of the root of the requested tree.
     * @return 
     */
    private BinaryTree getViterbiTree(int start, int end, int rootSymbol) {
        ChartCell cell = chart[start][end];
        BackPointer bp = cell.getBp(rootSymbol);
        if (bp == null) {
            return null;
        }
        
        BinaryTree leftChild;
        BinaryTree rightChild;
        if (bp.r.isLexical()) {
            leftChild = new BinaryTree(bp.r.getLeftChild(), start, end, null, null, true, grammar.getLexAlphabet());
            rightChild = null;
        } else if (bp.r.isUnary()) {
            leftChild = getViterbiTree(start, bp.mid, bp.r.getLeftChild());
            rightChild = null;
        } else {
            leftChild = getViterbiTree(start, bp.mid, bp.r.getLeftChild());
            rightChild = getViterbiTree(bp.mid, end, bp.r.getRightChild());
        }
        
        return new BinaryTree(rootSymbol, start, end, leftChild, rightChild, false, grammar.getNtAlphabet());
    }

    public ChartCell getCell(int start, int end) {
        return chart[start][end];
    }
}
