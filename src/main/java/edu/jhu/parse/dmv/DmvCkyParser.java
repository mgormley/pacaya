package edu.jhu.parse.dmv;


import java.util.Arrays;

import org.apache.log4j.Logger;

import edu.jhu.data.DepTree;
import edu.jhu.data.DepTreebank;
import edu.jhu.data.Sentence;
import edu.jhu.data.SentenceCollection;
import edu.jhu.data.WallDepTreeNode;
import edu.jhu.data.conll.ValidParentsSentence;
import edu.jhu.globalopt.dmv.DmvObjective;
import edu.jhu.globalopt.dmv.IndexedDmvModel;
import edu.jhu.globalopt.dmv.DmvObjective.DmvObjectivePrm;
import edu.jhu.model.Model;
import edu.jhu.model.dmv.DmvModel;
import edu.jhu.parse.cky.CkyPcfgParser;
import edu.jhu.parse.cky.CkyPcfgParser.CkyPcfgParserPrm;
import edu.jhu.parse.cky.CkyPcfgParser.LoopOrder;
import edu.jhu.parse.cky.chart.Chart;
import edu.jhu.parse.cky.chart.Chart.BackPointer;
import edu.jhu.parse.cky.chart.Chart.ChartCellType;
import edu.jhu.parse.cky.chart.ChartCell;
import edu.jhu.parse.dep.DepParser;
import edu.jhu.parse.dmv.DmvRule.DmvRuleType;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.train.dmv.DmvTrainCorpus;
import edu.jhu.util.Timer;

/**
 * CKY parser for the Dependency Model with Valence (DMV).
 * 
 * This wraps a CKY PCFG parser, and creates a grammar based on the provided DMV
 * model.
 * 
 * @author mgormley
 * 
 */
public class DmvCkyParser implements DepParser {

    public static class DmvCkyParserPrm {
        public DmvObjectivePrm objPrm = new DmvObjectivePrm();
        public CkyPcfgParserPrm ckyPrm = new CkyPcfgParserPrm();
        public DmvCkyParserPrm() {
            ckyPrm.loopOrder = LoopOrder.LEFT_CHILD;
            ckyPrm.cellType = ChartCellType.FULL_BREAK_TIES;
            ckyPrm.cacheChart = true;
        }
    }
    
    private static final Logger log = Logger.getLogger(DmvCkyParser.class);

    private double parseWeight;
    private DmvObjective dmvObj;
    private SentenceCollection sents;
    private Timer timer;
    
    private DmvCkyParserPrm prm;
    
    // Cached for efficiency.
    private DmvCnfGrammar grammar;
    private DmvModel dmv;
    private CkyPcfgParser parser;
    
    public DmvCkyParser() {
        this(new DmvCkyParserPrm());
    }
    
    public DmvCkyParser(DmvCkyParserPrm prm) {
        if ((prm.ckyPrm.cellType == ChartCellType.CONSTRAINED_FULL || 
                prm.ckyPrm.cellType == ChartCellType.CONSTRAINED_SINGLE)&& prm.ckyPrm.constraint == null) {
            // Set the default constraint if it was not already.
            prm.ckyPrm.constraint = new DmvChartCellConstraint();
        }
        this.prm = prm;
        this.parser = new CkyPcfgParser(prm.ckyPrm);
        this.timer = new Timer();
    }

    @Override
    public double getLastParseWeight() {
        return parseWeight;
    }
    
    public DepTreebank getViterbiParse(DmvTrainCorpus corpus, Model genericModel) {
        // Lazily construct the objective.
        if (dmvObj == null || this.sents != corpus.getSentences()) {
            this.dmvObj = new DmvObjective(prm.objPrm, new IndexedDmvModel(corpus, true));
            this.sents = corpus.getSentences();
        }
        DmvModel model = (DmvModel) genericModel;
        DepTreebank treebank = new DepTreebank(model.getTagAlphabet());

        parseWeight = 0.0;
     
        for (int s = 0; s < corpus.size(); s++) {
            if (corpus.isLabeled(s)) {
                treebank.add(corpus.getTree(s));
            } else {
                Pair<DepTree, Double> pair = parse(corpus.getSentence(s), model);
                treebank.add(pair.get1());
                log.trace("Average seconds per sentence: " + timer.avgSec());
            }
        }
        log.debug("Average seconds per sentence: " + timer.avgSec());

        parseWeight = dmvObj.computeTrueObjective((DmvModel)model, treebank);
        return treebank;
    }

    @Override
    public DepTreebank getViterbiParse(SentenceCollection sentences, Model genericModel) {
        DmvTrainCorpus corpus = new DmvTrainCorpus(sentences);
        return getViterbiParse(corpus, genericModel);
    }

    /**
     * Gets the maximum-likelihood dependency tree and its log-likelihood.
     */
    public Pair<DepTree, Double> parse(Sentence sentence, DmvModel dmv) {
        Pair<int[], Double> pair = parseForParents(sentence, dmv);        
        int[] parents = pair.get1();
        double logProb = pair.get2();        
        
        if (parents[0] == -2) {
            log.warn("Unable to parse sentence: " + sentence);
        }
        
        timer.start();
        Pair<DepTree,Double> ret = new Pair<DepTree, Double>(new DepTree(sentence, parents, true), logProb);
        timer.stop();        
        return ret;
    }

    public Pair<int[], Double> parseForParents(Sentence sentence, DmvModel dmv) {
        timer.start();
        DmvCnfGrammar grammar = getDmvGrammar(sentence, dmv);
        Sentence annoSentence = getAnnotatedSentence(sentence, grammar);
        Chart chart = parser.parseSentence(annoSentence, grammar.getCnfGrammar());        
        Pair<int[], Double> pair= extractParentsFromChart(chart, grammar);
        timer.stop();
        return pair;
    }

