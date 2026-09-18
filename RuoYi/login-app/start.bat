@echo off
chcp 65001 >nul
cd /d %~dp0

echo 正在编译 Java 登录服务...
javac -encoding UTF-8 LoginServer.java
if %errorlevel% neq 0 (
  echo 编译失败，请确认已安装 JDK 并加入 PATH。
  pause
  exit /b
)

echo 启动服务中...
start "RouYi 登录服务" java LoginServer
timeout /t 2 >nul
start "" http://localhost:8080
echo 已在浏览器打开 http://localhost:8080
echo 演示账号：admin / admin123     demo / demo123
pause
