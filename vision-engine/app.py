import asyncio
import cv2
import time
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from inference_worker import InferenceWorker

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

worker = InferenceWorker()

@app.on_event("startup")
async def startup_event():
    asyncio.create_task(worker.start_stream(0))  # ← webcam

@app.on_event("shutdown")
def shutdown_event():
    print("--- Shutting down FastAPI Server ---")
    worker.stop()

@app.get("/api/live-metrics")
async def live_metrics():
    async def event_generator():
        while True:
            real_fps = getattr(worker, 'latest_fps', 25.0)
            fps_display = f"{real_fps} FPS"
            real_anomalies = getattr(worker, 'latest_anomalies', 0)
            active_targets = getattr(worker, 'latest_detections', 0)
            yield f"data: {{\"activeTargets\": {active_targets}, \"processingSpeed\": \"{fps_display}\", \"anomalyAlerts\": {real_anomalies}}}\n\n"
            await asyncio.sleep(1)

    return StreamingResponse(event_generator(), media_type="text/event-stream")

@app.get("/api/video-feed")
def video_feed():
    def frame_generator():
        while True:
            frame = getattr(worker, 'current_frame', None)
            if frame is None:
                time.sleep(0.03)
                continue
            success, encoded_jpeg = cv2.imencode('.jpg', frame)
            if not success:
                time.sleep(0.03)
                continue
            frame_bytes = encoded_jpeg.tobytes()
            yield (b'--frame\r\n'
                   b'Content-Type: image/jpeg\r\n\r\n' + frame_bytes + b'\r\n')
            time.sleep(0.033)

    return StreamingResponse(frame_generator(), media_type="multipart/x-mixed-replace; boundary=frame")