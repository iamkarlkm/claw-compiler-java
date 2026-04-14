Claw 编译器 - Java 实现方案（解耦绑定层设计）
一、整体架构思路
核心原则：编译器前端（语言无关）与后端绑定层（目标语言相关）完全解耦，通过中间表示（IR）衔接，未来切换目标语言只需替换绑定层实现。

源代码(.claw)
    │
    ▼
┌──────────────────────────────────┐
│  编译器前端（语言无关）            │
│  Scanner → Pairer → Hierarchy    │
│  → Processors → IR Generator    │
└──────────────┬───────────────────┘
               │  ClawIR（中间表示）
               ▼
┌──────────────────────────────────┐
│  绑定层接口（抽象）               │
│  TargetCodeGenerator (interface) │
│  TargetRuntime (interface)       │
└──────────┬───────────┬───────────┘
           │           │
     ┌─────▼─────┐ ┌──▼──────────┐
     │ Java绑定层 │ │ Python绑定层│
     │ (当前实现) │ │ (未来扩展)  │
     └───────────┘ └─────────────┘
	 
	 
	 这个 IRGenerator.java 完整实现了以下设计要点：

思想2的第5步：从最底层代码块开始，通过 flattenBottomUp() 实现自下至上的生成顺序
思想1的三层操作流：NORMAL_FLOW_BEGIN/END、EXCEPTION_CATCH（去掉try和{}）、FLOW_TO（不记录堆栈）
思想4的18种代码块：每种块类型都有对应的 generate*Block() 方法
思想5的注解系统：4个程序注解（@BeforeName/@AfterName/@BeforeProps/@AfterProps）和5个系统注解（@@description/@@param/@@return/@@example/@@deprecated）
赋值块中的属性监听：自动检测 @BeforeProps/@AfterProps，在赋值前后插入钩子指令
构造/析构钩子：内存分配时触发 @BeforeName，内存回收时触发 @AfterName

解耦层次总结
┌─────────────────────────────────────────────────────┐
│                   编译器前端（不变）                    │
│                                                       │
│  Scanner → Pairer → Hierarchy → Processors → IR Gen  │
│                                                       │
│  产出：ClawIR（语言无关中间表示）                       │
└──────────────────────┬──────────────────────────────┘
                       │
              ClawIR 接口边界
                       │
        ┌──────────────┼──────────────┐
        │              │              │
   ┌────▼────┐   ┌────▼────┐   ┌────▼────┐
   │  Java   │   │ Python  │   │   JS    │
   │ Binding │   │ Binding │   │ Binding │
   ├─────────┤   ├─────────┤   ├─────────┤
   │CodeGen  │   │CodeGen  │   │CodeGen  │
   │TypeMap  │   │TypeMap  │   │TypeMap  │
   │Runtime  │   │Runtime  │   │Runtime  │
   └─────────┘   └─────────┘   └─────────┘
解耦点：

层次	接口	职责
ClawIR	数据契约	前端产出 → 后端输入，完全语言无关
TargetCodeGenerator	代码生成接口	每种目标语言实现自己的生成逻辑
TypeMapper	类型映射接口	Claw类型到目标语言类型的转换规则
TargetRuntime	运行时支持接口	注解钩子、异常处理、流转等特性的目标语言实现
GenerationConfig	配置	输出格式、缩进风格等与语言无关的配置
新增一种目标语言只需要实现：

XxxCodeGenerator implements TargetCodeGenerator
XxxTypeMapper implements TypeMapper
XxxRuntime implements TargetRuntime（可选）
前端所有已实现的 Java 代码（Scanner、Pairer、Hierarchy、19个Processor等）完全不需要改动。

PreprocessedSource,TokenStream,SemanticContext,StructureContext

