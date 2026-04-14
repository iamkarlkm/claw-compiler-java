package claw.compiler.generators.ffi;

import claw.compiler.generators.ffi.FFIBindingTable.ExternConstant;
import claw.compiler.generators.ffi.FFIBindingTable.ExternFunction;
import claw.compiler.generators.ffi.FFIBindingTable.ExternParam;
import claw.compiler.generators.ffi.FFIBindingTable.ExternType;
import claw.compiler.generators.ffi.FFIBindingTable.LinkDirective;
import java.util.*;

/**
 * PythonFFIGenerator 完整使用示例
 *
 * 演示如何使用 PythonFFIGenerator 生成完整的 ctypes 绑定代码
 */
public class PythonFFIGeneratorExample {

    public static void main(String[] args) {
        FFIBindingTable table = new FFIBindingTable();

        System.out.println("========== Python ctypes FFI Code Generator Example ==========\n");

        // 1. 添加头文件
        System.out.println("1. 添加头文件:");
        table.getAllIncludes().add("sqlite3.h");
        table.getAllIncludes().add("curl/curl.h");

        // 2. 添加链接指令
        System.out.println("\n2. 添加链接指令:");
        table.getAllLinks().add(new LinkDirective("sqlite3", "sqlite3.h"));
        table.getAllLinks().add(new LinkDirective("curl", "curl/curl.h"));

        // 3. 添加类型定义
        System.out.println("\n3. 添加类型定义:");
        ExternType sqlite3Type = new ExternType("sqlite3", "OpaquePointer");
        table.getAllTypes().put("sqlite3", sqlite3Type);

        ExternType CURLType = new ExternType("CURL", "OpaquePointer");
        CURLType.description = "libcurl easy handle type";
        table.getAllTypes().put("CURL", CURLType);

        // 4. 添加函数
        System.out.println("\n4. 添加函数声明:");
        ExternFunction sqlite3_open = new ExternFunction("sqlite3_open",
            List.of(new ExternParam("filename", "String"), new ExternParam("ppDb", "Pointer")),
            "Int", false);
        sqlite3_open.description = "Open a SQLite database connection";
        sqlite3_open.threadSafety = FFIBindingTable.ThreadSafety.THREAD_SAFE;
        table.getAllFunctions().put("sqlite3_open", sqlite3_open);

        ExternFunction curl_easy_setopt = new ExternFunction("curl_easy_setopt",
            List.of(new ExternParam("curl", "Pointer"), new ExternParam("option", "Int"), new ExternParam("arg", "Any")),
            "CURLcode", false);
        curl_easy_setopt.description = "Set a curl option";
        table.getAllFunctions().put("curl_easy_setopt", curl_easy_setopt);

        ExternFunction curl_easy_init = new ExternFunction("curl_easy_init",
            List.of(),
            "CURL", false);
        curl_easy_init.description = "Initialize a curl easy handle";
        table.getAllFunctions().put("curl_easy_init", curl_easy_init);

        ExternFunction curl_easy_cleanup = new ExternFunction("curl_easy_cleanup",
            List.of(new ExternParam("curl", "Pointer")),
            "Void", false);
        table.getAllFunctions().put("curl_easy_cleanup", curl_easy_cleanup);

        // 5. 添加常量
        System.out.println("\n5. 添加常量:");
        ExternConstant CURLE_OK = new ExternConstant();
        CURLE_OK.name = "CURLE_OK";
        CURLE_OK.value = "0";
        table.getAllConstants().put("CURLE_OK", CURLE_OK);

        ExternConstant CURLE_NOT_IMPLEMENTED = new ExternConstant();
        CURLE_NOT_IMPLEMENTED.name = "CURLE_NOT_IMPLEMENTED";
        CURLE_NOT_IMPLEMENTED.value = "42";
        table.getAllConstants().put("CURLE_NOT_IMPLEMENTED", CURLE_NOT_IMPLEMENTED);

        // 6. 创建并配置一个完整的 extern 块
        System.out.println("\n6. 创建完整的 extern 块:");
        FFIBindingTable.ExternBlock block = table.newExternBlock();
        block.comment = "SQLite3 Database Library Bindings";

        // 添加类型
        block.types.add(sqlite3Type);

        // 添加函数
        block.functions.add(sqlite3_open);

        // 添加常量
        block.constants.add(CURLE_OK);

        // 7. 生成完整的 Python 代码
        System.out.println("\n========== Generated Python Code ==========");
        PythonFFIGenerator generator = new PythonFFIGenerator(table);

        // 完整代码
        String allCode = generator.generateAll();
        System.out.println(allCode);

        // 添加平台检测代码
        String platformCode = generator.generatePlatformDetection();
        System.out.println("\n========== Generated Platform Detection ==========");
        System.out.println(platformCode);

        // 平台条件加载
        String platformLoading = generator.generatePlatformConditionalLoading(table);
        System.out.println("\n========== Generated Platform Conditional Loading ==========");
        System.out.println(platformLoading);

        // 8. 单独生成各个组件
        System.out.println("\n========== Library Loading ==========");
        System.out.println(generator.generateLibraryLoading());

        System.out.println("\n========== Constants ==========");
        System.out.println(generator.generateConstants());

        System.out.println("\n========== Function Bindings ==========");
        System.out.println(generator.generateFunctionBindings());

        System.out.println("\n========== Wrapper Functions ==========");
        System.out.println(generator.generateWrapperFunctions());

        // 9. 显示类型映射
        System.out.println("\n========== Type Mapping Examples ==========");
        System.out.println("String -> " + PythonFFIGenerator.mapClawTypeToCtype("String"));
        System.out.println("Int -> " + PythonFFIGenerator.mapClawTypeToCtype("Int"));
        System.out.println("Float -> " + PythonFFIGenerator.mapClawTypeToCtype("Float"));
        System.out.println("Pointer -> " + PythonFFIGenerator.mapClawTypeToCtype("Pointer"));
        System.out.println("Void -> " + PythonFFIGenerator.mapClawTypeToCtype("Void"));
        System.out.println("Ref<Int> -> " + PythonFFIGenerator.mapClawTypeToCtype("Ref<Int>"));

        System.out.println("\n========== Python Type Hints ==========");
        System.out.println("String -> " + PythonFFIGenerator.mapClawTypeToPythonTypeHint("String"));
        System.out.println("Int -> " + PythonFFIGenerator.mapClawTypeToPythonTypeHint("Int"));
        System.out.println("Float -> " + PythonFFIGenerator.mapClawTypeToPythonTypeHint("Float"));
        System.out.println("Void -> " + PythonFFIGenerator.mapClawTypeToPythonTypeHint("Void"));
    }

