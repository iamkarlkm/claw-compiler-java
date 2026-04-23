package examples;

import com.q3lives.compiler.generators.ffi.*;
import com.q3lives.compiler.generators.ffi.FFIBindingTable.*;
import com.q3lives.compiler.processors.semantic.ExternProcessor;

import java.util.Arrays;
import java.util.List;

/**
 * FFI system end-to-end integration example.
 *
 * Demonstrates the complete flow from extern "C" source parsing
 * to C/Java/Python target code generation.
 */
public class FFIIntegrationExample {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("FFI System End-to-End Integration Example");
        System.out.println("========================================\n");

        // 1. Define extern "C" source lines
        List<String> sourceLines = Arrays.asList(
            "extern \"C\" {",
            "    link \"sqlite3\"",
            "    include \"sqlite3.h\"",
            "",
            "    type SQLite3 = OpaquePointer",
            "    type SQLiteStmt = OpaquePointer",
            "",
            "    const SQLITE_OK: Int = 0",
            "    const SQLITE_ROW: Int = 100",
            "",
            "    function sqlite3_open(filename: String, ppDb: Pointer) -> Int",
            "    function sqlite3_close(db: Pointer) -> Int",
            "    function sqlite3_exec(db: Pointer, sql: String) -> Int",
            "}"
        );

        System.out.println("[Input] extern \"C\" source:");
        for (String line : sourceLines) {
            System.out.println("  " + line);
        }
        System.out.println();

        // 2. Create binding table and processor
        FFIBindingTable bindingTable = new FFIBindingTable();
        ExternProcessor processor = new ExternProcessor(bindingTable);

        // 3. Parse extern blocks
        boolean success = processor.process(sourceLines, "example.claw");

        if (!success) {
            System.out.println("[Error] Parsing failed:");
            processor.reportDiagnostics();
            System.exit(1);
        }

        System.out.println("[Success] Extern block parsing completed");
        System.out.println("  - Link directives: " + bindingTable.getAllLinks().size());
        System.out.println("  - Type definitions: " + bindingTable.getAllTypes().size());
        System.out.println("  - Constant definitions: " + bindingTable.getAllConstants().size());
        System.out.println("  - Function declarations: " + bindingTable.getAllFunctions().size());
        System.out.println();

        // 4. Generate C FFI code
        System.out.println("----------------------------------------");
        System.out.println("[Output 1] C FFI Code (CFFIGenerator)");
        System.out.println("----------------------------------------");
        CFFIGenerator cGenerator = new CFFIGenerator(bindingTable);
        String cCode = cGenerator.generateAll();
        System.out.println(cCode);

        // 5. Generate Java FFI code (Panama)
        System.out.println("----------------------------------------");
        System.out.println("[Output 2] Java FFI Code (JavaFFIGenerator - Panama)");
        System.out.println("----------------------------------------");
        JavaFFIGenerator javaGenerator = new JavaFFIGenerator(bindingTable, "Sqlite3FFI");
        String javaCode = javaGenerator.generateAll();
        System.out.println(javaCode);

        // 6. Generate Python FFI code (ctypes)
        System.out.println("----------------------------------------");
        System.out.println("[Output 3] Python FFI Code (PythonFFIGenerator - ctypes)");
        System.out.println("----------------------------------------");
        PythonFFIGenerator pyGenerator = new PythonFFIGenerator(bindingTable);
        String pythonCode = pyGenerator.generateAll();
        System.out.println(pythonCode);

        // 7. Verify generated code
        System.out.println("========================================");
        System.out.println("Verification Results");
        System.out.println("========================================");

        boolean cOk = verifyCCode(cCode);
        boolean javaOk = verifyJavaCode(javaCode);
        boolean pyOk = verifyPythonCode(pythonCode);

        System.out.println("C FFI code:       " + (cOk ? "PASS" : "FAIL"));
        System.out.println("Java FFI code:    " + (javaOk ? "PASS" : "FAIL"));
        System.out.println("Python FFI code:  " + (pyOk ? "PASS" : "FAIL"));

        if (cOk && javaOk && pyOk) {
            System.out.println("\nAll target code verification passed!");
        } else {
            System.out.println("\nSome verification failed");
            System.exit(1);
        }
    }

    // Verify C code contains key elements
    private static boolean verifyCCode(String code) {
        return code.contains("#include <sqlite3.h>")
            && code.contains("extern int sqlite3_open(")
            && code.contains("extern int sqlite3_close(")
            && code.contains("#define SQLITE_OK (0)")
            && code.contains("CLAW_PLATFORM_WINDOWS");
    }

    // Verify Java code contains key elements
    private static boolean verifyJavaCode(String code) {
        return code.contains("public final class Sqlite3FFI")
            && code.contains("Linker.nativeLinker()")
            && code.contains("MH_sqlite3_open")
            && code.contains("public static final int SQLITE_OK = 0;")
            && code.contains("Arena arena");
    }

    // Verify Python code contains key elements
    private static boolean verifyPythonCode(String code) {
        return code.contains("import ctypes")
            && code.contains("_lib_sqlite3 = ctypes.CDLL")
            && code.contains("_lib_sqlite3.sqlite3_open.argtypes")
            && code.contains("SQLITE_OK = 0")
            && code.contains("def _claw_ffi_sqlite3_open(");
    }
}
