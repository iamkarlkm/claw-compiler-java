package com.q3lives.binding;

import com.q3lives.binding.c.CRuntime;
import com.q3lives.binding.java.JavaRuntime;
import com.q3lives.binding.python.PythonRuntime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 标准库绑定单元测试
 *
 * 验证各目标语言 Runtime 对 Claw 标准库模块名的正确映射。
 */
class StdLibraryBindingTest {

    private final JavaRuntime javaRuntime = new JavaRuntime();
    private final PythonRuntime pythonRuntime = new PythonRuntime();
    private final CRuntime cRuntime = new CRuntime();

    // ==================== Java 标准库映射 ====================

    @Test
    void testJavaStdIo() {
        assertEquals("import java.io.*;", javaRuntime.generateImport("std.io", null));
    }

    @Test
    void testJavaStdCollections() {
        assertEquals("import java.util.*;", javaRuntime.generateImport("std.collections", null));
    }

    @Test
    void testJavaStdTime() {
        assertEquals("import java.time.*;", javaRuntime.generateImport("std.time", null));
    }

    @Test
    void testJavaStdNet() {
        assertEquals("import java.net.*;", javaRuntime.generateImport("std.net", null));
    }

    @Test
    void testJavaStdDatabase() {
        assertEquals("import java.sql.*;", javaRuntime.generateImport("std.database", null));
    }

    @Test
    void testJavaStdConcurrent() {
        assertEquals("import java.util.concurrent.*;", javaRuntime.generateImport("std.concurrent", null));
    }

    @Test
    void testJavaStdRegex() {
        assertEquals("import java.util.regex.*;", javaRuntime.generateImport("std.regex", null));
    }

    @Test
    void testJavaCustomModule() {
        // 非标准库模块应原样传递
        assertEquals("import com.example.*;", javaRuntime.generateImport("com.example", null));
    }

    // ==================== Python 标准库映射 ====================

    @Test
    void testPythonStdIo() {
        assertEquals("import io", pythonRuntime.generateImport("std.io", null));
    }

    @Test
    void testPythonStdCollections() {
        assertEquals("import collections", pythonRuntime.generateImport("std.collections", null));
    }

    @Test
    void testPythonStdJson() {
        assertEquals("import json", pythonRuntime.generateImport("std.json", null));
    }

    @Test
    void testPythonStdDatabase() {
        assertEquals("import sqlite3", pythonRuntime.generateImport("std.database", null));
    }

    @Test
    void testPythonStdNet() {
        assertEquals("import socket", pythonRuntime.generateImport("std.net", null));
    }

    @Test
    void testPythonStdConcurrent() {
        assertEquals("import threading", pythonRuntime.generateImport("std.concurrent", null));
    }

    @Test
    void testPythonStdRegex() {
        assertEquals("import re", pythonRuntime.generateImport("std.regex", null));
    }

    @Test
    void testPythonWithSymbol() {
        assertEquals("from java.util import HashMap",
            pythonRuntime.generateImport("java.util", "HashMap"));
    }

    // ==================== C 标准库映射 ====================

    @Test
    void testCStdIo() {
        assertEquals("#include <stdio.h>", cRuntime.generateImport("std.io", null));
    }

    @Test
    void testCStdMath() {
        assertEquals("#include <math.h>", cRuntime.generateImport("std.math", null));
    }

    @Test
    void testCStdCollections() {
        assertEquals("#include <stdlib.h>", cRuntime.generateImport("std.collections", null));
    }

    @Test
    void testCStdNet() {
        assertEquals("#include <sys/socket.h>", cRuntime.generateImport("std.net", null));
    }

    @Test
    void testCStdRegex() {
        assertEquals("#include <regex.h>", cRuntime.generateImport("std.regex", null));
    }

    @Test
    void testCStdConcurrent() {
        assertEquals("#include <pthread.h>", cRuntime.generateImport("std.concurrent", null));
    }

    @Test
    void testCCustomModule() {
        assertEquals("#include \"mylib.h\"", cRuntime.generateImport("mylib", null));
    }
}
