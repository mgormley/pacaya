package edu.jhu.hltcoe.util.rproj;

import java.util.LinkedHashMap;
import java.util.Set;

public class RRow {

    LinkedHashMap<String,Object> map = new LinkedHashMap<String,Object>();
        
    public void put(String rowName, double value) {
        map.put(rowName, value);
    }

    public Object get(String colName) {
        Object val = map.get(colName);
        if (val == null) {
            return "";
        }
        return val;
    }
    
    public Set<String> getColNames() {
        return map.keySet();
    }

}
