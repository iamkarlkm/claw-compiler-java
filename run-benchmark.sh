#!/bin/bash

# 运行性能基准测试

echo "=========================================="
echo "Claw 编译器性能基准测试"
echo "=========================================="
echo ""

# 编译项目
echo "1. 编译项目..."
mvn clean compile -q

if [ $? -ne 0 ]; then
    echo "❌ 编译失败"
    exit 1
fi

echo "✅ 编译成功"
echo ""

# 编译测试代码
echo "2. 编译测试代码..."
mvn test-compile -q

if [ $? -ne 0 ]; then
    echo "❌ 测试代码编译失败"
    exit 1
fi

echo "✅ 测试代码编译成功"
echo ""

# 运行基准测试
echo "3. 运行性能基准测试..."
echo ""

# 使用 java 命令直接运行
java -cp "target/classes:target/test-classes" \
     -Djava.library.path=target/classes \
     com.claw.compiler.benchmark.PerformanceBenchmark

# 检查结果
if [ $? -eq 0 ]; then
    echo ""
    echo "✅ 基准测试完成"
else
    echo ""
    echo "❌ 基准测试失败"
    exit 1
fi
