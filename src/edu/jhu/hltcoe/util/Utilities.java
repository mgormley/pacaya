package edu.jhu.hltcoe.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import edu.jhu.hltcoe.math.LogAddTable;

public class Utilities {
	
	private Utilities() {
		// private constructor
	}
	
	private static final Integer INTEGER_ZERO = Integer.valueOf(0);
	private static final Double DOUBLE_ZERO = Double.valueOf(0.0);
	public static final double LOG2 = log(2);
	
	public static <X> Integer safeGet(Map<X,Integer> map, X key) {
        Integer value = map.get(key);
        if (value == null) {
            return INTEGER_ZERO;
        }
        return value;
    }
	
	public static <X> Double safeGet(Map<X,Double> map, X key) {
        Double value = map.get(key);
        if (value == null) {
            return DOUBLE_ZERO;
        }
        return value;
    }
	
	public static boolean safeEquals(Object o1, Object o2) {
		if (o1 == null || o2 == null) {
			return o1 == o2;
		} else {
			return o1.equals(o2);
		}
	}
	
	public static <X> void increment(Map<X,Integer> map, X key, Integer incr) {
		if (map.containsKey(key)) {
			Integer value = map.get(key);
			map.put(key, value + incr);
		} else {
			map.put(key, incr);
		}
	}
	
	public static <X> void increment(Map<X,Double> map, X key, Double incr) {
		if (map.containsKey(key)) {
			Double value = map.get(key);
			map.put(key, value + incr);
		} else {
			map.put(key, incr);
		}
	}

	/**
	 * @return The resulting set
	 */
	public static <X,Y> Set<Y> addToSet(Map<X,Set<Y>> map, X key, Y value) {
	    Set<Y> values;
	    if (map.containsKey(key)) {
            values = map.get(key);
            values.add(value);
        } else {
            values = new HashSet<Y>();
            values.add(value);
            map.put(key, values);
        }
	    return values;
    }
	
	/**
	 * Choose the <X> with the greatest number of votes.
	 * @param <X> The type of the thing being voted for.
	 * @param votes Maps <X> to a Double representing the number of votes
	 * 				it received.
	 * @return The <X> that received the most votes.
	 */
	public static <X> List<X> mostVotedFor(Map<X, Double> votes) {
		// Choose the label with the greatest number of votes
		// If there is a tie, choose the one with the least index
		double maxTalley = Double.NEGATIVE_INFINITY;
		List<X> maxTickets = new ArrayList<X>();
		for(Entry<X,Double> entry : votes.entrySet()) {
			X ticket = entry.getKey();
			double talley = entry.getValue();
			if (talley > maxTalley) {
				maxTickets = new ArrayList<X>();
				maxTickets.add(ticket);
				maxTalley = talley;
			} else if (talley == maxTalley) {
				maxTickets.add(ticket);
			}
		}
		return maxTickets;
	}
	
    public static int factorial(int n)
    {
        if( n <= 1 ) {
            return 1;
        } else {
            return n * factorial(n - 1);
        }
    }
    
    /**
     * Adds two probabilities that are stored as log probabilities.
     * @param x log(p)
     * @param y log(q)
     * @return log(p + q) = log(exp(x) + exp(y))
     */
    public static double logAdd(double x, double y) {
        //return logAddExact(x,y);
        return LogAddTable.logAdd(x,y);
    }
    
    public static double logAddExact(double x, double y) {

        // p = 0 or q = 0, where x = log(p), y = log(q)
        if (Double.NEGATIVE_INFINITY == x) {
            return y;
        } else if (Double.NEGATIVE_INFINITY == y) {
            return x;
        }

        // p != 0 && q != 0
        if (y <= x) {
            return x + log(1 + exp(y - x));
        } else {
            return y + log(1 + exp(x - y));
        }
    }

    public static double log(double d) {
        return Math.log(d);
    }
    
    public static double exp(double d) {
        return Math.exp(d);
    }
    
    public static double log2(double d) {
        return log(d) / LOG2;
    }
    
}
