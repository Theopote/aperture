# Aperture 项目重构 - 清理旧文件脚本 (PowerShell)
# 在项目根目录 (aperture-26.1) 下运行此脚本

Write-Host "🧹 开始清理 Aperture 项目旧文件..." -ForegroundColor Cyan
Write-Host ""

# 1. 删除旧的几何类（aperture-core）
Write-Host "1️⃣ 删除旧的几何类 (aperture-core/src/main/java/dev/aperture/core/geometry/)..." -ForegroundColor Yellow
$path1 = "aperture-core/src/main/java/dev/aperture/core/geometry"
if (Test-Path $path1) {
    Remove-Item -Recurse -Force $path1
    Write-Host "   ✅ 已删除" -ForegroundColor Green
} else {
    Write-Host "   ⏭️  目录不存在，跳过" -ForegroundColor Gray
}
Write-Host ""

# 2. 删除旧的 generator 包（aperture-geometry）
Write-Host "2️⃣ 删除旧的 generators 包 (aperture-geometry/src/main/java/dev/aperture/geometry/generators/)..." -ForegroundColor Yellow
$path2 = "aperture-geometry/src/main/java/dev/aperture/geometry/generators"
if (Test-Path $path2) {
    Remove-Item -Recurse -Force $path2
    Write-Host "   ✅ 已删除" -ForegroundColor Green
} else {
    Write-Host "   ⏭️  目录不存在，跳过" -ForegroundColor Gray
}
Write-Host ""

# 3. 删除旧的 Fabric placement 包
Write-Host "3️⃣ 删除旧的 Fabric placement 包 (src/main/java/dev/aperture/placement/)..." -ForegroundColor Yellow
$path3 = "src/main/java/dev/aperture/placement"
if (Test-Path $path3) {
    Remove-Item -Recurse -Force $path3
    Write-Host "   ✅ 已删除" -ForegroundColor Green
} else {
    Write-Host "   ⏭️  目录不存在，跳过" -ForegroundColor Gray
}
Write-Host ""

# 4. 删除错误位置的 client 目录
Write-Host "4️⃣ 删除错误位置的 client 目录 (src/main/java/dev/aperture/client/)..." -ForegroundColor Yellow
$path4 = "src/main/java/dev/aperture/client"
if (Test-Path $path4) {
    Remove-Item -Recurse -Force $path4
    Write-Host "   ✅ 已删除" -ForegroundColor Green
} else {
    Write-Host "   ⏭️  目录不存在，跳过" -ForegroundColor Gray
}
Write-Host ""

# 5. 删除空的测试目录
Write-Host "5️⃣ 清理空的测试目录..." -ForegroundColor Yellow
$path5 = "aperture-geometry/src/test/java/dev/aperture/geometry/generators"
if (Test-Path $path5) {
    try {
        Remove-Item $path5 -ErrorAction SilentlyContinue
        Write-Host "   ✅ 已清理" -ForegroundColor Green
    } catch {
        Write-Host "   ⏭️  无法删除（可能不为空）" -ForegroundColor Gray
    }
} else {
    Write-Host "   ⏭️  目录不存在，跳过" -ForegroundColor Gray
}
Write-Host ""

Write-Host "✨ 清理完成！" -ForegroundColor Green
Write-Host ""
Write-Host "📋 下一步：" -ForegroundColor Cyan
Write-Host "   运行 './gradlew clean build' 来验证重构" -ForegroundColor White
