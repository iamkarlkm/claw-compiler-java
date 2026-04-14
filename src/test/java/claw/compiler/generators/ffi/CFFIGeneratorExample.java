package claw.compiler.generators.ffi;

import claw.compiler.generators.ffi.FFIBindingTable.ExternConstant;
import claw.compiler.generators.ffi.FFIBindingTable.ExternFunction;
import claw.compiler.generators.ffi.FFIBindingTable.ExternParam;
import claw.compiler.generators.ffi.FFIBindingTable.ExternType;
import claw.compiler.generators.ffi.FFIBindingTable.LinkDirective;
import java.util.*;

/**
 * CFFIGenerator 使用示例
 *
 * 演示如何使用 CFFIGenerator 生成完整的 C FFI 代码
 */
public class CFFIGeneratorExample {

    public static void main(String[] args) {
        FFIBindingTable table = new FFIBindingTable();

        System.out.println("========== C FFI Code Generator Example ==========\n");

        // 1. 添加头文件
        System.out.println("1. 添加头文件:");
        table.getAllIncludes().add("stdio.h");
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

        // 5. 添加常量
        System.out.println("\n5. 添加常量:");
        ExternConstant CURLE_OK = new ExternConstant();
        CURLE_OK.name = "CURLE_OK";
        CURLE_OK.value = "0";
        table.getAllConstants().put("CURLE_OK", CURLE_OK);

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

        // 7. 生成完整的 C 代码
        System.out.println("\n========== Generated C Code ==========");
        CFFIGenerator generator = new CFFIGenerator(table);

        String allCode = generator.generateAll();
        System.out.println(allCode);

        // 8. 生成 extern 块的代码
        System.out.println("\n========== Generated Block Code ==========");
        String blockCode = generator.generateBlockCode(block);
        System.out.println(blockCode);

        // 9. 生成链接标志和构建命令
        System.out.println("\n========== Build Information ==========");
        System.out.println("Link flags: " + generator.generateLinkFlags());
        System.out.println("Build command: " + generator.generateBuildCommand("example.c", "example"));

        // 10. 生成平台条件编译代码
        System.out.println("\n========== Platform Guarded Code ==========");
        FFIBindingTable.PlatformConstraint winConstraint = new FFIBindingTable.PlatformConstraint();
        winConstraint.addPlatform("windows");

        FFIBindingTable.ExternBlock winBlock = table.newExternBlock();
        winBlock.comment = "Windows-specific API";
        winBlock.platform = winConstraint;
        table.getExternBlocks().add(winBlock);

        String platformCode = generator.generatePlatformGuardedCode(table);
        System.out.println(platformCode);
    }
}
