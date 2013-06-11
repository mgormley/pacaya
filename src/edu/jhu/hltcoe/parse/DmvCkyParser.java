package edu.jhu.hltcoe.parse;


import java.util.Arrays;

import org.apache.log4j.Logger;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.data.WallDepTreeNode;
import edu.jhu.hltcoe.data.conll.ValidParentsSentence;
import edu.jhu.hltcoe.gridsearch.dmv.DmvObjective;
import edu.jhu.hltcoe.gridsearch.dmv.DmvObjective.DmvObjectivePrm;
import edu.jhu.hltcoe.gridsearch.dmv.IndexedDmvModel;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.parse.cky.CkyPcfgParser;
import edu.jhu.hltcoe.parse.cky.CkyPcfgParser.CkyPcfgParserPrm;
import edu.jhu.hltcoe.parse.cky.CkyPcfgParser.LoopOrder;
import edu.jhu.hltcoe.parse.cky.DmvCnfGrammar;
import edu.jhu.hltcoe.parse.cky.DmvRule;
import edu.jhu.hltcoe.parse.cky.DmvRule.DmvRuleType;
import edu.jhu.hltcoe.parse.cky.chart.Chart;
import edu.jhu.hltcoe.parse.cky.chart.Chart.BackPointer;
import edu.jhu.hltcoe.parse.cky.chart.Chart.ChartCellType;
import edu.jhu.hltcoe.parse.cky.chart.ChartCell;
import edu.jhu.hltcoe.train.DmvTrainCorpus;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Timer;

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
            this.dmvObj = new DmvObjective(prm.objPrm, new IndexedDmvModel(corpus));
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
            }
        }
        parseWeight = dmvObj.computeTrueObjective((DmvModel)model, treebank);
        return treebank;
    }

    @Override
    public DepTreebank getViterbiParse(SentenceCollection sentences, Model genericModel) {
        DmvTrainCorpus corpus = new DmvTrainCorpus(sentences);
        return getViterbiParse(corpus, genericModel);
    }

    public Pair<DepTree, Double> parse(Sentence sentence, DmvModel dmv) {
        timer.start();
        Pair<DepTree,Double> pair = parse2(sentence, dmv);
        timer.stop();
        log.debug("Average seconds per sentence: " + timer.avgSec());
        return pair;
    }
    
    private Pair<DepTree, Double> parse2(Sentence sentence, DmvModel dmv) {
        if (this.grammar == null || this.dmv != dmv) {
            this.dmv = dmv;
            this.grammar = new DmvCnfGrammar(dmv, sentence.getAlphabet());
        } else {
            this.grammar.updateLogProbs(dmv);
        }

        // Get an annotated version of the sentence (twice as long as the
        // original sentence), where each word has two copies of itself with a
        // Left and Right subscript.
        int[] annoSent = grammar.getAnnotatedSent(sentence);
        Sentence annoSentence = new Sentence(grammar.getCnfGrammar().getLexAlphabet(), annoSent);
        if (sentence instanceof ValidParentsSentence) {
            // Add the Valid Parent annotations to the annotated sentence.
            boolean[] vr = ((ValidParentsSentence) sentence).getValidRoot();
            boolean[][] vps = ((ValidParentsSentence) sentence).getValidParents();
            annoSentence = new ValidParentsSentence(grammar.getCnfGrammar().getLexAlphabet(), annoSentence, vr, vps);
        }
        Chart chart = parser.parseSentence(annoSentence, grammar.getCnfGrammar());
        
        Pair<int[], Double> pair= extractParentsFromChart(annoSent, chart, grammar);
        int[] parents = pair.get1();
        double logProb = pair.get2();
        
        return new Pair<DepTree, Double>(new DepTree(sentence, parents, true), logProb);
    }

    private static Pair<int[], Double> extractParentsFromChart(int[] sent, Chart chart, DmvCnfGrammar grammar) {
        // Create an empty parents array.
        int[] parents = new int[sent.length / 2];
        Arrays.fill(parents, -2);
        // Get the viterbi dependency tree.
        int rootSymbol = grammar.getCnfGrammar().getRootSymbol();
        getViterbiTree(0, sent.length, rootSymbol, chart, grammar, parents);
        // Get the score of the viterbi dependency tree.
        double rootScore = chart.getCell(0, sent.length).getScore(rootSymbol);
        return new Pair<int[], Double>(parents, rootScore);
    }
    
    /**
     * Gets the highest probability tree with the span (start, end) and the root symbol rootSymbol.
     * 
     * @param start The start of the span of the requested tree.
     * @param end The end of the span of the requested tree.
     * @param rootSymbol The symbol of the root of the requested tree.
     * @param chart The parse chart.
     * @param grammar The DMV grammar.
     * @param parents The output array specifying the parent of each word in the sentence.
     * @return The head of the span.
     */
    private static int getViterbiTree(int start, int end,
            int rootSymbol, Chart chart, DmvCnfGrammar grammar, int[] parents) {
        ChartCell cell = chart.getCell(start, end);
        BackPointer bp = cell.getBp(rootSymbol);
        
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
