package edu.jhu.pacaya.parse.cky.chart;

public class FullScores implements ScoresSnapshot {

    private double[] scores;

    public FullScores(double[] scores) {
        this.scores = scores;
    }

    @Override
    public double getScore(int symbol) {
        return scores[symbol];
    }
    
}