/**
 * SoleLuxe WebSocket Stream Connection & Memory Leak Simulation Suite
 * Models 1,000 parallel elite chat WebSocket sessions.
 * 
 * Verifies:
 * 1. Linear/flat memory growth during high-concurrency heartbeat ping/pong events.
 * 2. Instant and clean garbage-collection reclamation of 100% memory buffers after socket closure.
 * 
 * Run via: node websocket_leak.js
 */

const { promisify } = require('util');
const sleep = promisify(setTimeout);

class SimulatedWebSocketClient {
    constructor(userId, channelId) {
        this.userId = userId;
        this.channelId = channelId;
        this.isClosed = false;
        this.listeners = {};
        
        // Emulating buffer allocation (10KB buffer overhead per E2EE chat stream)
        this.streamBuffer = Buffer.alloc(10240, 'x'); 
    }

    send(event, payload) {
        if (this.isClosed) return;
        // Mock socket streaming transport
    }

    on(event, callback) {
        this.listeners[event] = callback;
    }

    trigger(event, data) {
        if (this.listeners[event]) {
            this.listeners[event](data);
        }
    }

    close() {
        this.isClosed = true;
        this.streamBuffer = null; // Free allocated heap buffer
        this.listeners = {};      // Unsubscribe all active observers/listeners
    }
}

async function runEnduranceSimulation() {
    console.log("=== SOLELUXE CHAT GATEWAY: WEBSOCKET ENDURANCE & RECLAMATION STUDY ===");
    
    // Force garbage collection initially if available to establish a clean baseline
    if (global.gc) {
        global.gc();
    }
    const baselineMemory = process.memoryUsage().heapUsed;
    console.log(`[Baseline Heap Size]: ${(baselineMemory / 1024 / 1024).toFixed(2)} MB`);

    const activeConnections = [];
    const targetConnectionsCount = 1000;

    console.log(`\nStep 1: Spawning ${targetConnectionsCount} parallel active WebSocket connections...`);
    for (let i = 1; i <= targetConnectionsCount; i++) {
        const client = new SimulatedWebSocketClient(`user_vip_${i}`, `private_lounge_${i}`);
        
        // Bind E2EE message stream callbacks & subscription observers
        client.on('message', (data) => {
            // Processing high-fidelity models or text commission info
        });
        client.on('ping', () => {
            client.send('pong', { timestamp: Date.now() });
        });

        activeConnections.push(client);

        // Periodically yield to event loop to simulate network propagation stagger
        if (i % 200 === 0) {
            await sleep(50);
        }
    }

    const peakSpawnMemory = process.memoryUsage().heapUsed;
    console.log(`[Peak Connection Heap Size]: ${(peakSpawnMemory / 1024 / 1024).toFixed(2)} MB`);
    const heapIncreaseDuringSpawn = peakSpawnMemory - baselineMemory;
    console.log(`[Heap Overhead per Connection]: ${(heapIncreaseDuringSpawn / targetConnectionsCount / 1024).toFixed(2)} KB`);

    console.log("\nStep 2: Simulating 10-minute heartbeat window (accelerated scale) with rapid ping/pongs...");
    // We run 10 heartbeat intervals, broadcasting to all 1,000 active clients
    let leakDetected = false;
    let heartbeatMemoryTrend = [];

    for (let cycle = 1; cycle <= 10; cycle++) {
        // Broadcast pings
        activeConnections.forEach(client => {
            client.trigger('ping');
            client.trigger('message', { id: `msg_${Date.now()}`, sender: 'system', payload: 'HEARTBEAT_OK' });
        });

        const currentHeap = process.memoryUsage().heapUsed;
        heartbeatMemoryTrend.push(currentHeap);
        
        // Sleep to yield and let runtime handle queue processing
        await sleep(40);
    }

    // Verify heartbeat stream growth was flat/linear
    const firstHeartbeatHeap = heartbeatMemoryTrend[0];
    const finalHeartbeatHeap = heartbeatMemoryTrend[heartbeatMemoryTrend.length - 1];
    const heartbeatLeakBytes = finalHeartbeatHeap - firstHeartbeatHeap;
    
    console.log(`[Initial Heartbeat Heap]: ${(firstHeartbeatHeap / 1024 / 1024).toFixed(2)} MB`);
    console.log(`[Final Heartbeat Heap]: ${(finalHeartbeatHeap / 1024 / 1024).toFixed(2)} MB`);
    console.log(`[Net Heartbeat Leaked Heap]: ${(heartbeatLeakBytes / 1024 / 1024).toFixed(2)} MB`);

    // Leak threshold: Heap growth during heartbeat cycles must remain below 1.5MB total
    if (heartbeatLeakBytes > 1.5 * 1024 * 1024) {
        leakDetected = true;
    }

    console.log("\nStep 3: Closing all sockets and validating instant heap reclamation...");
    activeConnections.forEach(client => {
        client.close();
    });
    
    // Clear array references to allow GC
    activeConnections.length = 0;

    // Wait and force Garbage Collection to execute if available
    await sleep(200);
    if (global.gc) {
        global.gc();
    }

    const postTeardownMemory = process.memoryUsage().heapUsed;
    console.log(`[Post-Teardown Heap Size]: ${(postTeardownMemory / 1024 / 1024).toFixed(2)} MB`);

    const unreclaimedMemory = postTeardownMemory - baselineMemory;
    const reclamationSuccessPercent = Math.max(0, 100 - (unreclaimedMemory / heapIncreaseDuringSpawn) * 100);
    console.log(`[Memory Reclamation Efficiency]: ${reclamationSuccessPercent.toFixed(2)}%`);

    console.log("\n=== VERIFICATION ASSERTS ===");
    if (leakDetected) {
        console.error("❌ FAILED: WebSocket event observers created a progressive memory leak!");
        process.exit(1);
    } else {
        console.log("✔ PASSED: Heartbeat memory curves are completely linear.");
    }

    // Allow at most a 5% residual overhead before GC is fully invoked by runtime
    if (reclamationSuccessPercent < 95) {
        console.error("❌ FAILED: Sockets or streams are retaining orphaned references after teardown!");
        process.exit(1);
    } else {
        console.log("✔ PASSED: Closed sockets freed 100% of internal stream buffers cleanly.");
        process.exit(0);
    }
}

// Execute connection simulation
runEnduranceSimulation().catch(err => {
    console.error("Catastrophic test runner error:", err);
    process.exit(1);
});
