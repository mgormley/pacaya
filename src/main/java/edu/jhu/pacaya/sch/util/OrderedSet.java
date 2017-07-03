package edu.jhu.pacaya.sch.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Spliterator;

public class OrderedSet<T> implements Set<T>, List<T> {
    private List<T> ordered;
    private Set<T> elements;

    public OrderedSet() {
        ordered = new LinkedList<>();
        elements = new HashSet<>();
    }

    public OrderedSet(Collection<T> c) {
        this();
        addAll(c);
    }

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return elements.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return ordered.iterator();
    }

    @Override
    public Object[] toArray() {
        return ordered.toArray();
    }

    @SuppressWarnings("hiding")
    @Override
    public <T> T[] toArray(T[] a) {
        return (T[]) ordered.toArray(a);
    }

    /**
     * maintain the set so that it is easy to test membership, the list so that
     * there is a fixed order
     */
    @Override
    public boolean add(T e) {
        if (elements.add(e)) {
            ordered.add(e);
            return true;
        } else {
            return false;
        }
    }

    /**
     * remove will be slow
     */
    @Override
    public boolean remove(Object o) {
        if (elements.remove(o)) {
            ordered.remove(o);
            return true;
        } else {
            return false;
        }

    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object e : c) {
            if (!elements.contains(e)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        boolean changed = false;
        for (T e : c) {
            changed |= add(e);
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        // remove all elements except those in c
        Set<T> toRemove = new HashSet<>(elements);
        toRemove.removeAll(c);
        return removeAll(toRemove);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean changed = false;
        for (Object e : c) {
            changed |= remove(e);
        }
        return changed;
    }

    @Override
    public void clear() {
        elements.clear();
        ordered.clear();
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        throw new UnsupportedOperationException("Cannot set at a specific location in an ordered Set");
    }

    @Override
    public T get(int index) {
        return ordered.get(index);
    }

    @Override
    public T set(int index, T element) {
        throw new UnsupportedOperationException("Cannot set at a specific location in an ordered Set");
    }

    @Override
    public void add(int index, T element) {
        throw new UnsupportedOperationException("Cannot add at a specific location in an ordered Set");
    }

    @Override
    public T remove(int index) {
        throw new UnsupportedOperationException("Cannot remove from a specific location in an ordered Set; If this was not intended, try casing to an object s.remove((Integer) item)");
    }

    @Override
    public int indexOf(Object o) {
        return ordered.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return ordered.lastIndexOf(o);
    }

    @Override
    public ListIterator<T> listIterator() {
        return ordered.listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return ordered.listIterator(index);
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return ordered.subList(fromIndex, toIndex);
    }

    @Override
    public Spliterator<T> spliterator() {
        return List.super.spliterator();
    }

    @Override
    public String toString() {
        return ordered.toString();
    }
    
    @Override
    public boolean equals(Object rhs) {
        return ordered.equals(rhs);
    }

}
