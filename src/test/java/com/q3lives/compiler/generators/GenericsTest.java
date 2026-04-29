package com.q3lives.compiler.generators;

import com.q3lives.binding.c.CRuntime;
import com.q3lives.binding.python.PythonRuntime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 泛型系统测试
 */
class GenericsTest {

    // ================================================================
    // 1. Python 泛型类型映射测试
    // ================================================================

    @Test
    void testPythonArrayMapping() {
        PythonRuntime runtime = new PythonRuntime();

        // Array<Int> -> list[int]
        assertEquals("list[int]", runtime.mapType("Array<Int>"));
    }

    @Test
    void testPythonListMapping() {
        PythonRuntime runtime = new PythonRuntime();

        // List<Float> -> list[float]
        assertEquals("list[float]", runtime.mapType("List<Float>"));
    }

    @Test
    void testPythonMapMapping() {
        PythonRuntime runtime = new PythonRuntime();

        // Map<String, Int> -> dict[str, int]
        assertEquals("dict[str, int]", runtime.mapType("Map<String,Int>"));
    }

    @Test
    void testPythonOptionalMapping() {
        PythonRuntime runtime = new PythonRuntime();

        // Optional<Int> -> int | None
        assertEquals("int | None", runtime.mapType("Optional<Int>"));
    }

    @Test
    void testPythonSetMapping() {
        PythonRuntime runtime = new PythonRuntime();

        // Set<String> -> set[str]
        assertEquals("set[str]", runtime.mapType("Set<String>"));
    }

    // ================================================================
    // 2. C 泛型类型映射测试
    // ================================================================

    @Test
    void testCArrayMapping() {
        CRuntime runtime = new CRuntime();

        // C 后端使用 void* 表示泛型
        assertEquals("void*", runtime.mapType("CArray<Int>"));
    }

    @Test
    void testCMapMapping() {
        CRuntime runtime = new CRuntime();

        // Map 映射为 void*
        assertEquals("void*", runtime.mapType("Map<String, Int>"));
    }

    @Test
    void testCGenericHelpers() {
        CRuntime runtime = new CRuntime();

        // 提取基础类型
        assertEquals("Map", runtime.extractGenericBase("Map<String, Int>"));
        assertEquals("List", runtime.extractGenericBase("List<Float>"));

        // 提取类型参数
        assertEquals("String,Int", runtime.extractGenericParam("Map<String,Int>"));

        // 检查是否为泛型类型
        assertTrue(runtime.isGenericType("List<Int>"));
        assertFalse(runtime.isGenericType("Int"));
    }
}