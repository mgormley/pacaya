package edu.jhu.hltcoe.parse.cky.chart;

import java.util.Arrays;

import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.conll.ValidParentsSentence;
import edu.jhu.hltcoe.parse.cky.CnfGrammar;
import edu.jhu.hltcoe.parse.cky.Rule;
import edu.jhu.hltcoe.parse.cky.chart.Chart.BackPointer;
import edu.jhu.hltcoe.parse.cky.chart.Chart.ParseType;
import edu.jhu.hltcoe.parse.dmv.DmvRule;
import edu.jhu.hltcoe.parse.dmv.DmvRule.DmvRuleType;
import edu.jhu.hltcoe.util.Utilities;
import gnu.trove.TIntArrayList;

/**
 * This is a wrapper class, that constrains the wrapped chart cell so that it
 * only updates cells if they respect the constraints on the set of valid
 * parents for each child.
 * 
 * NOTE: This class can only be used with a DmvCnfGrammar. 
 * 
 * @author mgormley
 * 
 */
public final class ConstrainedChartCell implements ChartCell {

    /** The start the span dominated by this chart cell. */
    private final int start;
    /** The end the span dominated by this chart cell. */
    private final int end;
    /** Sentence which also indicates whether a parent/child arc is valid. */
    private ValidParentsSentence sent;
    /** The wrapped chart cell. */
    private ChartCell cell;
    
    public ConstrainedChartCell(int start, int end, ValidParentsSentence sentence,
            ChartCell cell) {
        this.start = start;
        this.end = end;
        this.sent = sentence;
        this.cell = cell;        
    }

    public void reset(Sentence sentence) {
        cell.reset(sentence);
        this.sent = (ValidParentsSentence)sentence;
    }
    
    public final void updateCell(int mid, Rule r, double score) {
        final DmvRule dmvRule = (DmvRule)r;
        if (dmvRule.getType() == DmvRuleType.STRUCTURAL) {
            // This is only true for structural rules.
            int leftHead = start / 2;
            int rightHead = end / 2;
            // Determine which is the head and child.
            boolean isLeftHead = dmvRule.isLeftHead();
            int head = isLeftHead ? leftHead : rightHead;
            int child = isLeftHead ? rightHead : leftHead;
            
            // Check that the constraints allow this arc.
            if (!sent.isValid(child, head)) {
                // Do not update this chart cell.
                return;
            }
        } else if (dmvRule.getType() == DmvRuleType.ROOT) {
            int head = -1;
            int child = mid / 2;
            // Check that the constraints allow this arc.
            if (!sent.isValid(child, head)) {
                // Do not update this chart cell.
                return;
            }
        }
        
        // Update this chart cell.
        cell.updateCell(mid, r, score);
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