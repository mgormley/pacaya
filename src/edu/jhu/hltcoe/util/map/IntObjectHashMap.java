package edu.jhu.hltcoe.util.map;

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
    
    public void clear() {
        map.clear();
    }

    public boolean containsKey(int key) {
        return map.containsKey(key);
    }

    public boolean containsValue(T value) {
        return map.containsValue(value);
    }

    @SuppressWarnings("unchecked")
    public final T get(int key) {
        return (T) map.get(key);
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean put(int key, T value) {
        return map.put(key, value);
    }

    public boolean removeKey(int key) {
        return map.removeKey(key);
    }

    public int size() {
        return map.size();
    }

    public ObjectArrayList values() {
        return map.values();
    }
    
    public int[] keys() {
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
