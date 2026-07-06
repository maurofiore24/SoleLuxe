import { serve } from "https://deno.land/std@0.168.0/http/server.ts";

const GITHUB_RELEASE_APK_URL = "https://github.com/maurofiore24/SoleLuxe/raw/master/public/soleluxe-release.apk";
const FALLBACK_DESKTOP_URL = "https://soleluxe.premium";

const iOS_PWA_HTML = `<!DOCTYPE html><html lang="en"><head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover">
    <title>SoleLuxe VIP Access Portal</title>
    <link rel="manifest" href="/manifest.json">
    <meta name="apple-mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
    <meta name="apple-mobile-web-app-title" content="SoleLuxe">
    <link rel="apple-touch-icon" href="/assets/icon-192.png">
    <style>
        body { background-color: #0C0D12; color: #FFFFFF; font-family: -apple-system, BlinkMacSystemFont, sans-serif; margin: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100vh; text-align: center; }
        .container { padding: 24px; max-width: 400px; }
        .gold-logo { color: #FFFFDF; font-size: 32px; font-weight: 900; letter-spacing: 4px; text-shadow: 0 0 15px rgba(255, 223, 0, 0.4); margin-bottom: 24px; }
        .title { font-size: 20px; font-weight: 700; margin-bottom: 8px; }
        .description { font-size: 13px; color: #A0A5B5; line-height: 1.6; margin-bottom: 32px; }
        .pwa-prompt-overlay { border: 1px solid rgba(255, 223, 0, 0.35); background: linear-gradient(135deg, #13141C 0%, #0C0D12 100%); border-radius: 16px; padding: 18px; box-shadow: 0 10px 30px rgba(0,0,0,0.5); }
        .pwa-instruction-step { display: flex; align-items: center; margin-bottom: 12px; text-align: left; font-size: 13px; }
        .pwa-icon-badge { width: 28px; height: 28px; background: rgba(255, 223, 0, 0.1); border-radius: 6px; display: flex; align-items: center; justify-content: center; margin-right: 12px; border: 0.5px solid rgba(255, 223, 0, 0.4); }
        .highlight { color: #FFFFDF; font-weight: bold; }
    </style></head><body>
    <div class="container">
        <div class="gold-logo">SOLELUXE</div>
        <div class="pwa-prompt-overlay">
            <div class="title">INSTALL ELITE CLIENT</div>
            <div class="description">To unlock full E2EE chat channels and high-fidelity video portfolio previews, please install the SoleLuxe iOS client.</div>
            <div class="pwa-instruction-step">
                <div class="pwa-icon-badge"><span style="color:#FFFFDF;font-size:16px;">⎋</span></div>
                <div>Tap the <span class="highlight">Share</span> button in your Safari menu bar.</div>
            </div>
            <div class="pwa-instruction-step">
                <div class="pwa-icon-badge"><span style="color:#FFFFDF;font-size:16px;">＋</span></div>
                <div>Scroll down and select <span class="highlight">"Add to Home Screen"</span>.</div>
            </div>
        </div>
    </div></body></html>
`;

const DESKTOP_LANDING_HTML = `<!DOCTYPE html><html lang="en"><head>
    <meta charset="UTF-8">
    <title>SoleLuxe - Premium High-Fashion Exclusive Portal</title>
    <style>
        body { background-color: #040508; color: #E2E8F0; font-family: -apple-system, sans-serif; margin: 0; display: flex; justify-content: center; align-items: center; height: 100vh; }
        .container { text-align: center; max-width: 650px; padding: 40px; border: 1px solid rgba(255, 223, 0, 0.15); background: linear-gradient(135deg, #090A0F 0%, #040508 100%); border-radius: 24px; }
        h1 { color: #FFFFDF; font-weight: 900; letter-spacing: 6px; font-size: 36px; text-shadow: 0 0 20px rgba(255, 223, 0, 0.35); }
        p { color: #94A3B8; font-size: 16px; line-height: 1.7; margin-bottom: 30px; }
        .cta-btn { background: linear-gradient(90deg, #FFFFDF 0%, #D4AF37 100%); color: #040508; padding: 14px 32px; border: none; border-radius: 30px; font-weight: 700; text-decoration: none; display: inline-block; box-shadow: 0 5px 15px rgba(212, 175, 55, 0.4); }
    </style></head><body>
    <div class="container">
        <h1>SOLELUXE ELITE</h1>
        <p>Welcome to the premium footwear, exclusive pedicure modeling, and luxury lifestyle portfolio ecosystem.</p>
        <a href="${GITHUB_RELEASE_APK_URL}" class="cta-btn">DOWNLOAD APK CLIENT</a>
    </div></body></html>
`;

serve(async (req) => {
    const userAgent = req.headers.get("user-agent") || "";
    const secChUaPlatform = req.headers.get("sec-ch-ua-platform") || "";
    
    const ua = userAgent.toLowerCase();
    let deviceType = "desktop";

    if (ua.includes("android")) {
        deviceType = "android";
    } else if (ua.includes("iphone") || ua.includes("ipod")) {
        deviceType = "ios";
    } else if (
        ua.includes("ipad") ||
        (ua.includes("macintosh") && (secChUaPlatform === '"iOS"' || req.headers.get("x-requested-with") === "com.apple.mobilesafari"))
    ) {
        deviceType = "ios";
    }

    const responseHeaders = new Headers();
    responseHeaders.set("Cache-Control", "no-store, no-cache, must-revalidate, proxy-revalidate, max-age=0");
    responseHeaders.set("Pragma", "no-cache");
    responseHeaders.set("Expires", "0");
    responseHeaders.set("X-Device-Routed", deviceType);

    switch (deviceType) {
        case "android":
            responseHeaders.set("Location", GITHUB_RELEASE_APK_URL);
            return new Response(null, { status: 302, headers: responseHeaders });
        case "ios":
            responseHeaders.set("Content-Type", "text/html; charset=utf-8");
            return new Response(iOS_PWA_HTML, { status: 200, headers: responseHeaders });
        case "desktop":
        default:
            responseHeaders.set("Content-Type", "text/html; charset=utf-8");
            return new Response(DESKTOP_LANDING_HTML, { status: 200, headers: responseHeaders });
    }
});
