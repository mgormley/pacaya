package edu.jhu.hltcoe.util;

import java.util.Random;

public class Prng {

    public static final int DEFAULT_SEED = 195982758;
    
    public static final Random random = new Random(DEFAULT_SEED);
}
