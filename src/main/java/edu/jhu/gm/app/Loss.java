package edu.jhu.gm.app;

public interface Loss<Y> {

    double loss(Y pred, Y gold);
    
}
