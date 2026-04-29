package com.q3lives.compiler.tools;

import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * DocGenerator - Claw Library Documentation Generator
 */
public class DocGenerator {

    public static void main(String[] args) throws Exception {
        String input = args.length > 0 ? args[0] : "src/main/resources/std";
        run(input);
    }

    public static void run(String input) throws Exception {
        var sb = new StringBuilder();
        var path = Paths.get(input);

        // Collect all .claw files
        if (Files.isDirectory(path)) {
            try (var files = Files.walk(path)) {
                files.filter(f -> f.toString().endsWith(".claw"))
                    .sorted()
                    .forEach(f -> {
                        try { sb.append(Files.readString(f)).append("\n"); }
                        catch (Exception e) {}
                    });
            }
        } else {
            sb.append(Files.readString(path));
        }

        String source = sb.toString();

        // Output documentation
        System.out.println("# Claw Standard Library");
        System.out.println();

        // Modules
        var modules = extractModules(source);
        if (!modules.isEmpty()) {
            System.out.println("## Modules");
            System.out.println();
            for (String m : modules) {
                System.out.println("- **" + m + "**");
            }
            System.out.println();
        }

        // Types
        var types = extractTypes(source);
        if (!types.isEmpty()) {
            System.out.println("## Types");
            System.out.println();
            for (TypeInfo t : types) {
                System.out.println("- **" + t.name + "**");
            }
            System.out.println();
        }

        // Functions
        var funcs = extractFunctions(source);
        if (!funcs.isEmpty()) {
            System.out.println("## Functions");
            System.out.println();
            for (FunctionInfo f : funcs) {
                System.out.println("- **" + f.name + "**(" + f.params + ") -> " + f.returnType);
            }
        }
    }

    private static List<String> extractModules(String source) {
        var modules = new ArrayList<String>();
        var m = Pattern.compile("(?m)^module\\s+(\\S+)").matcher(source);
        while (m.find()) modules.add(m.group(1));
        return modules;
    }

    private static List<TypeInfo> extractTypes(String source) {
        var types = new ArrayList<TypeInfo>();
        var m = Pattern.compile("(?m)^type\\s+(\\w+)").matcher(source);
        while (m.find()) {
            TypeInfo info = new TypeInfo();
            info.name = m.group(1);
            types.add(info);
        }
        return types;
    }

    private static List<FunctionInfo> extractFunctions(String source) {
        var funcs = new ArrayList<FunctionInfo>();
        // Match: function name(params) -> ReturnType
        var m = Pattern.compile("function\\s+(\\w+)\\s*\\(").matcher(source);
        while (m.find()) {
            FunctionInfo info = new FunctionInfo();
            info.name = m.group(1);
            funcs.add(info);
        }
        return funcs;
    }

    static class TypeInfo { String name; }
    static class FunctionInfo { String name, params, returnType; }
}