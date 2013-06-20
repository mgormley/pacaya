package edu.jhu.util.map;

import cern.colt.function.IntProcedure;
import cern.colt.list.ObjectArrayList;
import cern.colt.map.OpenIntObjectHashMap;


public class IntObjectHashMap<T> {

    private OpenIntObjectHashMap map;
    
    public IntObjectHashMap() {
        map = new OpenIntObjectHashMap();
    }
    
    public IntObjectHashMap(IntObjectHashMap<T> other) {
        map = (OpenIntObjectHashMap)other.map.clone();
    }
    
    public final void clear() {
        map.clear();
    }

    public final boolean containsKey(int key) {
        return map.containsKey(key);
    }

    public final boolean containsValue(T value) {
        return map.containsValue(value);
    }

    @SuppressWarnings("unchecked")
    public final T get(int key) {
        return (T) map.get(key);
    }

    public final boolean isEmpty() {
        return map.isEmpty();
    }

    public final boolean put(int key, T value) {
        return map.put(key, value);
    }

    public final boolean removeKey(int key) {
        return map.removeKey(key);
    }

    public final int size() {
        return map.size();
    }

    public final ObjectArrayList values() {
        return map.values();
    }
    
    public final int[] keys() {
        final int[] keys = new int[map.size()];
        map.forEachKey(new IntProcedure() {
            int i = 0;
            @Override
            public boolean apply(int element) {
                keys[i++] = element;
                return true;
            }
        });
        return keys;
    }
    
    @Override
    public Object clone() {
        IntObjectHashMap<T> clone = new IntObjectHashMap<T>(); // (IntObjectHashMap<T>) super.clone();
        clone.map = (OpenIntObjectHashMap)map.clone();
        return clone;
    }
    
}
