import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class SmartCityGateway {

    private static final int IPC_PORT = 9999;
    private static final int WEB_PORT = 8080;
    
    // Using a Cached Thread Pool for multi-threaded dynamic scaling
    private static final ExecutorService threadPool = Executors.newCachedThreadPool();
    private static final ReentrantLock fileLock = new ReentrantLock();
    private static final CopyOnWriteArrayList<HttpExchange> activeSseClients = new CopyOnWriteArrayList<>();
    
    private static volatile String latestTelemetryJson = "{\"activeTargets\":0,\"processingSpeed\":\"0 FPS\",\"anomalyAlerts\":0,\"activeZoneBreach\":\"NONE\"}";

    private static final String LOG_FILE_PATH = "smartcity_transaction_ledger.log";
    private static final String REPORT_FILE_PATH = "STATE_REPORT.md";

    // 🟢 WEEK 4 CONFIGURATION: Paths for historical batch processing
    private static final String INPUT_DIR = "historical_input";
    private static final String OUTPUT_DIR = "historical_output";
    
    // Throttler: Only allow a maximum of 3 files to be processed concurrently to protect system RAM
    private static final Semaphore memoryThrottler = new Semaphore(3);
    
    // 🟢 FIXED REGISTRY: Thread-safe memory checklist to lock filenames currently being processed
    private static final java.util.Set<String> activeProcessingRegistry = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public static void main(String[] args) {
        System.out.println("=== Starting Enterprise Reactive Gateway ===");
        initializeReportHeader();
        
        // 1. Start Web Streaming Server (Port 8080) for Next.js
        startWebStreamingServer();

        // 2. 🟢 WEEK 4: Launch the Asynchronous Historical Batch Processing Loop
        threadPool.submit(() -> startHistoricalBatchQueue());

        // 3. Start Socket Engine Listener (Port 9999) for Live Python Stream
        try (ServerSocket serverSocket = new ServerSocket(IPC_PORT)) {
            System.out.println("[GATEWAY ACTIVE] Listening for Python AI Worker on Port " + IPC_PORT + "...");

            while (true) {
                Socket pythonSocket = serverSocket.accept();
                System.out.println("[IPC CONNECTED] Python AI Vision Engine linked successfully!");
                threadPool.submit(() -> handlePythonStream(pythonSocket));
            }
        } catch (Exception e) {
            System.err.println("[GATEWAY CRASH] Server error: " + e.getMessage());
        }
    }

    // 🟢 WEEK 4: The Batch Ingestion Queue Manager
    private static void startHistoricalBatchQueue() {
        System.out.println("[BATCH QUEUE ACTIVE] Watching folder '" + INPUT_DIR + "' for historical asset data...");
        File inputFolder = new File(INPUT_DIR);

        if (!inputFolder.exists()) {
            inputFolder.mkdirs();
        }
        File outputFolder = new File(OUTPUT_DIR);
        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }

        while (true) {
            try {
                File[] files = inputFolder.listFiles();
                if (files != null && files.length > 0) {
                    for (File file : files) {
                        String filename = file.getName();
                        
                        // 🟢 FIX: If file is a valid file AND NOT already locked in the registry, proceed
                        if (file.isFile() && !filename.startsWith(".") && !activeProcessingRegistry.contains(filename)) {
                            
                            // Immediately register/lock the file in memory so no other loop thread grabs it
                            activeProcessingRegistry.add(filename);
                            
                            // Acquire a execution slot token (max 3)
                            memoryThrottler.acquire();
                            
                            // Dispatch task to the thread pool asynchronously
                            threadPool.submit(() -> processHistoricalFileAsync(file));
                        }
                    }
                }
                // Sleep for 1 second before scanning directory contents again to balance CPU usage
                Thread.sleep(1000);
            } catch (Exception e) {
                System.err.println("[BATCH ERROR] Queue management exception: " + e.getMessage());
            }
        }
    }

    // 🟢 WEEK 4: Processing worker task running inside the concurrent sandbox
    private static void processHistoricalFileAsync(File file) {
        long startTime = System.currentTimeMillis();
        String filename = file.getName();
        System.out.println("[PROCESSING START] Ingesting historical file: " + filename);

        try {
            // Simulate deep analytical computing time (e.g., parsing historical coordinate arrays)
            Thread.sleep(4000); 

            // Safely rename and move file from historical_input to historical_output folder
            File destFile = new File(OUTPUT_DIR + File.separator + filename);
            if (file.renameTo(destFile)) {
                long duration = System.currentTimeMillis() - startTime;
                String logMessage = "HISTORICAL_BATCH_SUCCESS -> File: " + filename + " Processed in " + duration + "ms";
                
                System.out.println("[PROCESSING COMPLETE] Archived: " + filename);
                logTransactionAtomic(logMessage);
            } else {
                System.err.println("[BATCH WARNING] Failed to archive file: " + filename);
            }

        } catch (Exception e) {
            System.err.println("[BATCH CRASH] Failed processing " + filename + ": " + e.getMessage());
        } finally {
            // 🟢 FIX: Always clean up registry and release the throttle slot back to the line
            activeProcessingRegistry.remove(filename);
            memoryThrottler.release();
        }
    }

    private static void startWebStreamingServer() {
        try {
            HttpServer webServer = HttpServer.create(new InetSocketAddress(WEB_PORT), 0);
            webServer.createContext("/api/live-metrics", new SseStreamHandler());
            webServer.setExecutor(threadPool); 
            webServer.start();
            System.out.println("[WEB SERVER RUNNING] Next.js listening at http://localhost:" + WEB_PORT + "/api/live-metrics");
        } catch (Exception e) {
            System.err.println("Failed to initialize web layer: " + e.getMessage());
        }
    }

    static class SseStreamHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws java.io.IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "*");
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.getResponseHeaders().add("Cache-Control", "no-cache");
            exchange.getResponseHeaders().add("Connection", "keep-alive");
            exchange.sendResponseHeaders(200, 0);
            activeSseClients.add(exchange);
            System.out.println("[UI LINKED] Dashboard session joined. Total: " + activeSseClients.size());
        }
    }

    private static void handlePythonStream(Socket socket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"))) {
            String incomingPacket;
            while ((incomingPacket = reader.readLine()) != null) {
                incomingPacket = incomingPacket.trim();
                if (incomingPacket.isEmpty()) continue;
                System.out.println("[REACTIVE CORE INTAKE]: " + incomingPacket);
                latestTelemetryJson = incomingPacket;
                broadcastToDashboard(incomingPacket);
                logTransactionAtomic(incomingPacket);
                updateMarkdownReport(incomingPacket);
            }
        } catch (Exception e) {
            System.out.println("[IPC DISCONNECT] Live Python worker closed the channel.");
        } finally {
            try { socket.close(); } catch (Exception e) {}
        }
    }

    private static void broadcastToDashboard(String telemetryData) {
        if (activeSseClients.isEmpty()) return;
        String sseFormattedPacket = "data: " + telemetryData + "\n\n";
        byte[] sseBytes = sseFormattedPacket.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        for (HttpExchange client : activeSseClients) {
            try {
                OutputStream os = client.getResponseBody();
                os.write(sseBytes);
                os.flush();
            } catch (Exception e) {
                activeSseClients.remove(client);
            }
        }
    }

    private static void logTransactionAtomic(String logData) {
        fileLock.lock();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE_PATH, true))) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
            writer.write("[" + timestamp + "] " + logData);
            writer.newLine();
            writer.flush(); 
        } catch (Exception e) {
            System.err.println("Failed writing to log: " + e.getMessage());
        } finally {
            fileLock.unlock();
        }
    }

    private static void initializeReportHeader() {
        fileLock.lock();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(REPORT_FILE_PATH, false))) {
            writer.write("# Smart City Vision Pipeline - System State Report\n");
            writer.write("## Last Operational Metrics Snapshot\n\n");
            writer.write("| Metrics Parameters | Current System Values |\n");
            writer.write("| :--- | :--- |\n");
            writer.write("| **Status** | Waiting for Python Pipeline connection... |\n");
            writer.flush();
        } catch (Exception e) {
            System.err.println("Failed initializing report template structure.");
        } finally {
            fileLock.unlock();
        }
    }

    private static void updateMarkdownReport(String rawJson) {
        fileLock.lock();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(REPORT_FILE_PATH, false))) {
            String updateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            writer.write("# Smart City Vision Pipeline - System State Report\n");
            writer.write("## System Status: RUNNING ✅\n\n");
            writer.write("### Live Metrics Snapshot Table\n");
            writer.write("| Metrics Parameters | Current System Values |\n");
            writer.write("| :--- | :--- |\n");
            
            String targets = extractCleanValue(rawJson, "activeTargets");
            String speed = extractCleanValue(rawJson, "processingSpeed");
            String anomalies = extractCleanValue(rawJson, "anomalyAlerts");
            String zoneBreach = extractCleanValue(rawJson, "activeZoneBreach");

            writer.write("| **Active Detected Targets** | " + targets + " units |\n");
            writer.write("| **Inference Compute Speed** | " + speed + " |\n");
            writer.write("| **Programmatic Anomalies** | " + anomalies + " alerts |\n");
            writer.write("| **Active Breach Quadrant** | `" + zoneBreach + "` |\n");
            writer.write("| **Last Pipeline Sync Time** | " + updateTime + " IST |\n");
            writer.write("\n### Raw Telemetry Stream Frame Check\n");
            writer.write("`" + rawJson + "`\n");
            writer.flush(); 
        } catch (Exception e) {
            // Fail silent
        } finally {
            fileLock.unlock();
        }
    }

    private static String extractCleanValue(String json, String key) {
        try {
            if (!json.contains(key)) return "N/A";
            int startIndex = json.indexOf(key) + key.length();
            while (startIndex < json.length() && (json.charAt(startIndex) == '"' || json.charAt(startIndex) == ':' || json.charAt(startIndex) == ' ')) {
                startIndex++;
            }
            int endIndex = startIndex;
            while (endIndex < json.length() && json.charAt(endIndex) != ',' && json.charAt(endIndex) != '}') {
                endIndex++;
            }
            String val = json.substring(startIndex, endIndex).trim();
            return val.replace("\"", "");
        } catch (Exception e) {
            return "Parse Error";
        }
    }
}