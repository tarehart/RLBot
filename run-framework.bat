@rem Change the working directory to the location of this file so that relative paths will work
cd /D "%~dp0"

python -c "from rlbot import runner; runner.main();"

pause
