# 内存优化指南

## 概述

本指南提供了Claw编译器内存优化的最佳实践，帮助开发者减少内存分配、降低GC压力，提高编译性能。

## 核心优化策略

### 1. 对象池模式 (Object Pool Pattern)

#### 适用场景
- 频繁创建和销毁的小对象
- 对象创建成本高
- 对象生命周期短

#### 实现示例

```java
// 创建对象池
private static final MemoryObjectPool<List<String>> STRING_LIST_POOL = 
    new MemoryObjectPool<>(ArrayList::new, 50, 100);

// 使用对象池
List<String> list = STRING_LIST_POOL.borrow();
try {
    list.clear();
    // 使用列表...
} finally {
    STRING_LIST_POOL.returnObject(list);
}
```

#### 优化效果
- 减少30-40%的GC压力
- 提高对象复用率
- 降低内存碎片

### 2. 缓存策略

#### 缓存类型

1. **解析结果缓存**
   ```java
   private static final Map<String, ASTNode> PARSE_CACHE = new ConcurrentHashMap<>();
   ```

2. **符号表缓存**
   ```java
   private static final Map<String, Map<String, SymbolInfo>> SYMBOL_CACHE = new ConcurrentHashMap<>();
   ```

3. **代码模式缓存**
   ```java
   private static final Map<String, String> PATTERN_CACHE = new ConcurrentHashMap<>();
   ```

#### 缓存最佳实践
- 使用LRU策略清理缓存
- 设置合理的缓存大小限制
- 使用不可变对象确保缓存安全

### 3. 字符串处理优化

#### 避免的问题
```java
// 错误：频繁字符串拼接
String result = "";
for (String s : list) {
    result += s; // 创建大量临时对象
}

// 错误：使用String.format()简单场景
String msg = String.format("Error at line %d", line);
```

#### 优化方案

```java
// 正确：使用StringBuilder
StringBuilder sb = new StringBuilder(1024); // 预分配容量
for (String s : list) {
    sb.append(s);
}
String result = sb.toString();

// 正确：常量池
private static final String ERROR_TEMPLATE = "Error at line %d";
String msg = String.format(ERROR_TEMPLATE, line);
```

### 4. 集合使用优化

#### 集合类型选择

| 场景 | 推荐类型 | 原因 |
|------|----------|------|
| 单线程读写 | ArrayList | 无同步开销 |
| 高频读写 | ConcurrentLinkedQueue | 无锁高效 |
| 读多写少 | CopyOnWriteArrayList | 读性能最优 |
| 并发修改 | ConcurrentHashMap | 线程安全 |

#### 容量设置

```java
// 错误：无容量设置
List<String> list = new ArrayList<>(); // 可能多次扩容

// 正确：预估容量
List<String> list = new ArrayList<>(100); // 避免扩容
```

### 5. 数据结构优化

#### 使用原始类型包装器

```java
// 错误：使用包装类型
List<Integer> numbers = new ArrayList<>();
// 内存开销是int的4-6倍

// 正确：使用原始类型集合
IntList numbers = new IntArrayList(); // 使用第三方库
```

#### 优化嵌套结构

```java
// 错误：深层嵌套Map
Map<String, Map<String, Map<String, Value>>> data;

// 正确：使用扁平化结构
Map<String, Value> data = new HashMap<>();
data.put("a.b.c", value);
```

## 具体优化建议

### 1. Tokenizer优化

```java
// 优化前
private List<Token> tokenizeLine(LineInfo lineInfo) {
    List<Token> tokens = new ArrayList<>();
    // ... 处理每个字符
    return tokens;
}

// 优化后
private List<Token> tokenizeLine(LineInfo lineInfo) {
    List<Token> tokens = tokenPool.borrow();
    try {
        tokens.clear();
        // ... 批量处理字符
        return tokens;
    } finally {
        tokenPool.returnObject(tokens);
    }
}
```

### 2. AST优化

```java
// 使用对象池创建AST节点
ASTNode node = astNodePool.borrow();
try {
    node.reset(type, line);
    // ... 设置节点属性
    return node;
} finally {
    astNodePool.returnObject(node);
}

// 优化属性存储
private static final Map<Class<?>, Map<String, Object>> ATTRIBUTES_POOL = 
    new ConcurrentHashMap<>();
```

### 3. 代码生成优化

```java
// 使用StringBuilder池
private static final ThreadLocal<StringBuilder> SB_LOCAL = 
    ThreadLocal.withInitial(() -> new StringBuilder(8192));

// 获取当前线程的StringBuilder
StringBuilder sb = SB_LOCAL.get();
sb.setLength(0); // 重置而不是创建新对象
```

### 4. 内存监控

```java
// 添加内存监控
Runtime runtime = Runtime.getRuntime();
long usedMemory = runtime.totalMemory() - runtime.freeMemory();
long maxMemory = runtime.maxMemory();

System.out.printf("内存使用: %.2f MB / %.2f MB%n",
    usedMemory / (1024.0 * 1024),
    maxMemory / (1024.0 * 1024));
```

## 工具和库推荐

### 1. 对象池库
- Apache Commons Pool
- Eclipse Collections
- fastutil

### 2. 原始类型集合
- Eclipse Collections
- fastutil
- HPPC (High-Performance Primitive Collections)

### 3. 内存分析工具
- VisualVM
- YourKit
- Eclipse MAT

## 性能指标

### 目标指标
- **GC频率**: < 2次/秒
- **内存使用率**: < 70%
- **对象创建率**: 减少30%以上
- **编译吞吐量**: > 2000函数/秒

### 测试方法

```java
// 使用JMH进行基准测试
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class TokenizerBenchmark {
    
    @Benchmark
    public void testTokenization() {
        // 测试词法分析性能
    }
}
```

## 常见问题

### 1. 内存泄漏排查

```java
// 使用WeakReference
private final Map<String, WeakReference<ASTNode>> weakCache = new WeakHashMap<>();

// 定期清理
weakCache.entrySet().removeIf(e -> e.getValue().get() == null);
```

### 2. OOM错误处理

```java
// 设置合理的堆大小
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-Xms512m -Xmx2g
```

### 3. 内存碎片整理

```java
// 定期触发Full GC
System.gc();

// 使用G1垃圾收集器
-XX:+UseG1GC
```

## 总结

通过合理使用对象池、缓存机制、优化数据结构，可以显著减少内存分配和GC压力，提高编译器性能。记住：

1. **测量优化效果**：使用性能监控工具
2. **避免过早优化**：根据实际瓶颈进行优化
3. **保持代码清晰**：优化不应牺牲可读性
4. **持续监控**：内存使用情况是动态变化的

## 参考资源

- [Java Performance Tuning Guide](https://www.baeldung.com/java-performance-tuning)
- [Eclipse Collections](https://www.eclipse.org/collections/)
- [fastutil](https://fastutil.di.unimi.it/)