    /**
     * 高级示例：创建完整的 SQLite3 绑定
     */
    public static void sqlite3Example() {
        FFIBindingTable table = new FFIBindingTable();

        // 添加头文件和链接
        table.getAllIncludes().add("sqlite3.h");
        table.getAllLinks().add(new LinkDirective("sqlite3", "sqlite3.h"));

        // 添加常量
        ExternConstant ok = new ExternConstant();
        ok.name = "SQLITE_OK";
        ok.value = "0";
        table.getAllConstants().put("SQLITE_OK", ok);

        ExternConstant row = new ExternConstant();
        row.name = "SQLITE_ROW";
        row.value = "100";
        table.getAllConstants().put("SQLITE_ROW", row);

        // 添加函数
        ExternFunction open = new ExternFunction("sqlite3_open",
            List.of(new ExternParam("filename", "String"), new ExternParam("ppDb", "Pointer")),
            "Int", false);
        open.description = "Open a database connection";
        open.threadSafety = FFIBindingTable.ThreadSafety.THREAD_SAFE;
        table.getAllFunctions().put("sqlite3_open", open);

        ExternFunction close = new ExternFunction("sqlite3_close",
            List.of(new ExternParam("db", "Pointer")),
            "Int", false);
        table.getAllFunctions().put("sqlite3_close", close);

        ExternFunction exec = new ExternFunction("sqlite3_exec",
            List.of(new ExternParam("db", "Pointer"), new ExternParam("sql", "String"),
                new ExternParam("callback", "FuncPointer"), new ExternParam("arg", "Any")),
            "Int", false);
        exec.description = "Execute SQL statement";
        table.getAllFunctions().put("sqlite3_exec", exec);

        // 生成 Python 代码
        PythonFFIGenerator generator = new PythonFFIGenerator(table);
        String pyCode = generator.generateAll();

        System.out.println("========== SQLite3 Python Bindings ==========");
        System.out.println(pyCode);
    }

    /**
     * 高级示例：创建完整的 libcurl 绑定
     */
    public static void curlExample() {
        FFIBindingTable table = new FFIBindingTable();

        // 添加头文件和链接
        table.getAllIncludes().add("curl/curl.h");
        table.getAllLinks().add(new LinkDirective("curl", "curl"));

        // 添加常量
        ExternConstant ok = new ExternConstant();
        ok.name = "CURLE_OK";
        ok.value = "0";
        table.getAllConstants().put("CURLE_OK", ok);

        // 添加函数
        ExternFunction init = new ExternFunction("curl_easy_init",
            List.of(),
            "CURL", false);
        init.description = "Initialize a curl easy handle";
        table.getAllFunctions().put("curl_easy_init", init);

        ExternFunction cleanup = new ExternFunction("curl_easy_cleanup",
            List.of(new ExternParam("curl", "Pointer")),
            "Void", false);
        table.getAllFunctions().put("curl_easy_cleanup", cleanup);

        ExternFunction perform = new ExternFunction("curl_easy_perform",
            List.of(new ExternParam("curl", "Pointer")),
            "CURLcode", false);
        perform.description = "Perform a curl transfer";
        table.getAllFunctions().put("curl_easy_perform", perform);

        // 生成 Python 代码
        PythonFFIGenerator generator = new PythonFFIGenerator(table);
        String pyCode = generator.generateAll();

        System.out.println("========== libcurl Python Bindings ==========");
        System.out.println(pyCode);
    }
}
