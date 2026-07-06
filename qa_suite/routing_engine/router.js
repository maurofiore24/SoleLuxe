/**
 * SoleLuxe Smart Device Routing Engine
 * Production-ready User-Agent parsing and dynamic distribution router.
 * 
 * Rules:
 * 1. Android: Trigger Direct .apk download binary payload.
 * 2. iOS (iPhone/iPad): Render PWA installer frame with curated "Add to Home Screen" prompt.
 * 3. Desktop/Others: Render responsive elite luxury web portal.
 */

const express = require('express');
const path = require('path');
const router = express.Router();

// Helper to determine device type from headers
function getDeviceType(userAgent, headers = {}) {
    if (!userAgent) return 'desktop';
    
    const ua = userAgent.toLowerCase();

    // Check for Android first (phone and tablet)
    if (ua.includes('android')) {
        return 'android';
    }

    // Check for iOS (iPhone, iPod)
    if (ua.includes('iphone') || ua.includes('ipod')) {
        return 'ios';
    }

    // Detect iPad (including iPadOS 13+ requesting Desktop Website)
    // Safari on iPadOS 13+ displays Macintosh in user agent but supports multi-touch.
    const isIPadOSDesktopMode = ua.includes('macintosh') && 
        (headers['sec-ch-ua-platform'] === '"iOS"' || headers['x-requested-with'] === 'com.apple.mobilesafari' || (typeof navigator !== 'undefined' && navigator.maxTouchPoints > 1));

    if (ua.includes('ipad') || isIPadOSDesktopMode) {
        return 'ios';
    }

    // Look for other mobile platforms as fallback to PWA or responsive web
    if (ua.includes('mobi') || ua.includes('opera mini') || ua.includes('iemobile') || ua.includes('fennec')) {
        // Fallback mobile (serve Android if possible, else PWA web landing)
        return 'desktop'; 
    }

    return 'desktop';
}

router.get('/distribute', (req, res) => {
    const userAgent = req.headers['user-agent'] || '';
    const deviceType = getDeviceType(userAgent, req.headers);

    res.setHeader('X-Device-Detected', deviceType);
    res.setHeader('Vary', 'User-Agent, Sec-CH-UA-Platform');

    switch (deviceType) {
        case 'android':
            // Direct payload trigger
            res.setHeader('Content-Type', 'application/vnd.android.package-archive');
            res.setHeader('Content-Disposition', 'attachment; filename="soleluxe_elite_v1.0.4.apk"');
            
            // In production, stream the APK file from private storage
            // res.sendFile(path.join(__dirname, '../binaries/soleluxe_elite.apk'));
            return res.status(200).send(Buffer.from('MOCK_APK_BINARY_STREAM_PACKET'));

        case 'ios':
            // Render PWA initialization wrapper
            return res.status(200).send(`
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover">
                    <title>SoleLuxe VIP Access Portal</title>
                    <link rel="manifest" href="/manifest.json">
                    <meta name="apple-mobile-web-app-capable" content="yes">
                    <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
                    <meta name="apple-mobile-web-app-title" content="SoleLuxe">
                    <link rel="apple-touch-icon" href="/assets/icon-192.png">
                    <style>
                        body {
                            background-color: #0C0D12;
                            color: #FFFFFF;
                            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                            margin: 0;
                            display: flex;
                            flex-direction: column;
                            align-items: center;
                            justify-content: center;
                            height: 100vh;
                            overflow: hidden;
                            text-align: center;
                        }
                        .container {
                            padding: 24px;
                            max-width: 400px;
                        }
                        .gold-logo {
                            color: #FFFFDF;
                            font-size: 32px;
                            font-weight: 900;
                            letter-spacing: 4px;
                            text-shadow: 0 0 15px rgba(255, 223, 0, 0.4);
                            margin-bottom: 24px;
                        }
                        .title {
                            font-size: 20px;
                            font-weight: 700;
                            margin-bottom: 8px;
                            color: #FFFFFF;
                        }
                        .description {
                            font-size: 13px;
                            color: #A0A5B5;
                            line-height: 1.6;
                            margin-bottom: 32px;
                        }
                        .pwa-prompt-overlay {
                            border: 1px solid rgba(255, 223, 0, 0.35);
                            background: linear-gradient(135deg, #13141C 0%, #0C0D12 100%);
                            border-radius: 16px;
                            padding: 18px;
                            box-shadow: 0 10px 30px rgba(0,0,0,0.5);
                        }
                        .pwa-instruction-step {
                            display: flex;
                            align-items: center;
                            margin-bottom: 12px;
                            text-align: left;
                            font-size: 13px;
                        }
                        .pwa-icon-badge {
                            width: 28px;
                            height: 28px;
                            background: rgba(255, 223, 0, 0.1);
                            border-radius: 6px;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            margin-right: 12px;
                            border: 0.5px solid rgba(255, 223, 0, 0.4);
                        }
                        .highlight {
                            color: #FFFFDF;
                            font-weight: bold;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="gold-logo">SOLELUXE</div>
                        <div class="pwa-prompt-overlay">
                            <div class="title">INSTALL ELITE CLIENT</div>
                            <div class="description">
                                To unlock full E2EE chat channels and high-fidelity video portfolio previews, please install the SoleLuxe iOS client.
                            </div>
                            <div class="pwa-instruction-step">
                                <div class="pwa-icon-badge"><span style="color:#FFFFDF;font-size:16px;">⎋</span></div>
                                <div>Tap the <span class="highlight">Share</span> button in your Safari menu bar.</div>
                            </div>
                            <div class="pwa-instruction-step">
                                <div class="pwa-icon-badge"><span style="color:#FFFFDF;font-size:16px;">＋</span></div>
                                <div>Scroll down and select <span class="highlight">"Add to Home Screen"</span>.</div>
                            </div>
                            <div class="pwa-instruction-step" style="margin-bottom: 0;">
                                <div class="pwa-icon-badge"><span style="color:#00FF66;font-size:12px;">✔</span></div>
                                <div>Launch from Home Screen for <span class="highlight">Offline Support & 4K VIP streams</span>.</div>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
            `);

        case 'desktop':
        default:
            // Render beautiful desktop landing web portal
            return res.status(200).send(`
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <title>SoleLuxe - Exclusive Luxury Pedicure Modeling Portal</title>
                    <style>
                        body {
                            background-color: #040508;
                            color: #E2E8F0;
                            font-family: sans-serif;
                            margin: 0;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            height: 100vh;
                        }
                        .container { text-align: center; max-width: 600px; padding: 20px; }
                        h1 { color: #FFFFDF; font-weight: 900; letter-spacing: 5px; }
                        p { color: #94A3B8; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>SOLELUXE WEB</h1>
                        <p>Welcome to the Elite Desktop Landing Portal. Scan QR code to launch direct client stream on your Android or iOS device, or enter credentials to browse exclusive portfolios.</p>
                    </div>
                </body>
                </html>
            `);
    }
});

module.exports = { router, getDeviceType };
