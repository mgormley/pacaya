package edu.jhu.hltcoe.util.cli;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;

public class ArgParser {

    private static final Logger log = Logger.getLogger(ArgParser.class);

    private Options options;
    private Map<Option, Field> optionFieldMap;
    private Class<?> mainClass = null;

    public ArgParser() {
        this(null);
    }
    
    public ArgParser(Class<?> mainClass) {
        options = new Options();
        optionFieldMap = new HashMap<Option, Field>();
        this.mainClass = mainClass;  
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

                    Option apacheOpt = new Option(name, name, option.hasArg(), option.description());
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
        try {
            CommandLineParser parser = new PosixParser();
            cmd = parser.parse(options, args);
        } catch (ParseException e1) {
            log.error(e1.getMessage());
            printUsage();
            throw e1;
        }

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
        formatter.printHelp(usage, options, true);
    }

    @SuppressWarnings("unchecked")
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
            field.setInt(null, Integer.parseInt(value));
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
}
