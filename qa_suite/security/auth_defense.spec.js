/**
 * SoleLuxe Auth Defense & Brute-Force Protection Test Suite
 * Asserts rate-limiter, strict 429 status response, and automated IP ban rules.
 * 
 * Run via: npm install @playwright/test && npx playwright test auth_defense.spec.js
 */

const { test, expect } = require('@playwright/test');

const BASE_URL = process.env.BASE_URL || 'https://api.soleluxe.premium';
const AUTH_ENDPOINT = `${BASE_URL}/api/auth/login`;

test.describe('SoleLuxe Automated Brute-Force Prevention Tests', () => {

    test('1. Verify high-velocity brute-force trigger blocks client with strict 429 Too Many Requests', async ({ request }) => {
        const targetEmail = "elmaurofiore@gmail.com";
        const totalRequests = 100;
        const requestPromises = [];

        // Concurrently dispatch 100 login attempts within a 5-second interval
        for (let i = 0; i < totalRequests; i++) {
            requestPromises.push(
                request.post(AUTH_ENDPOINT, {
                    data: {
                        email: targetEmail,
                        password: `attacker_guess_number_${i}`
                    },
                    headers: {
                        'X-Forwarded-For': '198.51.100.42', // Simulated static IP
                        'Content-Type': 'application/json'
                    }
                })
            );
        }

        // Wait for all requests to finish processing
        const responses = await Promise.all(requestPromises);

        // Analyze status distribution.
        // The first few requests might return 401 Unauthorized, but the rest MUST trigger 429 Rate Limiting.
        let status401Count = 0;
        let status429Count = 0;

        responses.forEach((res) => {
            if (res.status() === 401) {
                status401Count++;
            } else if (res.status() === 429) {
                status429Count++;
            }
        });

        console.log(`Brute force results: 401 (Unauthorized) = ${status401Count}, 429 (Too Many Requests) = ${status429Count}`);

        // We assert that the rate limiter engaged and successfully protected the user's account with at least some 429s
        expect(status429Count).toBeGreaterThan(0);
        
        // Assert that the last request is strictly 429 Rate Limited
        const finalCheckResponse = await request.post(AUTH_ENDPOINT, {
            data: { email: targetEmail, password: "correct_password" },
            headers: {
                'X-Forwarded-For': '198.51.100.42',
                'Content-Type': 'application/json'
            }
        });

        expect(finalCheckResponse.status()).toBe(429);
        const body = await finalCheckResponse.json();
        expect(body.error).toContain('Too many login attempts');
        expect(body.retry_after_seconds).toBeDefined();
    });

    test('2. IP block list blocks origin requests for standard endpoints immediately', async ({ request }) => {
        // Attempt a harmless GET request to the platform metadata using the banned IP
        const appInfoResponse = await request.get(`${BASE_URL}/api/distribution/center`, {
            headers: {
                'X-Forwarded-For': '198.51.100.42' // Same banned IP
            }
        });

        // The entire gateway must reject any requests from that blacklisted IP address with a 403 or 429 status
        expect([403, 429]).toContain(appInfoResponse.status());
    });
});
