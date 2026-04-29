package com.q3lives.compiler.tools;

import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Header2Claw - C++ 头文件转 Claw FFI 封装
 * 解析 .h/.hpp 文件生成 .claw 绑定文件
 */
public class Header2Claw {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String headerPath = args[0];
        String moduleName = args.length > 1 ? args[1] : "ext";
        String outputPath = args.length > 2 ? args[2] : headerPath.replace(".hpp", ".claw").replace(".h", ".claw");

        new Header2Claw().convert(headerPath, moduleName, outputPath);
    }

    private static void printUsage() {
        System.out.println("Header2Claw - C++ 头文件转 Claw FFI");
        System.out.println();
        System.out.println("用法: java Header2Claw <header.h> [module] [output.claw]");
        System.out.println();
        System.out.println("示例: java Header2Claw opencv2/core.hpp opencv");
    }

    public void convert(String headerPath, String moduleName, String outputPath) throws Exception {
        String source = Files.readString(Paths.get(headerPath));

        var sb = new StringBuilder();
        sb.append("/**\n * ").append(moduleName).append(" - 自动生成的 FFI 绑定\n");
        sb.append(" * 源文件: ").append(headerPath).append("\n */\n\n");
        sb.append("module ").append(moduleName).append(" {\n\n");

        // extern "C" 块
        sb.append("    extern \"C\" {\n");
        sb.append("      // link 和 include 在使用时添加\n");
        sb.append("      type ").append(moduleName).append("Handle = OpaquePointer\n\n");

        // 提取常量 (#define)
        var consts = extractConstants(source);
        for (var c : consts) {
            sb.append("      const ").append(c.name).append(": ").append(c.type).append(" = ");
            sb.append(c.value).append("\n");
        }
        if (!consts.isEmpty()) sb.append("\n");

        // 提取类型 (struct/class)
        var types = extractTypes(source);
        for (var t : types) {
            sb.append("      struct ").append(t.name).append(" {\n");
            for (var f : t.fields) {
                sb.append("        ").append(f.name).append(": ").append(f.type).append("\n");
            }
            sb.append("      }\n\n");
        }

        // 提取函数声明
        var funcs = extractFunctions(source);
        for (var f : funcs) {
            sb.append("      function ").append(f.name).append("(");
            sb.append(f.params).append(") -> ").append(f.returnType).append("\n");
        }

        sb.append("    }\n\n");

        // 生成包装函数
        for (var f : funcs) {
            if (!f.returnType.equals("void")) {
                sb.append("    function ");
                sb.append(f.name).append("(");
                sb.append(f.params).append(") -> ");
                sb.append(f.returnType).append(" {\n");
                sb.append("      return ext.").append(f.name).append("(").append(f.paramNames).append(")\n");
                sb.append("    }\n\n");
            } else {
                sb.append("    function ");
                sb.append(f.name).append("(");
                sb.append(f.params).append(") -> Void {\n");
                sb.append("      ext.").append(f.name).append("(").append(f.paramNames).append(")\n");
                sb.append("    }\n\n");
            }
        }

        sb.append("}\n");

        // 写入输出文件
        Files.writeString(Paths.get(outputPath), sb.toString());

        System.out.println("生成: " + outputPath);
        System.out.println("  模块: " + moduleName);
        System.out.println("  类型: " + types.size());
        System.out.println("  常量: " + consts.size());
        System.out.println("  函数: " + funcs.size());
    }

    private List<ConstInfo> extractConstants(String source) {
        var list = new ArrayList<ConstInfo>();
        // #define NAME value 或 #define NAME(value)
        var m = Pattern.compile("#define\\s+(\\w+)\\s+(.+?)(?:\\n|/)").matcher(source);
        while (m.find() && list.size() < 50) {
            var info = new ConstInfo();
            info.name = m.group(1);
            String val = m.group(2).trim();
            // 推断类型
            if (val.matches("\\d+")) {
                info.type = "Int";
                info.value = val;
            } else if (val.matches("\\d+\\.\\d+")) {
                info.type = "Float";
                info.value = val;
            } else if (val.startsWith("\"")) {
                info.type = "String";
                info.value = val;
            } else {
                info.type = "Int";
                info.value = "0";
            }
            if (!info.name.matches("(.*_h|_H__|INC|DEFS)")) {
                list.add(info);
            }
        }
        return list;
    }

    private List<TypeInfo> extractTypes(String source) {
        var list = new ArrayList<TypeInfo>();
        // struct/class NAME { ... }
        var m = Pattern.compile("(?:struct|class)\\s+(\\w+)\\s*\\{([^}]+)\\}").matcher(source);
        while (m.find() && list.size() < 20) {
            var info = new TypeInfo();
            info.name = m.group(1);
            var body = m.group(2);
            // 提取字段
            var fm = Pattern.compile("(?:\\w+|float|int|double|bool|void|uchar)\\s+(\\w+)").matcher(body);
            while (fm.find()) {
                var f = new FieldInfo();
                f.name = fm.group(1);
                f.type = mapType(fm.group(0).trim().split("\\s+")[0]);
                info.fields.add(f);
            }
            if (!info.fields.isEmpty()) list.add(info);
        }
        return list;
    }

    private List<FunctionInfo> extractFunctions(String source) {
        var list = new ArrayList<FunctionInfo>();
        // 匹配: void cv::imread(...) 或 Mat imread(...)
        var m = Pattern.compile(
            "(?:void|int|float|double|bool)\\s+(?:cv::)?(\\w+)\\s*\\(([^)]*)\\)"
        ).matcher(source);
        while (m.find() && list.size() < 100) {
            String name = m.group(1);
            if (name.matches("(.*)_h|_H$|INC$|DEFS$|test|main")) continue;

            var info = new FunctionInfo();
            info.name = name;
            info.returnType = "Void";
            String params = m.group(2);
            info.params = parseParams(params);
            info.paramNames = extractParamNames(params);
            list.add(info);
        }
        // Mat 返回值的函数
        var m2 = Pattern.compile(
            "(?:Mat|String|Point|Size|Rect|Scalar)\\s+(?:cv::)?(\\w+)\\s*\\(([^)]*)\\)"
        ).matcher(source);
        while (m2.find() && list.size() < 100) {
            String name = m2.group(1);
            if (name.matches("(.*)_h|_H$|INC$|DEFS$|test|main")) continue;

            var info = new FunctionInfo();
            info.name = name;
            info.returnType = mapType(name.replace("cv::", ""));
            String params = m2.group(2);
            info.params = parseParams(params);
            info.paramNames = extractParamNames(params);
            list.add(info);
        }
        return list;
    }

    private String parseParams(String params) {
        if (params.trim().isEmpty()) return "";
        var sb = new StringBuilder();
        var parts = params.split(",");
        for (int i = 0; i < parts.length; i++) {
            var p = parts[i].trim();
            if (p.isEmpty() || p.equals("void")) continue;
            // 清理 const 和引用
            p = p.replace("const ", "").replace("&", "").replace("*", "").trim();
            var ps = p.split("\\s+");
            if (ps.length >= 2) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(ps[ps.length - 1]).append(": ").append(mapType(ps[0]));
            } else if (ps.length == 1) {
                if (sb.length() > 0) sb.append(", ");
                sb.append("arg").append(i).append(": Any"); // 未知类型
            }
        }
        return sb.toString();
    }

    private String extractParamNames(String params) {
        if (params.trim().isEmpty()) return "";
        var sb = new StringBuilder();
        var parts = params.split(",");
        for (int i = 0; i < parts.length; i++) {
            var p = parts[i].trim();
            if (p.isEmpty() || p.equals("void")) continue;
            // 提取变量名
            p = p.replace("const ", "").replace("&", "").replace("*", "").trim();
            var ps = p.split("\\s+");
            if (ps.length >= 2) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(ps[ps.length - 1]);
            } else {
                if (sb.length() > 0) sb.append(", ");
                sb.append("arg").append(i);
            }
        }
        return sb.toString();
    }

    private String mapType(String cppType) {
        return switch (cppType) {
            case "void" -> "Void";
            case "int", "int32_t", "int64_t" -> "Int";
            case "float", "double" -> "Float";
            case "bool" -> "Bool";
            case "char*", "const char*", "string", "String" -> "String";
            case "Mat" -> "Mat";
            case "Point", "Point2i" -> "Point";
            case "Size", "Size2i" -> "Size";
            case "Rect", "Rect2i" -> "Rect";
            case "Scalar" -> "Scalar";
            default -> cppType.contains("*") ? "Pointer" : "Any";
        };
    }

    static class ConstInfo { String name, type, value; }
    static class TypeInfo { String name; List<FieldInfo> fields = new ArrayList<>(); }
    static class FieldInfo { String name, type; }
    static class FunctionInfo { String name, returnType, params, paramNames; }
}