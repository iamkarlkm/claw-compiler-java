package claw.compiler.generators.ffi;

import claw.compiler.generators.ffi.FFIBindingTable.ExternConstant;
import claw.compiler.generators.ffi.FFIBindingTable.ExternFunction;
import claw.compiler.generators.ffi.FFIBindingTable.ExternParam;
import claw.compiler.generators.ffi.FFIBindingTable.LinkDirective;
import java.util.*;

/**
 * JavaFFIGenerator 完整使用示例
 *
 * 演示如何使用 JavaFFIGenerator 生成 Panama/JNI FFI 绑定代码
 */
public class JavaFFIGeneratorExample {

    public static void main(String[] args) {
        FFIBindingTable table = new FFIBindingTable();

        System.out.println("========== Java FFI Code Generator Example ==========\n");

        // 1. 添加头文件和链接
        System.out.println("1. 添加头文件和链接:");
        table.getAllIncludes().add("sqlite3.h");
        table.getAllLinks().add(new LinkDirective("sqlite3", "sqlite3.h"));

        // 2. 添加常量
        System.out.println("\n2. 添加常量:");
        ExternConstant ok = new ExternConstant();
        ok.name = "SQLITE_OK";
        ok.value = "0";
        table.getAllConstants().put("SQLITE_OK", ok);

        ExternConstant row = new ExternConstant();
        row.name = "SQLITE_ROW";
        row.value = "100";
        table.getAllConstants().put("SQLITE_ROW", row);

        // 3. 添加函数
        System.out.println("\n3. 添加函数声明:");
        ExternFunction open = new ExternFunction("sqlite3_open",
            List.of(new ExternParam("filename", "String"), new ExternParam("ppDb", "Pointer")),
            "Int", false);
        open.description = "Open a database connection";
        open.threadSafety = FFIBindingTable.ThreadSafety.THREAD_SAFE;
        table.getAllFunctions().put("sqlite3_open", open);

        ExternFunction close = new ExternFunction("sqlite3_close",
            List.of(new ExternParam("db", "Pointer")),
            "Void", false);
        table.getAllFunctions().put("sqlite3_close", close);

        ExternFunction exec = new ExternFunction("sqlite3_exec",
            List.of(new ExternParam("db", "Pointer"), new ExternParam("sql", "String"),
                new ExternParam("callback", "FuncPointer"), new ExternParam("arg", "Any")),
            "Int", false);
        exec.description = "Execute SQL statement";
        table.getAllFunctions().put("sqlite3_exec", exec);

        // 4. 生成 Panama 绑定
        System.out.println("\n========== Generated Panama Binding ==========");
        JavaFFIGenerator panamaGenerator = new JavaFFIGenerator(table, JavaFFIGenerator.Strategy.PANAMA, "com.example");
        String panamaCode = panamaGenerator.generateAll();
        System.out.println(panamaCode);

        // 5. 生成 JNI 绑定
        System.out.println("\n========== Generated JNI Binding ==========");
        JavaFFIGenerator jniGenerator = new JavaFFIGenerator(table, JavaFFIGenerator.Strategy.JNI, "com.example");
        String jniCode = jniGenerator.generateAll();
        System.out.println(jniCode);

        // 6. 生成平台检测
        System.out.println("\n========== Generated Platform Detection ==========");
        String platformDetection = panamaGenerator.generatePlatformDetection();
        System.out.println(platformDetection);

        // 7. 生成条件绑定
        System.out.println("\n========== Generated Platform Conditional Bindings ==========");
        String platformBindings = panamaGenerator.generatePlatformConditionalBindings(table);
        System.out.println(platformBindings);

        // 8. 显示类型映射
        System.out.println("\n========== Type Mapping Examples ==========");
        System.out.println("String → " + JavaFFIGenerator.mapClawTypeToJavaType("String"));
        System.out.println("Int → " + JavaFFIGenerator.mapClawTypeToJavaType("Int"));
        System.out.println("Pointer → " + JavaFFIGenerator.mapClawTypeToJavaType("Pointer"));
        System.out.println("MemorySegment → " + JavaFFIGenerator.mapClawTypeToJavaType("OpaquePointer"));
        System.out.println("void → " + JavaFFIGenerator.mapClawTypeToJavaType("Void"));

        System.out.println("\n========== MemoryLayout Mapping ==========");
        System.out.println("String → " + JavaFFIGenerator.mapClawTypeToMemoryLayout("String"));
        System.out.println("Int → " + JavaFFIGenerator.mapClawTypeToMemoryLayout("Int"));
        System.out.println("Pointer → " + JavaFFIGenerator.mapClawTypeToMemoryLayout("Pointer"));
        System.out.println("Bool → " + JavaFFIGenerator.mapClawTypeToMemoryLayout("Bool"));

        System.out.println("\n========== JNI Type Mapping ==========");
        System.out.println("String → " + JavaFFIGenerator.mapClawTypeToJNIType("String"));
        System.out.println("Int → " + JavaFFIGenerator.mapClawTypeToJNIType("Int"));
        System.out.println("Pointer → " + JavaFFIGenerator.mapClawTypeToJNIType("Pointer"));
    }

