package edu.jhu.util;

import java.util.Random;

import org.uncommons.maths.random.UnlockedXORShiftRNG;
import org.uncommons.maths.random.UnlockedXORShiftRNG.DeterministicSeedGenerator;

import ec.util.MersenneTwisterFast;

public class Prng {
    
    // Just use: http://maths.uncommons.org/
    // XOR shift http://www.jstatsoft.org/v08/i14/paper
    //
    // George Marsaglia's recommendations:
    // http://www.ms.uky.edu/~mai/RandomNumber
    // http://school.anhb.uwa.edu.au/personalpages/kwessen/shared/Marsaglia03.html
    // http://groups.google.com/group/comp.lang.c/browse_thread/thread/a9915080a4424068/
    
    public static final long DEFAULT_SEED;

    // Using default seed
    public static UnlockedXORShiftRNG xorShift;
    public static Random javaRandom;
    public static MersenneTwisterFast mtf;
    public static ec.util.MersenneTwister mt;
        
    public static Random curRandom;
    
    public static long seed;
    
    public static void seed(long seed) {
        Prng.seed = seed;
        System.out.println("SEED="+seed);
        javaRandom = new Random(seed);
        mtf = new MersenneTwisterFast(seed);
        mt = new ec.util.MersenneTwister(seed);
        
        xorShift = new UnlockedXORShiftRNG(new DeterministicSeedGenerator(seed));
        
        setRandom(xorShift);
    }

    public static void setRandom(Random curRandom) {
        Prng.curRandom = curRandom;
    }

    static {
        DEFAULT_SEED = 123456789101112l;
        //DEFAULT_SEED = System.currentTimeMillis();
        System.out.println("WARNING: pseudo random number generator is not thread safe");
        seed(DEFAULT_SEED);
    }
    
    
    public static double nextDouble() {
        return curRandom.nextDouble();
    }
    
    public static boolean nextBoolean() {
        return curRandom.nextBoolean();
    }
    
    public static int nextInt(int n) {
        return curRandom.nextInt(n);
    }
    
}
