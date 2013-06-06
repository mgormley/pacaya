package edu.jhu.hltcoe.parse.cky.chart;

import edu.jhu.hltcoe.parse.cky.Rule;
import edu.jhu.hltcoe.parse.cky.chart.Chart.BackPointer;

/**
 * Chart cell for a chart parsing algorithm.
 * 
 * @author mgormley
 *
 */
public interface ChartCell {
    
    public BackPointer getBp(int symbol);
    
    public double getScore(int symbol);
    
    public int[] getNts();

    public void updateCell(int mid, Rule r, double score);

    public ScoresSnapshot getScoresSnapshot();

    public void close();


    /**
     * Ensures that the chart cell is open and all future calls will be just as
     * if it was newly constructed.
     */
    public void reset();

}