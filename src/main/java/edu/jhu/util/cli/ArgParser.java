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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.prim.util.SafeCast;

/**
 * Command line argument parser.
 * 
 * @author mgormley
 */
public class ArgParser {

    private static final Logger log = LoggerFactory.getLogger(ArgParser.class);
    private static final Pattern capitals = Pattern.compile("[A-Z]");
    
    private Options options;
    private Map<Option, Field> optionFieldMap;
    private Map<Field, String> fieldValueMap;
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

    /** Registers all the @Opt annotations on a class with this ArgParser. */
    public void registerClass(Class<?> clazz) {
        for (Field field : clazz.getFields()) {
            if (field.isAnnotationPresent(Opt.class)) {
                int mod = field.getModifiers();
                if (!Modifier.isPublic(mod)) { throw new IllegalStateException("@"+Opt.class.getName()+" on non-public field: " + field); }
                if (Modifier.isFinal(mod)) { throw new IllegalStateException("@"+Opt.class.getName()+" on final field: " + field); }
                if (Modifier.isAbstract(mod)) { throw new IllegalStateException("@"+Opt.class.getName()+" on abstract field: " + field); }

                // Add an Apache Commons CLI Option for this field.
                Opt opt = field.getAnnotation(Opt.class);

                String name = getName(opt, field);
                if (!names.add(name)) {
                    throw new RuntimeException("Multiple options have the same name: --" + name);
                }

                String shortName = null;
                if (createShortNames) {
                    shortName = getAndAddUniqueShortName(name);
                }
                
                Option apacheOpt = new Option(shortName, name, opt.hasArg(), opt.description());                    
                apacheOpt.setRequired(opt.required());
                options.addOption(apacheOpt);

                optionFieldMap.put(apacheOpt, field);

                // Check that only boolean has hasArg() == false.
                if (!field.getType().equals(Boolean.TYPE) && !opt.hasArg()) {
                    throw new RuntimeException("Only booleans can not have arguments.");
                }
            }
        }
    }

    /**
     * Creates a unique short name from the name of a field. For example, the field "trainPredOut"
     * would have a long name of "--trainPredOut" and a short name of "-tpo".
     */
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

    /** Gets the name specified in @Opt(name=...) if present, or the name of the field otherwise. */
    private static String getName(Opt option, Field field) {
        if (option.name().equals(Opt.DEFAULT_STRING)) {
            return field.getName();
        } else {
            return option.name();
        }
    }

    /**
     * Parses the command line arguments and sets any of public static fields annotated with @Opt
     * that have been registered with this {@link ArgParser} via {@link #registerClass(Class)}.
     */
    public void parseArgs(String[] args) throws ParseException {
        // Parse command line.
        CommandLine cmd = null;
        CommandLineParser parser = new PosixParser();
        cmd = parser.parse(options, args);

        fieldValueMap = new HashMap<>();
        try {
            for (Option apacheOpt : optionFieldMap.keySet()) {
                Field field = optionFieldMap.get(apacheOpt);
                if (cmd.hasOption(apacheOpt.getLongOpt())) {
                    String value = apacheOpt.hasArg() ? cmd.getOptionValue(apacheOpt.getLongOpt()) : "true";
                    if (Modifier.isStatic(field.getModifiers())) {
                        // For static fields, set the value directly to the field.
                        setStaticField(field, cmd.getOptionValue(apacheOpt.getLongOpt()));
                    } else {
                        // For non-static fields, cache the value for later use by getInstanceFromParsedArgs(). 
                        fieldValueMap.put(field, value);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets an instance of the given class by using the no-argument constructor (which must exist)
     * and then setting all fields annotated with @Opt with values cached by {@link #parseArgs(String[])}.
     */
    public <T> T getInstanceFromParsedArgs(Class<T> clazz) {
        try {
            T obj = clazz.newInstance();
            for (Field field : clazz.getFields()) {
                if (field.isAnnotationPresent(Opt.class)) {
                    String value = fieldValueMap.get(field);
                    if (value != null) {
                        setField(obj, field, value);
                    }
                }
            }
            return obj;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    /** Prints the usage of the main class for this ArgParser. */ 
    public void printUsage() {
        String name = mainClass == null ? "<MainClass>" : mainClass.getName();
        String usage = "java " + name + " [OPTIONS]";
        final HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(120);
        formatter.printHelp(usage, options, true);
    }

    private static void setStaticField(Field field, String value) throws IllegalArgumentException,
            IllegalAccessException {
        setField(null, field, value);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void setField(Object obj, Field field, String value) throws IllegalArgumentException,
            IllegalAccessException {
        Class<?> type = field.getType();
        if (type.equals(Boolean.TYPE)) {
            field.setBoolean(obj, Boolean.parseBoolean(value));
        } else if (type.equals(Byte.TYPE)) {
            field.setByte(obj, Byte.parseByte(value));
        } else if (type.equals(Character.TYPE)) {
            field.setChar(obj, parseChar(value));
        } else if (type.equals(Double.TYPE)) {
            field.setDouble(obj, Double.parseDouble(value));
        } else if (type.equals(Float.TYPE)) {
            field.setFloat(obj, Float.parseFloat(value));
        } else if (type.equals(Integer.TYPE)) {
            field.setInt(obj, safeStrToInt(value));
        } else if (type.equals(Long.TYPE)) {
            field.setLong(obj, Long.parseLong(value));
        } else if (type.equals(Short.TYPE)) {
            field.setShort(obj, Short.parseShort(value));
        } else if (type.isEnum()) {
            field.set(obj, Enum.valueOf((Class<Enum>) field.getType(), value));
        } else if (type.equals(String.class)) {
            field.set(obj, value);
        } else if (type.equals(File.class)) {
            field.set(obj, new File(value));
        } else if (type.equals(Date.class)) {
            DateFormat df = new SimpleDateFormat("MM-dd-yy.hh:mma");
            try {
                field.set(obj, df.parse(value));
            } catch (java.text.ParseException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("Field type not supported: " + type.getName());
        }
    }

    private static char parseChar(String value) {
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
