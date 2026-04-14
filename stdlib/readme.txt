更新后的目录结构
claw_compiler/
├── scanner/                    # 扫描器组件
├── pairer/                     # 配对器组件
├── hierarchy/                  # 层级构建器
├── decomposer/                 # 实体分解器
│
├── src/                        # 4层处理器
│   ├── core/                   # 第1层
│   ├── processors/             # 第2-3层
│   └── generators/             # 第4层
│
├── stdlib/                     # ⭐ 新增：C标准库支持
│   ├── __init__.py
│   ├── c_stdlib_registry.py    # 标准库注册表（类型映射+函数映射）
│   ├── c_stdlib_codegen.py     # C代码生成器（调用生成+文件生成）
│   ├── import_resolver.py      # import解析器（5种导入语法）
│   ├── stdlib_integration.py   # 编译管道集成层
│   └── stdlib_constants.py     # 标准库常量定义（SEEK_SET等）
│
├── annotation/                 # 注解子系统
├── flow/                       # 三层操作流
├── integration/                # 系统集成组件
├── frontend/                   # 编译器前端
├── tests/
├── examples/
├── docs/
└── scripts/
8. 模块覆盖统计
Claw模块	C头文件	函数数量	说明
std.io	stdio.h	14	输入输出、文件操作
std.mem	stdlib.h + string.h	7	内存管理（malloc/free/memset/memcpy...）
std.string	string.h	12	字符串操作
std.math	math.h + stdlib.h	25	数学函数 + 常量
std.char	ctype.h	12	字符分类与转换
std.time	time.h	4	时间函数
std.convert	stdlib.h	3	类型转换
std.random	stdlib.h	2	随机数
std.process	stdlib.h	3	进程控制
std.algorithm	stdlib.h	2	排序、搜索
总计	7个头文件	84个函数	—
这套设计实现了Claw到C标准库的完整映射，包含类型安全检查、自动头文件管理、unsafe/deprecated标记、三层操作流错误处理集成，以及5种灵活的import语法支持。