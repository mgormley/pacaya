package edu.jhu.util.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class Lists {

    private Lists() {
        // private constructor
    }

    /**
     * Creates a new list containing a slice of the original list.
     * @param list The original list.
     * @param start The index of the first element of the slice (inclusive).
     * @param end The index of the last element of the slice (exclusive).
     * @return A sublist containing elements [start, end).
     */
    public static <X> ArrayList<X> sublist(List<X> list, int start, int end) {
        ArrayList<X> sublist = new ArrayList<X>();
        for (int i=start; i<end; i++) {
            sublist.add(list.get(i));
        }
        return sublist;
    }

    public static <T> void addAll(ArrayList<T> list, T[] array) {
        if (array == null) {
            return;
        }
        list.addAll(Arrays.asList(array));
    }

    public static <T> void addAll(ArrayList<T> list, T[][] array) {
        for (int i=0; i<array.length; i++) {
            addAll(list, array[i]);
        }
    }

    public static <T> void addAll(ArrayList<T> list, T[][][] array) {
        for (int i=0; i<array.length; i++) {
            addAll(list, array[i]);
        }
    }

    public static <T> void addAll(ArrayList<T> list, T[][][][] array) {
        for (int i=0; i<array.length; i++) {
            addAll(list, array[i]);
        }
    }

    @SafeVarargs
    public static <T> List<T> getList(T... args) {
        return Arrays.asList(args);
    }

    @SafeVarargs
    public static <T> List<T> cons(T val, T... values) {
        List<T> list = new ArrayList<T>(Arrays.asList(values));
        list.add(val);
        return list;
    }
    
    public static <T> List<T> cons(T val, List<T> values) {
        List<T> list = new ArrayList<T>(values);
        list.add(val);
        return list;
    }

    public static <T> List<T> union(List<T> ats1, List<T> ats2) {
        ArrayList<T> newList = new ArrayList<T>(ats1.size() + ats2.size());
        newList.addAll(ats1);
        newList.addAll(ats2);
        return newList;
    }

    public static <T> ArrayList<T> copyOf(List<T> list) {
        return list == null ? null : new ArrayList<T>(list);
    }

    /** Gets a new list of Strings that have been interned. */
    public static ArrayList<String> getInternedList(List<String> oldList) {
        ArrayList<String> newList = new ArrayList<String>(oldList.size());
        for (String elem : oldList) {
            newList.add(elem.intern());
        }
        return newList;
    }
    
    /** Interns a list of strings in place. */
    public static void intern(List<String> list) {
        if (list == null) {
            return;
        }
        for (int i=0; i<list.size(); i++) {
            String interned = list.get(i);
            if (interned != null) {
                interned = interned.intern();
            }
            list.set(i, interned);
        }
    }

    public static <T> Iterable<T> asIterable(final Iterator<T> iterator) {
        return new Iterable<T>() {
            public Iterator<T> iterator() {
                return iterator;
            }            
        };
    }
    
    public static int[] asArray(List<Integer> list) {
        int[] array = new int[list.size()];
        int i=0;
        for (Integer v : list) {
            array[i++] = v;
        }
        return array;
    }
    
}
