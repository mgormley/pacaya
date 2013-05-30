package edu.jhu.hltcoe.parse.cky;

import java.util.Arrays;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.WallDepTreeNode;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.parse.cky.Chart.ChartCellType;
import edu.jhu.hltcoe.parse.cky.CkyPcfgParser.LoopOrder;
import edu.jhu.hltcoe.util.Pair;

public class DmvCkyPcfgParser {

    private static final Logger log = Logger.getLogger(DmvCkyPcfgParser.class);
    private DmvCnfGrammar grammar;
    private DmvModel dmv;
    
    public Pair<DepTree, Double> parse(Sentence sentence, DmvModel dmv) {
        if (this.grammar == null || this.dmv != dmv) {
            this.dmv = dmv;
            this.grammar = new DmvCnfGrammar(dmv, sentence.getAlphabet());
        }
        
        CkyPcfgParser parser = new CkyPcfgParser(LoopOrder.LEFT_CHILD, ChartCellType.FULL);
        
        int[] sent = grammar.getSent(sentence);
        Chart chart = parser.parseSentence(sent, grammar.getCnfGrammar());
        
        Pair<BinaryTree, Double> pair = chart.getViterbiParse();        
        BinaryTree tree = pair.get1();
        double logProb = pair.get2();
        log.debug("DMV Tree:\n" + tree.getAsPennTreebankString());
        int[] parents = extractParents(tree, grammar);
        log.debug("parents: " + Arrays.toString(parents));
        
        return new Pair<DepTree, Double>(new DepTree(sentence, parents, true), logProb);
    }

    private int[] extractParents(BinaryTree tree, DmvCnfGrammar grammar) {
        int[] parents = new int[tree.getEnd() / 2];
        Arrays.fill(parents, -2);
        int head = extractParents(tree, parents, grammar);
        parents[head] = WallDepTreeNode.WALL_POSITION;
        return parents;
    }

    private int extractParents(BinaryTree tree, int[] parents, DmvCnfGrammar grammar) {
        if (tree.isLeaf()) {
            // Leaf node. Must map the left/right annotated tag back to the standard tag.
            //return grammar.getUnannotatedTag(tree.getSymbol());
            return tree.getStart() / 2;
        }

        int leftHead = extractParents(tree.getLeftChild(), parents, grammar);
        BinaryTree rightChild = tree.getRightChild();
        if (rightChild == null) {
            // Tree has only left child.
            return leftHead;
        }
        
        // Binary rule.        
        int rightHead = extractParents(rightChild, parents, grammar);
        String symbol = tree.getSymbolStr();

        if (symbol.charAt(0) == 'S') { // TODO: this should refer to the root symbol string.
            // This is not a structural rule, so heads are identical.
            assert leftHead == rightHead;
            return leftHead;
        } else {
            // This is a structural rule, so determine which is the head.
            boolean isLeftHead;
            if (symbol.charAt(0) == 'M') {
                isLeftHead = !(symbol.charAt(symbol.length()-2) == '*') ;
            } else {
                // If it starts with L than C is a left child of H.
                isLeftHead = !(symbol.charAt(0) == 'L');
            }
            log.debug("Structural symbol: " + symbol + " Is left head:" + isLeftHead);
            int head = isLeftHead ? leftHead : rightHead;
            int child = isLeftHead ? rightHead : leftHead;
            // Set that parent in the parents array.
            parents[child] = head;
            return head;
        }        
    }
    
}
