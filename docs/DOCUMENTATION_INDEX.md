# Claw Compiler Java 文档索引

## 📚 文档结构

### 1. 核心文档
- [CLAUDE.md](../CLAUDE.md) - Claude Code 工作指导（当前文件）
- [README.md](../README.md) - 项目概述和简介
- [ROADMAP.md](../ROADMAP.md) - 发展路线图

### 2. 架构设计
- [ARCHITECTURE.md](./ARCHITECTURE.md) - 系统架构设计
- [DEVELOPMENT.md](./DEVELOPMENT.md) - 开发指南
- [IMPLEMENTATION_SUMMARY.md](./IMPLEMENTATION_SUMMARY.md) - 实现总结

### 3. 语言特性
- [ADVANCED_FEATURES.md](./ADVANCED_FEATURES.md) - 高级特性说明
- [LOOP_SUPPORT.md](./LOOP_SUPPORT.md) - 循环支持
- [EXCEPTION_HANDLING.md](./EXCEPTION_HANDLING.md) - 异常处理
- [TYPE_ANNOTATIONS_SUMMARY.md](./TYPE_ANNOTATIONS_SUMMARY.md) - 类型注解
- [FUNCTION_DOC.md](./FUNCTION_DOC.md) - 函数文档

### 4. 代码生成
- [JAVA_CODE_GENERATION_IMPROVEMENTS.md](./JAVA_CODE_GENERATION_IMPROVEMENTS.md) - Java 代码生成改进
- [PythonCodeGenerator_Implementation.md](./PythonCodeGenerator_Implementation.md) - Python 代码生成器实现
- [CFFIGenerator_Implementation.md](./CFFIGenerator_Implementation.md) - C FFI 生成器实现
- [PythonFFIGenerator_Implementation.md](./PythonFFIGenerator_Implementation.md) - Python FFI 生成器实现
- [JavaFFIGenerator_Implementation.md](./JavaFFIGenerator_Implementation.md) - Java FFI 生成器实现

### 5. AOP 和装饰器
- [AOP_IMPLEMENTATION_SUMMARY.md](./AOP_IMPLEMENTATION_SUMMARY.md) - AOP 实现总结
- [DECORATOR_LAMBDA_IMPLEMENTATION.md](./DECORATOR_LAMBDA_IMPLEMENTATION.md) - 装饰器和 Lambda 实现
- [PROPERTY_MONITORING.md](./PROPERTY_MONITORING.md) - 属性监控

### 6. 性能优化
- [PERFORMANCE_OPTIMIZATION.md](./PERFORMANCE_OPTIMIZATION.md) - 性能优化
- [PERFORMANCE_OPTIMIZATION_COMPLETE.md](./PERFORMANCE_OPTIMIZATION_COMPLETE.md) - 性能优化完成报告
- [OPTIMIZATION_SUMMARY.md](./OPTIMIZATION_SUMMARY.md) - 优化总结

### 7. 错误处理
- [ERROR_HANDLING.md](./ERROR_HANDLING.md) - 错误处理设计
- [ERROR_HANDLING_IMPLEMENTATION.md](./ERROR_HANDLING_IMPLEMENTATION.md) - 错误处理实现

### 8. LSP 支持
- [LSP_IMPLEMENTATION_PLAN.md](./LSP_IMPLEMENTATION_PLAN.md) - LSP 实现计划
- [LSP_IMPLEMENTATION_INITIAL.md](./LSP_IMPLEMENTATION_INITIAL.md) - LSP 初始实现
- [LSP_IMPLEMENTATION_PROGRESS.md](./LSP_IMPLEMENTATION_PROGRESS.md) - LSP 实现进度
- [LSP_IMPLEMENTATION_PHASE2.md](./LSP_IMPLEMENTATION_PHASE2.md) - LSP 实现第二阶段
- [LSP_IMPLEMENTATION_PHASE3.md](./LSP_IMPLEMENTATION_PHASE3.md) - LSP 实现第三阶段
- [LSP_IMPLEMENTATION_PHASE4.md](./LSP_IMPLEMENTATION_PHASE4.md) - LSP 实现第四阶段
- [LSP_FINAL_SUMMARY.md](./LSP_FINAL_SUMMARY.md) - LSP 最终总结

### 9. 测试和调试
- [LOOP_SUPPORT.md](./LOOP_SUPPORT.md) - 循环支持测试
- [LSP_TESTING_STATUS.md](./LSP_TESTING_STATUS.md) - LSP 测试状态
- [LSP_TEST_RESULTS_SUMMARY_CN.md](./LSP_TEST_RESULTS_SUMMARY_CN.md) - LSP 测试结果中文摘要
- [LSP_TEST_REPORT_CN.md](./LSP_TEST_REPORT_CN.md) - LSP 测试报告中文版
- [TEST_FIXES_NEEDED.md](./TEST_FIXES_NEEDED.md) - 需要的测试修复

### 10. 重构和恢复
- [PACKAGE_REFACTORING_REPORT.md](./PACKAGE_REFACTORING_REPORT.md) - 包重构报告
- [FINAL_REFACTORING_REPORT.md](./FINAL_REFACTORING_REPORT.md) - 最终重构报告
- [FILE_RECOVERY_GUIDE.md](./FILE_RECOVERY_GUIDE.md) - 文件恢复指南

### 11. 资源和工具
- [PARALLEL_COMPILATION_DEMO.md](./PARALLEL_COMPILATION_DEMO.md) - 并行编译演示
- [CODE_BEAUTIFICATION.md](./CODE_BEAUTIFICATION.md) - 代码美化
- [DOCUMENTATION_MERGE_PLAN.md](./DOCUMENTATION_MERGE_PLAN.md) - 文档合并计划

## 🔄 文档更新历史

### 2026-04-12
- 创建文档索引结构
- 整合所有分散的文档

### 2026-04-15
- 完成循环支持文档
- 更新实现状态

### 2026-04-23
- 创建统一的 CLAUDE.md
- 整理所有开发文档

## 📝 使用说明

1. **新开发者**：先阅读 [CLAUDE.md](../CLAUDE.md) 了解项目结构和开发规范
2. **架构理解**：阅读 [ARCHITECTURE.md](./ARCHITECTURE.md) 了解系统设计
3. **功能开发**：根据需要查看对应的特性文档
4. **问题排查**：查看错误处理和测试相关文档

## 🎯 快速导航

- [项目概述](../README.md) - 5分钟了解项目
- [开发指南](./DEVELOPMENT.md) - 开始开发
- [实现状态](./IMPLEMENTATION_SUMMARY.md) - 当前进度
- [路线图](../ROADMAP.md) - 未来计划

## 📞 联系方式

如有文档相关问题，请：
1. 查看 [IMPLEMENTATION_SUMMARY.md](./IMPLEMENTATION_SUMMARY.md) 了解实现细节
2. 运行测试：`mvn test -Dtest="*"`
3. 检查代码生成器的实现