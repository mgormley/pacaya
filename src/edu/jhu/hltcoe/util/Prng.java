package edu.jhu.hltcoe.util;

import java.util.Random;

public class Prng {
        
    public static final Random random = new Random();
    
    static {
        long seed;
//        seed = 195982758;
        seed = System.currentTimeMillis();
        seed(seed);
    }
    
    public static void seed(long seed) {
        System.out.println("Using PRNG seed="+seed);
        random.setSeed(seed);
    }
}
