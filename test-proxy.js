/**
 * Automated Verification Test for Vercel Setup & Proxy
 */
const { spawn } = require('child_process');
const http = require('http');

async function runTests() {
  console.log('🧪 [BloodBridge Test] Starting Vercel compatibility tests...\n');

  // Test 1: Build verification
  console.log('Test 1: Running asset build...');
  require('./build.js');
  console.log('✅ Test 1 Passed: Assets built into /public\n');

  // Test 2: Proxy handler load
  console.log('Test 2: Loading serverless proxy handlers...');
  const proxyHandler = require('./api/[...path].js');
  const healthHandler = require('./api/health.js');
  if (typeof proxyHandler !== 'function' || typeof healthHandler !== 'function') {
    throw new Error('Handlers are not exported functions');
  }
  console.log('✅ Test 2 Passed: Serverless handlers loaded\n');

  // Test 3: Start dev server in background and query it
  console.log('Test 3: Testing live serverless proxy with local server...');
  const devServer = require('./dev-server.js');
  
  // Wait 1 second for server to bind
  await new Promise(r => setTimeout(r, 1000));

  // Query health
  const healthRes = await fetch('http://localhost:3000/api/health');
  const healthJson = await healthRes.json();
  console.log('Health Check Response:', healthJson);
  if (healthRes.status !== 200) {
    throw new Error(`Health check returned status ${healthRes.status}`);
  }
  console.log('✅ Test 3 Passed: /api/health returned 200 OK\n');

  // Query CSRF via proxy
  console.log('Test 4: Testing /api/auth/csrf via local proxy...');
  const csrfRes = await fetch('http://localhost:3000/api/auth/csrf');
  const csrfJson = await csrfRes.json();
  const cookies = csrfRes.headers.getSetCookie ? csrfRes.headers.getSetCookie() : [csrfRes.headers.get('set-cookie')];
  console.log('CSRF Status:', csrfRes.status);
  console.log('CSRF Token Header:', csrfJson.headerName);
  console.log('Cookies Rewritten:', cookies);
  if (!csrfJson.token) {
    throw new Error('No CSRF token returned through proxy');
  }
  console.log('✅ Test 4 Passed: CSRF token successfully proxied and cookies rewritten\n');

  // Test 5: CORS Preflight OPTIONS
  console.log('Test 5: Testing CORS preflight OPTIONS...');
  const optionsRes = await fetch('http://localhost:3000/api/auth/csrf', {
    method: 'OPTIONS',
    headers: {
      'Origin': 'https://bloodbridge.vercel.app',
      'Access-Control-Request-Method': 'POST'
    }
  });
  console.log('OPTIONS Status:', optionsRes.status);
  console.log('Access-Control-Allow-Origin:', optionsRes.headers.get('access-control-allow-origin'));
  if (optionsRes.status !== 200) {
    throw new Error(`OPTIONS returned status ${optionsRes.status}`);
  }
  console.log('✅ Test 5 Passed: CORS preflight handled cleanly\n');

  // Test 6: Static file serving & clean URLs
  console.log('Test 6: Testing static file serving & Clean URLs...');
  const indexRes = await fetch('http://localhost:3000/');
  const indexHtml = await indexRes.text();
  if (!indexHtml.includes('BloodBridge')) {
    throw new Error('index.html did not contain expected markup');
  }
  const dashboardRes = await fetch('http://localhost:3000/dashboard');
  const dashboardHtml = await dashboardRes.text();
  if (!dashboardHtml.includes('BloodBridge')) {
    throw new Error('Clean URL /dashboard failed');
  }
  console.log('✅ Test 6 Passed: Static files and Clean URLs working\n');

  // Test 7: End-to-End Authentication Flow & Smart Matching via Proxy
  console.log('Test 7: Testing End-to-End Registration (no phone), Login, Session & Smart Match via Proxy...');
  const cookieJar = new Map();
  function recordCookies(response) {
    const list = response.headers.getSetCookie ? response.headers.getSetCookie() : [response.headers.get('set-cookie')].filter(Boolean);
    for (const c of list) {
      if (!c) continue;
      const [nameVal] = c.split(';');
      const [name, ...valParts] = nameVal.split('=');
      if (name) cookieJar.set(name.trim(), valParts.join('='));
    }
  }
  function cookieString() {
    return Array.from(cookieJar.entries()).map(([k, v]) => `${k}=${v}`).join('; ');
  }

  // Seed cookieJar from CSRF request
  recordCookies(csrfRes);

  const testEmail = 'automated_test_' + Date.now() + '@example.com';
  // Signup WITHOUT phone number (Priority 3 verification)
  const regRes = await fetch('http://localhost:3000/api/auth/register', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Cookie': cookieString(),
      [csrfJson.headerName]: csrfJson.token
    },
    body: JSON.stringify({
      fullName: 'Vercel Auto Tester',
      email: testEmail,
      password: 'StrongPassword123!',
      role: 'ROLE_USER'
    })
  });
  recordCookies(regRes);
  if (regRes.status !== 201) {
    throw new Error(`Registration failed with status ${regRes.status}: ${await regRes.text()}`);
  }

  const loginRes = await fetch('http://localhost:3000/api/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Cookie': cookieString(),
      [csrfJson.headerName]: csrfJson.token
    },
    body: JSON.stringify({
      email: testEmail,
      password: 'StrongPassword123!'
    })
  });
  recordCookies(loginRes);
  if (loginRes.status !== 200) {
    throw new Error(`Login failed with status ${loginRes.status}: ${await loginRes.text()}`);
  }

  const meRes = await fetch('http://localhost:3000/api/auth/me', {
    headers: { 'Cookie': cookieString() }
  });
  if (meRes.status !== 200) {
    throw new Error(`Session /api/auth/me failed with status ${meRes.status}: ${await meRes.text()}`);
  }
  const meData = await meRes.json();
  if (meData.email !== testEmail) {
    throw new Error(`Expected email ${testEmail}, got ${meData.email}`);
  }
  console.log(`   Session verified for user: ${meData.email} (id: ${meData.id})`);

  // Priority 2: Verify Smart Donor Matching is authorized and returns 200
  const matchRes = await fetch('http://localhost:3000/api/donors/smart-match?bloodGroup=O%2B&city=Hyderabad', {
    headers: { 'Cookie': cookieString() }
  });
  if (matchRes.status !== 200) {
    throw new Error(`Smart donor matching failed with status ${matchRes.status}: ${await matchRes.text()}`);
  }
  const matches = await matchRes.json();
  console.log(`   Smart Donor Matching returned status 200 OK (${matches.length} matches found)`);

  console.log('✅ Test 7 Passed: End-to-End Register -> Login -> Session -> Smart Match verified!\n');

  console.log('🎉 ALL 7 VERCEL COMPATIBILITY TESTS PASSED!');
  process.exit(0);
}

runTests().catch(err => {
  console.error('❌ Test failed:', err);
  process.exit(1);
});
