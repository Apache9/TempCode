package com.github.apache9.log4j2;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zhangduo
 */
public class HadoopLog4j1ConfigurationConverter {

    private static final Pattern FILE_NAME_PATTERN = Pattern.compile(".*log4j.*\\.properties");

    private static final Pattern COMMENT = Pattern.compile("^\\s*#.*");

    private static final Pattern COMMENT_CONFIG = Pattern.compile("^\\s*#\\s*(log4j\\..+|.+=.+)");

    private static String canNotConvert(String v) {
        return "{{{{" + v + "}}}}";
    }

    private static void println(PrintWriter writer, String line, boolean inComment) {
        if (inComment) {
            writer.print("#");
        }
        writer.println(line);
    }

    enum AppenderType {
        NULL, CONSOLE, ROLLING, DAILY_ROLLING, CUSTOMIZED
    }

    private static void convertConsole(PrintWriter writer, String name, String property, String value,
            boolean inComment) {
        if (!property.equalsIgnoreCase("target")) {
            throw new RuntimeException("Unknown console appender property: " + property);
        }
        switch (value) {
        case "System.out":
            println(writer, "appender." + name + ".target = SYSTEM_OUT", inComment);
            break;
        case "System.err":
            println(writer, "appender." + name + ".target = SYSTEM_ERR", inComment);
            break;
        default:
            throw new RuntimeException("Unknown console appender target: " + value);
        }
    }

    private static void convertRolling(PrintWriter writer, String name, String property, String value,
            boolean inComment) {
        switch (property) {
        case "File":
            println(writer, "appender." + name + ".fileName = " + value, inComment);
            println(writer, "appender." + name + ".filePattern = " + value + ".%i", inComment);
            break;
        case "MaxFileSize":
            println(writer, "appender." + name + ".policies.type = Policies", inComment);
            println(writer, "appender." + name + ".policies.size.type = SizeBasedTriggeringPolicy", inComment);
            println(writer, "appender." + name + ".policies.size.size = " + value, inComment);
            break;
        case "MaxBackupIndex":
            println(writer, "appender." + name + ".strategy.type = DefaultRolloverStrategy", inComment);
            println(writer, "appender." + name + ".strategy.max = " + value, inComment);
            break;
        default:
            throw new RuntimeException("Unknown rolling file appender property: " + property);
        }
    }

    private static void convertDailyRolling(PrintWriter writer, String name, String property, String value,
            boolean inComment) {
        switch (property) {
        case "File":
            println(writer, "appender." + name + ".fileName = " + value, inComment);
            break;
        case "DatePattern":
            if (value.equals(".yyyy-MM-dd") || value.equals("'.'yyyy-MM-dd'.log'")) {
                // handle the rolling per day specially
                println(writer, "appender." + name + ".filePattern = " + value + ".%d{yyyy-MM-dd}", inComment);
                println(writer, "appender." + name + ".policies.type = Policies", inComment);
                println(writer, "appender." + name + ".policies.time.type = TimeBasedTriggeringPolicy", inComment);
                println(writer, "appender." + name + ".policies.time.interval = 1", inComment);
                println(writer, "appender." + name + ".policies.time.modulate = true", inComment);
                break;
            }
            println(writer, "appender." + name + "." + property + " = " + canNotConvert(value), inComment);
            break;
        default:
            throw new RuntimeException("Unknown daily rolling file appender property: " + property);
        }
    }

