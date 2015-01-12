package edu.jhu.parse.cky.chart;

import edu.jhu.nlp.data.Sentence;
import edu.jhu.parse.cky.Rule;
import edu.jhu.parse.cky.chart.Chart.BackPointer;

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

    public void updateCell(int symbol, double addendLeft, int mid, Rule r);

    public ScoresSnapshot getScoresSnapshot();

    public void close();

    /**
     * Ensures that the chart cell is open and all future calls will be just as
     * if it was newly constructed.
     * @param sentence TODO
     */
    public void reset(Sentence sentence);

}