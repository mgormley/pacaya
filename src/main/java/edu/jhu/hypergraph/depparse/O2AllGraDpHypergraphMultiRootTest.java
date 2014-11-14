package edu.jhu.hypergraph.depparse;

import edu.jhu.hypergraph.AbstractHypergraphTest;
import edu.jhu.util.semiring.Algebras;
import edu.jhu.util.semiring.RealAlgebra;

public class O2AllGraDpHypergraphMultiRootTest extends AbstractHypergraphTest {

    RealAlgebra s = Algebras.REAL_ALGEBRA;

    protected O2AllGraDpHypergraph getHypergraph() {
        return new O2AllGraDpHypergraph(O2AllGraDpHypergraphTest.getScorer(), s, false);
    }

}