    /**
     * Gets a CNF grammar for this DMV. If possible the resulting grammar will
     * be an cached version of the grammar with the parameters values updated.
     * 
     * @param sentence The original sentence.
     * @param dmv The DMV model.
     * @return A CNF grammar representing the model.
     */
    private DmvCnfGrammar getDmvGrammar(Sentence sentence, DmvModel dmv) {
        if (this.grammar == null || this.dmv != dmv) {
            this.dmv = dmv;
            this.grammar = new DmvCnfGrammar(dmv, sentence.getAlphabet(), prm.ckyPrm.loopOrder);
        } else {
            this.grammar.updateLogProbs(dmv);
        }
        return this.grammar;
    }

    /**
     * Get an annotated version of the sentence (twice as long as the
     * original sentence), where each word has two copies of itself with a
     * Left and Right subscript.
     * 
     * @param sentence The original sentence.
     * @param grammar A CNF grammar representing a DMV.
     * @return An annotated version of the sentence.
     */
    private Sentence getAnnotatedSentence(Sentence sentence,
            DmvCnfGrammar grammar) {
        int[] annoSent = grammar.getAnnotatedSent(sentence);
        Sentence annoSentence = new Sentence(grammar.getCnfGrammar().getLexAlphabet(), annoSent);
        if (sentence instanceof ValidParentsSentence) {
            // Add the Valid Parent annotations to the annotated sentence.
            boolean[] vr = ((ValidParentsSentence) sentence).getValidRoot();
            boolean[][] vps = ((ValidParentsSentence) sentence).getValidParents();
            annoSentence = new ValidParentsSentence(grammar.getCnfGrammar().getLexAlphabet(), annoSentence, vr, vps);
        }
        return annoSentence;
    }

    /**
     * Gets the maximum-likelihood dependency tree and its log-likelihood.
     * 
     * @param chart The parse chart for the annotated sentence.
     * @param grammar The CNF grammar for the DMV.
     * @return A pair containing a parents array representing the dependency tree, and the log-likelihood of the parse.
     */
    private static Pair<int[], Double> extractParentsFromChart(Chart chart, DmvCnfGrammar grammar) {
        final int annoSentSize = chart.getSentenceSize();
        // Create an empty parents array.
        int[] parents = new int[annoSentSize / 2];
        Arrays.fill(parents, -2);
        // Get the viterbi dependency tree.
        int rootSymbol = grammar.getCnfGrammar().getRootSymbol();
        getViterbiTree(0, annoSentSize, rootSymbol, chart, grammar, parents);
        // Get the score of the viterbi dependency tree.
        double rootScore = chart.getCell(0, annoSentSize).getScore(rootSymbol);
        return new Pair<int[], Double>(parents, rootScore);
    }
    
    /**
     * Fills in the parents array with the highest probability subtree with the
     * span (start, end) and the root symbol rootSymbol.
     * 
     * If there was no available parse (e.g. due to constraints), a warning is
     * printed and the parents array is filled with -1.
     * 
     * @param start The start of the span of the requested tree.
     * @param end The end of the span of the requested tree.
     * @param rootSymbol The symbol of the root of the requested tree.
     * @param chart The parse chart.
     * @param grammar The DMV grammar.
     * @param parents The output array specifying the parent of each word in the
     *            sentence.
     * @return The head of the span.
     */
    private static int getViterbiTree(int start, int end,
            int rootSymbol, Chart chart, DmvCnfGrammar grammar, int[] parents) {
        ChartCell cell = chart.getCell(start, end);
        BackPointer bp = cell.getBp(rootSymbol);
        
        if (start == 0 && end == parents.length * 2 && bp == null) {
            // This sentence has no parse.
            return -2;
        }
        // The backpointer will never be null because we return on lexical rules.
        assert(bp != null);
        
        if (bp.r.isLexical()) {
            // Leaf node. Must map to the *position* of the corresponding node.
            return start / 2;
        } else if (bp.r.isUnary()) {
            // Return the head of the left child.
            return getViterbiTree(start, bp.mid, bp.r.getLeftChild(), chart, grammar, parents);
        } else {
            // Get the left and right heads.
            int leftHead = getViterbiTree(start, bp.mid, bp.r.getLeftChild(), chart, grammar, parents);
            int rightHead = getViterbiTree(bp.mid, end, bp.r.getRightChild(), chart, grammar, parents);
            
            //TODO: Remove: 
            //log.debug(String.format("lh: %d rh: %d s: %d mid: %d e: %d %s", leftHead, rightHead, start/2, bp.mid/2, end/2, bp.r.getParentLabel().getLabel()));
            
            // Then determine whether the rule defines a left or right dependency.
            DmvRule dmvRule = (DmvRule)bp.r;
            boolean isLeftHead = dmvRule.isLeftHead();
            int head = isLeftHead ? leftHead : rightHead;
            int child = isLeftHead ? rightHead : leftHead;
            // Set that parent in the parents array.
            if (dmvRule.getType() == DmvRuleType.STRUCTURAL) {
                assert parents[child] == -2;
                parents[child] = head;
            } else if (dmvRule.getType() == DmvRuleType.ROOT) {
                // Set the parent of the head of the sentence to be the wall.
                // 
                // This is only possible because we keep track of the heads recursively.
                parents[head] = WallDepTreeNode.WALL_POSITION;
            }
            return head;
        }
    }

}
