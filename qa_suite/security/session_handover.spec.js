/**
 * SoleLuxe Mobile Carrier Handover & Auth Metadata Protection Tests
 * Simulates active network interface changes (WiFi to LTE/5G handover) mid-transaction.
 * 
 * Verifies:
 * 1. Double billing protection via strict database idempotency checks.
 * 2. Absolute protection against session token hijacking when origin signatures change.
 * 
 * Run via: npm install @playwright/test && npx playwright test session_handover.spec.js
 */

const { test, expect } = require('@playwright/test');

const BASE_URL = process.env.BASE_URL || 'https://api.soleluxe.premium';
const MEDIA_PURCHASE_ENDPOINT = `${BASE_URL}/api/transactions/purchase-media`;

test.describe('SoleLuxe Carrier Handover & Idempotent Secure Transactions', () => {

    test('1. Switch from Wi-Fi to Cellular mid-request must execute exactly once (idempotency key protection)', async ({ request }) => {
        const idempotencyKey = "idem_key_handover_test_99211a";
        const purchasePayload = {
            creator_id: "creator_aurelia_992",
            media_id: "exclusive_lookbook_4k_set_1",
            price_cents: 2999
        };

        // --- Step A: Initiate purchase on Wi-Fi connection (Original IP: 198.51.100.12) ---
        const wifiResponse = await request.post(MEDIA_PURCHASE_ENDPOINT, {
            data: purchasePayload,
            headers: {
                'Authorization': 'Bearer active_patron_sec_jwt_token',
                'X-Idempotency-Key': idempotencyKey,
                'X-Forwarded-For': '198.51.100.12', // WiFi IP
                'Content-Type': 'application/json'
            }
        });

        // The initial purchase must succeed or report processing
        expect([200, 201]).toContain(wifiResponse.status());
        const wifiResult = await wifiResponse.json();
        expect(wifiResult.transaction_status).toBe('COMPLETED');
        expect(wifiResult.charged_cents).toBe(2999);

        // --- Step B: Simulate immediate Carrier Handover (Cellular IP: 203.0.113.88) ---
        // Client retries or sends a follow-up request using the exact same idempotency key
        const cellularResponse = await request.post(MEDIA_PURCHASE_ENDPOINT, {
            data: purchasePayload,
            headers: {
                'Authorization': 'Bearer active_patron_sec_jwt_token',
                'X-Idempotency-Key': idempotencyKey, // Match original key exactly
                'X-Forwarded-For': '203.0.113.88',  // New Cellular/LTE Carrier IP
                'Content-Type': 'application/json'
            }
        });

        // The server must identify the duplicate idempotency key and return the cached original result safely
        expect(cellularResponse.status()).toBe(200);
        const cellularResult = await cellularResponse.json();
        
        expect(cellularResult.transaction_status).toBe('COMPLETED');
        expect(cellularResult.is_cached_response).toBe(true); // Flag verifying safety fallback triggered
        expect(cellularResult.transaction_id).toBe(wifiResult.transaction_id); // Match IDs perfectly
        expect(cellularResult.charged_cents).toBe(2999); // No double charging!
    });

    test('2. Drastic metadata fingerprint signature change must block session immediately (session hijacking block)', async ({ request }) => {
        // A hacker steals the JWT token but has a completely different User-Agent, origin country, and device fingerprint.
        // We simulate a secure request first from the legitimate patron's device
        const validDeviceResponse = await request.get(`${BASE_URL}/api/auth/verify-session`, {
            headers: {
                'Authorization': 'Bearer active_patron_sec_jwt_token',
                'User-Agent': 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15',
                'X-Forwarded-For': '198.51.100.12' // Wi-Fi IP
            }
        });

        expect(validDeviceResponse.status()).toBe(200);

        // Now, a malicious agent attempts to use the same token with a suspicious desktop Linux environment and foreign proxy IP
        const hijackedResponse = await request.get(`${BASE_URL}/api/auth/verify-session`, {
            headers: {
                'Authorization': 'Bearer active_patron_sec_jwt_token',
                'User-Agent': 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36', // Drastic shift
                'X-Forwarded-For': '45.112.8.21' // Malicious Proxy IP
            }
        });

        // The session security layer must flags the dynamic fingerprint metadata mismatch and block access immediately.
        // It should prompt re-authentication to re-bind the session to the new network interface safely
        expect(hijackedResponse.status()).toBe(401);
        const errorBody = await hijackedResponse.json();
        expect(errorBody.error).toContain('Session integrity compromise detected');
        expect(errorBody.reauth_required).toBe(true);
    });
});
