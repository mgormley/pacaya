package edu.jhu.util.collections;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import edu.jhu.prim.sort.Sort;

/**
 * Maintains a set as a sorted list.
 * 
 * @author mgormley
 * 
  */
public class SmallSet<E extends Comparable<E>> implements Set<E>, Serializable {

    private static final long serialVersionUID = 1L;
    private ArrayList<E> list;
    
    public SmallSet() {
        list = new ArrayList<E>();
    }
    
    public SmallSet(int initialCapacity) {
        list = new ArrayList<E>(initialCapacity);
    }
    
    /** Copy constructor. */
    public SmallSet(SmallSet<E> set) {
        list = new ArrayList<E>(set);
    }
    
    /**
     * Constructs a new set that is the union of the other sets.
     * @param sets
     */
    public SmallSet(SmallSet<E> set1, SmallSet<E> set2) {  
        this();
        Sort.mergeSortedLists(set1.list, set2.list, this.list);
    }
        
    // TODO: containsAll should call this and it should become private.
    public boolean isSuperset(SmallSet<E> other) {
        if (this.list.size() < other.list.size()) {
            return false;
        }
        int j = 0;
        for (int i=0; i<list.size() && j<other.list.size(); i++) {
            E e1 = this.list.get(i);
            E e2 = other.list.get(j);
            int diff = e1.compareTo(e2);
            if (diff == 0) {
                // Equal entries. Just continue.
                j++;
                continue;
            } else if (diff > 0) {
                // e1 is greater than e2, which means e2 must not appear in this.list.
                return false;
            } else {
                // e1 is less than e2, so e2 might appear later in this.list.
                continue;
            }
        }
        if (j == other.list.size()) {
            return true;
        } else {
            return false;
        }
    }
    
    /** @inheritDoc */
    @Override
    public boolean add(E e) {
        int index = Collections.binarySearch(list, e);
        if (index < 0) {
            int insertionPoint = -(index + 1);
            list.add(insertionPoint, e);
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
        
        // TODO: this might be faster:
        //        ArrayList<E> newList = new ArrayList<E>();
        //        mergeSortedLists(list, other.list, newList);
        //        list = newList;
    }

    /** @inheritDoc */
    @Override
    public void clear() {
        list.clear();
    }

    /** @inheritDoc */
    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object o) {
        if (o instanceof Comparable) {
            return Collections.binarySearch(list, (E)o) >= 0;
        }
        return false;
    }

    /** @inheritDoc */
    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
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
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public boolean removeAll(Collection<?> c) {
        if (c instanceof SmallSet) {
            SmallSet other = (SmallSet)c;
            ArrayList<E> tmp = new ArrayList<E>(this.size() - other.size());
            Sort.diffSortedLists(this.list, other.list, tmp);
            boolean changed = (tmp.size() != this.list.size());
            this.list = tmp;
            return changed;
        } else {
            return list.removeAll(c);
        }
    }
    
    /** Gets a new SmallSet containing the difference of this set with the other. */
    public SmallSet<E> diff(SmallSet<E> other) {
        SmallSet<E> tmp = new SmallSet<E>(this.size() - other.size());
        Sort.diffSortedLists(this.list, other.list, tmp.list);
        return tmp;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((list == null) ? 0 : list.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SmallSet other = (SmallSet) obj;
        if (list == null) {
            if (other.list != null)
                return false;
        } else if (!list.equals(other.list))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "SmallSet [list=" + list + "]";
    }    

}
