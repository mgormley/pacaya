package edu.jhu.util.semiring;

import org.junit.After;
import org.junit.Before;

import edu.jhu.prim.util.math.FastMath;


/**
 * This test is identical to {@link LogSemiringTest} except that it uses the log-add tables.
 * @author mgormley
 */
public class LogSignAlgebraWithTableTest extends LogSignAlgebraTest {

    private boolean previous;
    
    @Before
    public void setUp() {
        previous = FastMath.useLogAddTable;
        FastMath.useLogAddTable = true;
    }
    
    @After
    public void tearDown() {
        FastMath.useLogAddTable = previous;
    }
    
}
