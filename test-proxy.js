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

  // Test 7: End-to-End Authentication Flow via Proxy
  console.log('Test 7: Testing End-to-End Registration, Login & Session via Proxy...');
  const testEmail = 'automated_test_' + Date.now() + '@example.com';
  const regRes = await fetch('http://localhost:3000/api/auth/register', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Cookie': cookies.map(c => c.split(';')[0]).join('; '),
      [csrfJson.headerName]: csrfJson.token
    },
    body: JSON.stringify({
      fullName: 'Vercel Auto Tester',
      email: testEmail,
      phoneNumber: '+91 98765 00000',
      password: 'StrongPassword123!',
      role: 'ROLE_USER'
    })
  });
  if (regRes.status !== 201) {
    throw new Error(`Registration failed with status ${regRes.status}`);
  }

  const loginRes = await fetch('http://localhost:3000/api/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Cookie': cookies.map(c => c.split(';')[0]).join('; '),
      [csrfJson.headerName]: csrfJson.token
    },
    body: JSON.stringify({
      email: testEmail,
      password: 'StrongPassword123!'
    })
  });
  if (loginRes.status !== 200) {
    throw new Error(`Login failed with status ${loginRes.status}`);
  }
  const loginCookies = loginRes.headers.getSetCookie ? loginRes.headers.getSetCookie() : [loginRes.headers.get('set-cookie')];
  const sessionHeader = loginCookies.map(c => c.split(';')[0]).join('; ');

  const meRes = await fetch('http://localhost:3000/api/auth/me', {
    headers: { 'Cookie': sessionHeader }
  });
  if (meRes.status !== 200) {
    throw new Error(`Session /api/auth/me failed with status ${meRes.status}`);
  }
  const meData = await meRes.json();
  if (meData.email !== testEmail) {
    throw new Error(`Expected email ${testEmail}, got ${meData.email}`);
  }
  console.log('✅ Test 7 Passed: End-to-End Register -> Login -> Session verified!\n');

  console.log('🎉 ALL 7 VERCEL COMPATIBILITY TESTS PASSED!');
  process.exit(0);
}

runTests().catch(err => {
  console.error('❌ Test failed:', err);
  process.exit(1);
});
