# 文件恢复指南

## 📦 已创建的备份

**备份包**: `claw_compiler_backup_20260419.zip`

**位置**: `I:\2026\claw-compiler-java\claw_compiler_backup_20260419.zip`

**包含文件**: 37个文件（共367KB）

### 备份内容列表

```
claw_compiler_backup_20260419.zip
├── src/main/java/claw/compiler/
│   ├── binding/
│   │   ├── c/
│   │   │   ├── CRuntime.java (33KB)
│   │   │   └── CCodeGenerator.java
│   │   ├── java/
│   │   │   ├── JavaCodeGenerator.java
│   │   │   └── JavaRuntime.java
│   │   ├── python/
│   │   │   ├── PythonCodeGenerator.java (17KB)
│   │   │   └── PythonRuntime.java (26KB)
│   │   ├── TargetCodeGenerator.java (1.5KB)
│   │   └── TargetRuntime.java (22KB)
│   ├── generators/
│   │   ├── ClawIR.java (2.7KB)
│   │   ├── IRGenerator.java (55KB)
│   │   └── ffi/
│   │       ├── AbstractStringBuilder.java (325B)
│   │       ├── CFFIGenerator.java (14KB)
│   │       ├── FFIBindingTable.java (31KB)
│   │       ├── JavaFFIGenerator.java (30KB)
│   │       ├── PlatformConstraint.java (396B)
│   │       ├── PythonFFIGenerator.java (19KB)
│   │       └── platform/
│   │           ├── PlatformConstraint.java (16KB)
│   │           ├── PlatformFilterCache.java (2KB)
│   │           ├── PlatformLibraryMapper.java (3.4KB)
│   │           └── TargetTriple.java (4.6KB)
│   ├── pipeline/
│   │   └── CompilePipeline.java (19KB)
│   └── processors/
│       ├── blocks/
│       │   ├── ExternBlockProcessor.java (18KB)
│       │   └── 其他块处理器...
│       └── semantic/
│           ├── ExternProcessor.java (23KB)
│           └── 其他语义处理器...
```

## 🔍 主要缺失文件

根据备份检查，以下文件需要在你的新 `com.q3lives` 结构中恢复：

### 1. Python 绑定（已恢复）
- ✅ `PythonCodeGenerator.java` - 已恢复
- ✅ `PythonRuntime.java` - 已恢复

### 2. C 绑定（已恢复）
- ✅ `CRuntime.java` - 已恢复
- ✅ `CCodeGenerator.java` - 已恢复

### 3. 目标代码生成器
- ✅ `TargetCodeGenerator.java` - 已恢复

### 4. IRGenerator 相关
- ✅ `IRGenerator.java` - 已创建（在重构中）
- ✅ `IRProgram.java` - 已创建
- ✅ `IRBasicBlock.java` - 已创建
- ✅ `IRInstruction.java` - 已创建
- ✅ `OpCode.java` - 已创建

### 5. FFI 相关
- ✅ `FFIBindingTable.java` - 已存在
- ✅ `CFFIGenerator.java` - 已存在
- ✅ `JavaFFIGenerator.java` - 已存在
- ✅ `PythonFFIGenerator.java` - 已存在
- ✅ `AbstractStringBuilder.java` - 已删除（占位文件）
- ⚠️ `platform/` 子目录的文件 - 需要检查

## 📋 恢复步骤

### 步骤 1: 解压备份
```bash
# 解压到临时目录
unzip claw_compiler_backup_20260419.zip -d temp_restore

# 或者使用其他工具解压
```

### 步骤 2: 识别需要的文件
备份中包含完整的 `src/main/java/claw/compiler/` 目录结构。

### 步骤 3: 恢复缺失的文件

对于需要恢复的文件，执行以下操作：

1. **从备份中复制文件**
   ```bash
   cp temp_restore/src/main/java/claw/compiler/<文件路径> \
      src/main/java/com/q3lives/<对应路径>
   ```

2. **检查文件内容是否正确**

3. **测试编译**
   ```bash
   mvn compile
   ```

## 🎯 建议的恢复优先级

### 高优先级（核心功能）
1. `PythonRuntime.java` ✅ 已恢复
2. `TargetCodeGenerator.java` ✅ 已恢复
3. `IRGenerator.java` ✅ 已创建

### 中优先级（绑定功能）
1. `CCodeGenerator.java` ✅ 已恢复
2. `CRuntime.java` ✅ 已恢复
3. `JavaCodeGenerator.java`
4. `JavaRuntime.java`

### 低优先级（辅助文件）
1. `AbstractStringBuilder.java` (占位文件，不需要)
2. `PlatformConstraint.java` (在 platform 子目录中)

## ⚠️ 注意事项

1. **包名统一**：
   - 重构后的文件应该使用 `com.q3lives.*` 包名
   - 如果需要，批量替换包名

2. **Lombok 注解**：
   - 备份中的文件可能使用了 `@Slf4j`
   - 如果编译错误，参考之前的手动修复方案

3. **依赖关系**：
   - 确保所有必需的依赖类都已存在
   - 检查导入语句是否正确

## 🆘 快速恢复命令

如果你想快速恢复某个文件，可以使用这个命令：

```bash
# 恢复 PythonRuntime.java
cp claw_compiler_backup_20260419.zip::backup_claw_compiler/src/main/java/claw/compiler/binding/python/PythonRuntime.java \
   src/main/java/com/q3lives/binding/python/PythonRuntime.java
```

但更简单的方法是解压备份，手动比较，然后复制需要的文件。

---

**备份创建时间**: 2026-04-19
**备份大小**: 367KB
**包含文件数**: 37个

如有任何问题，请告诉我！
