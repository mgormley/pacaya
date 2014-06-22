package edu.jhu.util.collections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import edu.jhu.prim.list.IntArrayList;
import edu.jhu.prim.map.IntObjectHashMap;

public class Maps {

    public static final Integer INTEGER_ZERO = Integer.valueOf(0);
    public static final Double DOUBLE_ZERO = Double.valueOf(0.0);

    private Maps() {
        // private constructor
    }

    public static <X> Integer safeGetInt(Map<X, Integer> map, X key) {
        Integer value = map.get(key);
        if (value == null) {
            return INTEGER_ZERO;
        }
        return value;
    }

    public static <X> Double safeGetDouble(Map<X,Double> map, X key) {
        Double value = map.get(key);
        if (value == null) {
            return DOUBLE_ZERO;
        }
        return value;
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

    public static <X,Y> void increment(Map<X,Map<Y,Integer>> map, X key1, Y key2, Integer incr) {
        if (map.containsKey(key1)) {
            Map<Y,Integer> subMap = map.get(key1);
            increment(subMap, key2, incr);
        } else {
            Map<Y,Integer> subMap = new HashMap<Y,Integer>();
            increment(subMap, key2, incr);
            map.put(key1, subMap);
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
     * @return The resulting list.
     */
    public static <X,Y> List<Y> addToList(Map<X,List<Y>> map, X key, Y value) {
        List<Y> values;
        if (map.containsKey(key)) {
            values = map.get(key);
            values.add(value);
        } else {
            values = new ArrayList<Y>();
            values.add(value);
            map.put(key, values);
        }
        return values;
    }

    public static <X,Y> List<Y> safeGetList(Map<X, List<Y>> map, X key) {
        List<Y> list = map.get(key);
        if (list == null) {
            return Collections.emptyList();
        } else {
            return list;
        }
    }

    /**
     * @return The resulting list.
     */
    public static IntArrayList addToList(IntObjectHashMap<IntArrayList> map, int key, int value) {
        IntArrayList values;
        if (map.containsKey(key)) {
            values = map.get(key);
            values.add(value);
        } else {
            values = new IntArrayList();
            values.add(value);
            map.put(key, values);
        }
        return values;
    }

    public static IntArrayList safeGetList(IntObjectHashMap<IntArrayList> map, int key) {
        IntArrayList list = map.get(key);
        if (list == null) {
            return new IntArrayList();
        } else {
            return list;
        }
    }

    /**
     * Choose the <X> with the greatest number of votes.
     * @param <X> The type of the thing being voted for.
     * @param votes Maps <X> to a Double representing the number of votes
     *              it received.
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

}
