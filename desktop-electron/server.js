// SYNC ENGINE - Headless / Standalone Node.js Server
// Barrantes Co. - Port 8123
const http = require('http');
const WebSocket = require('ws');
const fs = require('fs');
const path = require('path');
const os = require('os');
const { exec } = require('child_process');
const { startDiscovery } = require('./discovery');

const PORT = 8123;
const LIVE_TYPES = new Set(['IOT_TELEMETRY', 'HARDWARE_TELEMETRY', 'PING', 'PONG', 'CONNECTION_STATE', 'COMMAND_ACK']);
const clients = new Set();
let telemetryInterval = null;

function getPrimaryLocalIp() {
  const interfaces = os.networkInterfaces();
  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name]) {
      if (iface.family === 'IPv4' && !iface.internal) {
        return iface.address;
      }
    }
  }
  return '127.0.0.1';
}

function connectedCount() {
  let n = 0;
  for (const ws of clients) {
    if (ws.readyState === WebSocket.OPEN) n++;
  }
  return n;
}

function broadcast(obj, except = null) {
  const raw = typeof obj === 'string' ? obj : JSON.stringify(obj);
  for (const ws of clients) {
    if (ws !== except && ws.readyState === WebSocket.OPEN) {
      try { ws.send(raw); } catch (_e) {}
    }
  }
}

function executeCommand(type, payload) {
  console.log(`[Sync Engine Command]: ${type}`, payload || '');
  switch (type) {
    case 'LOCK_SCREEN':
      exec('powershell -c "rundll32.exe user32.dll,LockWorkStation"');
      break;

    case 'VOLUME_MUTE':
      exec('powershell -c "$w = New-Object -ComObject wscript.shell; $w.SendKeys([char]173)"');
      break;

    case 'VOLUME_UP':
      exec('powershell -c "$w = New-Object -ComObject wscript.shell; $w.SendKeys([char]175)"');
      break;

    case 'VOLUME_DOWN':
      exec('powershell -c "$w = New-Object -ComObject wscript.shell; $w.SendKeys([char]174)"');
      break;

    case 'MEDIA_PLAY_PAUSE':
      exec('powershell -c "$w = New-Object -ComObject wscript.shell; $w.SendKeys([char]179)"');
      break;

    case 'PRESENTATION_NEXT':
      exec('powershell -c "$w = New-Object -ComObject wscript.shell; $w.SendKeys(\'{RIGHT}\')"');
      break;

    case 'PRESENTATION_PREV':
      exec('powershell -c "$w = New-Object -ComObject wscript.shell; $w.SendKeys(\'{LEFT}\')"');
      break;

    case 'SAVE_NOTE': {
      try {
        const notesDir = path.join(os.homedir(), 'Desktop');
        const notesFile = path.join(notesDir, 'Notas_SyncEngine.txt');
        const noteContent = (payload && (payload.text || payload.content)) || (typeof payload === 'string' ? payload : '');
        const timeStr = new Date().toLocaleString('es-ES', { dateStyle: 'short', timeStyle: 'medium' });
        const entry = `\n========================================\n[${timeStr}] NOTA DESDE CELULAR:\n${noteContent}\n========================================\n`;
        fs.appendFileSync(notesFile, entry, 'utf8');
        console.log(`[Sync Engine] Nota guardada en: ${notesFile}`);
      } catch (err) {
        console.error('[Sync Engine] Error guardando nota:', err);
      }
      break;
    }

    case 'OPEN_DOWNLOADS': {
      const dl = path.join(os.homedir(), 'Downloads');
      exec(`explorer.exe "${dl}"`);
      break;
    }

    case 'OPEN_NOTEPAD': {
      const notesFile = path.join(os.homedir(), 'Desktop', 'Notas_SyncEngine.txt');
      if (!fs.existsSync(notesFile)) {
        fs.writeFileSync(notesFile, '=== NOTAS SYNC ENGINE BY BARRANTES CO. ===\n', 'utf8');
      }
      exec(`notepad.exe "${notesFile}"`);
      break;
    }

    case 'OPEN_EXPLORER': {
      const target = (payload && payload.path) ? payload.path : os.homedir();
      exec(`explorer.exe "${target}"`);
      break;
    }
  }
}

