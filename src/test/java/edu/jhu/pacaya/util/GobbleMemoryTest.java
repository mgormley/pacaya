package edu.jhu.pacaya.util;

import java.util.ArrayList;

import org.apache.commons.cli.ParseException;

import edu.jhu.pacaya.util.cli.ArgParser;
import edu.jhu.pacaya.util.cli.Opt;

public class GobbleMemoryTest {

    @Opt(hasArg = true, description = "The number of megabytes to Gobble.")
    public static int megsToGobble = Integer.MAX_VALUE;


    private static void gobble() {
        ArrayList<byte[]> list = new ArrayList<byte[]>();
        for (int i=0; i<megsToGobble; i++) {
            list.add(new byte[1000 * 1000]);
        }
    }
    
    public static void main(String[] args) {     
        try {
            ArgParser parser = new ArgParser(GobbleMemoryTest.class);
            parser.registerClass(GobbleMemoryTest.class);
            try {
                parser.parseArgs(args);
            } catch (ParseException e) {
                System.err.println(e.getMessage());
                parser.printUsage();
                System.exit(1);
            }

            gobble();
            
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
        
    }

}
