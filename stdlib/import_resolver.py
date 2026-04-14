# claw_compiler/stdlib/import_resolver.py

"""
Import 解析器
处理 Claw 源码中的 import 语句，解析标准库引用
"""

from typing import Dict, List, Set, Optional, Tuple
from dataclasses import dataclass, field
from enum import Enum, auto

from .c_stdlib_registry import CStdlibRegistry, CFunctionMapping


class ImportType(Enum):
    """导入类型"""
    FULL_MODULE = auto()      # import std.io
    SELECTIVE   = auto()      # import { print, println } from std.io
    ALIAS       = auto()      # import std.io as io
    WILDCARD    = auto()      # import std.io.*


@dataclass
class ImportDeclaration:
    """导入声明"""
    import_type: ImportType
    module_path: str                      # "std.io"
    selected_names: List[str] = None      # ["print", "println"]
    alias: str = None                     # "io"
    line_number: int = 0
    source_text: str = ""


@dataclass  
class ResolvedImport:
    """解析完成的导入"""
    declaration: ImportDeclaration
    resolved_functions: Dict[str, CFunctionMapping]  # 可用函数
    c_headers: Set[str]                              # 需要的C头文件
    errors: List[str] = field(default_factory=list)
    warnings: List[str] = field(default_factory=list)


