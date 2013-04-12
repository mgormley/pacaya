package edu.jhu.hltcoe.parse.cky;

import edu.jhu.hltcoe.data.Sentence;

public class Chart {

	public Chart(Sentence sent, CnfGrammar grammar) {
		//ChartCell[][] chart = new ChartCell[sent.size()][sent.size()];
		
		// Initilaize cells to negative infinity.
	}

	public boolean updateCell(int i, int j, Rule r, double score) {
		// TODO Auto-generated method stub
		return false;
	}

	public int[] getNonTerminals(int start, int mid) {
		// TODO Auto-generated method stub
		return null;
	}

	public double getScore(int start, int mid, int leftChild) {
		// TODO Auto-generated method stub
		return 0;
	}

	public void updateCell(int start, int mid, int end, Rule r, double score) {
		// TODO Auto-generated method stub
		
	}

}
