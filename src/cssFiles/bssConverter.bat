@echo off

for %%f in (*.css) do (
    "%JAVA_HOME%\bin\javapackager" -createbss -srcfiles "%cd%\%%f" -outdir . 
)

echo.
echo Press any key to exit . . .
pause>nul