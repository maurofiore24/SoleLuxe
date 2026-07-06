/**
 * SoleLuxe Network Fault & Socket Disconnection Stress Test (k6)
 * Simulates a "dirty socket drop" during high-concurrency cryptographic key exchanges.
 * 
 * Verifies:
 * 1. Database connection pool doesn't leak or hang on disconnected sockets.
 * 2. Instant reconnection handles replay events without message duplication.
 * 
 * Run via: k6 run dirty_disconnect.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics to measure resilience
const deadThreadLeakRate = new Rate('dead_thread_leaks');
const messageReplaySuccessRate = new Rate('message_replay_success');
const reconnectionLatencyTrend = new Trend('reconnection_latency_ms');

export const options = {
    scenarios: {
        dirty_socket_drops: {
            executor: 'constant-vus',
            vus: 500, // 500 concurrent elite users during active E2EE handshakes
            duration: '2m',
        },
    },
    thresholds: {
        dead_thread_leaks: ['rate<0.01'],            // Less than 1% leak tolerance
        message_replay_success: ['rate>0.99'],        // 99% of replayed packets must deliver flawlessly
        reconnection_latency_ms: ['p(95)<400'],       // Reconnections must establish under 400ms
    },
};

const BASE_URL = 'https://api.soleluxe.premium';
const AUTH_TOKEN = 'token_resilience_vault_4492d';

export default function () {
    const headers = {
        'Authorization': `Bearer ${AUTH_TOKEN}`,
        'Content-Type': 'application/json',
        'X-Connection-Mode': 'Keep-Alive'
    };

    // --- Phase 1: Initiate Cryptographic Key Exchange ---
    const payload = JSON.stringify({
        channel_id: "private_channel_881",
        client_pub_key: "04debf993adcb1a907ff2a105c308b9ad17e2e1fa009087cfef4019a",
        handshake_step: "1_INIT"
    });

    let step1Res = http.post(`${BASE_URL}/api/chat/handshake`, payload, { headers });
    const isStep1Ok = check(step1Res, {
        'handshake_step_1_ok': (r) => r.status === 200 || r.status === 201
    });

    if (!isStep1Ok) {
        deadThreadLeakRate.add(1);
        return;
    }

    // --- Phase 2: Simulate Sudden "Dirty" Socket Disconnect / Drop Connection ---
    // In our k6 simulation, we drop connection state by sending a custom simulated headers
    // trigger to the premium load-balancer signaling abnormal socket termination (no FIN/RST packet)
    const dropHeaders = {
        'Authorization': `Bearer ${AUTH_TOKEN}`,
        'Content-Type': 'application/json',
        'X-Simulated-Fault': 'DIRTY_SOCKET_RESET_ABRUPT'
    };

    let dropRes = http.post(`${BASE_URL}/api/connection/simulate-drop`, JSON.stringify({
        socket_id: step1Res.json().socket_id || "mock_socket_uuid_881"
    }), { headers: dropHeaders });

    check(dropRes, {
        'fault_injection_acknowledged': (r) => r.status === 200
    });

    // Let the server handle connection timeout & database pooling reclamation for a brief sleep
    sleep(1);

    // --- Phase 3: Instant Client Reconnection & Replay Engine Request ---
    const startReconnect = Date.now();
    const reconnectPayload = JSON.stringify({
        last_received_message_id: "msg_lounge_402",
        reconnect_session_token: step1Res.json().reconnect_token || "mock_rec_token_112"
    });

    let reconnectRes = http.post(`${BASE_URL}/api/connection/reconnect-replay`, reconnectPayload, { headers });
    const endReconnect = Date.now();

    const isReconnectOk = check(reconnectRes, {
        'reconnection_status_200': (r) => r.status === 200,
        'missed_messages_delivered': (r) => r.body.includes('messages_replayed'),
        'no_duplicate_messages': (r) => !r.body.includes('DUPLICATE_REPLAY_ERROR')
    });

    if (isReconnectOk) {
        reconnectionLatencyTrend.add(endReconnect - startReconnect);
        messageReplaySuccessRate.add(1);
        deadThreadLeakRate.add(0);
    } else {
        messageReplaySuccessRate.add(0);
        // If reconnect failed with database exhaustion or timeout, report thread leak threat
        if (reconnectRes.status === 500 || reconnectRes.status === 503) {
            deadThreadLeakRate.add(1);
        }
    }

    sleep(2);
}
