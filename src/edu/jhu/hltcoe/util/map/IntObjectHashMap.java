package edu.jhu.hltcoe.util.map;

import cern.colt.list.ObjectArrayList;
import cern.colt.map.OpenIntObjectHashMap;


public class IntObjectHashMap<T> {

    private OpenIntObjectHashMap map;
    
    public IntObjectHashMap() {
        map = new OpenIntObjectHashMap();
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
    public T get(int key) {
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
    
}
