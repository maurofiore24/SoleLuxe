/**
 * SoleLuxe Anti-Leak & Private Media Privacy Verification Test Suite
 * Built with Playwright for fast headful/headless browser or programmatic API validation.
 * 
 * Run via: npm install @playwright/test && npx playwright test media_privacy.spec.js
 */

const { test, expect } = require('@playwright/test');

// Mock API endpoints for storage checking
const STORAGE_BASE_URL = process.env.BASE_URL || 'https://api.soleluxe.premium';
const PRIVATE_ASSET_ENDPOINT = `${STORAGE_BASE_URL}/api/storage/private-portfolio`;
const EXIF_CHECK_ENDPOINT = `${STORAGE_BASE_URL}/api/media/upload-strip`;

test.describe('SoleLuxe Media Privacy & Anti-Leak Safeguards', () => {

    test('1. Pre-signed private portfolio URLs must expire securely after 60 seconds', async ({ request }) => {
        // Step A: Generate a secure pre-signed URL with a 60-second TTL
        const generateUrlResponse = await request.post(`${PRIVATE_ASSET_ENDPOINT}/sign`, {
            data: {
                asset_id: "portfolio_heels_gold_9918.heic",
                ttl_seconds: 60,
                user_id: "user_777_gold_patron"
            },
            headers: {
                'Authorization': 'Bearer active_vip_patron_secret_jwt_token',
                'Content-Type': 'application/json'
            }
        });

        expect(generateUrlResponse.status()).toBe(200);
        const { signed_url, expires_at } = await generateUrlResponse.json();
        expect(signed_url).toBeDefined();
        
        // Step B: Assert immediate access is successful (within valid TTL window)
        const immediateAccessResponse = await request.get(signed_url);
        expect(immediateAccessResponse.status()).toBe(200);

        // Step C: Simulate 60 seconds passing (or simulate expired timestamp parameter injection)
        const expiredUrl = signed_url.replace(/expires=[0-9]+/, `expires=${Math.floor(Date.now() / 1000) - 120}`);
        const expiredAccessResponse = await request.get(expiredUrl);
        
        // Assert strict 403 Forbidden on expired signature
        expect(expiredAccessResponse.status()).toBe(403);
        const errorData = await expiredAccessResponse.json();
        expect(errorData.error).toContain('Access Signature Expired');
    });

    test('2. Storage API must securely reject unauthenticated requests or expired tokens', async ({ request }) => {
        // Attempt to fetch private portfolio assets without an authorization header
        const unauthorizedResponse = await request.get(`${PRIVATE_ASSET_ENDPOINT}/list?creator_id=creator_aurelia_992`);
        expect(unauthorizedResponse.status()).toBe(401);

        // Attempt to fetch with a forged or expired token
        const badTokenResponse = await request.get(`${PRIVATE_ASSET_ENDPOINT}/list?creator_id=creator_aurelia_992`, {
            headers: {
                'Authorization': 'Bearer expired_or_malformed_token_payload'
            }
        });
        expect(badTokenResponse.status()).toBe(401);
    });

    test('3. Media upload engine must strip all GPS, Device, and EXIF Metadata from HEIC/JPEG files', async ({ request }) => {
        // Simulate uploading a photo that contains sensitive camera and GPS coordinates
        const mockHEICFileWithEXIF = {
            title: 'Gold Silk Sandals Shoot',
            exif_gps_latitude: '37.7749 N',
            exif_gps_longitude: '122.4194 W',
            camera_model: 'iPhone 15 Pro Max',
            file_data: 'MOCK_BINARY_WITH_METADATA_HEADER'
        };

        const uploadResponse = await request.post(EXIF_CHECK_ENDPOINT, {
            data: mockHEICFileWithEXIF,
            headers: {
                'Authorization': 'Bearer active_vip_patron_secret_jwt_token',
                'Content-Type': 'application/json'
            }
        });

        expect(uploadResponse.status()).toBe(201);
        const result = await uploadResponse.json();
        
        // Verify that the file was processed, stored securely, and stripped of metadata
        expect(result.exif_stripped).toBe(true);
        expect(result.metadata_removed).toContain('GPS');
        expect(result.metadata_removed).toContain('Software/Device');
        expect(result.sanitized_object_url).toBeDefined();
    });
});
