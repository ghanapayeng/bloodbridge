/**
 * BloodBridge Vercel Build Script
 * Automatically prepares and packages static assets from src/main/resources/static
 * into the public directory for Vercel edge deployment.
 */
const fs = require('fs');
const path = require('path');

const SOURCE_DIR = path.join(__dirname, 'src', 'main', 'resources', 'static');
const TARGET_DIR = path.join(__dirname, 'public');

console.log('🩸 [BloodBridge] Starting Vercel build preparation...');
console.log(`📂 Source: ${SOURCE_DIR}`);
console.log(`📂 Target: ${TARGET_DIR}`);

if (!fs.existsSync(SOURCE_DIR)) {
  console.error(`❌ Error: Source directory does not exist: ${SOURCE_DIR}`);
  process.exit(1);
}

// Ensure target directory exists
if (!fs.existsSync(TARGET_DIR)) {
  fs.mkdirSync(TARGET_DIR, { recursive: true });
}

function copyDirectoryRecursive(src, dest) {
  if (!fs.existsSync(dest)) {
    fs.mkdirSync(dest, { recursive: true });
  }

  const entries = fs.readdirSync(src, { withFileTypes: true });

  for (const entry of entries) {
    const srcPath = path.join(src, entry.name);
    const destPath = path.join(dest, entry.name);

    if (entry.isDirectory()) {
      copyDirectoryRecursive(srcPath, destPath);
    } else {
      fs.copyFileSync(srcPath, destPath);
    }
  }
}

try {
  copyDirectoryRecursive(SOURCE_DIR, TARGET_DIR);
  const copiedFiles = fs.readdirSync(TARGET_DIR);
  console.log(`✅ [BloodBridge] Successfully deployed ${copiedFiles.length} static assets to /public:`);
  console.log(`   - HTML Entrypoints: ${copiedFiles.filter(f => f.endsWith('.html')).join(', ')}`);
  console.log(`   - Core Stylesheets: ${copiedFiles.filter(f => f.endsWith('.css')).length} CSS files`);
  console.log(`   - Client Scripts:   ${copiedFiles.filter(f => f.endsWith('.js')).length} JavaScript files`);
  console.log('🚀 Build ready for Vercel deployment!');
} catch (err) {
  console.error('❌ Build failed during asset copy:', err);
  process.exit(1);
}
