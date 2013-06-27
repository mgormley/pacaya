package org.uncommons.maths.random;

import java.util.Random;

import edu.jhu.util.Prng;

/**
 * Note: Removed the Reentrant lock that was ensuring thread safe
 *         operation of this class -MRG
 * 
 *         Very fast pseudo random number generator. See <a href=
 *         "http://school.anhb.uwa.edu.au/personalpages/kwessen/shared/Marsaglia03.html"
 *         >this page</a> for a description. This RNG has a period of about
 *         2^160, which is not as long as the MersenneTwisterRNG but it
 *         is faster.
 * 
 * @author Daniel Dyer
 * @since 1.2
 */
public class UnlockedXORShiftRNG extends Random {

    /**
     * An easy way to generate deterministic seeds from a single long value or
     * System.currentTimeMillis().
     * 
     * @author mgormley
     * 
     */
    public static class DeterministicSeedGenerator {

        private Random random;

        public DeterministicSeedGenerator(long seed) {
            random = new Random(seed);
        }

        public byte[] generateSeed(int length) {
            byte[] bytes = new byte[length];
            random.nextBytes(bytes);
            return bytes;
        }

    }
    
    private static final long serialVersionUID = 1889695261586701491L;

    private static final int SEED_SIZE_BYTES = 20; // Needs 5 32-bit integers.

    // Previously used an array for state but using separate fields proved to be
    // faster.
    private int state1;
    private int state2;
    private int state3;
    private int state4;
    private int state5;

    private final byte[] seed;

    /**
     * Creates a new RNG and seeds it using the default seeding strategy.
     */
    public UnlockedXORShiftRNG() {
        this(new DeterministicSeedGenerator(Prng.seed));
    }

    /**
     * Seed the RNG using the provided seed generation strategy.
     * 
     * @param seedGenerator
     *            The seed generation strategy that will provide the seed value
     *            for this RNG.
     * @throws SeedException
     *             If there is a problem generating a seed.
     */
    public UnlockedXORShiftRNG(DeterministicSeedGenerator seedGenerator) {
        this(seedGenerator.generateSeed(SEED_SIZE_BYTES));
    }

    /**
     * Creates an RNG and seeds it with the specified seed data.
     * 
     * @param seed
     *            The seed data used to initialise the RNG.
     */
    public UnlockedXORShiftRNG(byte[] seed) {
        if (seed == null || seed.length != SEED_SIZE_BYTES) {
            throw new IllegalArgumentException("XOR shift RNG requires 160 bits of seed data.");
        }
        this.seed = seed.clone();
        int[] state = convertBytesToInts(seed);
        this.state1 = state[0];
        this.state2 = state[1];
        this.state3 = state[2];
        this.state4 = state[3];
        this.state5 = state[4];
    }

    /**
     * {@inheritDoc}
     */
    public byte[] getSeed() {
        return seed.clone();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int next(int bits) {
        int t = (state1 ^ (state1 >> 7));
        state1 = state2;
        state2 = state3;
        state3 = state4;
        state4 = state5;
        state5 = (state5 ^ (state5 << 6)) ^ (t ^ (t << 13));
        int value = (state2 + state2 + 1) * state5;
        return value >>> (32 - bits);
    }

    /* ---- Remainder is copied from BinaryUtils ---- */
    
    // Mask for casting a byte to an int, bit-by-bit (with
    // bitwise AND) with no special consideration for the sign bit.
    private static final int BITWISE_BYTE_TO_INT = 0x000000FF;

    /**
     * Take four bytes from the specified position in the specified
     * block and convert them into a 32-bit int, using the big-endian
     * convention.
     * @param bytes The data to read from.
     * @param offset The position to start reading the 4-byte int from.
     * @return The 32-bit integer represented by the four bytes.
     */
    public static int convertBytesToInt(byte[] bytes, int offset)
    {
        return (BITWISE_BYTE_TO_INT & bytes[offset + 3])
                | ((BITWISE_BYTE_TO_INT & bytes[offset + 2]) << 8)
                | ((BITWISE_BYTE_TO_INT & bytes[offset + 1]) << 16)
                | ((BITWISE_BYTE_TO_INT & bytes[offset]) << 24);
    }
    
    /**
     * Convert an array of bytes into an array of ints.  4 bytes from the
     * input data map to a single int in the output data.
     * @param bytes The data to read from.
     * @return An array of 32-bit integers constructed from the data.
     * @since 1.1
     */
    public static int[] convertBytesToInts(byte[] bytes)
    {
        if (bytes.length % 4 != 0)
        {
            throw new IllegalArgumentException("Number of input bytes must be a multiple of 4.");
        }
        int[] ints = new int[bytes.length / 4];
        for (int i = 0; i < ints.length; i++)
        {
            ints[i] = convertBytesToInt(bytes, i * 4);
        }
        return ints;
    }
    
}
