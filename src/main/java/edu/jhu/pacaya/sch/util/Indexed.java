package edu.jhu.pacaya.sch.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import edu.jhu.prim.util.SafeEquals;

/**
 * An object paired with an integer to facilitate enumerate
 *
 * @param <T>
 */
public class Indexed<T> {

    private int index;
    private T obj;

    public Indexed(T obj, int index) {
        this.obj = obj;
        this.index = index;
    }

    public int index() {
        return index;
    }

    public T get() {
        return obj;
    }

    public String toString() {
        return String.format("<%s, %d>", get(), index());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + index;
        result = prime * result + ((obj == null) ? 0 : obj.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object rhs) {
        if (this == rhs) {
            // pointer equality
            return true;
        } else if (rhs == null) {
            return false;
        } else {
            try {
                @SuppressWarnings("unchecked")
                Indexed<T> rhsIndexed = (Indexed<T>) rhs;
                if (index != rhsIndexed.index) {
                    // index doesn't match
                    return false;
                } else {
                    return SafeEquals.safeEquals(obj, rhsIndexed.obj);
                }
            } catch (ClassCastException e){
                return false;
            }
        }
    }

    /**
     * Returns a Collections that contains all of the objects from the
     * underlying stream in order
     */
    public static <T> Collection<T> collect(Iterable<T> stream) {
        LinkedList<T> collection = new LinkedList<>();
        for (T e : stream) {
            collection.add(e);
        }
        return collection;
    }

    /**
     * Returns an iterable over Index objects that each hold one of the original
     * objects of the stream as well its index in the stream
     */
    public static <T> Iterable<Indexed<T>> enumerate(Iterable<T> stream) {
        return new Iterable<Indexed<T>>() {

            @Override
            public Iterator<Indexed<T>> iterator() {
                Iterator<T> itr = stream.iterator();
                return new Iterator<Indexed<T>>() {

                    private int i = 0;

                    @Override
                    public boolean hasNext() {
                        return itr.hasNext();
                    }

                    @Override
                    public Indexed<T> next() {
                        Indexed<T> nextPair = new Indexed<T>(itr.next(), i);
                        i++;
                        return nextPair;
                    }
                };
            }
        };
    }

}
