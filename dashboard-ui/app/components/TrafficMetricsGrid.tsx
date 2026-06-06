'use client';
import { useEffect, useState } from 'react';
interface Metrics {
  activeTargets: number;
  processingSpeed: string;
  anomalyAlerts: number;
}
export default function TrafficMetricsGrid() {
  const [metrics, setMetrics] = useState<Metrics>({
    activeTargets: 0,
    processingSpeed: "0.0 FPS",
    anomalyAlerts: 0,
  });
  useEffect(() => {
    const eventSource = new EventSource('http://localhost:8000/api/live-metrics');
    eventSource.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        setMetrics(data);
      } catch (error) {
        console.error("Error parsing live metrics stream:", error);
      }
    };
    return () => {
      eventSource.close();
    };
  }, []);
  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
      {/* Card 1: Real AI Target Counts */}
      <div className="bg-slate-900 p-6 rounded-xl border border-emerald-500/30 shadow-lg transition-all duration-500">
        <div className="flex justify-between items-center">
          <h3 className="text-slate-400 text-sm font-medium uppercase tracking-wider">Active Targets Detected</h3>
          <span className="flex h-2 w-2 relative">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
            <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
          </span>
        </div>
        <p className="text-4xl font-bold text-emerald-400 mt-2 font-mono tabular-nums">{metrics.activeTargets}</p>
        <span className="text-xs text-slate-500 mt-1 block">Live Vehicles & Pedestrians in ROI</span>
      </div>

      {/* Card 2: Compute Speed */}
      <div className="bg-slate-900 p-6 rounded-xl border border-blue-500/30 shadow-lg">
        <h3 className="text-slate-400 text-sm font-medium uppercase tracking-wider">Inference Compute Speed</h3>
        <p className="text-4xl font-bold text-blue-400 mt-2 font-mono tabular-nums">{metrics.processingSpeed}</p>
        <span className="text-xs text-slate-500 mt-1 block">YOLOv8-nano Execution Output</span>
      </div>

      {/* Card 3: Anomalies */}
      <div className="bg-slate-900 p-6 rounded-xl border border-rose-500/30 shadow-lg">
        <h3 className="text-slate-400 text-sm font-medium uppercase tracking-wider">Infrastructure Anomalies</h3>
        <p className="text-4xl font-bold text-rose-400 mt-2 font-mono tabular-nums">{metrics.anomalyAlerts}</p>
        <span className="text-xs text-slate-500 mt-1 block">Automated structural flags</span>
      </div>
    </div>
  );
}