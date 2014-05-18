package edu.jhu.gm.app;

import edu.jhu.gm.data.LFgExample;
import edu.jhu.gm.data.UFgExample;
import edu.jhu.gm.model.FgModel;

public interface Encoder<X, Y> {

    LFgExample encode(FgModel model, X x, Y y);
    
    UFgExample encode(FgModel model, X x);
    
}
