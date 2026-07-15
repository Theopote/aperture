#!/bin/bash
# Aperture 项目重构 - 清理旧文件脚本
# 在项目根目录 (aperture-26.1) 下运行此脚本

set -e

echo "🧹 开始清理 Aperture 项目旧文件..."
echo ""

# 1. 删除旧的几何类（aperture-core）
echo "1️⃣ 删除旧的几何类 (aperture-core/src/main/java/dev/aperture/core/geometry/)..."
if [ -d "aperture-core/src/main/java/dev/aperture/core/geometry" ]; then
    rm -rf aperture-core/src/main/java/dev/aperture/core/geometry/
    echo "   ✅ 已删除"
else
    echo "   ⏭️  目录不存在，跳过"
fi
echo ""

# 2. 删除旧的 generator 包（aperture-geometry）
echo "2️⃣ 删除旧的 generators 包 (aperture-geometry/src/main/java/dev/aperture/geometry/generators/)..."
if [ -d "aperture-geometry/src/main/java/dev/aperture/geometry/generators" ]; then
    rm -rf aperture-geometry/src/main/java/dev/aperture/geometry/generators/
    echo "   ✅ 已删除"
else
    echo "   ⏭️  目录不存在，跳过"
fi
echo ""

# 3. 删除旧的 Fabric placement 包
echo "3️⃣ 删除旧的 Fabric placement 包 (src/main/java/dev/aperture/placement/)..."
if [ -d "src/main/java/dev/aperture/placement" ]; then
    rm -rf src/main/java/dev/aperture/placement/
    echo "   ✅ 已删除"
else
    echo "   ⏭️  目录不存在，跳过"
fi
echo ""

# 4. 删除错误位置的 client 目录
echo "4️⃣ 删除错误位置的 client 目录 (src/main/java/dev/aperture/client/)..."
if [ -d "src/main/java/dev/aperture/client" ]; then
    rm -rf src/main/java/dev/aperture/client/
    echo "   ✅ 已删除"
else
    echo "   ⏭️  目录不存在，跳过"
fi
echo ""

# 5. 删除空的测试目录
echo "5️⃣ 清理空的测试目录..."
if [ -d "aperture-geometry/src/test/java/dev/aperture/geometry/generators" ]; then
    rmdir aperture-geometry/src/test/java/dev/aperture/geometry/generators/ 2>/dev/null || true
    echo "   ✅ 已清理"
else
    echo "   ⏭️  目录不存在，跳过"
fi
echo ""

echo "✨ 清理完成！"
echo ""
echo "📋 下一步："
echo "   运行 './gradlew clean build' 来验证重构"
