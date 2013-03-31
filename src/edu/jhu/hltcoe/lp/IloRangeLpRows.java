package edu.jhu.hltcoe.lp;

import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloLinearNumExprIterator;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import util.Alphabet;
import edu.jhu.hltcoe.util.vector.SortedLongDoubleVector;

public class IloRangeLpRows {

	private LpRows lpRows;
	private Alphabet<IloNumVar> alphabet;
	
	public IloRangeLpRows(boolean setNames) {
		lpRows = new LpRows(setNames);
		alphabet = new Alphabet<IloNumVar>();
	}
	
	public void addRow(IloRange range) throws IloException {
		if (range == null) { return; }
    	IloLinearNumExpr expr = (IloLinearNumExpr) range.getExpr();
    	IloLinearNumExprIterator iter = expr.linearIterator();
		SortedLongDoubleVector coef = new SortedLongDoubleVector();
    	while (iter.hasNext()) {
    		int idx = alphabet.lookupObject(iter.nextNumVar());
    		coef.add(idx, iter.getValue());
    	}
		lpRows.addRow(range.getLB(), coef, range.getUB(), range.getName());
	}
	
	public void addRows(IloRange[] ranges) throws IloException {
		if (ranges == null) { return; }
		for (int i=0; i<ranges.length; i++) {
			addRow(ranges[i]);
		}
	}

	public void addRows(IloRange[][] ranges) throws IloException {
		if (ranges == null) { return; }
		for (int i=0; i<ranges.length; i++) {
			addRows(ranges[i]);
		}
	}
	
	public void addRows(IloRange[][][] ranges) throws IloException {
		if (ranges == null) { return; }
		for (int i=0; i<ranges.length; i++) {
			addRows(ranges[i]);
		}
	}

	public void addRowsToMatrix(IloLPMatrix mat) throws IloException {
		IloNumVar[] numVars = alphabet.index2feat.toArray(new IloNumVar[]{});
		mat.addCols(numVars);
		lpRows.addRowsToMatrix(mat);
	}
}
