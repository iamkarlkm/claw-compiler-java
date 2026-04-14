# claw_compiler/stdlib/stdlib_integration.py

"""
标准库集成模块
将标准库支持集成到Claw编译器主处理管道中
"""

from typing import List, Dict, Set, Optional
from dataclasses import dataclass

from .c_stdlib_registry import CStdlibRegistry
from .c_stdlib_codegen import CStdlibCodeGenerator, CGeneratedCall
from .import_resolver import ImportResolver, ImportDeclaration


@dataclass
class StdlibCallNode:
    """标准库调用的AST节点"""
    module: str           # "std.io"
    function: str         # "println"
    arguments: List[str]  # 参数表达式（已解析为C表达式）
    result_var: str       # 结果变量名（如果需要）
    line_number: int      # 源码行号
    flow_type: str        # "normal" | "exception" | "flow"


class StdlibIntegration:
    """
    标准库集成层
    
    在编译管道的以下阶段介入：
    
    阶段1（扫描→配对→分层）后：
      - 解析import语句，建立模块依赖
    
    阶段2（语义处理器）中：
      - 声明处理器识别标准库import
      - 函数处理器识别标准库函数调用
    
    阶段3（块处理器）中：
      - 函数调用块处理器处理标准库调用
      
    阶段4（代码生成）中：
      - 生成#include
      - 生成C函数调用代码
      - 生成错误检查代码（对接三层操作流）
    """
    
    def __init__(self):
        self.registry = CStdlibRegistry()
        self.codegen = CStdlibCodeGenerator(self.registry)
        self.import_resolver = ImportResolver(self.registry)
    
    # =========================================================
    #    阶段1集成：import 解析
    # =========================================================
    
    def process_imports(self, import_lines: List[tuple]) -> Dict:
        """
        处理所有import行
        
        Args:
            import_lines: [(line_number, source_text), ...]
        
        Returns:
            解析结果字典
        """
        results = {
            "resolved": [],
            "errors": [],
            "warnings": [],
            "required_headers": set()
        }
        
        for line_num, source_text in import_lines:
            try:
                decl = self.import_resolver.parse_import(source_text, line_num)
                resolved = self.import_resolver.resolve(decl)
                
                results["resolved"].append(resolved)
                results["errors"].extend(resolved.errors)
                results["warnings"].extend(resolved.warnings)
                results["required_headers"].update(resolved.c_headers)
                
            except Exception as e:
                results["errors"].append(f"行 {line_num}: {str(e)}")
        
        return results
    
    # =========================================================
    #    阶段2集成：函数调用识别
    # =========================================================
    
    def is_stdlib_call(self, function_name: str) -> bool:
        """判断是否为标准库函数调用"""
        return self.import_resolver.resolve_function_call(function_name) is not None
    
    def get_stdlib_mapping(self, function_name: str):
        """获取标准库函数映射"""
        return self.import_resolver.resolve_function_call(function_name)
    
    # =========================================================
    #    阶段4集成：C代码生成
    # =========================================================
    
    def generate_stdlib_call(self, call_node: StdlibCallNode) -> CGeneratedCall:
        """
        为标准库调用生成C代码
        
        对接三层操作流：
        - normal流：标准调用
        - exception流：生成错误检查 + 异常触发代码
        - flow流：生成错误检查 + goto跳转代码
        """
        error_handling = {
            "normal": "none",
            "exception": "exception",
            "flow": "flow"
        }.get(call_node.flow_type, "exception")
        
        return self.codegen.generate_call(
            claw_module=call_node.module,
            claw_func_name=call_node.function,
            args=call_node.arguments,
            result_var=call_node.result_var,
            error_handling=error_handling
        )
    
    def generate_c_file(
        self,
        claw_file_name: str,
        function_bodies: List[str],
        main_body: List[str] = None
    ) -> str:
        """生成完整的C文件"""
        return self.codegen.generate_full_file(
            claw_file_name, function_bodies, main_body
        )
    
    def get_includes(self) -> List[str]:
        """获取所有需要的 #include"""
        return self.codegen.generate_includes()
    
    def get_report(self) -> str:
        """生成综合报告"""
        lines = ["=== Claw标准库集成报告 ===", ""]
        
        # 注册表统计
        stats = self.registry.get_stats()
        lines.append(f"注册表统计:")
        lines.append(f"  总函数数: {stats['total_functions']}")
        lines.append(f"  总模块数: {stats['total_modules']}")
        lines.append(f"  unsafe函数: {stats['unsafe_count']}")
        lines.append(f"  deprecated函数: {stats['deprecated_count']}")
        lines.append("")
        
        lines.append("各模块函数数:")
        for module, count in sorted(stats['functions_per_module'].items()):
            lines.append(f"  {module}: {count}")
        lines.append("")
        
        # 导入报告
        lines.append(self.import_resolver.get_import_report())
        
        return "
".join(lines)
