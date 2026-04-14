# claw_compiler/stdlib/c_stdlib_codegen.py

"""
C标准库调用代码生成器
将Claw的标准库调用AST节点转换为C代码
"""

from typing import List, Dict, Set, Optional, Tuple
from dataclasses import dataclass, field

from .c_stdlib_registry import (
    CStdlibRegistry, CFunctionMapping, CParamMapping,
    CReturnMapping, ParamPassMode, CHeader
)


@dataclass
class CGeneratedCall:
    """生成的C函数调用结果"""
    c_code: str                          # 生成的C代码行
    required_headers: Set[str]           # 需要的头文件
    pre_statements: List[str] = field(default_factory=list)   # 前置语句
    post_statements: List[str] = field(default_factory=list)  # 后置语句
    temp_vars: List[str] = field(default_factory=list)        # 临时变量声明
    warnings: List[str] = field(default_factory=list)         # 编译器警告


@dataclass
class CGeneratedFile:
    """生成的完整C文件"""
    headers: List[str]           # #include 行
    forward_decls: List[str]     # 前置声明
    global_vars: List[str]       # 全局变量
    function_defs: List[str]     # 函数定义
    main_body: List[str]         # main函数体


class CStdlibCodeGenerator:
    """
    C标准库调用代码生成器
    
    职责：
    1. 将Claw标准库函数调用转换为对应的C函数调用
    2. 自动管理#include头文件依赖
    3. 处理类型转换和参数适配
    4. 生成错误检查代码（对接三层操作流）
    5. 处理unsafe标记和deprecated警告
    """
    
    def __init__(self, registry: CStdlibRegistry = None):
        self.registry = registry or CStdlibRegistry()
        self._used_headers: Set[str] = set()
        self._temp_var_counter: int = 0
        self._warnings: List[str] = []
    
    def generate_call(
        self,
        claw_module: str,
        claw_func_name: str,
        args: List[str],
        result_var: str = None,
        error_handling: str = "exception"  # "exception" | "flow" | "none"
    ) -> CGeneratedCall:
        """
        生成C函数调用代码
        
        Args:
            claw_module: Claw模块名 (如 "std.io")
            claw_func_name: Claw函数名 (如 "println")
            args: 参数列表（已经是C表达式）
            result_var: 结果变量名（如果需要捕获返回值）
            error_handling: 错误处理模式
        
        Returns:
            CGeneratedCall: 生成的C代码和元信息
        """
        mapping = self.registry.lookup(claw_module, claw_func_name)
        if mapping is None:
            raise CompilationError(
                f"未知的标准库函数: {claw_module}.{claw_func_name}"
            )
        
        warnings = []
        
        # 检查deprecated
        if mapping.deprecated_alternative:
            warnings.append(
                f"警告: {claw_module}.{claw_func_name} 已废弃，"
                f"建议使用 {mapping.deprecated_alternative}"
            )
        
        # 检查unsafe
        if mapping.is_unsafe:
            warnings.append(
                f"注意: {claw_module}.{claw_func_name} 标记为unsafe，"
                f"请确保在unsafe块中调用"
            )
        
        # 收集头文件
        required_headers = set()
        for h in mapping.c_headers:
            required_headers.add(h.value)
            self._used_headers.add(h.value)
        
        pre_statements = list(mapping.c_pre_code)
        post_statements = list(mapping.c_post_code)
        temp_vars = []
        
        # 构建参数列表
        c_args = self._build_c_args(mapping, args)
        
        # 生成调用表达式
        if mapping.c_call_template:
            # 使用自定义模板
            call_expr = self._render_template(
                mapping.c_call_template, mapping, args
            )
        else:
            # 标准调用格式
            call_expr = f"{mapping.c_name}({', '.join(c_args)})"
        
        # 处理返回值
        c_code = self._generate_with_return(
            call_expr, mapping.c_return, result_var, 
            error_handling, pre_statements, post_statements, temp_vars
        )
        
        return CGeneratedCall(
            c_code=c_code,
            required_headers=required_headers,
            pre_statements=pre_statements,
            post_statements=post_statements,
            temp_vars=temp_vars,
            warnings=warnings
        )
    
    def _build_c_args(
        self, mapping: CFunctionMapping, args: List[str]
    ) -> List[str]:
        """构建C函数参数列表"""
        c_args = []
        
        for i, param in enumerate(mapping.c_params):
            if i < len(args):
                arg = args[i]
                
                # 应用参数转换
                if param.transform:
                    arg = param.transform.replace("{value}", arg)
                
                # 处理传递模式
                if param.pass_mode == ParamPassMode.BY_POINTER:
                    # 如果Claw传的是值，需要取地址
                    # （实际中需要AST类型信息判断是否需要取地址）
                    pass
                
                c_args.append(arg)
            elif param.is_optional and param.default_value:
                c_args.append(param.default_value)
            else:
                raise CompilationError(
                    f"参数不足: {mapping.claw_name} 需要参数 {param.claw_name}"
                )
        
        return c_args
    
    def _render_template(
        self, template: str, mapping: CFunctionMapping, args: List[str]
    ) -> str:
        """渲染自定义调用模板"""
        result = template
        
        # 替换命名参数
        for i, param in enumerate(mapping.c_params):
            if i < len(args):
                result = result.replace(f"{{{param.claw_name}}}", args[i])
        
        # 处理可变参数
        variadic_start = len(mapping.c_params)
        if variadic_start < len(args):
            variadic = ", " + ", ".join(args[variadic_start:])
            result = result.replace("{variadic_args}", variadic)
        else:
            result = result.replace("{variadic_args}", "")
        
        return result
    
    def _generate_with_return(
        self,
        call_expr: str,
        c_return: CReturnMapping,
        result_var: str,
        error_handling: str,
        pre_statements: List[str],
        post_statements: List[str],
        temp_vars: List[str]
    ) -> str:
        """生成包含返回值处理的完整调用代码"""
        
        if c_return.claw_type == "Void":
            # 无返回值
            return f"{call_expr};"
        
        # 有返回值
        actual_result_var = result_var or self._new_temp_var()
        
        if result_var is None:
            temp_vars.append(f"{c_return.c_type} {actual_result_var};")
        
        # 基本赋值
        code_lines = [f"{actual_result_var} = {call_expr};"]
        
        # 返回值转换
        if c_return.transform:
            transform_var = self._new_temp_var()
            transformed = c_return.transform.replace("{result}", actual_result_var)
            temp_vars.append(f"/* transformed */ {c_return.c_type} {transform_var};")
            code_lines.append(f"{transform_var} = {transformed};")
        
        # 错误检查（对接三层操作流）
        if c_return.error_check and error_handling != "none":
            error_expr = c_return.error_check.replace("{result}", actual_result_var)
            
            if error_handling == "exception":
                # 异常流：生成catch兼容代码
                code_lines.append(f"if ({error_expr}) {{")
                code_lines.append(f"    /* exception flow: 触发异常流 */")
                code_lines.append(f"    _claw_raise_error(\"{call_expr} failed\");")
                code_lines.append(f"}}")
            elif error_handling == "flow":
                # 业务流：生成flow跳转代码
                code_lines.append(f"if ({error_expr}) {{")
                code_lines.append(f"    /* business flow: 跳转到错误处理 */")
                code_lines.append(f"    goto _claw_flow_error;")
                code_lines.append(f"}}")
        
        return "