claw_compiler/
├── src/main/java/claw/compiler/
│   ├── CompilePipeline.java              # 管道入口
│   │
│   ├── binding/                          # 绑定层（解耦）
│   │   ├── TargetRuntime.java            # 运行时抽象接口
│   │   ├── TargetCodeGenerator.java      # 代码生成器抽象接口
│   │   │
│   │   ├── java/                         # Java目标语言绑定
│   │   │   ├── JavaRuntime.java          # Java运行时实现
│   │   │   └── JavaCodeGenerator.java    # Java代码生成器
│   │   │
│   │   └── python/                       # Python目标语言绑定（未来）
│   │       ├── PythonRuntime.java        # Python运行时实现
│   │       └── PythonCodeGenerator.java  # Python代码生成器
│   │
│   ├── context/                          # 上下文数据
│   │   ├── StructureContext.java         # 结构上下文
│   │   └── SemanticContext.java          # 语义上下文
│   │
│   ├── generators/                       # IR生成
│   │   ├── IRGenerator.java             # IR生成器
│   │   └── ClawIR.java                  # IR封装
│   │
│   └── annotation/                       # 注解处理
│       └── AnnotationResult.java         # 注解处理结果


这个版本相比之前新增了以下内容以完全覆盖 IRGenerator.java 中所有 OpCode 对应的生成需求：

mapBoxedType — 支持泛型上下文的包装类型映射
generateArrayCreation / generateArrayGet / generateArraySet — 对应 ARRAY_NEW / ARRAY_GET / ARRAY_SET
generatePropertyGet / generatePropertySet — 对应 PROP_GET / PROP_SET
generateScopeEnter / generateScopeExit — 对应 SCOPE_ENTER / SCOPE_EXIT
generateStringLiteral / generateStringConcat — 字符串处理支持
generateThrow / generateExceptionTypeDefinition — 异常创建支持

Python 与 Java 的关键差异映射总结
特性	Java (JavaRuntime)	Python (PythonRuntime)
块界定	{ }	缩进 + :
语句终止	;	无
类型声明	int x = 5;	x: int = 5
常量	final int X = 5;	X: Final[int] = 5
空值	null	None
布尔	true / false	True / False
逻辑运算	&& || !	and or not
私有成员	private	_ / __ 前缀
异常捕获	catch (Type e)	except Type as e:
throws	throws IOException	# Raises: IOError（注释）
析构	AutoCloseable.close()	__del__
对象创建	new Type()	Type()
数组创建	new int[10]	[None] * 10
导入	import pkg.Class;	from pkg import Class
flow-to	break LABEL	raise FlowJumpException(label)
文档注释	/** JavaDoc */	"""docstring"""\

三种目标语言关键差异对照表
特性	Java	Python	C
内存管理	GC 自动	GC 自动	malloc/free 手动
异常机制	try/catch 原生	try/except 原生	setjmp/longjmp 模拟
flow-to	break LABEL 模拟	raise FlowJumpException	goto 原生支持 ✅
类型系统	静态类型	动态+注解	静态类型
对象系统	class 原生	class 原生	struct + 函数指针
析构函数	AutoCloseable.close()	__del__	手动调用 _destroy()
块界定	{ }	缩进 + :	{ }
语句终止	;	无	;
空值	null	None	NULL
布尔值	true/false	True/False	true/false (stdbool)
逻辑运算	&& || !	and or not	&& || !
字符串	String 对象	str 对象	char* 手动管理
字符串拼接	+ 原生	+ 原生	__claw_str_concat()
私有成员	private	_/__ 前缀	注释标注
导入	import pkg.Class;	from pkg import name	#include "path.h"
导出	public 关键字	__all__	头文件声明
数组创建	new Type[n]	[None] * n	calloc(n, sizeof)
属性访问	obj.field	obj.field	obj->field (指针)
文档注释	/** JavaDoc */	"""docstring"""	/** Doxygen @brief */
头文件	不需要	不需要	需要 .h 分离
前向声明	不需要	不需要	需要函数原型
空参数	()	()	(void)

好的，下面是完整的 C 目标语言绑定，包括 CRuntime.java 和 CCodeGenerator.java，与现有的 TargetRuntime/TargetCodeGenerator 接口完全对齐。

C 语言的特殊性在于：需要显式内存管理、手动构造/析构、头文件分离、无原生异常机制、无类/对象系统，所有这些都需要特殊映射策略。


Claw import → C #include 映射规则
Claw 导入	C 映射	说明
import std.io	#include <stdio.h>	标准 I/O
import std.string	#include <string.h>	字符串处理
import std.math	#include <math.h>	数学函数
import std.memory	#include <stdlib.h>	内存管理
import std.bool	#include <stdbool.h>	布尔类型
import std.time	#include <time.h>	时间处理
import std.assert	#include <assert.h>	断言
import std.errno	#include <errno.h>	错误码
import std.signal	#include <signal.h>	信号处理
import std.thread	#include <pthread.h>	POSIX 线程
import mymodule.utils	#include "mymodule/utils.h"	用户自定义模块
这个映射在 CRuntime.generateImport() 中实现：