    private static void convertAppender(PrintWriter writer, String key, String value,
            Map<String, AppenderType> appenderTypes, boolean inComment) {
        String nameAndProperty = key.substring("log4j.appender.".length());
        if (!nameAndProperty.contains(".")) {
            switch (value) {
            case "org.apache.log4j.varia.NullAppender":
                appenderTypes.put(nameAndProperty, AppenderType.NULL);
                println(writer, "appender." + nameAndProperty + ".name = " + nameAndProperty, inComment);
                println(writer, "appender." + nameAndProperty + ".type = Null", inComment);
                break;
            case "org.apache.log4j.ConsoleAppender":
                appenderTypes.put(nameAndProperty, AppenderType.CONSOLE);
                println(writer, "appender." + nameAndProperty + ".name = " + nameAndProperty, inComment);
                println(writer, "appender." + nameAndProperty + ".type = Console", inComment);
                break;
            case "org.apache.log4j.RollingFileAppender":
                appenderTypes.put(nameAndProperty, AppenderType.ROLLING);
                println(writer, "appender." + nameAndProperty + ".name = " + nameAndProperty, inComment);
                println(writer, "appender." + nameAndProperty + ".type = RollingFile", inComment);
                break;
            case "org.apache.log4j.DailyRollingFileAppender":
                appenderTypes.put(nameAndProperty, AppenderType.DAILY_ROLLING);
                println(writer, "appender." + nameAndProperty + ".name = " + nameAndProperty, inComment);
                println(writer, "appender." + nameAndProperty + ".type = RollingFile", inComment);
                break;
            default:
                appenderTypes.put(nameAndProperty, AppenderType.CUSTOMIZED);
                println(writer, "appender." + nameAndProperty + ".name = " + nameAndProperty, inComment);
                println(writer, "appender." + nameAndProperty + ".type = " + canNotConvert(value), inComment);
                break;
            }
            return;
        }
        int indexOfDot = nameAndProperty.indexOf('.');
        String name = nameAndProperty.substring(0, indexOfDot);
        String property = nameAndProperty.substring(indexOfDot + 1);
        AppenderType type = appenderTypes.get(name);
        if (type == null) {
            throw new RuntimeException("Unknown appender: " + key);
        }
        switch(property) {
        case "layout":
            if (value.equals("org.apache.log4j.PatternLayout")) {
                println(writer, "appender." + name + ".layout.type = PatternLayout", inComment);
            } else {
                throw new RuntimeException("Unknown layout type: " + value);
            }
            return;
        case "layout.ConversionPattern":
            println(writer, "appender." + name + ".layout.pattern = " + value, inComment);
            return;
        case "Append":
            println(writer, "appender." + name + ".append = " + value, inComment);
            return;
        case "Encoding":
            println(writer, "appender." + name + ".layout.charset = " + value, inComment);
            return;
        case "ImmediateFlush":
            println(writer, "appender." + name + ".immediateFlush = " + value, inComment);
            return;
        }
        switch (type) {
        case NULL:
            throw new RuntimeException("Unknown null appender property: " + key);
        case CONSOLE:
            convertConsole(writer, name, property, value, inComment);
            break;
        case ROLLING:
            convertRolling(writer, name, property, value, inComment);
            break;
        case DAILY_ROLLING:
            convertDailyRolling(writer, name, property, value, inComment);
            break;
        case CUSTOMIZED:
            println(writer, "appender." + nameAndProperty + " = " + canNotConvert(value), inComment);
            break;
        }
    }

    private static void convert(Path file) throws IOException {
        Path outputFile = Paths.get(file.getParent().toString(),
                file.getFileName().toString().replace("log4j", "log4j2"));
        System.out.println("convert " + file + " to " + outputFile);
        Map<String, AppenderType> appenderTypes = new HashMap<>();
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outputFile))) {
            for (String line : Files.readAllLines(file)) {
                boolean inComment = false;
                if (line.trim().length() == 0) {
                    println(writer, line, inComment);
                    continue;
                }
                if (COMMENT.matcher(line).matches()) {
                    Matcher matcher = COMMENT_CONFIG.matcher(line);
                    if (matcher.matches()) {
                        line = matcher.group(1);
                        inComment = true;
                    } else {
                        println(writer, line, inComment);
                        continue;
                    }
                }
                line = line.trim();
                int indexOfEqual = line.indexOf('=');
                String key = line.substring(0, indexOfEqual).trim();
                String value = line.substring(indexOfEqual + 1).trim();
                if (!key.startsWith("log4j")) {
                    // should be a property
                    println(writer, "property." + key + " = ${sys:" + key + ":-" + value + "}", inComment);
                    continue;
                }
                // typo in our log4j.properties...
                if (key.equals("log4j.threshold") || key.equals("log4j.threshhold")) {
                    println(writer, "filter.threshold.type = ThresholdFilter", inComment);
                    println(writer, "filter.threshold.level = " + value, inComment);
                    continue;
                }
                // log4j.logger is invalid, actually...
                if (key.equals("log4j.rootLogger") || key.equals("log4j.logger")) {
                    println(writer, "rootLogger = " + value, inComment);
                    continue;
                }
                if (key.startsWith("log4j.category.")) {
                    String loggerName = key.substring("log4j.category.".length());
                    println(writer, "logger." + loggerName + ".name = " + loggerName, inComment);
                    println(writer, "logger." + loggerName + " = " + value, inComment);
                    continue;
                }
                if (key.startsWith("log4j.logger.")) {
                    String loggerName = key.substring("log4j.logger.".length());
                    println(writer, "logger." + loggerName + ".name = " + loggerName, inComment);
                    println(writer, "logger." + loggerName + " = " + value, inComment);
                    continue;
                }
                if (key.startsWith("log4j.additivity.")) {
                    String loggerName = key.substring("log4j.additivity.".length());
                    println(writer, "logger." + loggerName + ".additivity = " + value, inComment);
                    continue;
                }
                if (!key.startsWith("log4j.appender.")) {
                    throw new RuntimeException("Can not parse line: " + line);
                }
                convertAppender(writer, key, value, appenderTypes, inComment);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Files.walkFileTree(Paths.get("/home/zhangduo/hadoop/code"), new FileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileName = file.getFileName().toString();
                if (FILE_NAME_PATTERN.matcher(fileName).matches()) {
                    convert(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
