package edu.jhu.util.semiring;

import org.junit.Test;

import edu.jhu.prim.util.Timer;

public class SemiringSpeedTest {

    @Test
    public void testRealSemiringSpeed() {
        Semiring s = new RealAlgebra();
        
        int numTrials = 10;
        int numOps = 100000000;

        {
            for (int t=0; t<numTrials; t++) {
                Timer timer = new Timer();
                timer.start();
                double value = 0;
                for (int i=0; i<numOps; i++) {                
                    //value = s.plus(value, s.plus(s.times(s.plus(s.one(), i), i), s.zero()));
                    value = value + (((1 + i) * i) + 0);
                }
                timer.stop();
                System.out.println("Avg time (ms): " + timer.avgMs());
            }
            System.out.println("DONE");        
        }
        {
            for (int t=0; t<numTrials; t++) {
                Timer timer = new Timer();
                timer.start();
                double value = 0;
                for (int i=0; i<numOps; i++) {                
                    value = s.plus(value, s.plus(s.times(s.plus(s.one(), i), i), s.zero()));
                }
                timer.stop();
                System.out.println("Avg time (ms): " + timer.avgMs());
            }
            System.out.println("DONE");        
        }
        
    }

}