java
@Override
public String generateImport(String modulePath, String symbolName) {
    // 标准库映射
    Map<String, String> stdMapping = Map.of(
        "std.io",      "<stdio.h>",
        "std.string",  "<string.h>",
        "std.math",    "<math.h>",
        "std.memory",  "<stdlib.h>",
        "std.bool",    "<stdbool.h>",
        "std.time",    "<time.h>",
        "std.assert",  "<assert.h>",
        "std.errno",   "<errno.h>",
        "std.signal",  "<signal.h>",
        "std.thread",  "<pthread.h>"
    );

    String mapped = stdMapping.get(modulePath);
    if (mapped != null) {
        return "#include " + mapped;
    }

    // 用户自定义模块
    String headerPath = modulePath.replace(".", "/") + ".h";
    return "#include \"" + headerPath + "\"";
}

# 基本编译
gcc -std=c11 -o output output.c

# 使用了 math.h
gcc -std=c11 -o output output.c -lm

符号对照表
生成的 C 代码中的符号	定义位置	说明
CLAW_TRY	claw_runtime.h 宏	开始 try 块，push jmp_buf + setjmp
CLAW_CATCH(type)	claw_runtime.h 宏	匹配异常类型
CLAW_CATCH_ALL	claw_runtime.h 宏	捕获所有未匹配异常
CLAW_END_TRY	claw_runtime.h 宏	结束 try 块，未处理则传播
__claw_throw(type, msg)	claw_runtime.h 宏→函数	抛出异常
CLAW_EX_IOERROR 等	claw_runtime.h 枚举	异常类型常量
ClawException	claw_runtime.h 结构体	异常信息载体
__claw_current_exception	claw_runtime.h 宏	当前活跃异常
__claw_before_prop_change	claw_runtime.h 函数	属性变更前钩子
__claw_after_prop_change	claw_runtime.h 函数	属性变更后钩子
__claw_str_concat(...)	claw_runtime.h 宏→函数	字符串拼接
__claw_safe_malloc	claw_runtime.h 函数	安全 malloc，失败抛异常
__claw_safe_free(ptr)	claw_runtime.h 宏	安全 free + 置 NULL
所有符号都通过 #include "claw_runtime.h" 一个头文件引入，编译器生成的每个 .c 文件都会自动包含它。

# 使用了 pthread.h
gcc -std=c11 -o output output.c -lpthread

# 调试模式
gcc -std=c11 -g -Wall -Wextra -o output output.c -lm

gcc -std=c11 -o demo demo.c
./demo

你说得对，之前生成的 C 代码中直接使用了 CLAW_TRY、CLAW_CATCH、CLAW_END_TRY、CLAW_EX_IOERROR、ClawException、__claw_throw 等符号，但并没有给出它们的完整定义。这些都属于 Claw 编译器为 C 目标生成的运行时辅助代码，需要完整实现。

下面提供完整的 claw_runtime.h，包含所有必要的宏、类型定义和辅助函数：

输入: compile("src/user_service.claw", null, "c")

推导过程:
  sourceFilePath  = "src/user_service.claw"
  sourceFileName  = "user_service"          (去掉扩展名)
  outputDir       = "src/build/"            (源文件目录/build/)
  outputBasePath  = "src/build/user_service"

输出文件:
  主文件:     src/build/user_service.c
  头文件:     src/build/user_service.h
  运行时:     src/build/claw_runtime.h
  编译脚本:   src/build/build.sh


compile("src/my_module.claw", null, "c")
  -> src/build/my_module.c
  -> src/build/my_module.h
  -> src/build/claw_runtime.h
  -> src/build/build.sh

compile("src/my_module.claw", null, "java")
  -> src/build/MyModule.java          (首字母大写)

compile("src/my_module.claw", null, "python")
  -> src/build/my_module.py

compile("src/my_module.claw", "output/gen", "c")
  -> output/gen/my_module.c           (用户指定输出目录)
  -> output/gen/my_module.h
  -> output/gen/claw_runtime.h
  -> output/gen/build.sh