".join(code_lines)
    
    def _new_temp_var(self) -> str:
        """生成唯一临时变量名"""
        self._temp_var_counter += 1
        return f"_claw_tmp_{self._temp_var_counter}"
    
    def generate_includes(self) -> List[str]:
        """生成所有需要的 #include 语句"""
        includes = []
        for header in sorted(self._used_headers):
            includes.append(f"#include <{header}>")
        return includes
    
    def generate_full_file(
        self,
        claw_source_name: str,
        function_bodies: List[str],
        main_body: List[str] = None
    ) -> str:
        """
        生成完整的C源文件
        
        Args:
            claw_source_name: 原始Claw文件名
            function_bodies: 函数定义列表
            main_body: main函数体
        """
        lines = []
        
        # 文件头注释
        lines.append(f"/* Generated from {claw_source_name} by Claw Compiler */")
        lines.append(f"/* DO NOT EDIT - This file is auto-generated */")
        lines.append("")
        
        # 头文件
        includes = self.generate_includes()
        
        # 始终包含的头文件
        always_include = {"stdbool.h", "stdint.h", "stddef.h"}
        for h in sorted(always_include):
            inc = f"#include <{h}>"
            if inc not in includes:
                includes.append(inc)
        
        for inc in sorted(includes):
            lines.append(inc)
        lines.append("")
        
        # Claw运行时支持声明
        lines.append("/* Claw Runtime Support */")
        lines.append("void _claw_raise_error(const char* msg);")
        lines.append("void _claw_init(void);")
        lines.append("void _claw_cleanup(void);")
        lines.append("")
        
        # 函数定义
        for func_body in function_bodies:
            lines.append(func_body)
            lines.append("")
        
        # main函数
        if main_body:
            lines.append("int main(int argc, char* argv[]) {")
            lines.append("    _claw_init();")
            lines.append("")
            for line in main_body:
                lines.append(f"    {line}")
            lines.append("")
            lines.append("    _claw_cleanup();")
            lines.append("    return 0;")
            lines.append("}")
        
        return "
".join(lines)
    
    def reset(self):
        """重置生成器状态"""
        self._used_headers.clear()
        self._temp_var_counter = 0
        self._warnings.clear()


class CompilationError(Exception):
    """编译错误"""
    pass
