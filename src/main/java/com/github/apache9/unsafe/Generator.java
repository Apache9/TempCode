package com.github.apache9.unsafe;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Generator {

    private static final Pattern METHOD = Pattern.compile("public (final )?(native )?(\\S+)\\s+(\\S+)\\((.*)\\)");

    public static void main(String[] args) throws Exception {
        try (PrintWriter pw = new PrintWriter("/home/zhangduo/hbase/HBaseUnsafe0")) {
            for (String line : Files.readAllLines(Paths.get("/home/zhangduo/hbase/all_methods"))) {
                Matcher m = METHOD.matcher(line);
                if (m.find()) {
                    String returnType = m.group(3);
                    String name = m.group(4);
                    String params = m.group(5);
                    pw.println("public static " + returnType + " " + name + "(" + params + ") {");
                    if (!returnType.equals("void")) {
                        pw.print("return ");
                    }
                    pw.print("HBaseUnsafe0." + name + "(");
                    if (params.trim().length() > 0) {
                        String[] typeAndNames = params.split(",\\s*");
                        String str = Stream.of(typeAndNames).map(typeAndName -> typeAndName.split("\\s+")[1])
                                .collect(Collectors.joining(", "));
                        pw.print(str);
                    }
                    pw.println(");");
                    pw.println("}");
                    pw.println();
                } else {
                    System.out.println("error: " + line);
                }
            }
        }
    }
}
