package edu.jhu.parse.cky;

import java.util.Arrays;
import java.util.List;

import edu.jhu.data.Sentence;
import edu.jhu.gm.model.ConstituencyTreeFactor;
import edu.jhu.gm.model.ConstituencyTreeFactor.SpanVar;
import edu.jhu.gm.model.DenseFactor;
import edu.jhu.parse.cky.CkyPcfgParser.LoopOrder;
import edu.jhu.parse.cky.chart.Chart;
import edu.jhu.parse.cky.chart.Chart.ChartCellType;
import edu.jhu.parse.cky.chart.Chart.ParseType;
import edu.jhu.util.Alphabet;

/**
 * This class takes the bracketing grammar defined in {@link ConstituencyTreeFactor}
 * and parses.
 * 
 * @author travis
 */
public class ConstituencyTreeFactorParser {

    // TODO this can be faster if reset doesn't always allocate
    static class BeliefScorer implements Scorer {
        private int n;
        private double[][] logProbs;
        public void reset(int n) {
            this.n = n;
            this.logProbs = new double[n][n+1];
            for (int i = 0; i < n; i++)
                Arrays.fill(logProbs[i], Double.NEGATIVE_INFINITY);
        }
        public void set(int start, int end, double logProb) {
            assert start < end && end <= n;
            this.logProbs[start][end] = logProb;
        }
        @Override
        public double score(Rule r, int start, int mid, int end) {
            assert start < end && end <= n;
            if (start+1 == end) {
                return 0d;
            } else {
                return logProbs[start][end];
            }
        }
    }

    private BeliefScorer scorer = new BeliefScorer();
    private CnfGrammar grammar = ConstituencyTreeFactor.grammar;
    private int terminalSymbol = ConstituencyTreeFactor.terminalSymbol;
    private Alphabet<String> alph = ConstituencyTreeFactor.grammar.getLexAlphabet();

    public Chart parse(int n, List<SpanVar> constituencyVars, List<DenseFactor> beliefs) {
        // Set all of the words to be the same, terminal symbol
        int[] sentToks = new int[n];
        Arrays.fill(sentToks, terminalSymbol);
        Sentence sent = new Sentence(alph, sentToks);

        // Tell the scorer what the beliefs are
        if (constituencyVars.size() != beliefs.size())
            throw new IllegalArgumentException();
        scorer.reset(n);
        for (int i = 0; i < constituencyVars.size(); i++) {
            SpanVar sv = constituencyVars.get(i);
            DenseFactor b = beliefs.get(i);
            assert b.size() == 2 : "should be binary vars: " + b.size();
            scorer.set(sv.getStart(), sv.getEnd(), b.getValue(SpanVar.TRUE));
        }

        // Call the parser
        Chart chart = new Chart(
                sent, grammar, ChartCellType.FULL, ParseType.VITERBI, null);
        CkyPcfgParser.parseSentence(
                sentToks, grammar, LoopOrder.LEFT_CHILD, chart, scorer);
        return chart;
    }
}
