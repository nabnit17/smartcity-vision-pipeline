@echo off
echo === Starting Python AI Vision Engine Locally ===
cd vision-engine
pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 8000
pause