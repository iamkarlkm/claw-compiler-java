# claw_compiler/stdlib/c_stdlib_registry.py

"""
C标准库注册表
将Claw标准库函数映射到C标准库函数
"""

from dataclasses import dataclass, field
from enum import Enum, auto
from typing import List, Optional, Dict, Tuple


class CHeader(Enum):
    """C标准头文件枚举"""
    STDIO    = "stdio.h"
    STDLIB   = "stdlib.h"
    STRING   = "string.h"
    MATH     = "math.h"
    CTYPE    = "ctype.h"
    TIME     = "time.h"
    STDINT   = "stdint.h"
    STDBOOL  = "stdbool.h"
    STDARG   = "stdarg.h"
    ERRNO    = "errno.h"
    SIGNAL   = "signal.h"
    SETJMP   = "setjmp.h"
    ASSERT   = "assert.h"
    LIMITS   = "limits.h"
    FLOAT    = "float.h"
    STDDEF   = "stddef.h"
    LOCALE   = "locale.h"


class ParamPassMode(Enum):
    """参数传递模式"""
    BY_VALUE   = auto()   # 值传递
    BY_POINTER = auto()   # 指针传递
    BY_ARRAY   = auto()   # 数组传递
    VARIADIC   = auto()   # 可变参数


@dataclass
class CParamMapping:
    """C函数参数映射"""
    claw_name: str              # Claw参数名
    claw_type: str              # Claw类型
    c_type: str                 # 对应的C类型
    pass_mode: ParamPassMode    # 传递模式
    is_optional: bool = False   # 是否可选
    default_value: str = None   # 默认值（C表达式）
    transform: str = None       # 转换表达式模板，如 "(const char*){value}"


@dataclass
class CReturnMapping:
    """C函数返回值映射"""
    claw_type: str          # Claw返回类型
    c_type: str             # C返回类型
    transform: str = None   # 返回值转换表达式
    error_check: str = None # 错误检查表达式，如 "{result} == NULL"
    error_value: str = None # 错误时的Claw值


@dataclass
class CFunctionMapping:
    """
    单个C标准库函数的完整映射
    描述了Claw函数签名到C函数调用的完整转换规则
    """
    # Claw侧
    claw_module: str                    # Claw模块路径，如 "std.io"
    claw_name: str                      # Claw函数名
    claw_signature: str                 # Claw完整签名（用于文档）
    
    # C侧
    c_name: str                         # C函数名
    c_headers: List[CHeader]            # 需要include的头文件
    c_params: List[CParamMapping]       # 参数映射列表
    c_return: CReturnMapping            # 返回值映射
    
    # 代码生成
    c_call_template: str = None         # 自定义调用模板（用于复杂映射）
    c_pre_code: List[str] = field(default_factory=list)   # 调用前插入的C代码
    c_post_code: List[str] = field(default_factory=list)  # 调用后插入的C代码
    
    # 元信息
    description: str = ""               # 函数描述
    since_c_standard: str = "C89"       # 最低C标准要求
    is_unsafe: bool = False             # 是否标记为unsafe
    deprecated_alternative: str = None  # 如果已废弃，替代函数


