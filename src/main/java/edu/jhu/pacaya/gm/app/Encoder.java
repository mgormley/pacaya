package edu.jhu.pacaya.gm.app;

import edu.jhu.pacaya.gm.data.LFgExample;
import edu.jhu.pacaya.gm.data.UFgExample;

public interface Encoder<X, Y> {

    LFgExample encode(X x, Y y);
    
    UFgExample encode(X x);
    
}
