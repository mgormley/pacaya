package edu.jhu.util;


import org.apache.commons.cli.CommandLine;


public class Command {

    private Command() {
        // private constructor
    }

    public static String getOptionValue(CommandLine cmd, String name, String defaultValue) {
        return cmd.hasOption(name) ? cmd.getOptionValue(name) : defaultValue;
    }

    public static int getOptionValue(CommandLine cmd, String name, int defaultValue) {
        return cmd.hasOption(name) ? Integer.parseInt(cmd.getOptionValue(name)) : defaultValue;
    }

    public static long getOptionValue(CommandLine cmd, String name, long defaultValue) {
        return cmd.hasOption(name) ? Long.parseLong(cmd.getOptionValue(name)) : defaultValue;
    }

    public static double getOptionValue(CommandLine cmd, String name, double defaultValue) {
        return cmd.hasOption(name) ? Double.parseDouble(cmd.getOptionValue(name)) : defaultValue;
    }

    public static boolean getOptionValue(CommandLine cmd, String name, boolean defaultValue) {
        return cmd.hasOption(name) ? Boolean.parseBoolean(cmd.getOptionValue(name)) : defaultValue;
    }

}