class CStdlibRegistry:
    """
    C标准库注册表
    管理所有Claw→C的函数映射
    """
    
    def __init__(self):
        self._mappings: Dict[str, CFunctionMapping] = {}  # key: "module.name"
        self._header_deps: Dict[str, set] = {}            # 文件级头文件依赖
        self._type_map: Dict[str, str] = {}               # Claw类型→C类型
        
        self._register_type_mappings()
        self._register_stdio()
        self._register_stdlib()
        self._register_string()
        self._register_math()
        self._register_ctype()
        self._register_time()
    
    def _register_type_mappings(self):
        """注册Claw基础类型到C类型的映射"""
        self._type_map = {
            # 基础类型
            "Int":      "int",
            "Int8":     "int8_t",
            "Int16":    "int16_t",
            "Int32":    "int32_t",
            "Int64":    "int64_t",
            "UInt":     "unsigned int",
            "UInt8":    "uint8_t",
            "UInt16":   "uint16_t",
            "UInt32":   "uint32_t",
            "UInt64":   "uint64_t",
            "Float":    "float",
            "Float32":  "float",
            "Float64":  "double",
            "Double":   "double",
            "Bool":     "bool",
            "Char":     "char",
            "String":   "char*",
            "Void":     "void",
            "Any":      "void*",
            
            # 指针类型
            "Ptr<Int>":    "int*",
            "Ptr<Float>":  "float*",
            "Ptr<Double>": "double*",
            "Ptr<Char>":   "char*",
            "Ptr<Void>":   "void*",
            "Ptr<Any>":    "void*",
            
            # 特殊类型
            "Size":     "size_t",
            "CFile":    "FILE*",
            "NullPtr":  "NULL",
        }
    
    # =========================================================
    #                    stdio.h 映射
    # =========================================================
    
    def _register_stdio(self):
        """注册 stdio.h 函数映射"""
        
        # ---------- printf ----------
        self._register("std.io", CFunctionMapping(
            claw_module="std.io",
            claw_name="print",
            claw_signature="print(format: String, ...args: Any) -> Int",
            c_name="printf",
            c_headers=[CHeader.STDIO],
            c_params=[
                CParamMapping("format", "String", "const char*", 
                             ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("Int", "int"),
            c_call_template="printf({format}{variadic_args})",
            description="格式化输出到标准输出"
        ))
        
        # ---------- println ----------
        self._register("std.io", CFunctionMapping(
            claw_module="std.io",
            claw_name="println",
            claw_signature="println(format: String, ...args: Any) -> Int",
            c_name="printf",
            c_headers=[CHeader.STDIO],
            c_params=[
                CParamMapping("format", "String", "const char*",
                             ParamPassMode.BY_VALUE,
                             transform='{value} "\
"'),  # 自动追加换行
            ],
            c_return=CReturnMapping("Int", "int"),
            c_call_template='printf({format} "\
"{variadic_args})',
            description="格式化输出到标准输出（自动换行）"
        ))
        
        # ---------- sprintf / snprintf ----------
        self._register("std.io", CFunctionMapping(
            claw_module="std.io",
            claw_name="formatString",
            claw_signature="formatString(buffer: Ptr<Char>, size: Size, format: String, ...args: Any) -> Int",
            c_name="snprintf",
            c_headers=[CHeader.STDIO],
            c_params=[
                CParamMapping("buffer", "Ptr<Char>", "char*", ParamPassMode.BY_POINTER),
                CParamMapping("size", "Size", "size_t", ParamPassMode.BY_VALUE),
                CParamMapping("format", "String", "const char*", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("Int", "int"),
            description="格式化字符串到缓冲区（安全版本）"
        ))
        
        # ---------- fprintf ----------
        self._register("std.io", CFunctionMapping(
            claw_module="std.io",
            claw_name="fprint",
            claw_signature="fprint(file: CFile, format: String, ...args: Any) -> Int",
            c_name="fprintf",
            c_headers=[CHeader.STDIO],
            c_params=[
                CParamMapping("file", "CFile", "FILE*", ParamPassMode.BY_VALUE),
                CParamMapping("format", "String", "const char*", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("Int", "int"),
            description="格式化输出到文件"
        ))
        
        # ---------- scanf ----------
        self._register("std.io", CFunctionMapping(
            claw_module="std.io",
            claw_name="scan",
            claw_signature="scan(format: String, ...args: Any) -> Int",
            c_name="scanf",
            c_headers=[CHeader.STDIO],
            c_params=[
                CParamMapping("format", "String", "const char*", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("Int", "int",
                                   error_check="{result} == EOF",
                                   error_value="-1"),
            c_call_template="scanf({format}{variadic_args})",
            description="格式化输入"
        ))
        
        # ---------- fopen ----------
        self._register("std.io", CFunctionMapping(
            claw_module="std.io",
            claw_name="fileOpen",
            claw_signature="fileOpen(path: String, mode: String) -> CFile",
            c_name="fopen",
            c_headers=[CHeader.STDIO],
            c_params=[
                CParamMapping("path", "String", "const char*", ParamPassMode.BY_VALUE),
                CParamMapping("mode", "String", "const char*", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("CFile", "FILE*",
                                   error_check="{result} == NULL",
                                   error_value="null"),
            description="打开文件"
        ))
        
        # ---------- fclose ----------
        self._register("std.io", CFunctionMapping(
            claw_module="std.io",
            claw_name="fileClose",
            claw_signature="fileClose(file: CFile) -> Int",
            c_name="fclose",
            c_headers=[CHeader.STDIO],
            c_params=[
                CParamMapping("file", "CFile", "FILE*", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("Int", "int",
                                   error_check="{result} == EOF"),
            description="关闭文件"
        ))
        
        # ---------- fread ----------
        self._register("std.io", CFunctionMapping(
            claw_module="std.io",
            claw_name="fileRead",
            claw_signature="fileRead(buffer: Ptr<Void>, size: Size, count: Size, file: CFile) -> Size",
            c_name="fread",
            c_headers=[CHeader.STDIO],
            c_params=[
                CParamMapping("buffer", "Ptr<Void>", "void*", ParamPassMode.BY_POINTER),
                CParamMapping("size", "Size", "size_t", ParamPassMode.BY_VALUE),
                CParamMapping("count", "Size", "size_t", ParamPassMode.BY_VALUE),
                CParamMapping("file", "CFile", "FILE*", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("Size", "size_t"),
            description="从文件读取数据"
        ))
        
        # ---------- fwrite ----------
        self._register("std.io", CFunctionMapping(
            claw_module="std.io",
            claw_name="fileWrite",
            claw_signature="fileWrite(data: Ptr<Void>, size: Size, count: Size, file: CFile) -> Size",
            c_name="fwrite",
            c_headers=[CHeader.STDIO],
            c_params=[
                CParamMapping("data", "Ptr<Void>", "const void*", ParamPassMode.BY_POINTER),
                CParamMapping("size", "Size", "size_t", ParamPassMode.BY_VALUE),
                CParamMapping("count", "Size", "size_t", ParamPassMode.BY_VALUE),
                CParamMapping("file", "CFile", "FILE*", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("Size", "size_t"),
            description="写入数据到文件"
        ))
        
        # ---------- fgets ----------
        self._register("std.io", CFunctionMapping(
            claw_module="std.io",
            claw_name="fileReadLine",
            claw_signature="fileReadLine(buffer: Ptr<Char>, size: Int, file: CFile) -> String",
            c_name="fgets",
            c_headers=[CHeader.STDIO],
            c_params=[
                CParamMapping("buffer", "Ptr<Char>", "char*", ParamPassMode.BY_POINTER),
                CParamMapping("size", "Int", "int", ParamPassMode.BY_VALUE),
                CParamMapping("file", "CFile", "FILE*", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("String", "char*",
                                   error_check="{result} == NULL"),
            description="从文件读取一行"
        ))
        
        # ---------- fputs ----------
        self._register("std.io", CFunctionMapping(
            claw_module="std.io",
            claw_name="fileWriteString",
            claw_signature="fileWriteString(str: String, file: CFile) -> Int",
            c_name="fputs",
            c_headers=[CHeader.STDIO],
            c_params=[
                CParamMapping("str", "String", "const char*", ParamPassMode.BY_VALUE),
                CParamMapping("file", "CFile", "FILE*", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("Int", "int",
                                   error_check="{result} == EOF"),
            description="写入字符串到文件"
        ))
        
        # ---------- fseek ----------
        self._register("std.io", CFunctionMapping(
            claw_module="std.io",
            claw_name="fileSeek",
            claw_signature="fileSeek(file: CFile, offset: Int64, origin: Int) -> Int",
            c_name="fseek",
            c_headers=[CHeader.STDIO],
            c_params=[
                CParamMapping("file", "CFile", "FILE*", ParamPassMode.BY_VALUE),
                CParamMapping("offset", "Int64", "long", ParamPassMode.BY_VALUE),
                CParamMapping("origin", "Int", "int", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("Int", "int"),
            description="设置文件位置"
        ))
        
        # ---------- ftell ----------
        self._register("std.io", CFunctionMapping(
            claw_module="std.io",
            claw_name="fileTell",
            claw_signature="fileTell(file: CFile) -> Int64",
            c_name="ftell",
            c_headers=[CHeader.STDIO],
            c_params=[
                CParamMapping("file", "CFile", "FILE*", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("Int64", "long",
                                   error_check="{result} == -1L"),
            description="获取文件当前位置"
        ))
        
        # ---------- remove ----------
        self._register("std.io", CFunctionMapping(
            claw_module="std.io",
            claw_name="fileRemove",
            claw_signature="fileRemove(path: String) -> Int",
            c_name="remove",
            c_headers=[CHeader.STDIO],
            c_params=[
                CParamMapping("path", "String", "const char*", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("Int", "int"),
            description="删除文件"
        ))
        
        # ---------- rename ----------
        self._register("std.io", CFunctionMapping(
            claw_module="std.io",
            claw_name="fileRename",
            claw_signature="fileRename(oldPath: String, newPath: String) -> Int",
            c_name="rename",
            c_headers=[CHeader.STDIO],
            c_params=[
                CParamMapping("oldPath", "String", "const char*", ParamPassMode.BY_VALUE),
                CParamMapping("newPath", "String", "const char*", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("Int", "int"),
            description="重命名文件"
        ))
    
    # =========================================================
    #                    stdlib.h 映射
    # =========================================================
    
    def _register_stdlib(self):
        """注册 stdlib.h 函数映射"""
        
        # ---------- malloc ----------
        self._register("std.mem", CFunctionMapping(
            claw_module="std.mem",
            claw_name="alloc",
            claw_signature="alloc(size: Size) -> Ptr<Void>",
            c_name="malloc",
            c_headers=[CHeader.STDLIB],
            c_params=[
                CParamMapping("size", "Size", "size_t", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("Ptr<Void>", "void*",
                                   error_check="{result} == NULL",
                                   error_value="null"),
            description="分配内存",
            is_unsafe=True
        ))
        
        # ---------- calloc ----------
        self._register("std.mem", CFunctionMapping(
            claw_module="std.mem",
            claw_name="allocZeroed",
            claw_signature="allocZeroed(count: Size, size: Size) -> Ptr<Void>",
            c_name="calloc",
            c_headers=[CHeader.STDLIB],
            c_params=[
                CParamMapping("count", "Size", "size_t", ParamPassMode.BY_VALUE),
                CParamMapping("size", "Size", "size_t", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("Ptr<Void>", "void*",
                                   error_check="{result} == NULL"),
            description="分配并清零内存",
            is_unsafe=True
        ))
        
        # ---------- realloc ----------
        self._register("std.mem", CFunctionMapping(
            claw_module="std.mem",
            claw_name="realloc",
            claw_signature="realloc(ptr: Ptr<Void>, newSize: Size) -> Ptr<Void>",
            c_name="realloc",
            c_headers=[CHeader.STDLIB],
            c_params=[
                CParamMapping("ptr", "Ptr<Void>", "void*", ParamPassMode.BY_POINTER),
                CParamMapping("newSize", "Size", "size_t", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("Ptr<Void>", "void*",
                                   error_check="{result} == NULL"),
            description="重新分配内存",
            is_unsafe=True
        ))
        
        # ---------- free ----------
        self._register("std.mem", CFunctionMapping(
            claw_module="std.mem",
            claw_name="free",
            claw_signature="free(ptr: Ptr<Void>) -> Void",
            c_name="free",
            c_headers=[CHeader.STDLIB],
            c_params=[
                CParamMapping("ptr", "Ptr<Void>", "void*", ParamPassMode.BY_POINTER),
            ],
            c_return=CReturnMapping("Void", "void"),
            description="释放内存",
            is_unsafe=True
        ))
        
        # ---------- atoi ----------
        self._register("std.convert", CFunctionMapping(
            claw_module="std.convert",
            claw_name="toInt",
            claw_signature="toInt(str: String) -> Int",
            c_name="atoi",
            c_headers=[CHeader.STDLIB],
            c_params=[
                CParamMapping("str", "String", "const char*", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("Int", "int"),
            description="字符串转整数"
        ))
        
        # ---------- atof ----------
        self._register("std.convert", CFunctionMapping(
            claw_module="std.convert",
            claw_name="toFloat",
            claw_signature="toFloat(str: String) -> Double",
            c_name="atof",
            c_headers=[CHeader.STDLIB],
            c_params=[
                CParamMapping("str", "String", "const char*", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("Double", "double"),
            description="字符串转浮点数"
        ))
        
        # ---------- strtol ----------
        self._register("std.convert", CFunctionMapping(
            claw_module="std.convert",
            claw_name="toIntBase",
            claw_signature="toIntBase(str: String, base: Int) -> Int64",
            c_name="strtol",
            c_headers=[CHeader.STDLIB],
            c_params=[
                CParamMapping("str", "String", "const char*", ParamPassMode.BY_VALUE),
                CParamMapping("base", "Int", "int", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("Int64", "long"),
            c_call_template="strtol({str}, NULL, {base})",
            description="字符串按指定进制转整数"
        ))
        
        # ---------- abs ----------
        self._register("std.math", CFunctionMapping(
            claw_module="std.math",
            claw_name="absInt",
            claw_signature="absInt(value: Int) -> Int",
            c_name="abs",
            c_headers=[CHeader.STDLIB],
            c_params=[
                CParamMapping("value", "Int", "int", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("Int", "int"),
            description="整数绝对值"
        ))
        
        # ---------- rand / srand ----------
        self._register("std.random", CFunctionMapping(
            claw_module="std.random",
            claw_name="random",
            claw_signature="random() -> Int",
            c_name="rand",
            c_headers=[CHeader.STDLIB],
            c_params=[],
            c_return=CReturnMapping("Int", "int"),
            description="生成随机整数"
        ))
        
        self._register("std.random", CFunctionMapping(
            claw_module="std.random",
            claw_name="seed",
            claw_signature="seed(value: UInt) -> Void",
            c_name="srand",
            c_headers=[CHeader.STDLIB],
            c_params=[
                CParamMapping("value", "UInt", "unsigned int", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("Void", "void"),
            description="设置随机数种子"
        ))
        
        # ---------- exit ----------
        self._register("std.process", CFunctionMapping(
            claw_module="std.process",
            claw_name="exit",
            claw_signature="exit(code: Int) -> Void",
            c_name="exit",
            c_headers=[CHeader.STDLIB],
            c_params=[
                CParamMapping("code", "Int", "int", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("Void", "void"),
            description="终止程序"
        ))
        
        # ---------- system ----------
        self._register("std.process", CFunctionMapping(
            claw_module="std.process",
            claw_name="exec",
            claw_signature="exec(command: String) -> Int",
            c_name="system",
            c_headers=[CHeader.STDLIB],
            c_params=[
                CParamMapping("command", "String", "const char*", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("Int", "int"),
            description="执行系统命令",
            is_unsafe=True
        ))
        
        # ---------- getenv ----------
        self._register("std.process", CFunctionMapping(
            claw_module="std.process",
            claw_name="getEnv",
            claw_signature="getEnv(name: String) -> String",
            c_name="getenv",
            c_headers=[CHeader.STDLIB],
            c_params=[
                CParamMapping("name", "String", "const char*", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("String", "char*",
                                   error_check="{result} == NULL"),
            description="获取环境变量"
        ))
        
        # ---------- qsort ----------
        self._register("std.algorithm", CFunctionMapping(
            claw_module="std.algorithm",
            claw_name="sort",
            claw_signature="sort(base: Ptr<Void>, count: Size, size: Size, compare: Function) -> Void",
            c_name="qsort",
            c_headers=[CHeader.STDLIB],
            c_params=[
                CParamMapping("base", "Ptr<Void>", "void*", ParamPassMode.BY_POINTER),
                CParamMapping("count", "Size", "size_t", ParamPassMode.BY_VALUE),
                CParamMapping("size", "Size", "size_t", ParamPassMode.BY_VALUE),
                CParamMapping("compare", "Function", 
                             "int (*)(const void*, const void*)", 
                             ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("Void", "void"),
            description="快速排序"
        ))
        
        # ---------- bsearch ----------
        self._register("std.algorithm", CFunctionMapping(
            claw_module="std.algorithm",
            claw_name="binarySearch",
            claw_signature="binarySearch(key: Ptr<Void>, base: Ptr<Void>, count: Size, size: Size, compare: Function) -> Ptr<Void>",
            c_name="bsearch",
            c_headers=[CHeader.STDLIB],
            c_params=[
                CParamMapping("key", "Ptr<Void>", "const void*", ParamPassMode.BY_POINTER),
                CParamMapping("base", "Ptr<Void>", "const void*", ParamPassMode.BY_POINTER),
                CParamMapping("count", "Size", "size_t", ParamPassMode.BY_VALUE),
                CParamMapping("size", "Size", "size_t", ParamPassMode.BY_VALUE),
                CParamMapping("compare", "Function",
                             "int (*)(const void*, const void*)",
                             ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("Ptr<Void>", "void*",
                                   error_check="{result} == NULL"),
            description="二分查找"
        ))
    
    # =========================================================
    #                    string.h 映射
    # =========================================================
    
    def _register_string(self):
        """注册 string.h 函数映射"""
        
        # ---------- strlen ----------
        self._register("std.string", CFunctionMapping(
            claw_module="std.string",
            claw_name="length",
            claw_signature="length(str: String) -> Size",
            c_name="strlen",
            c_headers=[CHeader.STRING],
            c_params=[
                CParamMapping("str", "String", "const char*", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("Size", "size_t"),
            description="获取字符串长度"
        ))
        
        # ---------- strcpy ----------
        self._register("std.string", CFunctionMapping(
            claw_module="std.string",
            claw_name="copy",
            claw_signature="copy(dest: Ptr<Char>, src: String) -> String",
            c_name="strcpy",
            c_headers=[CHeader.STRING],
            c_params=[
                CParamMapping("dest", "Ptr<Char>", "char*", ParamPassMode.BY_POINTER),
                CParamMapping("src", "String", "const char*", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("String", "char*"),
            description="复制字符串（不安全）",
            is_unsafe=True,
            deprecated_alternative="std.string.safeCopy"
        ))
        
        # ---------- strncpy ----------
        self._register("std.string", CFunctionMapping(
            claw_module="std.string",
            claw_name="safeCopy",
            claw_signature="safeCopy(dest: Ptr<Char>, src: String, maxLen: Size) -> String",
            c_name="strncpy",
            c_headers=[CHeader.STRING],
            c_params=[
                CParamMapping("dest", "Ptr<Char>", "char*", ParamPassMode.BY_POINTER),
                CParamMapping("src", "String", "const char*", ParamPassMode.BY_VALUE),
                CParamMapping("maxLen", "Size", "size_t", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("String", "char*"),
            description="安全复制字符串（限制长度）"
        ))
        
        # ---------- strcat ----------
        self._register("std.string", CFunctionMapping(
            claw_module="std.string",
            claw_name="concat",
            claw_signature="concat(dest: Ptr<Char>, src: String) -> String",
            c_name="strcat",
            c_headers=[CHeader.STRING],
            c_params=[
                CParamMapping("dest", "Ptr<Char>", "char*", ParamPassMode.BY_POINTER),
                CParamMapping("src", "String", "const char*", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("String", "char*"),
            description="拼接字符串",
            is_unsafe=True,
            deprecated_alternative="std.string.safeConcat"
        ))
        
        # ---------- strncat ----------
        self._register("std.string", CFunctionMapping(
            claw_module="std.string",
            claw_name="safeConcat",
            claw_signature="safeConcat(dest: Ptr<Char>, src: String, maxLen: Size) -> String",
            c_name="strncat",
            c_headers=[CHeader.STRING],
            c_params=[
                CParamMapping("dest", "Ptr<Char>", "char*", ParamPassMode.BY_POINTER),
                CParamMapping("src", "String", "const char*", ParamPassMode.BY_VALUE),
                CParamMapping("maxLen", "Size", "size_t", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("String", "char*"),
            description="安全拼接字符串"
        ))
        
        # ---------- strcmp ----------
        self._register("std.string", CFunctionMapping(
            claw_module="std.string",
            claw_name="compare",
            claw_signature="compare(a: String, b: String) -> Int",
            c_name="strcmp",
            c_headers=[CHeader.STRING],
            c_params=[
                CParamMapping("a", "String", "const char*", ParamPassMode.BY_VALUE),
                CParamMapping("b", "String", "const char*", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("Int", "int"),
            description="比较两个字符串"
        ))
        
        # ---------- strncmp ----------
        self._register("std.string", CFunctionMapping(
            claw_module="std.string",
            claw_name="compareN",
            claw_signature="compareN(a: String, b: String, maxLen: Size) -> Int",
            c_name="strncmp",
            c_headers=[CHeader.STRING],
            c_params=[
                CParamMapping("a", "String", "const char*", ParamPassMode.BY_VALUE),
                CParamMapping("b", "String", "const char*", ParamPassMode.BY_VALUE),
                CParamMapping("maxLen", "Size", "size_t", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("Int", "int"),
            description="比较字符串前N个字符"
        ))
        
        # ---------- strstr ----------
        self._register("std.string", CFunctionMapping(
            claw_module="std.string",
            claw_name="find",
            claw_signature="find(haystack: String, needle: String) -> String",
            c_name="strstr",
            c_headers=[CHeader.STRING],
            c_params=[
                CParamMapping("haystack", "String", "const char*", ParamPassMode.BY_VALUE),
                CParamMapping("needle", "String", "const char*", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("String", "char*",
                                   error_check="{result} == NULL"),
            description="查找子字符串"
        ))
        
        # ---------- strchr ----------
        self._register("std.string", CFunctionMapping(
            claw_module="std.string",
            claw_name="findChar",
            claw_signature="findChar(str: String, ch: Char) -> String",
            c_name="strchr",
            c_headers=[CHeader.STRING],
            c_params=[
                CParamMapping("str", "String", "const char*", ParamPassMode.BY_VALUE),
                CParamMapping("ch", "Char", "int", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("String", "char*",
                                   error_check="{result} == NULL"),
            description="查找字符首次出现位置"
        ))
        
        # ---------- strrchr ----------
        self._register("std.string", CFunctionMapping(
            claw_module="std.string",
            claw_name="findCharLast",
            claw_signature="findCharLast(str: String, ch: Char) -> String",
            c_name="strrchr",
            c_headers=[CHeader.STRING],
            c_params=[
                CParamMapping("str", "String", "const char*", ParamPassMode.BY_VALUE),
                CParamMapping("ch", "Char", "int", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("String", "char*",
                                   error_check="{result} == NULL"),
            description="查找字符最后出现位置"
        ))
        
        # ---------- strtok ----------
        self._register("std.string", CFunctionMapping(
            claw_module="std.string",
            claw_name="tokenize",
            claw_signature="tokenize(str: String, delimiters: String) -> String",
            c_name="strtok",
            c_headers=[CHeader.STRING],
            c_params=[
                CParamMapping("str", "String", "char*", ParamPassMode.BY_POINTER),
                CParamMapping("delimiters", "String", "const char*", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("String", "char*",
                                   error_check="{result} == NULL"),
            description="分割字符串（有状态，非线程安全）",
            is_unsafe=True
        ))
        
        # ---------- memset ----------
        self._register("std.mem", CFunctionMapping(
            claw_module="std.mem",
            claw_name="set",
            claw_signature="set(ptr: Ptr<Void>, value: Int, size: Size) -> Ptr<Void>",
            c_name="memset",
            c_headers=[CHeader.STRING],
            c_params=[
                CParamMapping("ptr", "Ptr<Void>", "void*", ParamPassMode.BY_POINTER),
                CParamMapping("value", "Int", "int", ParamPassMode.BY_VALUE),
                CParamMapping("size", "Size", "size_t", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("Ptr<Void>", "void*"),
            description="填充内存块"
        ))
        
        # ---------- memcpy ----------
        self._register("std.mem", CFunctionMapping(
            claw_module="std.mem",
            claw_name="copy",
            claw_signature="copy(dest: Ptr<Void>, src: Ptr<Void>, size: Size) -> Ptr<Void>",
            c_name="memcpy",
            c_headers=[CHeader.STRING],
            c_params=[
                CParamMapping("dest", "Ptr<Void>", "void*", ParamPassMode.BY_POINTER),
                CParamMapping("src", "Ptr<Void>", "const void*", ParamPassMode.BY_POINTER),
                CParamMapping("size", "Size", "size_t", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("Ptr<Void>", "void*"),
            description="复制内存块"
        ))
        
        # ---------- memmove ----------
        self._register("std.mem", CFunctionMapping(
            claw_module="std.mem",
            claw_name="move",
            claw_signature="move(dest: Ptr<Void>, src: Ptr<Void>, size: Size) -> Ptr<Void>",
            c_name="memmove",
            c_headers=[CHeader.STRING],
            c_params=[
                CParamMapping("dest", "Ptr<Void>", "void*", ParamPassMode.BY_POINTER),
                CParamMapping("src", "Ptr<Void>", "const void*", ParamPassMode.BY_POINTER),
                CParamMapping("size", "Size", "size_t", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("Ptr<Void>", "void*"),
            description="安全移动内存块（处理重叠）"
        ))
        
        # ---------- memcmp ----------
        self._register("std.mem", CFunctionMapping(
            claw_module="std.mem",
            claw_name="compare",
            claw_signature="compare(a: Ptr<Void>, b: Ptr<Void>, size: Size) -> Int",
            c_name="memcmp",
            c_headers=[CHeader.STRING],
            c_params=[
                CParamMapping("a", "Ptr<Void>", "const void*", ParamPassMode.BY_POINTER),
                CParamMapping("b", "Ptr<Void>", "const void*", ParamPassMode.BY_POINTER),
                CParamMapping("size", "Size", "size_t", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("Int", "int"),
            description="比较内存块"
        ))
    
    # =========================================================
    #                    math.h 映射
    # =========================================================
    
    def _register_math(self):
        """注册 math.h 函数映射"""
        
        math_funcs = [
            # (claw_name, c_name, description, params, return)
            ("sin",   "sin",   "正弦",     [("x", "Double", "double")], "Double"),
            ("cos",   "cos",   "余弦",     [("x", "Double", "double")], "Double"),
            ("tan",   "tan",   "正切",     [("x", "Double", "double")], "Double"),
            ("asin",  "asin",  "反正弦",   [("x", "Double", "double")], "Double"),
            ("acos",  "acos",  "反余弦",   [("x", "Double", "double")], "Double"),
            ("atan",  "atan",  "反正切",   [("x", "Double", "double")], "Double"),
            ("atan2", "atan2", "二参反正切", [("y", "Double", "double"), ("x", "Double", "double")], "Double"),
            ("sinh",  "sinh",  "双曲正弦", [("x", "Double", "double")], "Double"),
            ("cosh",  "cosh",  "双曲余弦", [("x", "Double", "double")], "Double"),
            ("tanh",  "tanh",  "双曲正切", [("x", "Double", "double")], "Double"),
            ("exp",   "exp",   "指数",     [("x", "Double", "double")], "Double"),
            ("log",   "log",   "自然对数", [("x", "Double", "double")], "Double"),
            ("log10", "log10", "常用对数", [("x", "Double", "double")], "Double"),
            ("log2",  "log2",  "二进制对数", [("x", "Double", "double")], "Double"),
            ("pow",   "pow",   "幂运算",   [("base", "Double", "double"), ("exp", "Double", "double")], "Double"),
            ("sqrt",  "sqrt",  "平方根",   [("x", "Double", "double")], "Double"),
            ("cbrt",  "cbrt",  "立方根",   [("x", "Double", "double")], "Double"),
            ("ceil",  "ceil",  "向上取整", [("x", "Double", "double")], "Double"),
            ("floor", "floor", "向下取整", [("x", "Double", "double")], "Double"),
            ("round", "round", "四舍五入", [("x", "Double", "double")], "Double"),
            ("fabs",  "abs",   "浮点绝对值", [("x", "Double", "double")], "Double"),
            ("fmod",  "mod",   "浮点取余", [("x", "Double", "double"), ("y", "Double", "double")], "Double"),
            ("hypot", "hypot", "斜边长度", [("x", "Double", "double"), ("y", "Double", "double")], "Double"),
        ]
        
        for c_func, claw_func_name, desc, params, ret_type in math_funcs:
            c_params = [
                CParamMapping(p[0], p[1], p[2], ParamPassMode.BY_VALUE)
                for p in params
            ]
            
            param_sig = ", ".join(f"{p[0]}: {p[1]}" for p in params)
            
            self._register("std.math", CFunctionMapping(
                claw_module="std.math",
                claw_name=claw_func_name if claw_func_name != c_func else c_func,
                claw_signature=f"{claw_func_name}({param_sig}) -> {ret_type}",
                c_name=c_func,
                c_headers=[CHeader.MATH],
                c_params=c_params,
                c_return=CReturnMapping(ret_type, "double"),
                description=desc
            ))
        
        # 数学常量（通过宏映射）
        self._register("std.math", CFunctionMapping(
            claw_module="std.math",
            claw_name="PI",
            claw_signature="PI -> Double",
            c_name="M_PI",
            c_headers=[CHeader.MATH],
            c_params=[],
            c_return=CReturnMapping("Double", "double"),
            c_call_template="M_PI",
            description="圆周率常量"
        ))
        
        self._register("std.math", CFunctionMapping(
            claw_module="std.math",
            claw_name="E",
            claw_signature="E -> Double",
            c_name="M_E",
            c_headers=[CHeader.MATH],
            c_params=[],
            c_return=CReturnMapping("Double", "double"),
            c_call_template="M_E",
            description="自然对数底数常量"
        ))
    
    # =========================================================
    #                    ctype.h 映射
    # =========================================================
    
    def _register_ctype(self):
        """注册 ctype.h 函数映射"""
        
        char_funcs = [
            ("isAlpha",    "isalpha",  "是否为字母"),
            ("isDigit",    "isdigit",  "是否为数字"),
            ("isAlphaNum", "isalnum",  "是否为字母或数字"),
            ("isUpper",    "isupper",  "是否为大写字母"),
            ("isLower",    "islower",  "是否为小写字母"),
            ("isSpace",    "isspace",  "是否为空白字符"),
            ("isPrint",    "isprint",  "是否为可打印字符"),
            ("isPunct",    "ispunct",  "是否为标点符号"),
            ("isControl",  "iscntrl",  "是否为控制字符"),
            ("isHexDigit", "isxdigit", "是否为十六进制数字"),
        ]
        
        for claw_name, c_name, desc in char_funcs:
            self._register("std.char", CFunctionMapping(
                claw_module="std.char",
                claw_name=claw_name,
                claw_signature=f"{claw_name}(ch: Char) -> Bool",
                c_name=c_name,
                c_headers=[CHeader.CTYPE],
                c_params=[
                    CParamMapping("ch", "Char", "int", ParamPassMode.BY_VALUE),
                ],
                c_return=CReturnMapping("Bool", "int",
                                       transform="{result} != 0"),
                description=desc
            ))
        
        # toupper / tolower
        self._register("std.char", CFunctionMapping(
            claw_module="std.char",
            claw_name="toUpper",
            claw_signature="toUpper(ch: Char) -> Char",
            c_name="toupper",
            c_headers=[CHeader.CTYPE],
            c_params=[
                CParamMapping("ch", "Char", "int", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("Char", "int",
                                   transform="(char){result}"),
            description="转大写"
        ))
        
        self._register("std.char", CFunctionMapping(
            claw_module="std.char",
            claw_name="toLower",
            claw_signature="toLower(ch: Char) -> Char",
            c_name="tolower",
            c_headers=[CHeader.CTYPE],
            c_params=[
                CParamMapping("ch", "Char", "int", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("Char", "int",
                                   transform="(char){result}"),
            description="转小写"
        ))
    
    # =========================================================
    #                    time.h 映射
    # =========================================================
    
    def _register_time(self):
        """注册 time.h 函数映射"""
        
        # ---------- time ----------
        self._register("std.time", CFunctionMapping(
            claw_module="std.time",
            claw_name="now",
            claw_signature="now() -> Int64",
            c_name="time",
            c_headers=[CHeader.TIME],
            c_params=[],
            c_return=CReturnMapping("Int64", "time_t"),
            c_call_template="time(NULL)",
            description="获取当前时间戳"
        ))
        
        # ---------- clock ----------
        self._register("std.time", CFunctionMapping(
            claw_module="std.time",
            claw_name="clock",
            claw_signature="clock() -> Int64",
            c_name="clock",
            c_headers=[CHeader.TIME],
            c_params=[],
            c_return=CReturnMapping("Int64", "clock_t"),
            description="获取处理器时钟"
        ))
        
        # ---------- difftime ----------
        self._register("std.time", CFunctionMapping(
            claw_module="std.time",
            claw_name="diff",
            claw_signature="diff(end: Int64, start: Int64) -> Double",
            c_name="difftime",
            c_headers=[CHeader.TIME],
            c_params=[
                CParamMapping("end", "Int64", "time_t", ParamPassMode.BY_VALUE),
                CParamMapping("start", "Int64", "time_t", ParamPassMode.BY_VALUE),
            ],
            c_return=CReturnMapping("Double", "double"),
            description="计算时间差（秒）"
        ))
        
        # ---------- strftime ----------
        self._register("std.time", CFunctionMapping(
            claw_module="std.time",
            claw_name="format",
            claw_signature="format(buffer: Ptr<Char>, maxSize: Size, fmt: String, timeInfo: Ptr<Void>) -> Size",
            c_name="strftime",
            c_headers=[CHeader.TIME],
            c_params=[
                CParamMapping("buffer", "Ptr<Char>", "char*", ParamPassMode.BY_POINTER),
                CParamMapping("maxSize", "Size", "size_t", ParamPassMode.BY_VALUE),
                CParamMapping("fmt", "String", "const char*", ParamPassMode.BY_VALUE),
                CParamMapping("timeInfo", "Ptr<Void>", "const struct tm*", ParamPassMode.BY_POINTER),
            ],
            c_return=CReturnMapping("Size", "size_t"),
            description="格式化时间为字符串"
        ))
    
    # =========================================================
    #                    注册/查找方法
    # =========================================================
    
    def _register(self, module: str, mapping: CFunctionMapping):
        """注册一个函数映射"""
        key = f"{mapping.claw_module}.{mapping.claw_name}"
        self._mappings[key] = mapping
    
    def lookup(self, claw_module: str, claw_name: str) -> Optional[CFunctionMapping]:
        """查找函数映射"""
        key = f"{claw_module}.{claw_name}"
        return self._mappings.get(key)
    
    def lookup_by_name(self, claw_name: str) -> List[CFunctionMapping]:
        """按函数名查找（可能有多个同名函数在不同模块）"""
        results = []
        for mapping in self._mappings.values():
            if mapping.claw_name == claw_name:
                results.append(mapping)
        return results
    
    def get_module_functions(self, module: str) -> List[CFunctionMapping]:
        """获取某个模块下的所有函数"""
        return [m for m in self._mappings.values() if m.claw_module == module]
    
    def get_all_modules(self) -> List[str]:
        """获取所有已注册模块"""
        modules = set()
        for m in self._mappings.values():
            modules.add(m.claw_module)
        return sorted(modules)
    
    def get_required_headers(self, used_functions: List[str]) -> List[str]:
        """根据使用的函数列表，收集需要的C头文件"""
        headers = set()
        for func_key in used_functions:
            mapping = self._mappings.get(func_key)
            if mapping:
                for h in mapping.c_headers:
                    headers.add(h.value)
        return sorted(headers)
    
    def get_unsafe_functions(self) -> List[CFunctionMapping]:
        """获取所有标记为unsafe的函数"""
        return [m for m in self._mappings.values() if m.is_unsafe]
    
    def get_deprecated_functions(self) -> List[CFunctionMapping]:
        """获取所有已废弃的函数"""
        return [m for m in self._mappings.values() 
                if m.deprecated_alternative is not None]
    
    def get_stats(self) -> Dict[str, int]:
        """获取注册表统计信息"""
        modules = {}
        for m in self._mappings.values():
            modules[m.claw_module] = modules.get(m.claw_module, 0) + 1
        return {
            "total_functions": len(self._mappings),
            "total_modules": len(set(m.claw_module for m in self._mappings.values())),
            "functions_per_module": modules,
            "unsafe_count": len(self.get_unsafe_functions()),
            "deprecated_count": len(self.get_deprecated_functions()),
        }
