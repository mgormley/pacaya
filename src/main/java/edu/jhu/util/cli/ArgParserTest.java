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
            parser.addClass(ArgParserTest.class);
            parser.parseArgs(args);

            Assert.assertEquals(2, intVal);
            Assert.assertEquals(1e10, doubleVal, 1e-13);
        }
        {
            String[] args = "--intVal 3 --doubleVal=3e10".split(" ");

            ArgParser parser = new ArgParser();
            parser.addClass(ArgParserTest.class);
            parser.parseArgs(args);

            Assert.assertEquals(3, intVal);
            Assert.assertEquals(3e10, doubleVal, 1e-13);
        }
        {
            String[] args = "--intVal 3e+06 --doubleVal=1E+06".split(" ");

            ArgParser parser = new ArgParser();
            parser.addClass(ArgParserTest.class);
            parser.parseArgs(args);

            Assert.assertEquals(3000000, intVal);
            Assert.assertEquals(1e6, doubleVal, 1e-13);
        }
        {
            String[] args = "--strVal=4 --fileVal=4/4".split(" ");

            ArgParser parser = new ArgParser();
            parser.addClass(ArgParserTest.class);
            parser.parseArgs(args);

            Assert.assertEquals("4", strVal);
            Assert.assertEquals(new File("4/4"), fileVal);
        }
        {
            String[] args = "--enumVal=OPT2".split(" ");

            ArgParser parser = new ArgParser();
            parser.addClass(ArgParserTest.class);
            parser.parseArgs(args);

            Assert.assertEquals(MockEnum.OPT2, enumVal);
        }
    }
    
    @Test
    public void testDate() throws ParseException, java.text.ParseException {
        {
            String[] args = "--stopBy=01-10-14.06:00PM".split(" ");

            ArgParser parser = new ArgParser();
            parser.addClass(ArgParserTest.class);
            parser.parseArgs(args);

            Assert.assertEquals("Fri Jan 10 18:00:00 EST 2014", stopBy.toString());
        }
        {
            String[] args = "--stopBy=01-10-14.06:22AM".split(" ");

            ArgParser parser = new ArgParser();
            parser.addClass(ArgParserTest.class);
            parser.parseArgs(args);

            Assert.assertEquals("Fri Jan 10 06:22:00 EST 2014", stopBy.toString());
        }
    }

    @Test
    public void testHasArg() throws ParseException {
        String[] args = "--boolArg FALSE --boolNoArg".split(" ");

        ArgParser parser = new ArgParser();
        parser.addClass(ArgParserTest.class);
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
            parser.addClass(RequiredOpts.class);
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
            parser.addClass(RequiredOpts.class);
            try {
                parser.parseArgs(args);
                Assert.fail();
            } catch (ParseException e) {
                // success
            }

        }
    }

    public void testName() {
        // TODO: write this test.
    }

    public void testUsage() {
        // TODO: write this test.
    }
}
