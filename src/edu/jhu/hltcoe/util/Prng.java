package edu.jhu.hltcoe.util;

import java.util.Random;

public class Prng {
    
    public static final long DEFAULT_SEED;

    // Using default seed
    public static Random random;
    
    public static void seed(long seed) {
        random = new Random(seed);
    }

    static {
//        DEFAULT_SEED = 1325449947035l;
        DEFAULT_SEED = System.currentTimeMillis();
        System.out.println("DEFAULT_SEED="+DEFAULT_SEED);
        seed(DEFAULT_SEED);
    }
    
    
    public static double nextDouble() {
        return random.nextDouble();
    }
    
    public static boolean nextBoolean() {
        return random.nextBoolean();
    }
    
    public static int nextInt(int n) {
        return random.nextInt(n);
    }
    
}
