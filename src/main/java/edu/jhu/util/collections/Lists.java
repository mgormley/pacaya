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

    public static <T> List<T> getList(T... args) {
        return Arrays.asList(args);
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

    public static <T> Iterable<T> asIterable(final Iterator<T> iterator) {
        return new Iterable<T>() {
            public Iterator<T> iterator() {
                return iterator;
            }            
        };
    }

}
