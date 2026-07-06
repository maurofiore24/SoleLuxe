/**
 * SoleLuxe Routing Engine - Comprehensive Unit Test Suite
 * Run via: npm install jest supertest && npx jest router.test.js
 */

const request = require('supertest');
const express = require('express');
const { router, getDeviceType } = require('./router');

const app = express();
app.use(router);

describe('SoleLuxe Dynamic Distribution Engine Tests', () => {

    describe('Direct Unit Tests: getDeviceType() User-Agent Parser', () => {
        
        test('Standard Android User-Agent identification', () => {
            const ua = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";
            expect(getDeviceType(ua)).toBe('android');
        });

        test('Standard iOS iPhone User-Agent identification', () => {
            const ua = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1";
            expect(getDeviceType(ua)).toBe('ios');
        });

        test('Standard iPad User-Agent identification', () => {
            const ua = "Mozilla/5.0 (iPad; CPU OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1";
            expect(getDeviceType(ua)).toBe('ios');
        });

        test('iPadOS 13+ requesting Desktop Website (Spoofed as Mac but with iOS custom Sec header)', () => {
            const ua = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15";
            const headers = { 'sec-ch-ua-platform': '"iOS"' };
            expect(getDeviceType(ua, headers)).toBe('ios');
        });

        test('Standard macOS Desktop User-Agent identification', () => {
            const ua = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
            expect(getDeviceType(ua)).toBe('desktop');
        });

        test('Standard Windows 11 Desktop User-Agent identification', () => {
            const ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
            expect(getDeviceType(ua)).toBe('desktop');
        });

        test('Hybrid Samsung Dex desktop User-Agent string with embedded Android indicators', () => {
            // Samsung DeX desktop-mode user agent strings usually lack 'Mobile', but contain 'Android'
            const ua = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/22.0 Chrome/111.0.0.0 Safari/537.36 (DexMode)";
            // Since it contains DexMode/Linux, but also contains Android indicator if customized, let's check basic Android handling:
            const uaWithAndroid = "Mozilla/5.0 (X11; Linux x86_64; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36 (DexMode)";
            expect(getDeviceType(uaWithAndroid)).toBe('android');
        });
    });

    describe('End-to-End Route Validation via HTTP Requests', () => {

        test('Android User-Agent gets direct .apk package download', async () => {
            const androidUA = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36";
            
            const response = await request(app)
                .get('/distribute')
                .set('User-Agent', androidUA);

            expect(response.status).toBe(200);
            expect(response.headers['x-device-detected']).toBe('android');
            expect(response.headers['content-type']).toBe('application/vnd.android.package-archive');
            expect(response.headers['content-disposition']).toContain('attachment; filename="soleluxe_elite_v1.0.4.apk"');
            expect(response.text).toBe('MOCK_APK_BINARY_STREAM_PACKET');
        });

        test('iPhone gets PWA manual onboarding flow', async () => {
            const iosUA = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)";

            const response = await request(app)
                .get('/distribute')
                .set('User-Agent', iosUA);

            expect(response.status).toBe(200);
            expect(response.headers['x-device-detected']).toBe('ios');
            expect(response.text).toContain('apple-mobile-web-app-capable');
            expect(response.text).toContain('SOLELUXE VIP Access Portal');
            expect(response.text).toContain('Add to Home Screen');
        });

        test('Desktop Windows PC gets standard responsive luxury web landing', async () => {
            const desktopUA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

            const response = await request(app)
                .get('/distribute')
                .set('User-Agent', desktopUA);

            expect(response.status).toBe(200);
            expect(response.headers['x-device-detected']).toBe('desktop');
            expect(response.text).toContain('SOLELUXE WEB');
            expect(response.text).toContain('Welcome to the Elite Desktop Landing Portal');
        });

        test('Empty User-Agent falls back to desktop layout securely', async () => {
            const response = await request(app)
                .get('/distribute')
                .set('User-Agent', '');

            expect(response.status).toBe(200);
            expect(response.headers['x-device-detected']).toBe('desktop');
            expect(response.text).toContain('SOLELUXE WEB');
        });
    });
});
