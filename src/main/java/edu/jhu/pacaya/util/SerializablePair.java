package edu.jhu.pacaya.util;

import java.io.Serializable;

import edu.jhu.prim.tuple.Pair;

/**
 * The motivation for a pair class that is serializable is for use in template
 * keys for feature extraction.  In order to allow the model to be written out.
 * 
 * @author adam
 *
 */
public class SerializablePair<A extends Serializable, B extends Serializable> extends Pair<A, B>
        implements Serializable {

    private static final long serialVersionUID = -3890775915208596357L;

    public SerializablePair(A x, B y) {
        super(x, y);
    }

}
