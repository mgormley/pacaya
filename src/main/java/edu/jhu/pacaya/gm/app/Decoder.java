package edu.jhu.pacaya.gm.app;

import edu.jhu.pacaya.gm.data.UFgExample;
import edu.jhu.pacaya.gm.inf.FgInferencer;

public interface Decoder<X,Y> {

    Y decode(FgInferencer inf, UFgExample ex, X x);
    
}
