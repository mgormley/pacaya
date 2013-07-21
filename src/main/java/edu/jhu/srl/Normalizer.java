package edu.jhu.srl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * For normalization and escaping of word character sequences.
 * 
 * @author mmitchell
 * @author mgormley
 */
public class Normalizer {

    static {
        setChars();
    }
    
    private static final Pattern digits = Pattern.compile("\\d+");
    private static final Pattern punct = Pattern.compile("[^A-Za-z0-9_çƒêîò†„‡Ž’—œŸ–]");
    private static Map<String, String> stringMap;

    private boolean isOn;
    
    public Normalizer() {
        this.isOn = true;
    }

    public Normalizer(boolean normalize) {
        this.isOn = normalize;
    }

    public String escape(String s) {
        if (!isOn) { return s; }

        Iterator<Entry<String, String>> it = stringMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            String key = (String) entry.getKey();
            String val = (String) entry.getValue();
            s.replace(key, val);
        }
        return punct.matcher(s).replaceAll("_");
    }

    public String norm_digits(String s) {
        if (!isOn) { return s; }
        return digits.matcher(s).replaceAll("0");
    }

    public String clean(String s) {
        if (!isOn) { return s; }
        s = escape(s);
        s = norm_digits(s.toLowerCase());
        return s;
    }

    private static void setChars() {
        // Really this would be nicer using guava...
        stringMap = new HashMap<String, String>();
        stringMap.put("1", "2");
        stringMap.put(".", "_P_");
        stringMap.put(",", "_C_");
        stringMap.put("'", "_A_");
        stringMap.put("%", "_PCT_");
        stringMap.put("-", "_DASH_");
        stringMap.put("$", "_DOL_");
        stringMap.put("&", "_AMP_");
        stringMap.put(":", "_COL_");
        stringMap.put(";", "_SCOL_");
        stringMap.put("\\/", "_BSL_");
        stringMap.put("/", "_SL_");
        stringMap.put("`", "_QT_");
        stringMap.put("?", "_Q_");
        stringMap.put("À", "_QQ_");
        stringMap.put("=", "_EQ_");
        stringMap.put("*", "_ST_");
        stringMap.put("!", "_E_");
        stringMap.put("Á", "_EE_");
        stringMap.put("#", "_HSH_");
        stringMap.put("@", "_AT_");
        stringMap.put("(", "_LBR_");
        stringMap.put(")", "_RBR_");
        stringMap.put("\"", "_QT1_");
        stringMap.put("ç", "_A_ACNT_");
        stringMap.put("ƒ", "_E_ACNT_");
        stringMap.put("ê", "_I_ACNT_");
        stringMap.put("î", "_O_ACNT_");
        stringMap.put("ò", "_U_ACNT_");
        stringMap.put("†", "_U_ACNT2_");
        stringMap.put("„", "_N_ACNT_");
        stringMap.put("‡", "_a_ACNT_");
        stringMap.put("Ž", "_e_ACNT_");
        stringMap.put("’", "_i_ACNT_");
        stringMap.put("—", "_o_ACNT_");
        stringMap.put("œ", "_u_ACNT_");
        stringMap.put("Ÿ", "_u_ACNT2_");
        stringMap.put("–", "_n_ACNT_");
        stringMap.put("¼", "_deg_ACNT_");

    }
}