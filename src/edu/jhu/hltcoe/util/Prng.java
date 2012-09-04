package edu.jhu.hltcoe.util;

import java.util.Random;

import org.uncommons.maths.random.SeedException;

import umontreal.iro.lecuyer.rng.MRG32k3a;
import umontreal.iro.lecuyer.rng.RandomStream;
import cern.jet.random.engine.MersenneTwister;
import ec.util.MersenneTwisterFast;
import edu.jhu.hltcoe.util.random.DeterministicSeedGenerator;
import edu.jhu.hltcoe.util.random.UnlockedCMWC4096RNG;
import edu.jhu.hltcoe.util.random.UnlockedXORShiftRNG;

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
    public static final RandomStream stream = new MRG32k3a();
    public static UnlockedXORShiftRNG xorShift;
    public static UnlockedCMWC4096RNG mwc4096;
    public static Random random;
    public static MersenneTwisterFast mtf;
    public static ec.util.MersenneTwister mt;
    public static MersenneTwister mtColt;
    
    public static void seed(long seed) {
        random = new Random(seed);
        mtf = new MersenneTwisterFast(seed);
        mt = new ec.util.MersenneTwister(seed);
        mtColt = new MersenneTwister((int)seed);

        long value = seed % 4294944443l;
        long[] seedArray = new long[]{value,value,value,value,value,value};
        ((MRG32k3a)stream).setSeed(seedArray);

        try {
            xorShift = new UnlockedXORShiftRNG(new DeterministicSeedGenerator(seed));
            mwc4096 = new UnlockedCMWC4096RNG(new DeterministicSeedGenerator(seed));
        } catch (SeedException e) {
            throw new RuntimeException(e);
        }
    }

    static {
//        DEFAULT_SEED = 1325449947035l;
        DEFAULT_SEED = System.currentTimeMillis();
        System.out.println("DEFAULT_SEED="+DEFAULT_SEED);
        System.out.println("WARNING: pseudo random number generator is not thread safe");
        seed(DEFAULT_SEED);
    }
    
    
    public static double nextDouble() {
        return xorShift.nextDouble();
    }
    
    public static boolean nextBoolean() {
        return xorShift.nextBoolean();
    }
    
    public static int nextInt(int n) {
        return xorShift.nextInt(n);
    }
    
}
