package edu.jhu.hltcoe.util.prng;

import java.util.Random;

import org.uncommons.maths.random.SeedException;
import org.uncommons.maths.random.SeedGenerator;

/**
 * An easy way to generate deterministic seeds from a single long value or
 * System.currentTimeMillis().
 * 
 * @author mgormley
 * 
 */
public class DeterministicSeedGenerator implements SeedGenerator {

    private Random random;

    public DeterministicSeedGenerator(long seed) {
        random = new Random(seed);
    }

    @Override
    public byte[] generateSeed(int length) throws SeedException {
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

}
