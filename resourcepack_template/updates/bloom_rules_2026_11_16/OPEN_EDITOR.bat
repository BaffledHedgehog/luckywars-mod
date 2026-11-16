@echo off
cd /d "%~dp0"
py -3 edit_bloom.py
if errorlevel 1 pause
