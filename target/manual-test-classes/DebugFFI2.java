import com.q3lives.compiler.generators.ffi.*;
import com.q3lives.compiler.generators.ffi.FFIBindingTable.*;
import com.q3lives.compiler.processors.semantic.ExternProcessor;
import java.util.*;

public class DebugFFI2 {
    public static void main(String[] args) {
        List<String> sourceLines = Arrays.asList(
            "extern \"C\" {",
            "    link \"sqlite3\"",
            "    include \"sqlite3.h\"",
            "    type SQLite3 = OpaquePointer",
            "    type SQLiteStmt = OpaquePointer",
            "    const SQLITE_OK: Int = 0",
            "    const SQLITE_ROW: Int = 100",
            "    function sqlite3_open(filename: String, ppDb: Pointer) -> Int",
            "    function sqlite3_close(db: Pointer) -> Int",
            "    function sqlite3_exec(db: Pointer, sql: String) -> Int",
            "}"
        );

        FFIBindingTable bindingTable = new FFIBindingTable();
        ExternProcessor processor = new ExternProcessor(bindingTable);
        processor.process(sourceLines, "example.claw");

        CFFIGenerator cGen = new CFFIGenerator(bindingTable);
        JavaFFIGenerator javaGen = new JavaFFIGenerator(bindingTable, "Sqlite3FFI");
        PythonFFIGenerator pyGen = new PythonFFIGenerator(bindingTable);

        String cCode = cGen.generateAll();
        String javaCode = javaGen.generateAll();
        String pyCode = pyGen.generateAll();

        boolean cOk = cCode.contains("#include <sqlite3.h>")
            && cCode.contains("extern int sqlite3_open(")
            && cCode.contains("extern int sqlite3_close(")
            && cCode.contains("#define SQLITE_OK (0)")
            && cCode.contains("CLAW_PLATFORM_WINDOWS");

        boolean javaOk = javaCode.contains("public final class Sqlite3FFI")
            && javaCode.contains("Linker.nativeLinker()")
            && javaCode.contains("MH_sqlite3_open")
            && javaCode.contains("public static final int SQLITE_OK = 0;")
            && javaCode.contains("Arena arena");

        boolean pyOk = pyCode.contains("import ctypes")
            && pyCode.contains("_lib_sqlite3 = ctypes.CDLL")
            && pyCode.contains("_lib_sqlite3.sqlite3_open.argtypes")
            && pyCode.contains("SQLITE_OK = 0")
            && pyCode.contains("def _claw_ffi_sqlite3_open(");

        System.out.println("cOk=" + cOk + " javaOk=" + javaOk + " pyOk=" + pyOk);
    }
}
