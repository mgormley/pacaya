package edu.jhu.util;

import java.util.ArrayList;

public class GobbleMemoryTest {

    /**
     * @param args
     */
    public static void main(String[] args) {        
        ArrayList<byte[]> list = new ArrayList<byte[]>();
        while(true) {
            list.add(new byte[1000 * 1000]);
        }
    }

}