class ImportResolver:
    """
    Import 解析器
    
    支持的Claw导入语法：
    
    1. import std.io                           → 导入整个模块
    2. import { print, println } from std.io   → 选择性导入
    3. import std.io as io                     → 别名导入
    4. import std.io.*                         → 通配符导入（不推荐）
    5. import std.{io, string, math}           → 多模块导入
    """
    
    def __init__(self, registry: CStdlibRegistry = None):
        self.registry = registry or CStdlibRegistry()
        self._resolved_imports: Dict[str, ResolvedImport] = {}
        self._available_functions: Dict[str, CFunctionMapping] = {}
        self._name_to_module: Dict[str, str] = {}  # 函数名→模块路径
    
    def parse_import(self, source_line: str, line_number: int = 0) -> ImportDeclaration:
        """
        解析单行import语句
        
        支持格式：
          import std.io
          import { print, println } from std.io
          import std.io as io
          import std.io.*
        """
        line = source_line.strip()
        
        if not line.startswith("import"):
            raise ImportError(f"行 {line_number}: 不是有效的import语句: {line}")
        
        rest = line[len("import"):].strip()
        
        # 格式2: import { ... } from module
        if rest.startswith("{"):
            return self._parse_selective_import(rest, line_number, line)
        
        # 格式4: import module.*
        if rest.endswith(".*"):
            module_path = rest[:-2].strip()
            return ImportDeclaration(
                import_type=ImportType.WILDCARD,
                module_path=module_path,
                line_number=line_number,
                source_text=line
            )
        
        # 格式3: import module as alias
        if " as " in rest:
            parts = rest.split(" as ")
            module_path = parts[0].strip()
            alias = parts[1].strip()
            return ImportDeclaration(
                import_type=ImportType.ALIAS,
                module_path=module_path,
                alias=alias,
                line_number=line_number,
                source_text=line
            )
        
        # 格式5: import std.{io, string, math} → 拆分为多个
        if "{" in rest and "}" in rest:
            return self._parse_multi_module_import(rest, line_number, line)
        
        # 格式1: import module
        module_path = rest.strip()
        return ImportDeclaration(
            import_type=ImportType.FULL_MODULE,
            module_path=module_path,
            line_number=line_number,
            source_text=line
        )
    
    def _parse_selective_import(
        self, rest: str, line_number: int, source_text: str
    ) -> ImportDeclaration:
        """解析选择性导入: import { print, println } from std.io"""
        brace_end = rest.index("}")
        names_str = rest[1:brace_end]
        names = [n.strip() for n in names_str.split(",") if n.strip()]
        
        after_brace = rest[brace_end + 1:].strip()
        if not after_brace.startswith("from"):
            raise ImportError(
                f"行 {line_number}: 选择性导入语法错误，缺少 'from': {source_text}"
            )
        
        module_path = after_brace[len("from"):].strip()
        
        return ImportDeclaration(
            import_type=ImportType.SELECTIVE,
            module_path=module_path,
            selected_names=names,
            line_number=line_number,
            source_text=source_text
        )
    
    def _parse_multi_module_import(
        self, rest: str, line_number: int, source_text: str
    ) -> ImportDeclaration:
        """
        解析多模块导入: import std.{io, string, math}
        转换为完整模块导入（第一个模块作为主声明返回）
        """
        dot_pos = rest.index("{")
        prefix = rest[:dot_pos].rstrip(".")
        brace_end = rest.index("}")
        modules_str = rest[dot_pos + 1:brace_end]
        modules = [m.strip() for m in modules_str.split(",") if m.strip()]
        
        # 返回第一个，其余需要外部循环处理
        # 实际实现中应该返回一个MultiImport类型
        full_module = f"{prefix}.{modules[0]}" if prefix else modules[0]
        
        return ImportDeclaration(
            import_type=ImportType.FULL_MODULE,
            module_path=full_module,
            line_number=line_number,
            source_text=source_text
        )
    
    def resolve(self, declaration: ImportDeclaration) -> ResolvedImport:
        """
        解析导入声明，建立可用函数映射
        """
        resolved_functions = {}
        c_headers = set()
        errors = []
        warnings = []
        
        module = declaration.module_path
        
        # 查找模块中的所有函数
        module_functions = self.registry.get_module_functions(module)
        
        if not module_functions:
            errors.append(
                f"行 {declaration.line_number}: "
                f"未知模块 '{module}'，没有找到任何函数"
            )
            available_modules = self.registry.get_all_modules()
            if available_modules:
                errors.append(f"  可用模块: {', '.join(available_modules)}")
        
        if declaration.import_type == ImportType.SELECTIVE:
            # 选择性导入：只导入指定函数
            for name in declaration.selected_names:
                found = False
                for func in module_functions:
                    if func.claw_name == name:
                        resolved_functions[name] = func
                        for h in func.c_headers:
                            c_headers.add(h.value)
                        found = True
                        break
                if not found:
                    errors.append(
                        f"行 {declaration.line_number}: "
                        f"函数 '{name}' 在模块 '{module}' 中不存在"
                    )
                    # 推荐相似函数名
                    similar = self._find_similar(name, module_functions)
                    if similar:
                        errors.append(f"  你是否想用: {', '.join(similar)}")
        
        elif declaration.import_type == ImportType.WILDCARD:
            # 通配符导入
            warnings.append(
                f"行 {declaration.line_number}: "
                f"不推荐使用通配符导入 '{module}.*'，建议使用选择性导入"
            )
            for func in module_functions:
                resolved_functions[func.claw_name] = func
                for h in func.c_headers:
                    c_headers.add(h.value)
        
        else:
            # 完整模块导入或别名导入
            for func in module_functions:
                if declaration.alias:
                    # 别名导入: io.print 而不是 std.io.print
                    key = f"{declaration.alias}.{func.claw_name}"
                else:
                    key = func.claw_name
                resolved_functions[key] = func
                for h in func.c_headers:
                    c_headers.add(h.value)
        
        # 检查函数名冲突
        for name, func in resolved_functions.items():
            if name in self._available_functions:
                existing = self._available_functions[name]
                if existing.c_name != func.c_name:
                    warnings.append(
                        f"行 {declaration.line_number}: "
                        f"函数名 '{name}' 与 {existing.claw_module} 中的同名函数冲突，"
                        f"后导入的将覆盖前者"
                    )
        
        # 注册到全局可用函数表
        self._available_functions.update(resolved_functions)
        for name, func in resolved_functions.items():
            self._name_to_module[name] = func.claw_module
        
        resolved = ResolvedImport(
            declaration=declaration,
            resolved_functions=resolved_functions,
            c_headers=c_headers,
            errors=errors,
            warnings=warnings
        )
        
        self._resolved_imports[module] = resolved
        return resolved
    
    def _find_similar(
        self, name: str, functions: List[CFunctionMapping]
    ) -> List[str]:
        """简单的相似函数名推荐"""
        similar = []
        name_lower = name.lower()
        for func in functions:
            func_lower = func.claw_name.lower()
            # 前缀匹配或包含
            if (func_lower.startswith(name_lower[:3]) or 
                name_lower in func_lower or
                func_lower in name_lower):
                similar.append(func.claw_name)
        return similar[:3]
    
    def resolve_function_call(self, name: str) -> Optional[CFunctionMapping]:
        """
        在已导入的函数中查找
        供代码生成阶段使用
        """
        return self._available_functions.get(name)
    
    def get_all_required_headers(self) -> Set[str]:
        """获取所有已解析导入需要的C头文件"""
        headers = set()
        for resolved in self._resolved_imports.values():
            headers.update(resolved.c_headers)
        return headers
    
    def get_import_report(self) -> str:
        """生成导入解析报告"""
        lines = ["=== Import 解析报告 ===", ""]
        
        for module, resolved in self._resolved_imports.items():
            decl = resolved.declaration
            lines.append(f"模块: {module}")
            lines.append(f"  源码: {decl.source_text}")
            lines.append(f"  类型: {decl.import_type.name}")
            lines.append(f"  已解析函数: {len(resolved.resolved_functions)}")
            
            for name in sorted(resolved.resolved_functions.keys()):
                func = resolved.resolved_functions[name]
                unsafe = " [unsafe]" if func.is_unsafe else ""
                depr = " [deprecated]" if func.deprecated_alternative else ""
                lines.append(f"    - {name} → {func.c_name}(){unsafe}{depr}")
            
            lines.append(f"  需要头文件: {', '.join(sorted(resolved.c_headers))}")
            
            if resolved.errors:
                lines.append(f"  ❌ 错误:")
                for err in resolved.errors:
                    lines.append(f"    {err}")
            
            if resolved.warnings:
                lines.append(f"  ⚠ 警告:")
                for warn in resolved.warnings:
                    lines.append(f"    {warn}")
            
            lines.append("")
        
        lines.append(f"总计: {len(self._available_functions)} 个可用函数")
        lines.append(f"需要C头文件: {', '.join(sorted(self.get_all_required_headers()))}")
        
        return "
".join(lines)


class ImportError(Exception):
    """导入解析错误"""
    pass
