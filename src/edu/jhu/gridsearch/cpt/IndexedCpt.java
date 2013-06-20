package edu.jhu.gridsearch.cpt;

public interface IndexedCpt {

    public int getNumConds();

    public int getNumTotalParams();

    public int getNumParams(int c);

    public String getName(int c, int m);

    public int[][] getTotMaxFreqCm();

    public int[][] getTotSupervisedFreqCm();
    
    public int[][] getTotUnsupervisedMaxFreqCm();
    
    public int getTotUnsupervisedMaxFreqCm(int c, int m);

    public int getNumNonZeroUnsupMaxFreqCms();

}