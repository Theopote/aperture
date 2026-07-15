@echo off
REM Aperture 项目重构 - 清理旧文件脚本 (Windows批处理)
REM 在项目根目录 (aperture-26.1) 下运行此脚本

echo.
echo 🧹 开始清理 Aperture 项目旧文件...
echo.

REM 1. 删除旧的几何类（aperture-core）
echo 1️⃣ 删除旧的几何类 (aperture-core/src/main/java/dev/aperture/core/geometry/)...
if exist "aperture-core\src\main\java\dev\aperture\core\geometry" (
    rmdir /s /q "aperture-core\src\main\java\dev\aperture\core\geometry"
    echo    ✅ 已删除
) else (
    echo    ⏭️ 目录不存在，跳过
)
echo.

REM 2. 删除旧的 generator 包（aperture-geometry）
echo 2️⃣ 删除旧的 generators 包 (aperture-geometry/src/main/java/dev/aperture/geometry/generators/)...
if exist "aperture-geometry\src\main\java\dev\aperture\geometry\generators" (
    rmdir /s /q "aperture-geometry\src\main\java\dev\aperture\geometry\generators"
    echo    ✅ 已删除
) else (
    echo    ⏭️ 目录不存在，跳过
)
echo.

REM 3. 删除旧的 Fabric placement 包
echo 3️⃣ 删除旧的 Fabric placement 包 (src/main/java/dev/aperture/placement/)...
if exist "src\main\java\dev\aperture\placement" (
    rmdir /s /q "src\main\java\dev\aperture\placement"
    echo    ✅ 已删除
) else (
    echo    ⏭️ 目录不存在，跳过
)
echo.

REM 4. 删除错误位置的 client 目录
echo 4️⃣ 删除错误位置的 client 目录 (src/main/java/dev/aperture/client/)...
if exist "src\main\java\dev\aperture\client" (
    rmdir /s /q "src\main\java\dev\aperture\client"
    echo    ✅ 已删除
) else (
    echo    ⏭️ 目录不存在，跳过
)
echo.

REM 5. 删除空的测试目录
echo 5️⃣ 清理空的测试目录...
if exist "aperture-geometry\src\test\java\dev\aperture\geometry\generators" (
    rmdir "aperture-geometry\src\test\java\dev\aperture\geometry\generators" 2>nul
    if errorlevel 1 (
        echo    ⏭️ 无法删除（可能不为空）
    ) else (
        echo    ✅ 已清理
    )
) else (
    echo    ⏭️ 目录不存在，跳过
)
echo.

echo ✨ 清理完成！
echo.
echo 📋 下一步：
echo    运行 'gradlew clean build' 来验证重构
echo.
pause
