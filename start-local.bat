@echo off
echo Iniciando aplicacao Inventory API no perfil local...
echo.

REM Definir perfil ativo
set SPRING_PROFILES_ACTIVE=local

REM Executar aplicacao
mvnw.cmd spring-boot:run

echo.
echo Aplicacao finalizada.
pause




