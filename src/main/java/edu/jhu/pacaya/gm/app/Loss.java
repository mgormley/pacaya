package edu.jhu.pacaya.gm.app;

public interface Loss<Y> {

    double loss(Y pred, Y gold);
    
}
