package edu.jhu.hltcoe.gm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class SmallSet<E> implements Set<E> {

    private ArrayList<E> list;
    
    public SmallSet() {
        list = new ArrayList<E>();
    }
    
    public SmallSet(int initialCapacity) {
        list = new ArrayList<E>(initialCapacity);
    }

    /** @inheritDoc */
    @Override
    public boolean add(E e) {
        if (!list.contains(e)) {
            list.add(e);
            return true;
        } else {
            return false;
        }
    }

    /** @inheritDoc */
    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean changed = false;
        for (E e : c) {
            changed = (add(e) || changed);
        }
        return changed;
    }

    /** @inheritDoc */
    @Override
    public void clear() {
        list.clear();
    }

    /** @inheritDoc */
    @Override
    public boolean contains(Object o) {
        return list.contains(o);
    }

    /** @inheritDoc */
    @Override
    public boolean containsAll(Collection<?> c) {
        return list.containsAll(c);
    }

    /** @inheritDoc */
    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    /** @inheritDoc */
    @Override
    public Iterator<E> iterator() {
        return list.iterator();
    }

    /** @inheritDoc */
    @Override
    public boolean remove(Object o) {
        return list.remove(o);
    }

    /** @inheritDoc */
    @Override
    public boolean removeAll(Collection<?> c) {
        return list.removeAll(c);
    }

    /** @inheritDoc */
    @Override
    public boolean retainAll(Collection<?> c) {
        ArrayList<E> newList = new ArrayList<E>();
        for (E e : list) {
            if (c.contains(e)) {
                newList.add(e);
            }
        }
        boolean changed = list.size() != newList.size();
        list = newList;
        return changed;
        
    }

    /** @inheritDoc */
    @Override
    public int size() {
        return list.size();
    }

    /** @inheritDoc */
    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    /** @inheritDoc */
    @Override
    public <T> T[] toArray(T[] a) {
        return list.toArray(a);
    }

}
