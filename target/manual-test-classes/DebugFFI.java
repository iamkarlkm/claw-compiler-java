import com.q3lives.compiler.generators.ffi.*;
import com.q3lives.compiler.generators.ffi.FFIBindingTable.*;
import com.q3lives.compiler.processors.semantic.ExternProcessor;
import java.util.*;

public class DebugFFI {
    public static void main(String[] args) {
        List<String> sourceLines = Arrays.asList(
            "extern \"C\" {",
            "    link \"sqlite3\"",
            "    include \"sqlite3.h\"",
            "    const SQLITE_OK: Int = 0",
            "    function sqlite3_open(filename: String, ppDb: Pointer) -> Int",
            "}"
        );

        FFIBindingTable bindingTable = new FFIBindingTable();
        ExternProcessor processor = new ExternProcessor(bindingTable);
        processor.process(sourceLines, "example.claw");

        CFFIGenerator cGenerator = new CFFIGenerator(bindingTable);
        String cCode = cGenerator.generateAll();

        System.out.println("cCode length: " + cCode.length());
        System.out.println("contains sqlite3.h: " + cCode.contains("#include <sqlite3.h>"));
        System.out.println("contains sqlite3_open: " + cCode.contains("extern int sqlite3_open("));
        System.out.println("contains SQLITE_OK: " + cCode.contains("#define SQLITE_OK (0)"));
        System.out.println("contains WINDOWS: " + cCode.contains("CLAW_PLATFORM_WINDOWS"));
    }
}
