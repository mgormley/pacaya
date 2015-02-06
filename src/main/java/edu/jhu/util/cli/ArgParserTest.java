package edu.jhu.util.cli;

import java.io.File;
import java.util.Date;

import org.apache.commons.cli.ParseException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests/example usage of ArgParser and Opt.
 * 
 * @author mgormley
 */
public class ArgParserTest {

    public enum MockEnum {
        OPT1, OPT2
    }
    
    @Opt(hasArg = true, description = "my intVal")
    public static int intVal = 1;

    @Opt(hasArg = true, description = "my doubleVal")
    public static double doubleVal = 1e10;

    @Opt(hasArg = true, description = "my boolArg")
    public static boolean boolArg = true;

    @Opt(hasArg = false, description = "my boolNoArg")
    public static boolean boolNoArg = false;

    @Opt(hasArg = true, description = "my strVal")
    public static String strVal = "1";

    @Opt(hasArg = true, description = "my fileVal")
    public static File fileVal = new File("1/1");
    
    @Opt(hasArg = true, description = "my enumVal")
    public static MockEnum enumVal = MockEnum.OPT1;
    
    @Opt(hasArg=true, description="Stop training by this date/time.")
    public static Date stopBy = null;
    
    @BeforeClass
    public static void setUp() {
    }

    @Test
    public void testArgParser() throws ParseException {
        {
            String[] args = "--intVal=2".split(" ");

            ArgParser parser = new ArgParser();
            parser.registerClass(ArgParserTest.class);
            parser.parseArgs(args);

            Assert.assertEquals(2, intVal);
            Assert.assertEquals(1e10, doubleVal, 1e-13);
        }
        {
            String[] args = "--intVal 3 --doubleVal=3e10".split(" ");

            ArgParser parser = new ArgParser();
            parser.registerClass(ArgParserTest.class);
            parser.parseArgs(args);

            Assert.assertEquals(3, intVal);
            Assert.assertEquals(3e10, doubleVal, 1e-13);
        }
        {
            String[] args = "--intVal 3e+06 --doubleVal=1E+06".split(" ");

            ArgParser parser = new ArgParser();
            parser.registerClass(ArgParserTest.class);
            parser.parseArgs(args);

            Assert.assertEquals(3000000, intVal);
            Assert.assertEquals(1e6, doubleVal, 1e-13);
        }
        {
            String[] args = "--strVal=4 --fileVal=4/4".split(" ");

            ArgParser parser = new ArgParser();
            parser.registerClass(ArgParserTest.class);
            parser.parseArgs(args);

            Assert.assertEquals("4", strVal);
            Assert.assertEquals(new File("4/4"), fileVal);
        }
        {
            String[] args = "--enumVal=OPT2".split(" ");

            ArgParser parser = new ArgParser();
            parser.registerClass(ArgParserTest.class);
            parser.parseArgs(args);

            Assert.assertEquals(MockEnum.OPT2, enumVal);
        }
    }
    
    @Test
    public void testDate() throws ParseException, java.text.ParseException {
        {
            String[] args = "--stopBy=01-10-14.06:00PM".split(" ");

            ArgParser parser = new ArgParser();
            parser.registerClass(ArgParserTest.class);
            parser.parseArgs(args);

            Assert.assertEquals("Fri Jan 10 18:00:00 EST 2014", stopBy.toString());
        }
        {
            String[] args = "--stopBy=01-10-14.06:22AM".split(" ");

            ArgParser parser = new ArgParser();
            parser.registerClass(ArgParserTest.class);
            parser.parseArgs(args);

            Assert.assertEquals("Fri Jan 10 06:22:00 EST 2014", stopBy.toString());
        }
    }

    @Test
    public void testHasArg() throws ParseException {
        String[] args = "--boolArg FALSE --boolNoArg".split(" ");

        ArgParser parser = new ArgParser();
        parser.registerClass(ArgParserTest.class);
        parser.parseArgs(args);

        Assert.assertEquals(false, boolArg);
        Assert.assertEquals(true, boolNoArg);
    }

    public static class RequiredOpts {

        @Opt(hasArg = true, description = "my requiredInt", required = true)
        public static int requiredInt = 1;

    }

    @Test
    public void testRequired() {
        {
            String[] args = "--requiredInt 2".split(" ");

            ArgParser parser = new ArgParser();
            parser.registerClass(RequiredOpts.class);
            try {
                parser.parseArgs(args);
            } catch (ParseException e) {
                Assert.fail();
            }

            Assert.assertEquals(2, RequiredOpts.requiredInt);
        }
        {
            String[] args = "".split(" ");

            ArgParser parser = new ArgParser();
            parser.registerClass(RequiredOpts.class);
            try {
                parser.parseArgs(args);
                Assert.fail();
            } catch (ParseException e) {
                // success
            }

        }
    }
    
    public static class NamedOpts {
        @Opt(hasArg = true, name = "actualName", description = "my named int", required = true)
        public static int originalName = 1;
    }

    @Test
    public void testName() {
        {
            String[] args = "--actualName 2".split(" ");
            ArgParser parser = new ArgParser();
            parser.registerClass(NamedOpts.class);
            try {
                parser.parseArgs(args);
            } catch (ParseException e) {
                Assert.fail();
            }
            Assert.assertEquals(2, NamedOpts.originalName);
        }
        {
            String[] args = "--originalName 2".split(" ");
            ArgParser parser = new ArgParser();
            parser.registerClass(RequiredOpts.class);
            try {
                parser.parseArgs(args);
                Assert.fail();
            } catch (ParseException e) {
                // success
            }
        }
    }

    @Test
    public void testUsage() throws ParseException {
        String[] args = "--intVal=2".split(" ");
        ArgParser parser = new ArgParser(ArgParserTest.class, true);
        parser.registerClass(ArgParserTest.class);
        parser.parseArgs(args);
        // This test isn't very robust. It just prints the usage and doesn't check anything.       
        parser.printUsage();
    }
    
    @Test
    public void testShortNames() throws ParseException {
        String[] args = "-sv 4 -fv 4/4".split(" ");
        ArgParser parser = new ArgParser(ArgParserTest.class, true);
        parser.registerClass(ArgParserTest.class);
        parser.parseArgs(args);
        Assert.assertEquals("4", strVal);
        Assert.assertEquals(new File("4/4"), fileVal);
    }
    
    public static class InstanceOpts {
        @Opt(hasArg = true, description = "my intVal")
        public int intValI = 1;
        @Opt(hasArg = true, description = "my doubleVal")
        public double doubleValI = 1e10;
        @Opt(hasArg = true, description = "my boolArg")
        public boolean boolArgI = true;
        @Opt(hasArg = false, description = "my boolNoArg")
        public boolean boolNoArgI = false;
        @Opt(hasArg = true, description = "my strVal")
        public String strValI = "1";
    }
    
    @Test
    public void testInstanceFactory() throws ParseException {
        String[] args = "--intValI=4 --doubleValI=0.3 --boolArgI=false --boolNoArgI".split(" ");
        ArgParser parser = new ArgParser(InstanceOpts.class, true);
        parser.registerClass(InstanceOpts.class);
        parser.parseArgs(args);
        InstanceOpts io = parser.getInstanceFromParsedArgs(InstanceOpts.class);
        Assert.assertEquals(4, io.intValI);
        Assert.assertEquals(0.3, io.doubleValI, 1e-13);
        Assert.assertEquals(false, io.boolArgI);
        Assert.assertEquals(true, io.boolNoArgI);
        // Not set by ArgParser construction.
        Assert.assertEquals("1", io.strValI);
    }
    
}