function startServer() {
  const primaryIp = getPrimaryLocalIp();

  const server = http.createServer((req, res) => {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

    if (req.method === 'OPTIONS') {
      res.writeHead(204);
      res.end();
      return;
    }

    if (req.url === '/ping' || req.url === '/status') {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ status: 'CONNECTED', hostName: os.hostname(), ip: primaryIp, clients: connectedCount() }));
      return;
    }

    if (req.method === 'POST' && (req.url === '/api/command' || req.url === '/command')) {
      let body = '';
      req.on('data', chunk => { body += chunk.toString(); });
      req.on('end', () => {
        try {
          const msg = JSON.parse(body);
          if (!LIVE_TYPES.has(msg.type)) {
            executeCommand(msg.type, msg.payload);
          }
          res.writeHead(200, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ result: 'SUCCESS', action: msg.type }));
        } catch (e) {
          res.writeHead(400, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: e.message }));
        }
      });
      return;
    }

    if (req.method === 'GET' && req.url.startsWith('/api/fs/list')) {
      try {
        const u = new URL(req.url, `http://${req.headers.host}`);
        let reqPath = u.searchParams.get('path');
        let targetDir = (!reqPath || reqPath === '/' || reqPath === '') ? os.homedir() : path.resolve(reqPath);
        if (!fs.existsSync(targetDir)) {
          res.writeHead(404, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: 'Directorio no encontrado' }));
          return;
        }
        const entries = fs.readdirSync(targetDir, { withFileTypes: true });
        const items = [];
        for (const e of entries) {
          try {
            const fullPath = path.join(targetDir, e.name);
            let size = 0;
            let mtime = Date.now();
            try {
              const st = fs.statSync(fullPath);
              size = st.size;
              mtime = st.mtimeMs;
            } catch (_e) {}
            items.push({
              name: e.name,
              path: fullPath,
              isDirectory: e.isDirectory(),
              sizeBytes: size,
              modifiedAt: mtime
            });
          } catch (_e) {}
        }
        items.sort((a, b) => {
          if (a.isDirectory && !b.isDirectory) return -1;
          if (!a.isDirectory && b.isDirectory) return 1;
          return a.name.localeCompare(b.name);
        });
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          currentPath: targetDir,
          parentPath: path.dirname(targetDir),
          homeDir: os.homedir(),
          items: items.slice(0, 300)
        }));
      } catch (e) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: e.message }));
      }
      return;
    }

    if (req.method === 'GET' && req.url.startsWith('/api/fs/download')) {
      try {
        const u = new URL(req.url, `http://${req.headers.host}`);
        const filePath = u.searchParams.get('path');
        if (!filePath || !fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
          res.writeHead(404, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: 'Archivo no encontrado' }));
          return;
        }
        const stat = fs.statSync(filePath);
        res.writeHead(200, {
          'Content-Type': 'application/octet-stream',
          'Content-Length': stat.size,
          'Content-Disposition': `attachment; filename="${encodeURIComponent(path.basename(filePath))}"`
        });
        fs.createReadStream(filePath).pipe(res);
      } catch (e) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: e.message }));
      }
      return;
    }

    if (req.method === 'POST' && req.url.startsWith('/api/fs/upload')) {
      try {
        const u = new URL(req.url, `http://${req.headers.host}`);
        const fileName = u.searchParams.get('name') || `archivo_${Date.now()}.bin`;
        const targetFolder = u.searchParams.get('folder') || path.join(os.homedir(), 'Downloads');
        if (!fs.existsSync(targetFolder)) {
          fs.mkdirSync(targetFolder, { recursive: true });
        }
        const destPath = path.join(targetFolder, path.basename(fileName));
        const fileStream = fs.createWriteStream(destPath);
        req.pipe(fileStream);
        fileStream.on('finish', () => {
          res.writeHead(200, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ status: 'SUCCESS', savedPath: destPath }));
          console.log(`[Sync Engine] Archivo subido desde celular: ${destPath}`);
        });
      } catch (e) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: e.message }));
      }
      return;
    }

    res.writeHead(404);
    res.end();
  });

  const wss = new WebSocket.Server({ server });

  wss.on('connection', (ws, req) => {
    clients.add(ws);
    const ip = (req.socket.remoteAddress || '').replace('::ffff:', '');
    console.log(`[WebSocket] Dispositivo conectado desde ${ip}. Clientes activos: ${connectedCount()}`);

    ws.on('message', data => {
      try {
        const text = data.toString();
        const msg = JSON.parse(text);
        if (msg.type === 'PING') {
          ws.send(JSON.stringify({ type: 'PONG', timestamp: Date.now() }));
          return;
        }
        executeCommand(msg.type, msg.payload);
      } catch (_e) {}
    });

    ws.on('close', () => {
      clients.delete(ws);
      console.log(`[WebSocket] Cliente desconectado. Clientes activos: ${connectedCount()}`);
    });
  });

  server.listen(PORT, '0.0.0.0', () => {
    console.log('================================================================');
    console.log('       SYNC ENGINE by Barrantes Co. - Servidor Local Activo     ');
    console.log('================================================================');
    console.log(`  IP Local Laptop:   ${primaryIp}`);
    console.log(`  Puerto:            ${PORT}`);
    console.log(`  WebSocket URL:     ws://${primaryIp}:${PORT}`);
    console.log(`  API REST Disco:    http://${primaryIp}:${PORT}/api/fs/list`);
    console.log('================================================================');
    console.log('  Listo para enlazar con tu Xiaomi. Mantén esta ventana abierta.');
    console.log('================================================================');

    try {
      startDiscovery(PORT);
    } catch (_e) {}
  });
}

startServer();
