package edu.jhu.parse.dep;

/**
 * An inside/outside chart for a dependency parse.
 * 
 * @author mgormley
 */
public class DepIoChart {
    final int n;
    final DepParseChart inChart;
    final DepParseChart outChart;

    public DepIoChart(DepParseChart inChart, DepParseChart outChart) {
        this.n = inChart.scores.length;
        this.inChart = inChart;
        this.outChart = outChart;
    }

    public double getLogInsideScore(int parent, int child) {
        if (parent == -1) {
            checkChild(child);
            return inChart.wallScore[child];
        } else {
            int start = Math.min(parent, child);
            int end = Math.max(parent, child);
            int d = getDirection(parent, child);
            checkCell(start, end);
            return inChart.getScore(start, end, d, ProjectiveDependencyParser.INCOMPLETE);
        }
    }

    public double getLogOutsideScore(int parent, int child) {
        if (parent == -1) {
            checkChild(child);
            // These are always 0.0 on the outside chart, but it makes the
            // algorithmic differentiation clearer to include them.
            return outChart.wallScore[child];
        } else {
            int start = Math.min(parent, child);
            int end = Math.max(parent, child);
            int d = getDirection(parent, child);
            checkCell(start, end);
            return outChart.getScore(start, end, d, ProjectiveDependencyParser.INCOMPLETE);
        }
    }

    public double getLogPartitionFunction() {
        return inChart.goalScore;
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
     * Checks that start \in [0, n-1] and end \in [1, n], where n is the
     * length of the sentence.
     */
    private void checkCell(int start, int end) {
        if (start > n - 1 || end > n || start < 0 || end < 1) {
            throw new IllegalStateException(String.format("Invalid cell: start=%d end=%d", start, end));
        }
    }

    private void checkChild(int child) {
        if (child > n - 1 || child < 0) {
            throw new IllegalStateException(String.format("Invalid child: %d", child));
        }
    }
}