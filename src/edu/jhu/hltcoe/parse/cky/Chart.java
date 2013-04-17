package edu.jhu.hltcoe.parse.cky;

import java.util.Arrays;

import edu.jhu.hltcoe.data.Sentence;
import gnu.trove.TIntArrayList;

/**
 * Parsing chart that stores every cell explicitly. This is suitable for
 * grammars with a very small number of non-terminals (e.g. the DMV). Ï
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

	public Chart(Sentence sent, CnfGrammar grammar) {
		chart = new ChartCell[sent.size()][sent.size()];
		for (int i = 0; i < chart.length; i++) {
			for (int j = 0; j < chart[i].length; j++) {
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
		ChartCell cell = chart[start][end];
		int nt = r.getParent();
		if (score > cell.maxScores[nt]) {
			cell.maxScores[nt] = score;
			cell.bps[nt] = new BackPointer(r, start);
		}
	}

	public void updateCell(int start, int mid, int end, Rule r, double score) {
		ChartCell cell = chart[start][end];
		int nt = r.getParent();
		if (score > cell.maxScores[nt]) {
			cell.maxScores[nt] = score;
			cell.bps[nt] = new BackPointer(r, mid);
		}
	}

}
