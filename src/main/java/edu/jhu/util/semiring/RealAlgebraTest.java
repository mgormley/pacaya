package edu.jhu.util.semiring;

import static org.junit.Assert.*;

import org.junit.Test;

public class RealAlgebraTest extends AbstractAlgebraTest {

    @Override
    public Algebra getAlgebra() {
        return new RealAlgebra();
    }

}
