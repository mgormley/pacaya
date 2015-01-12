package edu.jhu.gm.app;

import edu.jhu.gm.data.LFgExample;
import edu.jhu.gm.data.UFgExample;

public interface Encoder<X, Y> {

    LFgExample encode(X x, Y y);
    
    UFgExample encode(X x);
    
}
