/**
 * claw_runtime.h
 * 
 * Claw 语言 -> C 目标的完整运行时支持
 * 
 * 包含:
 *   1. 异常模拟系统 (CLAW_TRY / CLAW_CATCH / CLAW_END_TRY)
 *   2. 异常类型枚举 (CLAW_EX_xxx)
 *   3. 异常结构体 (ClawException)
 *   4. 抛出异常 (__claw_throw)
 *   5. 属性监听钩子 (__claw_before_prop_change / __claw_after_prop_change)
 *   6. 字符串辅助 (__claw_str_concat / __claw_int_to_str / __claw_float_to_str)
 *   7. 业务流转 (flow-to 直接用 goto，无需额外支持)
 * 
 * 使用方式:
 *   #include "claw_runtime.h"
 * 
 * 编译:
 *   gcc -std=c11 -o output output.c -lm
 */

#ifndef CLAW_RUNTIME_H
#define CLAW_RUNTIME_H

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>
#include <setjmp.h>
#include <stdarg.h>

#ifdef __cplusplus
extern "C" {
#endif

/* ================================================================
 *  1. 异常类型枚举
 * ================================================================ */

typedef enum ClawExceptionType {
    CLAW_EX_NONE             = 0,

    /* 通用异常 */
    CLAW_EX_EXCEPTION        = 100,
    CLAW_EX_RUNTIME          = 101,
    CLAW_EX_LOGIC            = 102,

    /* I/O 相关 */
    CLAW_EX_IOERROR          = 200,
    CLAW_EX_FILEERROR        = 201,
    CLAW_EX_NETWORKERROR     = 202,

    /* 数据验证 */
    CLAW_EX_VALIDATIONERROR  = 300,
    CLAW_EX_TYPEERROR        = 301,
    CLAW_EX_RANGEERROR       = 302,
    CLAW_EX_NULLERROR        = 303,
    CLAW_EX_INDEXERROR       = 304,

    /* 业务逻辑 */
    CLAW_EX_PAYMENTERROR     = 400,
    CLAW_EX_AUTHERROR        = 401,
    CLAW_EX_PERMISSIONERROR  = 402,

    /* 内存 */
    CLAW_EX_OUTOFMEMORY      = 500,
    CLAW_EX_ALLOCERROR       = 501,

    /* 用户自定义范围: 1000+ */
    CLAW_EX_USER_BASE        = 1000

} ClawExceptionType;


/* ================================================================
 *  2. 异常结构体
 * ================================================================ */

#define CLAW_EX_MESSAGE_MAX 512

typedef struct ClawException {
    ClawExceptionType type;
    char              message[CLAW_EX_MESSAGE_MAX];
    const char*       file;        /* 源文件名 */
    int               line;        /* 源文件行号 */
    bool              is_active;   /* 是否有活跃异常 */
} ClawException;


/* ================================================================
 *  3. 异常上下文（线程局部，支持嵌套 TRY）
 * ================================================================ */

#define CLAW_MAX_TRY_DEPTH 32

typedef struct ClawExceptionContext {
    jmp_buf         jump_buffers[CLAW_MAX_TRY_DEPTH];
    int             depth;
    ClawException   current_exception;
} ClawExceptionContext;

/*
 * 线程局部异常上下文
 * 
 * C11: _Thread_local
 * GCC/Clang: __thread
 * MSVC: __declspec(thread)
 */
#if defined(__STDC_VERSION__) && __STDC_VERSION__ >= 201112L
    #define CLAW_THREAD_LOCAL _Thread_local
#elif defined(__GNUC__) || defined(__clang__)
    #define CLAW_THREAD_LOCAL __thread
#elif defined(_MSC_VER)
    #define CLAW_THREAD_LOCAL __declspec(thread)
#else
    #define CLAW_THREAD_LOCAL /* fallback: 非线程安全 */
#endif

static CLAW_THREAD_LOCAL ClawExceptionContext __claw_ex_ctx = {
    .depth = 0,
    .current_exception = {
        .type = CLAW_EX_NONE,
        .message = "",
        .file = NULL,
        .line = 0,
        .is_active = false
    }
};

/* 便捷访问当前异常 */
#define __claw_current_exception (__claw_ex_ctx.current_exception)


/* ================================================================
 *  4. TRY / CATCH / END_TRY 宏
 * 
 *  使用方式:
 * 
 *    CLAW_TRY
 *        // 正常代码
 *        // 可以调用 __claw_throw(CLAW_EX_IOERROR, "msg");
 * 
 *    CLAW_CATCH(CLAW_EX_IOERROR)
 *        // 处理 IOError
 *        ClawException* e = &__claw_current_exception;
 *        printf("error: %s
", e->message);
 * 
 *    CLAW_CATCH(CLAW_EX_VALIDATIONERROR)
 *        // 处理 ValidationError
 * 
 *    CLAW_END_TRY;
 * 
 *  实现原理:
 *    - CLAW_TRY: push jmp_buf, setjmp
 *    - __claw_throw: 填充异常信息, longjmp
 *    - CLAW_CATCH: 检查异常类型, 如匹配则处理
 *    - CLAW_END_TRY: pop jmp_buf, 如有未处理异常则向上传播
 * ================================================================ */

/*
 * CLAW_TRY 展开为:
 *   1. 检查嵌套深度
 *   2. push jmp_buf 到栈
 *   3. setjmp，返回0表示正常进入，非0表示异常跳回
 *   4. 如果正常进入(setjmp == 0)，执行 try 块代码
 */
#define CLAW_TRY                                                          \
    do {                                                                  \
        if (__claw_ex_ctx.depth >= CLAW_MAX_TRY_DEPTH) {                 \
            fprintf(stderr, "[Claw Runtime] TRY nesting overflow");    \
            abort();                                                      \
        }                                                                 \
        int __claw_try_depth = __claw_ex_ctx.depth;                      \
        __claw_ex_ctx.depth++;                                            \
        int __claw_setjmp_val = setjmp(                                  \
            __claw_ex_ctx.jump_buffers[__claw_try_depth]                 \
        );                                                                \
        bool __claw_exception_handled = false;                            \
        if (__claw_setjmp_val == 0) {                                    \
            /* === TRY block begin === */

/*
 * CLAW_CATCH(type) 展开为:
 *   1. 结束上一个块（try 或前一个 catch）
 *   2. 检查当前异常类型是否匹配
 *   3. 如果匹配，标记为已处理，执行 catch 块
 */
#define CLAW_CATCH(exception_type)                                        \
            /* === end of previous block === */                           \
        } else if (!__claw_exception_handled &&                          \
                   __claw_ex_ctx.current_exception.type == (exception_type)) { \
            __claw_exception_handled = true;                              \
            /* === CATCH block for exception_type === */

/*
 * CLAW_CATCH_ALL 展开为: 捕获所有未匹配的异常
 */
#define CLAW_CATCH_ALL                                                    \
        } else if (!__claw_exception_handled &&                          \
                   __claw_ex_ctx.current_exception.is_active) {          \
            __claw_exception_handled = true;                              \
            /* === CATCH ALL block === */

/*
 * CLAW_END_TRY 展开为:
 *   1. 结束最后一个块
 *   2. pop jmp_buf
 *   3. 如果有未处理异常，向上层传播
 */
#define CLAW_END_TRY                                                      \
        }                                                                 \
        __claw_ex_ctx.depth--;                                            \
        if (__claw_exception_handled) {                                   \
            /* 异常已处理，清除 */                                         \
            __claw_ex_ctx.current_exception.is_active = false;           \
            __claw_ex_ctx.current_exception.type = CLAW_EX_NONE;         \
            __claw_ex_ctx.current_exception.message[0] = '\0';           \
        } else if (__claw_ex_ctx.current_exception.is_active) {          \
            /* 未处理，向上传播 */                                         \
            if (__claw_ex_ctx.depth > 0) {                               \
                longjmp(                                                  \
                    __claw_ex_ctx.jump_buffers[__claw_ex_ctx.depth - 1], \
                    1                                                     \
                );                                                        \
            } else {                                                      \
                /* 没有外层 TRY，打印并终止 */                              \
                fprintf(stderr,                                           \
                    "[Claw Runtime] Unhandled exception (type=%d): %s" \
                    "  at %s:%d",                                       \
                    __claw_ex_ctx.current_exception.type,                 \
                    __claw_ex_ctx.current_exception.message,              \
                    __claw_ex_ctx.current_exception.file ?                \
                        __claw_ex_ctx.current_exception.file : "unknown", \
                    __claw_ex_ctx.current_exception.line               \
                );                                                        \
                abort();                                                  \
            }                                                             \
        }                                                                 \
    } while (0)


/* ================================================================
 *  5. 抛出异常
 * ================================================================ */

/**
 * __claw_throw - 抛出 Claw 异常
 * 
 * @param type    异常类型枚举值
 * @param message 错误消息
 * 
 * 行为:
 *   1. 填充当前异常结构体
 *   2. 如果在 TRY 块内，longjmp 跳回对应的 setjmp
 *   3. 如果不在 TRY 块内，打印错误并 abort
 */
static inline void __claw_throw_impl(
    ClawExceptionType type,
    const char* message,
    const char* file,
    int line
) {
    __claw_ex_ctx.current_exception.type = type;
    __claw_ex_ctx.current_exception.is_active = true;
    __claw_ex_ctx.current_exception.file = file;
    __claw_ex_ctx.current_exception.line = line;

    if (message != NULL) {
        strncpy(
            __claw_ex_ctx.current_exception.message,
            message,
            CLAW_EX_MESSAGE_MAX - 1
        );
        __claw_ex_ctx.current_exception.message[CLAW_EX_MESSAGE_MAX - 1] = '\0';
    } else {
        __claw_ex_ctx.current_exception.message[0] = '\0';
    }

    if (__claw_ex_ctx.depth > 0) {
        longjmp(
            __claw_ex_ctx.jump_buffers[__claw_ex_ctx.depth - 1],
            1
        );
    } else {
        fprintf(stderr,
            "[Claw Runtime] Unhandled exception (type=%d): %s"
            "  at %s:%d",
            type, __claw_ex_ctx.current_exception.message,
            file ? file : "unknown", line
        );
        abort();
    }
}

/* 带文件/行号的宏包装 */
#define __claw_throw(type, message) \
    __claw_throw_impl((type), (message), __FILE__, __LINE__)


/* ================================================================
 *  6. 异常类型名称查询（调试用）
 * ================================================================ */

static inline const char* __claw_exception_type_name(ClawExceptionType type) {
    switch (type) {
        case CLAW_EX_NONE:             return "None";
        case CLAW_EX_EXCEPTION:        return "Exception";
        case CLAW_EX_RUNTIME:          return "RuntimeError";
        case CLAW_EX_LOGIC:            return "LogicError";
        case CLAW_EX_IOERROR:          return "IOError";
        case CLAW_EX_FILEERROR:        return "FileError";
        case CLAW_EX_NETWORKERROR:     return "NetworkError";
        case CLAW_EX_VALIDATIONERROR:  return "ValidationError";
        case CLAW_EX_TYPEERROR:        return "TypeError";
        case CLAW_EX_RANGEERROR:       return "RangeError";
        case CLAW_EX_NULLERROR:        return "NullError";
        case CLAW_EX_INDEXERROR:       return "IndexError";
        case CLAW_EX_PAYMENTERROR:     return "PaymentError";
        case CLAW_EX_AUTHERROR:        return "AuthError";
        case CLAW_EX_PERMISSIONERROR:  return "PermissionError";
        case CLAW_EX_OUTOFMEMORY:      return "OutOfMemoryError";
        case CLAW_EX_ALLOCERROR:       return "AllocError";
        default:                       return "UnknownException";
    }
}

/* Claw 异常类型字符串 -> 枚举值 映射 */
static inline ClawExceptionType __claw_exception_from_name(const char* name) {
    if (name == NULL) return CLAW_EX_EXCEPTION;

    if (strcmp(name, "IOError") == 0)            return CLAW_EX_IOERROR;
    if (strcmp(name, "FileError") == 0)           return CLAW_EX_FILEERROR;
    if (strcmp(name, "NetworkError") == 0)        return CLAW_EX_NETWORKERROR;
    if (strcmp(name, "ValidationError") == 0)     return CLAW_EX_VALIDATIONERROR;
    if (strcmp(name, "TypeError") == 0)           return CLAW_EX_TYPEERROR;
    if (strcmp(name, "RangeError") == 0)          return CLAW_EX_RANGEERROR;
    if (strcmp(name, "NullError") == 0)           return CLAW_EX_NULLERROR;
    if (strcmp(name, "IndexError") == 0)          return CLAW_EX_INDEXERROR;
    if (strcmp(name, "PaymentError") == 0)        return CLAW_EX_PAYMENTERROR;
    if (strcmp(name, "AuthError") == 0)           return CLAW_EX_AUTHERROR;
    if (strcmp(name, "PermissionError") == 0)     return CLAW_EX_PERMISSIONERROR;
    if (strcmp(name, "OutOfMemoryError") == 0)    return CLAW_EX_OUTOFMEMORY;
    if (strcmp(name, "AllocError") == 0)          return CLAW_EX_ALLOCERROR;
    if (strcmp(name, "RuntimeError") == 0)        return CLAW_EX_RUNTIME;
    if (strcmp(name, "LogicError") == 0)          return CLAW_EX_LOGIC;

    return CLAW_EX_EXCEPTION; /* 默认通用异常 */
}


/* ================================================================
 *  7. 属性变更监听
 * ================================================================ */

/**
 * 属性监听回调函数类型
 * 
 * before 回调: (void* obj, const char* prop_path, const void* new_value)
 * after  回调: (void* obj, const char* prop_path, const void* old_value, const void* new_value)
 */
typedef void (*ClawBeforePropCallback)(void* obj, const char* prop_path, const void* new_value);
typedef void (*ClawAfterPropCallback)(void* obj, const char* prop_path, const void* old_value, const void* new_value);

#define CLAW_MAX_PROP_LISTENERS 64

typedef struct ClawPropListener {
    char                   prop_path[128];
    ClawBeforePropCallback before_cb;
    ClawAfterPropCallback  after_cb;
} ClawPropListener;

typedef struct ClawPropMonitor {
    ClawPropListener listeners[CLAW_MAX_PROP_LISTENERS];
    int              count;
} ClawPropMonitor;

/* 全局属性监听器注册表 */
static ClawPropMonitor __claw_prop_monitor = { .count = 0 };

static inline void __claw_register_before_prop(
    const char* prop_path,
    ClawBeforePropCallback cb
) {
    if (__claw_prop_monitor.count >= CLAW_MAX_PROP_LISTENERS) {
        fprintf(stderr, "[Claw Runtime] Property listener overflow");
        return;
    }
    int idx = __claw_prop_monitor.count;
    strncpy(__claw_prop_monitor.listeners[idx].prop_path, prop_path, 127);
    __claw_prop_monitor.listeners[idx].prop_path[127] = '\0';
    __claw_prop_monitor.listeners[idx].before_cb = cb;
    __claw_prop_monitor.listeners[idx].after_cb = NULL;
    __claw_prop_monitor.count++;
}

static inline void __claw_register_after_prop(
    const char* prop_path,
    ClawAfterPropCallback cb
) {
    /* 先查找是否已有同路径的 listener */
    for (int i = 0; i < __claw_prop_monitor.count; i++) {
        if (strcmp(__claw_prop_monitor.listeners[i].prop_path, prop_path) == 0) {
            __claw_prop_monitor.listeners[i].after_cb = cb;
            return;
        }
    }
    /* 新增 */
    if (__claw_prop_monitor.count >= CLAW_MAX_PROP_LISTENERS) {
        fprintf(stderr, "[Claw Runtime] Property listener overflow");
        return;
    }
    int idx = __claw_prop_monitor.count;
    strncpy(__claw_prop_monitor.listeners[idx].prop_path, prop_path, 127);
    __claw_prop_monitor.listeners[idx].prop_path[127] = '\0';
    __claw_prop_monitor.listeners[idx].before_cb = NULL;
    __claw_prop_monitor.listeners[idx].after_cb = cb;
    __claw_prop_monitor.count++;
}

static inline void __claw_before_prop_change(
    void* obj,
    const char* prop_path,
    const void* new_value
) {
    for (int i = 0; i < __claw_prop_monitor.count; i++) {
        if (strcmp(__claw_prop_monitor.listeners[i].prop_path, prop_path) == 0) {
            if (__claw_prop_monitor.listeners[i].before_cb != NULL) {
                __claw_prop_monitor.listeners[i].before_cb(obj, prop_path, new_value);
            }
        }
    }
}

static inline void __claw_after_prop_change(
    void* obj,
    const char* prop_path,
    const void* old_value,
    const void* new_value
) {
    for (int i = 0; i < __claw_prop_monitor.count; i++) {
        if (strcmp(__claw_prop_monitor.listeners[i].prop_path, prop_path) == 0) {
            if (__claw_prop_monitor.listeners[i].after_cb != NULL) {
                __claw_prop_monitor.listeners[i].after_cb(obj, prop_path, old_value, new_value);
            }
        }
    }
}


/* ================================================================
 *  8. 字符串辅助函数
 * ================================================================ */

/**
 * 字符串拼接（可变参数，以 NULL 结尾）
 * 
 * 调用者负责 free 返回的指针
 * 
 * 用法:
 *   char* s = __claw_str_concat("Hello", " ", "World", NULL);
 *   printf("%s
", s);
 *   free(s);
 */
static inline char* __claw_str_concat_impl(const char* first, ...) {
    if (first == NULL) return strdup("");

    /* 第一遍: 计算总长度 */
    size_t total_len = strlen(first);
    va_list args;
    va_start(args, first);
    const char* s;
    while ((s = va_arg(args, const char*)) != NULL) {
        total_len += strlen(s);
    }
    va_end(args);

    /* 分配 */
    char* result = (char*)malloc(total_len + 1);
    if (result == NULL) {
        fprintf(stderr, "[Claw Runtime] String concat: malloc failed");
        return strdup("");
    }

    /* 第二遍: 拼接 */
    strcpy(result, first);
    va_start(args, first);
    while ((s = va_arg(args, const char*)) != NULL) {
        strcat(result, s);
    }
    va_end(args);

    return result;
}

#define __claw_str_concat(...) __claw_str_concat_impl(__VA_ARGS__, NULL)

/**
 * 整数转字符串
 * 返回静态缓冲区指针（非线程安全的简化版）
 * 线程安全版本应使用 malloc
 */
static inline const char* __claw_int_to_str(int value) {
    static CLAW_THREAD_LOCAL char buf[32];
    snprintf(buf, sizeof(buf), "%d", value);
    return buf;
}

/**
 * 浮点转字符串
 */
static inline const char* __claw_float_to_str(double value) {
    static CLAW_THREAD_LOCAL char buf[64];
    snprintf(buf, sizeof(buf), "%g", value);
    return buf;
}

/**
 * 布尔转字符串
 */
static inline const char* __claw_bool_to_str(bool value) {
    return value ? "true" : "false";
}


/* ================================================================
 *  9. 安全内存管理辅助
 * ================================================================ */

/**
 * 安全 malloc，失败时抛出 CLAW_EX_OUTOFMEMORY
 */
static inline void* __claw_safe_malloc(size_t size) {
    void* ptr = malloc(size);
    if (ptr == NULL && size > 0) {
        __claw_throw(CLAW_EX_OUTOFMEMORY, "malloc failed");
    }
    return ptr;
}

/**
 * 安全 calloc
 */
static inline void* __claw_safe_calloc(size_t count, size_t size) {
    void* ptr = calloc(count, size);
    if (ptr == NULL && count > 0 && size > 0) {
        __claw_throw(CLAW_EX_OUTOFMEMORY, "calloc failed");
    }
    return ptr;
}

/**
 * 安全 realloc
 */
static inline void* __claw_safe_realloc(void* ptr, size_t new_size) {
    void* new_ptr = realloc(ptr, new_size);
    if (new_ptr == NULL && new_size > 0) {
        __claw_throw(CLAW_EX_OUTOFMEMORY, "realloc failed");
    }
    return new_ptr;
}

/**
 * 安全 free + 置空
 */
#define __claw_safe_free(ptr) do { \
    if ((ptr) != NULL) {           \
        free(ptr);                 \
        (ptr) = NULL;              \
    }                              \
} while (0)


#ifdef __cplusplus
}
#endif

// 用于与 C 交互的底层类型
type Pointer = __builtin_pointer        // void*
type OpaquePointer = __builtin_pointer  // 不透明指针（不能解引用）
type Ref<T> = __builtin_ref             // T** （传出参数）
type CArray<T> = __builtin_c_array      // C 风格数组 T*
type CString = __builtin_c_string       // const char*（与 String 自动转换）
type FuncPointer = __builtin_func_ptr   // 函数指针

// 固定宽度整数（对应 C 的 stdint.h）
type Int8  = __builtin_int8             // int8_t
type Int16 = __builtin_int16            // int16_t
type Int32 = __builtin_int32            // int32_t
type Int64 = __builtin_int64            // int64_t
type UInt8  = __builtin_uint8
type UInt16 = __builtin_uint16
type UInt32 = __builtin_uint32
type UInt64 = __builtin_uint64
type SizeT  = __builtin_size_t          // size_t


#endif /* CLAW_RUNTIME_H */
