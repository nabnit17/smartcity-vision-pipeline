'use client';

import { useEffect, useState, useRef } from 'react';

interface TelemetryData {
  activeTargets: number;
  processingSpeed: string;
  anomalyAlerts: number;
}

interface InfractionLog {
  time: string;
  type: string;
  vehicleNo: string;
  mockSnapshotUrl: string;
}

// 🟢 Standardized the component function identity name for Next.js App routing
export default function Page() {
  const [metrics, setMetrics] = useState<TelemetryData>({
    activeTargets: 0,
    processingSpeed: '0 FPS',
    anomalyAlerts: 0,
  });

  const [logs, setLogs] = useState<InfractionLog[]>([]);
  const logsBottomRef = useRef<HTMLDivElement | null>(null);

  const generateOdishaPlate = () => {
    const random2Digit = Math.floor(10 + Math.random() * 90);
    const alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    const randomLetter1 = alphabet[Math.floor(Math.random() * 26)];
    const randomLetter2 = alphabet[Math.floor(Math.random() * 26)];
    const random4Digit = Math.floor(1000 + Math.random() * 9000);
    
    return `OD ${random2Digit} ${randomLetter1}${randomLetter2} ${random4Digit}`;
  };

  useEffect(() => {
    if (logsBottomRef.current) {
      logsBottomRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [logs]);

  useEffect(() => {
    const javaSseUrl = 'http://localhost:8000/api/live-metrics';
    const eventSource = new EventSource(javaSseUrl);

    eventSource.onmessage = (event) => {
      try {
        const incomingPayload: TelemetryData = JSON.parse(event.data);
        setMetrics(incomingPayload);

        if (incomingPayload.anomalyAlerts > 0) {
          const currentTime = new Date().toLocaleTimeString();
          
          setLogs((prevLogs) => {
            if (prevLogs.length > 0 && prevLogs[prevLogs.length - 1].time === currentTime) return prevLogs;
            
            const randomId = Math.floor(Math.random() * 1000);
            const newLog: InfractionLog = {
              time: currentTime,
              type: 'Encroachment',
              vehicleNo: generateOdishaPlate(),
              mockSnapshotUrl: `https://picsum.photos/id/${randomId % 50}/120/70`,
            };
            
            return [...prevLogs, newLog];
          });
        }
      } catch (error) {
        console.error('Data parsing mismatch:', error);
      }
    };

    return () => eventSource.close();
  }, []);

  return (
    <div style={{ backgroundColor: '#0f172a', color: '#f8fafc', minHeight: '100vh', padding: '1.25rem', fontFamily: 'sans-serif', boxSizing: 'border-box' }}>
      
      <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #1e293b', paddingBottom: '0.5rem', marginBottom: '1.25rem' }}>
        <div style={{ display: 'flex', alignItems: 'baseline', gap: '1rem' }}>
          <h1 style={{ fontSize: '1.4rem', fontWeight: 'bold', margin: 0, color: '#38bdf8', letterSpacing: '0.015em' }}>
            SMART CITY VISION PIPELINE
          </h1>
          <span style={{ fontSize: '0.7rem', color: '#475569', fontFamily: 'monospace' }}>
            ({metrics.processingSpeed.toLowerCase()})
          </span>
        </div>
      </header>

      <div style={{ display: 'grid', gridTemplateColumns: '7fr 3fr', gap: '1.25rem', alignItems: 'start' }}>
        
        <div style={{ backgroundColor: '#1e293b', border: '1px solid #334155', borderRadius: '12px', padding: '0.75rem' }}>
          <div style={{ position: 'relative', width: '100%', borderRadius: '8px', overflow: 'hidden', backgroundColor: '#020617' }}>
            <img 
              src="http://localhost:8000/api/video-feed" 
              alt="Camera Stream Core Feed Processing" 
              style={{ width: '100%', height: 'auto', display: 'block' }}
            />
          </div>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', maxHeight: '85vh' }}>
          
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
            <div style={{ backgroundColor: '#1e293b', border: '1px solid #334155', borderRadius: '8px', padding: '0.75rem' }}>
              <p style={{ textTransform: 'uppercase', fontSize: '0.6rem', fontWeight: 'bold', color: '#94a3b8', margin: '0 0 0.15rem 0' }}>Targets</p>
              <p style={{ fontSize: '1.3rem', fontWeight: 'bold', margin: 0 }}>{metrics.activeTargets} <span style={{ fontSize: '0.75rem', color: '#64748b' }}>units</span></p>
            </div>

            <div style={{ 
              backgroundColor: metrics.anomalyAlerts > 0 ? '#450a0a' : '#1e293b', 
              border: metrics.anomalyAlerts > 0 ? '1px solid #f43f5e' : '1px solid #334155', 
              borderRadius: '8px', padding: '0.75rem', transition: 'all 0.2s ease'
            }}>
              <p style={{ textTransform: 'uppercase', fontSize: '0.6rem', fontWeight: 'bold', color: '#94a3b8', margin: '0 0 0.15rem 0' }}>Alarms</p>
              <p style={{ fontSize: '1.3rem', fontWeight: 'bold', margin: 0, color: metrics.anomalyAlerts > 0 ? '#f43f5e' : '#e2e8f0' }}>{metrics.anomalyAlerts}</p>
            </div>
          </div>

          <div style={{ 
            backgroundColor: '#1e293b', 
            border: '1px solid #334155', 
            borderRadius: '12px', 
            padding: '1rem', 
            display: 'flex',
            flexDirection: 'column',
            height: 'calc(100vh - 200px)', 
            boxSizing: 'border-box'
          }}>
            <h3 style={{ fontSize: '0.75rem', fontWeight: 'bold', margin: '0 0 0.75rem 0', color: '#f43f5e', letterSpacing: '0.05em' }}>
              🚨 LIVE INCIDENT TICKER HISTORY
            </h3>
            
            <div style={{ 
              flex: 1,
              overflowY: 'auto', 
              paddingRight: '4px',
              display: 'flex', 
              flexDirection: 'column', 
              gap: '0.6rem',
            }}>
              {logs.length === 0 ? (
                <div style={{ padding: '2rem 1rem', textAlign: 'center', color: '#64748b', fontSize: '0.75rem', fontStyle: 'italic', border: '1px dashed #334155', borderRadius: '6px', margin: 'auto 0' }}>
                  No active traffic line breaches detected.
                </div>
              ) : (
                logs.map((log, index) => {
                  const isNewestItem = index === logs.length - 1;
                  return (
                    <div 
                      key={index} 
                      style={{ 
                        display: 'grid',
                        gridTemplateColumns: '2fr 1fr',
                        gap: '0.5rem',
                        alignItems: 'center',
                        padding: '0.5rem', 
                        borderRadius: '6px', 
                        backgroundColor: isNewestItem ? '#450a0a' : '#0f172a',
                        border: isNewestItem ? '1px solid #f43f5e' : '1px solid #1e293b',
                        fontSize: '0.75rem',
                      }}
                    >
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.15rem' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', paddingRight: '4px' }}>
                          <span style={{ color: '#f43f5e', fontWeight: 'bold' }}>{log.type}</span>
                          <span style={{ color: '#64748b', fontSize: '0.65rem' }}>{log.time}</span>
                        </div>
                        <div style={{ fontSize: '0.85rem', color: '#38bdf8', fontFamily: 'monospace', fontWeight: 'bold' }}>
                          {log.vehicleNo}
                        </div>
                        <span style={{ fontSize: '0.6rem', color: '#10b981' }}>📷 ev_snapshot.jpg</span>
                      </div>

                      <div style={{ width: '100%', height: '48px', overflow: 'hidden', borderRadius: '4px', border: '1px solid #334155', backgroundColor: '#020617' }}>
                        <img 
                          src={log.mockSnapshotUrl} 
                          alt="Evidential Snapshot"
                          style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                        />
                      </div>
                    </div>
                  );
                })
              )}
              <div ref={logsBottomRef} />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}