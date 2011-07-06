package edu.jhu.hltcoe.util;

import java.util.Random;

public class Prng {
    
    public static final long DEFAULT_SEED;

    static {
//        DEFAULT_SEED = 195982758;
        DEFAULT_SEED = System.currentTimeMillis();
        System.out.println("DEFAULT_SEED="+DEFAULT_SEED);
    }
    
    public static final Random random = new Random(DEFAULT_SEED);
    
    public static void setSeed(long seed) {
        random.setSeed(seed);
    }
}
