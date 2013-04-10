package edu.jhu.hltcoe.parse.cky;

import edu.jhu.hltcoe.data.Sentence;

public class Chart {

	public Chart(Sentence sent, CnfGrammar grammar) {
		ChartCell[][] chart = new ChartCell[sent.size()][sent.size()];
		
		// Initilaize cells to negative infinity.
	}

	public boolean updateCell(int i, int j, int parent, double score) {
		// TODO Auto-generated method stub
		return false;
	}

}
