import asyncio
import cv2
import json
import socket
import time
from ultralytics import YOLO

class InferenceWorker:
    def __init__(self, model_path="yolov8n.onnx", java_port=9999):
        self.model = YOLO(model_path, task="detect")
        self.is_running = False
        self.latest_detections = 0
        self.latest_fps = 0.0
        self.latest_anomalies = 0
        self.java_host = "127.0.0.1"
        self.java_port = java_port
        self.current_frame = None

        self.signal_state = "GREEN"
        self.last_signal_change = time.time()
        self.state_duration = 30.0

    def update_traffic_signal(self):
        current_time = time.time()
        elapsed_time = current_time - self.last_signal_change

        if elapsed_time >= self.state_duration:
            self.last_signal_change = current_time
            if self.signal_state == "GREEN":
                self.signal_state = "YELLOW"
            elif self.signal_state == "YELLOW":
                self.signal_state = "RED"
            elif self.signal_state == "RED":
                self.signal_state = "GREEN"

        return int(self.state_duration - elapsed_time)

    async def start_stream(self, source=0):
        self.cap = cv2.VideoCapture(source)
        self.is_running = True

        print("--- AI Inference Engine: Upper Sector Rule Enforcement Activated ---")
        client_socket = None

        try:
            try:
                client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                client_socket.connect((self.java_host, self.java_port))
                print(f"[IPC LINKED] Pushing metrics to Java on port {self.java_port}")
            except Exception:
                print(f"[IPC WARNING] Port {self.java_port} closed. Running standalone mode.")
                client_socket = None

            while self.is_running:
                if not self.cap.isOpened():
                    break

                ret, frame = self.cap.read()
                if not ret:
                    break

                time_remaining = self.update_traffic_signal()
                height, width, _ = frame.shape
                stop_line_y = int(height * 0.60)

                start_time = time.time()
                loop = asyncio.get_running_loop()
                results = await loop.run_in_executor(None, lambda: self.model(frame, verbose=False))
                end_time = time.time()

                processing_time = end_time - start_time
                actual_fps = 1.0 / processing_time if processing_time > 0 else 30.0
                self.latest_fps = round(actual_fps, 1)

                detections = len(results[0].boxes)
                self.latest_detections = detections

                anomaly_count = 0
                for box in results[0].boxes:
                    x1, y1, x2, y2 = map(int, box.xyxy[0])
                    center_y = int((y1 + y2) / 2)
                    if self.signal_state == "RED":
                        if center_y < stop_line_y:
                            anomaly_count += 1

                self.latest_anomalies = anomaly_count

                telemetry_payload = {
                    "activeTargets": detections,
                    "processingSpeed": f"{self.latest_fps} FPS",
                    "anomalyAlerts": anomaly_count,
                    "activeZoneBreach": f"SIGNAL_{self.signal_state}",
                    "timestamp": time.time()
                }

                if client_socket:
                    try:
                        packet_string = json.dumps(telemetry_payload) + "\n"
                        client_socket.sendall(packet_string.encode('utf-8'))
                    except Exception:
                        client_socket = None

                annotated_frame = results[0].plot()
                line_color = (0, 255, 0) if self.signal_state == "GREEN" else ((0, 255, 255) if self.signal_state == "YELLOW" else (0, 0, 255))
                cv2.line(annotated_frame, (0, stop_line_y), (width, stop_line_y), line_color, 3)

                text_color = line_color
                cv2.putText(annotated_frame, f"Signal Status: {self.signal_state} ({time_remaining}s remaining)", (20, 40), cv2.FONT_HERSHEY_SIMPLEX, 0.7, text_color, 2)
                cv2.putText(annotated_frame, f"Rule Enforcement: {'ACTIVE (UPPER SECTOR)' if self.signal_state == 'RED' else 'INACTIVE (FREE ROW)'}", (20, 70), cv2.FONT_HERSHEY_SIMPLEX, 0.55, (255, 255, 255), 2)

                # ✅ Frame stored for dashboard streaming — no imshow needed
                self.current_frame = annotated_frame

                await asyncio.sleep(0.01)

        finally:
            self.is_running = False
            if client_socket:
                client_socket.close()
            if self.cap.isOpened():
                self.cap.release()
            # ✅ No cv2.destroyAllWindows() — headless opencv can't do this

    def stop(self):
        print("\n--- AI Engine: Initiating Rapid Hardware Shutdown Handshake ---")
        self.is_running = False
        if hasattr(self, 'cap') and self.cap.isOpened():
            self.cap.release()
            print("[HARDWARE] Camera frame buffer unhooked successfully.")