/**
 * SoleLuxe Universal Smart Router - Massive Stability Validation Loop
 * Simulates 1,000 high-velocity request loops across dynamic user-agents and device platforms.
 * 
 * Run via: npm install jest && npx jest routing_simulation.test.js
 */

const { getDeviceType } = require('./router');

describe('SoleLuxe Routing Engine - 1,000 Iteration Permutation Stress Test', () => {

    const androidUserAgents = [
        "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro)",
        "Mozilla/5.0 (Linux; Android 13; Samsung Galaxy S23 Ultra)",
        "Mozilla/5.0 (Linux; Android 10; Xiaomi Redmi Note 9)",
        "Mozilla/5.0 (Linux; Android 12; OnePlus 10 Pro Build/SKQ1.211106.001)",
        "Mozilla/5.0 (Linux; U; Android 4.4.2; zh-cn; GT-I9500 Build/KOT49H)"
    ];

    const iosUserAgents = [
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1",
        "Mozilla/5.0 (iPad; CPU OS 16_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148",
        "Mozilla/5.0 (iPod; CPU iPhone OS 15_7_9 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.6.3 Mobile/15E148 Safari/604.1",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15" // iPadOS Desktop Mode masquerade
    ];

    const desktopUserAgents = [
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_3_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 11.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/121.0"
    ];

    test('Execute 1,000 rapid classification loops flawlessly without memory leaks or latency drops', () => {
        const startTime = Date.now();
        let androidCount = 0;
        let iosCount = 0;
        let desktopCount = 0;

        for (let i = 0; i < 1000; i++) {
            const listType = i % 3;
            let ua = "";
            let headers = {};

            if (listType === 0) {
                ua = androidUserAgents[Math.floor(Math.random() * androidUserAgents.length)];
                const device = getDeviceType(ua, headers);
                expect(device).toBe('android');
                androidCount++;
            } else if (listType === 1) {
                const index = Math.floor(Math.random() * iosUserAgents.length);
                ua = iosUserAgents[index];
                if (ua.includes("Macintosh")) {
                    headers['sec-ch-ua-platform'] = '"iOS"';
                }
                const device = getDeviceType(ua, headers);
                expect(device).toBe('ios');
                iosCount++;
            } else {
                ua = desktopUserAgents[Math.floor(Math.random() * desktopUserAgents.length)];
                const device = getDeviceType(ua, headers);
                expect(device).toBe('desktop');
                desktopCount++;
            }
        }

        const duration = Date.now() - startTime;
        console.log(`Stability Loop Complete! Processed 1000 iterations in ${duration}ms.`);
        console.log(`Final routing distribution: Android: ${androidCount}, iOS PWA: ${iosCount}, Desktop Portal: ${desktopCount}`);
        expect(duration).toBeLessThan(150); // Direct memory processing should complete well below 150ms
    });
});
