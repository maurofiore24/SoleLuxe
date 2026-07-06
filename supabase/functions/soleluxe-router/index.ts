import { serve } from "https://deno.land/std@0.168.0/http/server.ts"

const APK_URL = "https://github.com/maurofiore24/SoleLuxe/raw/master/public/soleluxe-release.apk";

function isAndroid(userAgent: string | null) {
  if (!userAgent) return false;
  return /Android/i.test(userAgent);
}

function isIOS(userAgent: string | null) {
  if (!userAgent) return false;
  return /iPhone|iPad|iPod/i.test(userAgent);
}

serve(async (req: Request) => {
  try {
    const ua = req.headers.get("user-agent") || "";

    if (isAndroid(ua)) {
      return Response.redirect(APK_URL, 302);
    }

    if (isIOS(ua)) {
      const html = `<!doctype html>
<html>
<head>
  <meta charset="utf-8">
  <title>SoleLuxe — iOS PWA Setup</title>
  <style>
    body{font-family:system-ui,-apple-system,sans-serif;display:flex;align-items:center;justify-content:center;height:100vh;background:#0f1017;color:#fff;margin:0}
    .card{max-width:720px;padding:22px;border-radius:12px;background:linear-gradient(180deg,#111118, #0b0b0f);box-shadow:0 6px 30px rgba(0,0,0,0.6)}
    h1{margin-top:0}
    a.button{display:inline-block;margin-top:12px;padding:10px 14px;background:#ec4899;color:#fff;border-radius:8px;text-decoration:none}
  </style>
</head>
<body>
  <div class="card">
    <h1>Install SoleLuxe on iOS</h1>
    <p>To install the PWA on iPhone or iPad: tap the Share button in Safari and select "Add to Home Screen".</p>
    <a href="${APK_URL}" class="button">Download APK (Android)</a>
  </div>
</body>
</html>`;
      return new Response(html, {
        headers: { "content-type": "text/html; charset=utf-8" },
      });
    }

    const desktopHtml = `<!doctype html>
<html>
<head>
  <meta charset="utf-8">
  <title>SoleLuxe — Download</title>
  <style>
    body{font-family:system-ui,-apple-system,sans-serif;margin:0;background:#05050a;color:#fff}
    .container{max-width:1100px;margin:40px auto;padding:24px}
    .grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(240px,1fr));gap:16px}
    .card{background:linear-gradient(180deg,#0f1017,#08080b);padding:18px;border-radius:12px}
    a.button{display:inline-block;padding:10px 14px;background:#ec4899;color:#fff;border-radius:8px;text-decoration:none}
  </style>
</head>
<body>
  <div class="container">
    <h1>SoleLuxe — Download & Install</h1>
    <div class="grid">
      <div class="card">
        <h3>Android (recommended)</h3>
        <p>Tap to download the signed APK and install on your Android device.</p>
        <a href="${APK_URL}" class="button">Download APK</a>
      </div>
      <div class="card">
        <h3>iOS</h3>
        <p>Open this page in Safari on iPhone or iPad and use Add to Home Screen.</p>
      </div>
    </div>
  </div>
</body>
</html>`;

    return new Response(desktopHtml, { headers: { "content-type": "text/html; charset=utf-8" } });
  } catch (e) {
    return new Response("Internal Server Error", { status: 500 });
  }
});
