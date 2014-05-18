package edu.jhu.gm.app;

import edu.jhu.gm.data.LFgExample;
import edu.jhu.gm.inf.FgInferencer;

public interface IdxLoss {

    double getLoss(int i, LFgExample ex, FgInferencer infLatPred);
    
}