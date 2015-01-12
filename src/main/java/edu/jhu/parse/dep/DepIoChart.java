package edu.jhu.parse.dep;

/**
 * An inside/outside chart for a dependency parse.
 * 
 * @author mgormley
 */
public class DepIoChart {
    final int nplus;
    final ProjTreeChart inChart;
    final ProjTreeChart outChart;

    public DepIoChart(ProjTreeChart inChart, ProjTreeChart outChart) {
        this.nplus = inChart.scores.length;
        this.inChart = inChart;
        this.outChart = outChart;
    }

    public double getLogInsideScore(int parent, int child) {
        parent++; child++;
        int start = Math.min(parent, child);
        int end = Math.max(parent, child);
        int d = getDirection(parent, child);
        checkCell(start, end);
        return inChart.getScore(start, end, d, ProjectiveDependencyParser.INCOMPLETE);
    }

    public double getLogOutsideScore(int parent, int child) {
        parent++; child++;
        int start = Math.min(parent, child);
        int end = Math.max(parent, child);
        int d = getDirection(parent, child);
        checkCell(start, end);
        return outChart.getScore(start, end, d, ProjectiveDependencyParser.INCOMPLETE);
    }

    public double getLogPartitionFunction() {
        return inChart.getScore(0,0, ProjectiveDependencyParser.LEFT, ProjectiveDependencyParser.COMPLETE) +
                inChart.getScore(0, nplus-1, ProjectiveDependencyParser.RIGHT, ProjectiveDependencyParser.COMPLETE);
    }

    public double getLogSumOfPotentials(int parent, int child) {
        return getLogInsideScore(parent, child) + getLogOutsideScore(parent, child);
    }

    public double getLogExpectedCount(int parent, int child) {
        return getLogSumOfPotentials(parent, child) - getLogPartitionFunction();
    }

    private int getDirection(int parent, int child) {
        return (parent < child) ? ProjectiveDependencyParser.RIGHT : ProjectiveDependencyParser.LEFT;
    }

    /**
     * Checks that start \in [0, n-1] and end \in [1, n], where nplus is the sentence length plus 1.
     */
    private void checkCell(int start, int end) {
        if (start > nplus - 1 || end > nplus || start < 0 || end < 1) {
            throw new IllegalStateException(String.format("Invalid cell: start=%d end=%d", start, end));
        }
    }

}