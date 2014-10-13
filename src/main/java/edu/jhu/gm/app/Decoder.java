package edu.jhu.gm.app;

import edu.jhu.gm.data.UFgExample;
import edu.jhu.gm.inf.FgInferencer;

public interface Decoder<X,Y> {

    Y decode(FgInferencer inf, UFgExample ex, X x);
    
}
