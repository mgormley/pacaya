package edu.jhu.util.prng;

//============================================================================
//Copyright 2006-2010 Daniel W. Dyer
//
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//============================================================================

import java.util.Random;

import org.uncommons.maths.binary.BinaryUtils;
import org.uncommons.maths.random.DefaultSeedGenerator;
import org.uncommons.maths.random.MersenneTwisterRNG;
import org.uncommons.maths.random.RepeatableRNG;
import org.uncommons.maths.random.SeedException;
import org.uncommons.maths.random.SeedGenerator;

/**
 * @author mgormley Removed the Reentrant lock that was ensuring thread safe
 *         operation of this class
 * 
 *         <p>
 *         A Java version of George Marsaglia's <a href=
 *         "http://school.anhb.uwa.edu.au/personalpages/kwessen/shared/Marsaglia03.html"
 *         >Complementary Multiply With Carry (CMWC) RNG</a>. This is a very
 *         fast PRNG with an extremely long period (2^131104). It should be used
 *         in preference to the {@link MersenneTwisterRNG} when a very long
 *         period is required.
 *         </p>
 * 
 *         <p>
 *         One potential drawback of this RNG is that it requires significantly
 *         more seed data than the other RNGs provided by Uncommons Maths. It
 *         requires just over 16 kilobytes, which may be a problem if your are
 *         obtaining seed data from a slow or limited entropy source. In
 *         contrast, the Mersenne Twister requires only 128 bits of seed data.
 *         </p>
 * 
 * @author Daniel Dyer
 * @since 1.2
 */
public class UnlockedCMWC4096RNG extends Random implements RepeatableRNG {

    private static final long serialVersionUID = -8150055708596591413L;

    private static final int SEED_SIZE_BYTES = 16384; // Needs 4,096 32-bit
    // integers.

    private static final long A = 18782L;

    private final byte[] seed;
    private final int[] state;
    private int carry = 362436; // TO DO: This should be randomly generated.
    private int index = 4095;

    /**
     * Creates a new RNG and seeds it using the default seeding strategy.
     */
    public UnlockedCMWC4096RNG() {
        this(DefaultSeedGenerator.getInstance().generateSeed(SEED_SIZE_BYTES));
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
    public UnlockedCMWC4096RNG(SeedGenerator seedGenerator) throws SeedException {
        this(seedGenerator.generateSeed(SEED_SIZE_BYTES));
    }

    /**
     * Creates an RNG and seeds it with the specified seed data.
     * 
     * @param seed
     *            The seed data used to initialise the RNG.
     */
    public UnlockedCMWC4096RNG(byte[] seed) {
        if (seed == null || seed.length != SEED_SIZE_BYTES) {
            throw new IllegalArgumentException("CMWC RNG requires 16kb of seed data.");
        }
        this.seed = seed.clone();
        this.state = BinaryUtils.convertBytesToInts(seed);
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
        index = (index + 1) & 4095;
        long t = A * (state[index] & 0xFFFFFFFFL) + carry;
        carry = (int) (t >> 32);
        int x = ((int) t) + carry;
        if (x < carry) {
            x++;
            carry++;
        }
        state[index] = 0xFFFFFFFE - x;
        return state[index] >>> (32 - bits);
    }
}