    /**
     * 高级示例：完整的 SQLite3 绑定
     */
    public static void sqlite3PanamaExample() {
        FFIBindingTable table = new FFIBindingTable();

        table.getAllLinks().add(new LinkDirective("sqlite3", "sqlite3.h"));

        // 添加常量
        ExternConstant ok = new ExternConstant();
        ok.name = "SQLITE_OK";
        ok.value = "0";
        table.getAllConstants().put("SQLITE_OK", ok);

        // 添加函数
        ExternFunction open = new ExternFunction("sqlite3_open",
            List.of(new ExternParam("filename", "String"), new ExternParam("ppDb", "Pointer")),
            "Int", false);
        open.description = "Open a database connection";
        table.getAllFunctions().put("sqlite3_open", open);

        ExternFunction close = new ExternFunction("sqlite3_close",
            List.of(new ExternParam("db", "Pointer")),
            "Void", false);
        table.getAllFunctions().put("sqlite3_close", close);

        ExternFunction exec = new ExternFunction("sqlite3_exec",
            List.of(new ExternParam("db", "Pointer"), new ExternParam("sql", "String")),
            "Int", false);
        table.getAllFunctions().put("sqlite3_exec", exec);

        // 生成 Panama 绑定
        JavaFFIGenerator generator = new JavaFFIGenerator(table);
        String code = generator.generateAll();

        System.out.println("========== SQLite3 Panama Binding ==========");
        System.out.println(code);
    }

    /**
     * 高级示例：完整的 libcurl 绑定
     */
    public static void curlPanamaExample() {
        FFIBindingTable table = new FFIBindingTable();

        table.getAllLinks().add(new LinkDirective("curl", "curl"));

        // 添加常量
        ExternConstant ok = new ExternConstant();
        ok.name = "CURLE_OK";
        ok.value = "0";
        table.getAllConstants().put("CURLE_OK", ok);

        // 添加函数
        ExternFunction init = new ExternFunction("curl_easy_init",
            List.of(),
            "Pointer", false);
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

        // 生成 Panama 绑定
        JavaFFIGenerator generator = new JavaFFIGenerator(table);
        String code = generator.generateAll();

        System.out.println("========== libcurl Panama Binding ==========");
        System.out.println(code);
    }

    /**
     * JNI 绑定示例
     */
    public static void curlJNIExample() {
        FFIBindingTable table = new FFIBindingTable();

        table.getAllLinks().add(new LinkDirective("curl", "curl"));

        ExternFunction init = new ExternFunction("curl_easy_init",
            List.of(),
            "Pointer", false);
        table.getAllFunctions().put("curl_easy_init", init);

        // 生成 JNI 绑定
        JavaFFIGenerator generator = new JavaFFIGenerator(table, JavaFFIGenerator.Strategy.JNI);
        String code = generator.generateAll();

        System.out.println("========== libcurl JNI Binding ==========");
        System.out.println(code);
        System.out.println("\nNote: Compile with: gcc -shared -fPIC -I\"$JAVA_HOME/include\" -I\"$JAVA_HOME/include/linux\" -o libclaw_jni_bridge.so claw_jni_bridge.c -lcurl");
    }
}
