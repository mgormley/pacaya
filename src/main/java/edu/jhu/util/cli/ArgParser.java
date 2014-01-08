package edu.jhu.util.cli;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;

import edu.jhu.prim.util.SafeCast;

/**
 * Command line argument parser.
 * 
 * @author mgormley
 */
public class ArgParser {

    private static final Logger log = Logger.getLogger(ArgParser.class);
    private static final Pattern capitals = Pattern.compile("[A-Z]");
    
    private Options options;
    private Map<Option, Field> optionFieldMap;
    private Class<?> mainClass = null;
    private Set<String> names;
    private Set<String> shortNames;
    private boolean createShortNames;
    
    public ArgParser() {
        this(null);
    }
    
    public ArgParser(Class<?> mainClass) {
        this(mainClass, false);
    }
    
    public ArgParser(Class<?> mainClass, boolean createShortNames) {
        options = new Options();
        optionFieldMap = new HashMap<Option, Field>();
        this.mainClass = mainClass;
        names = new HashSet<String>();
        shortNames = new HashSet<String>();
        this.createShortNames = createShortNames;
    }

    public Options getOptions() {
        return options;
    }

    public void addClass(Class<?> clazz) {
        for (Field field : clazz.getFields()) {
            if (field.isAnnotationPresent(Opt.class)) {
                int mod = field.getModifiers();
                if (Modifier.isPublic(mod) && Modifier.isStatic(mod) && !Modifier.isFinal(mod)
                        && !Modifier.isAbstract(mod)) {
                    // Add an option for this field.
                    Opt option = field.getAnnotation(Opt.class);

                    String name = getName(option, field);
                    if (!names.add(name)) {
                        throw new RuntimeException("Multiple options have the same name: --" + name);
                    }

                    String shortName = null;
                    if (createShortNames) {
                        shortName = getAndAddUniqueShortName(name);
                    }
                    
                    Option apacheOpt = new Option(shortName, name, option.hasArg(), option.description());                    
                    apacheOpt.setRequired(option.required());
                    options.addOption(apacheOpt);

                    optionFieldMap.put(apacheOpt, field);

                    // Check that only boolean has hasArg() == false.
                    if (!field.getType().equals(Boolean.TYPE) && !option.hasArg()) {
                        throw new RuntimeException("Only booleans can not have arguments.");
                    }
                }
            }
        }
    }

    private String getAndAddUniqueShortName(String name) {
        // Capitalize the first letter of the name.
        String initCappedName = name.substring(0, 1).toUpperCase() + name.substring(1, name.length());
        // Make the short name all the capital letters in the long name.
        Matcher matcher = capitals.matcher(initCappedName);
        StringBuilder sbShortName = new StringBuilder();
        while(matcher.find()) {
            sbShortName.append(matcher.group());
        }
        String shortName = sbShortName.toString().toLowerCase();

        if (!shortNames.add(shortName)) {
            int i;
            for (i=0; i<10; i++) {
                String tmpsn = shortName + i;
                if (shortNames.add(tmpsn)) {
                    shortName = tmpsn;
                    break;
                }
            }
            if (i == 10) {
                throw new RuntimeException("Multiple options have the same short name: -" + shortName  + " (--" + name + ")");
            }
        }
        return shortName;
    }

    public static String getName(Opt option, Field field) {
        if (option.name().equals(Opt.DEFAULT_STRING)) {
            return field.getName();
        } else {
            return option.name();
        }
    }

    public CommandLine parseArgs(String[] args) throws ParseException {
        // Parse command line.
        CommandLine cmd = null;
        CommandLineParser parser = new PosixParser();
        cmd = parser.parse(options, args);

        // Set fields on added classes.
        try {
            for (Option option : optionFieldMap.keySet()) {
                Field field = optionFieldMap.get(option);
                if (cmd.hasOption(option.getLongOpt())) {
                    if (option.hasArg()) {
                        setStaticField(field, cmd.getOptionValue(option.getLongOpt()));
                    } else {
                        // We should have already checked that the field is a
                        // boolean.
                        field.setBoolean(null, true);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        
        return cmd;
    }

    public void printUsage() {        
        String name = mainClass == null ? "<MainClass>" : mainClass.getName();
        String usage = "java " + name + " [OPTIONS]";
        final HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(120);
        formatter.printHelp(usage, options, true);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void setStaticField(Field field, String value) throws IllegalArgumentException,
            IllegalAccessException {
        Class<?> type = field.getType();
        if (type.equals(Boolean.TYPE)) {
            field.setBoolean(null, Boolean.parseBoolean(value));
        } else if (type.equals(Byte.TYPE)) {
            field.setByte(null, Byte.parseByte(value));
        } else if (type.equals(Character.TYPE)) {
            field.setChar(null, parseChar(value));
        } else if (type.equals(Double.TYPE)) {
            field.setDouble(null, Double.parseDouble(value));
        } else if (type.equals(Float.TYPE)) {
            field.setFloat(null, Float.parseFloat(value));
        } else if (type.equals(Integer.TYPE)) {
            field.setInt(null, safeStrToInt(value));
        } else if (type.equals(Long.TYPE)) {
            field.setLong(null, Long.parseLong(value));
        } else if (type.equals(Short.TYPE)) {
            field.setShort(null, Short.parseShort(value));
        } else if (type.isEnum()) {
            field.set(null, Enum.valueOf((Class<Enum>) field.getType(), value));
        } else if (type.equals(String.class)) {
            field.set(field, value);
        } else if (type.equals(File.class)) {
            field.set(field, new File(value));
        } else if (type.equals(Date.class)) {
            DateFormat df = new SimpleDateFormat("MM-dd-yy.hh:mma");
            try {
                field.set(field, df.parse(value));
            } catch (java.text.ParseException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("Field type not supported: " + type.getName());
        }
    }

    public static char parseChar(String value) {
        if (value.length() != 1) {
            throw new IllegalArgumentException("value cannot be converted to a char: " + value);
        }
        return value.charAt(0);
    }


    private static final Pattern sciInt = Pattern.compile("^\\d+\\.?[e|E][+-]?\\d+$");
    /** Correctly casts 1e+06 to 1000000. */
    public static int safeStrToInt(String str) {
        if (sciInt.matcher(str).find()) {
            return SafeCast.safeDoubleToInt(Double.parseDouble(str.toUpperCase()));
        } else {
            return Integer.parseInt(str);
        }
    }
    
}
