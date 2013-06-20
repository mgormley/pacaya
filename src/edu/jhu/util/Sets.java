package edu.jhu.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class Sets {

    private Sets() {
        // private constructor
    }

    // TODO: move to Utilities
    /**
     * N choose K
     */
    public static int choose(int n, int k) {
        assert(n >= k);
        double val = 1;
        for (int i=1; i<=k; i++) {
            val *= (n - (k-i))/(double)i;
        }
        return (int)val;
    }
    
    /**
     * N choose K
     */
    public static long binomialCoefficient(int n, int k)
    {
            if(n - k == 1 || k == 1)
                return n;

            long [][] b = new long[n+1][n-k+1];
            b[0][0] = 1;
            for(int i = 1; i < b.length; i++)
            {
                for(int j = 0; j < b[i].length; j++)
                {
                    if(i == j || j == 0)    
                        b[i][j] = 1;
                    else if(j == 1 || i - j == 1)
                        b[i][j] = i;
                    else
                        b[i][j] = b[i-1][j-1] + b[i-1][j];
                }
            }
            return b[n][n-k];
    }

    public static List<int[]> getSets(int setSize, int numParams) {
        if (setSize > numParams) {
            throw new IllegalArgumentException("sizeSize must be <= numParams");
        }
        List<HashSet<Integer>> hss = getSetsHelper(setSize, numParams);
        List<int[]> sets = new ArrayList<int[]>(hss.size());
        for (HashSet<Integer> set : hss) {
            sets.add(asArray(set));
        }
        return sets;
    }
    
    private static int[] asArray(HashSet<Integer> hs) {
        int[] set = new int[hs.size()];
        int i=0;
        for (Integer v : hs) {
            set[i++] = v;
        }
        return set;
    }

    public static List<HashSet<Integer>> getSetsHelper(int setSize, int numParams) {
        int numSets = (int)binomialCoefficient(numParams, setSize);
        HashSet<HashSet<Integer>> sets = new HashSet<HashSet<Integer>>(numSets);
        
        int[] set = new int[setSize];
        while(sets.size() < numSets) {
            HashSet<Integer> hs = asSet(set);
            if (hs.size() == setSize) {
                sets.add(hs);
            }
            for (int j=setSize-1; j>=0; j--) {
                set[j] = (set[j] + 1) % numParams;
                if (set[j] != 0) {
                    break;
                }
            }
        }
        
        return new ArrayList<HashSet<Integer>>(sets);
    }
    
    // This is ugly
    private static int numUnique(int[] set) {
        HashSet<Integer> hs = asSet(set);
        return hs.size();
    }

    private static HashSet<Integer> asSet(int[] set) {
        HashSet<Integer> hs = new HashSet<Integer>();
        for (int i=0; i<set.length; i++) {
            hs.add(set[i]);
        }
        return hs;
    }
    
}
