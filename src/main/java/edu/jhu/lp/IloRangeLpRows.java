package edu.jhu.lp;

import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloLinearNumExprIterator;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;

import java.util.Arrays;

import org.apache.log4j.Logger;

import edu.jhu.util.Alphabet;
import edu.jhu.util.vector.LongDoubleSortedVector;

public class IloRangeLpRows {
    private static final Logger log = Logger.getLogger(IloRangeLpRows.class);

    private int maxRowsToCache;
	private LpRows rows;
	private Alphabet<IloNumVar> alphabet;
	private int numRowsAdded;
	private boolean setNames;
	private IloLPMatrix mat;
	
	public IloRangeLpRows(IloLPMatrix mat, boolean setNames, int maxRowsToCache) {
		this.mat = mat;
		this.maxRowsToCache = maxRowsToCache;
		this.setNames = setNames;
		rows = new LpRows(setNames);
		alphabet = new Alphabet<IloNumVar>();
        this.numRowsAdded = 0;
	}
	
	public void addRow(IloRange range) throws IloException {
		if (range == null) { return; }
    	IloLinearNumExpr expr = (IloLinearNumExpr) range.getExpr();
    	IloLinearNumExprIterator iter = expr.linearIterator();
		LongDoubleSortedVector coef = new LongDoubleSortedVector();
    	while (iter.hasNext()) {
    		int idx = alphabet.lookupIndex(iter.nextNumVar());
    		coef.add(idx, iter.getValue());
    	}
    	rows.addRow(range.getLB(), coef, range.getUB(), range.getName());
    	numRowsAdded++;
    	maybePush();
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

    private void reset() {
        this.rows = new LpRows(setNames);
    }
    
    private void maybePush() throws IloException {
        if (rows.getNumRows() >= maxRowsToCache) {
            pushRowsToCplex();
        }
    }

    public void pushRowsToCplex() throws IloException {
		IloNumVar[] numVars = alphabet.getObjects().toArray(new IloNumVar[]{});
		IloNumVar[] numVarsSlice = Arrays.copyOfRange(numVars, mat.getNcols(), numVars.length);
		mat.addCols(numVarsSlice);
		assert (mat.getNcols() == numVars.length);
		
        rows.addRowsToMatrix(mat);
        log.debug("Rows added: " + numRowsAdded);
        reset();
    }

    public int getNumRows() {
        return numRowsAdded;
    }
}
