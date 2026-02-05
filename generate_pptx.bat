@echo off
echo Installing python-pptx...
pip install python-pptx
if errorlevel 1 (
    echo Trying with Python 3.12...
    py -3.12 -m pip install python-pptx
)
echo.
echo Generating presentation...
python create_presentation.py
if errorlevel 1 (
    python create_presentation.py
)
if exist ReleaseManagementSystem_Presentation.pptx (
    echo.
    echo SUCCESS! Created: ReleaseManagementSystem_Presentation.pptx
    start ReleaseManagementSystem_Presentation.pptx
) else (
    echo.
    echo Failed. Make sure Python 3.8-3.12 is installed and run: pip install python-pptx
)
