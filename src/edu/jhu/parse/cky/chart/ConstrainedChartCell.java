package edu.jhu.parse.cky.chart;

import edu.jhu.data.Sentence;
import edu.jhu.parse.cky.Rule;
import edu.jhu.parse.cky.chart.Chart.BackPointer;

/**
 * This is a wrapper class, that constrains the wrapped chart cell so that it
 * only updates cells if they respect a set of constraints.
 * 
 * @author mgormley
 * 
 */
public final class ConstrainedChartCell implements ChartCell {

    /**
     * A set of constraints for chart cells.
     * 
     * @author mgormley
     */
    public interface ChartCellConstraint {
        /**
         * Sets the sentence for future calls to shouldUpdateCell.
         */
        void setSentence(Sentence sentence);

        /**
         * Returns whether or not this cell should be updated with the specified
         * rule, and midpoint split position (mid).
         */
        boolean shouldUpdateCell(int start, int end, int mid, Rule r, double score);
    }
    
    /** The start position of the span dominated by this chart cell. */
    private final int start;
    /** The end position of the span dominated by this chart cell. */
    private final int end;
    /** The wrapped chart cell. */
    private final ChartCell cell;
    /** The chart cell constraint */
    private ChartCellConstraint constraint;
    
    public ConstrainedChartCell(int start, int end, ChartCell cell, ChartCellConstraint constraint, Sentence sent) {
        this.start = start;
        this.end = end;
        this.cell = cell;
        this.constraint = constraint;
        this.constraint.setSentence(sent);
    }

    public void reset(Sentence sentence) {
        cell.reset(sentence);
        constraint.setSentence(sentence);
    }
    
    public final void updateCell(int nt, double score, int mid, Rule r) {
        if (!constraint.shouldUpdateCell(start, end, mid, r, score)) {
            // Do not update this chart cell.
            return;
        }
        // Update this chart cell.
        cell.updateCell(nt, score, mid, r);
    }

    public final BackPointer getBp(int symbol) {
        return cell.getBp(symbol);
    }
    
    public final double getScore(int symbol) {
        return cell.getScore(symbol);
    }
    
    public final int[] getNts() {
        return cell.getNts();
    }

    public ScoresSnapshot getScoresSnapshot() {
        return cell.getScoresSnapshot();
    }

    @Override
    public void close() {
        cell.close();
    }
    
}