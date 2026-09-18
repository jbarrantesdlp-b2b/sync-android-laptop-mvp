const { app, BrowserWindow, Tray, Menu, ipcMain, nativeImage, shell } = require('electron');
const path = require('path');
const fs = require('fs');
const http = require('http');
const WebSocket = require('ws');
const { exec } = require('child_process');
const os = require('os');
const qrcode = require('qrcode-terminal');
const { startDiscovery } = require('./discovery');

process.on('uncaughtException', (err) => {
  console.error('[Sync Engine Uncaught]:', err);
});
process.on('unhandledRejection', (reason, promise) => {
  console.error('[Sync Engine Unhandled Rejection]:', reason);
});

const PORT = 8123;
const LIVE_TYPES = new Set(['IOT_TELEMETRY', 'HARDWARE_TELEMETRY', 'PING', 'PONG', 'CONNECTION_STATE', 'COMMAND_ACK']);
let mainWindow = null;
let tray = null;
let server = null;
let wss = null;
const clients = new Set();
let telemetryInterval = null;
let stopDiscovery = null;

function getAllLocalIps() {
  const ips = [];
  const interfaces = os.networkInterfaces();
  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name]) {
      if (iface.family === 'IPv4' && !iface.internal) {
        ips.push({ name, address: iface.address });
      }
    }
  }
  return ips;
}

function getPrimaryLocalIp() {
  const ips = getAllLocalIps();
  const wifi = ips.find(i => i.name.toLowerCase().includes('wi-fi') || i.name.toLowerCase().includes('wireless') || i.name.toLowerCase().includes('wlan'));
  if (wifi) return wifi.address;
  return ips.length > 0 ? ips[0].address : '127.0.0.1';
}

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1240,
    height: 860,
    minWidth: 1080,
    minHeight: 740,
    resizable: true,
    autoHideMenuBar: true,
    title: 'SYNC ENGINE — By Barrantes Co.',
    backgroundColor: '#F3F5F9',
    icon: path.join(__dirname, 'icon.png'),
    webPreferences: {
      nodeIntegration: true,
      contextIsolation: false
    }
  });

  mainWindow.loadFile(path.join(__dirname, 'index.html'));

  mainWindow.on('close', (event) => {
    if (!app.isQuitting) {
      event.preventDefault();
      mainWindow.hide();
      if (tray) {
        try {
          tray.displayBalloon({
            title: 'Sync Engine',
            content: 'Sigue en segundo plano en la bandeja. Controlar. Conectar. Avanzar.'
          });
        } catch (_e) {}
      }
    }
    return false;
  });

  mainWindow.webContents.on('did-finish-load', () => {
    const localIp = getPrimaryLocalIp();
    const wsUrl = `ws://${localIp}:${PORT}`;
    mainWindow.webContents.send('server-info', { url: wsUrl, ip: localIp, port: PORT });
  });
}

function createTray() {
  try {
    const iconPath = path.join(__dirname, 'icon.png');
    const icon = nativeImage.createFromPath(iconPath).resize({ width: 24, height: 24 });

    tray = new Tray(icon);
    tray.setToolTip('Sync Engine \u2014 By Barrantes Co.');

    const contextMenu = Menu.buildFromTemplate([
      {
        label: 'Abrir Sync Engine',
        click: () => {
          if (mainWindow) {
            mainWindow.show();
            mainWindow.focus();
          }
        }
      },
      { type: 'separator' },
      {
        label: 'Salir',
        click: () => {
          app.isQuitting = true;
          app.quit();
        }
      }
    ]);

    tray.setContextMenu(contextMenu);
    tray.on('double-click', () => {
      if (mainWindow) {
        mainWindow.show();
        mainWindow.focus();
      }
    });
  } catch (err) {
    console.warn('Could not create tray icon:', err);
  }
}

function executeCommand(type, payload) {
  console.log(`[Sync Engine Command]: ${type}`, payload);
  switch (type) {
    case 'LOCK_SCREEN':
      exec('powershell -c "rundll32.exe user32.dll,LockWorkStation"');
      break;

    case 'OPEN_URL':
      if (payload && payload.url) {
        shell.openExternal(payload.url);
      }
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

    case 'SHUTDOWN':
      exec('shutdown /s /t 60');
      break;

    case 'REBOOT':
      exec('shutdown /r /t 60');
      break;

    case 'ABORT_SHUTDOWN':
      exec('shutdown /a');
      break;

    case 'SYNC_CLIPBOARD':
      if (payload && payload.text) {
        const text = payload.text;
        const clean = text.replace(/"/g, '`"');
        exec(`powershell -c "Set-Clipboard -Value \\"${clean}\\""`);
        if (mainWindow) {
          mainWindow.webContents.send('clipboard-received', { text });
        }
        if (text.startsWith('http://') || text.startsWith('https://')) {
          shell.openExternal(text);
        }
      }
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
        if (mainWindow) {
          mainWindow.webContents.send('note-received', { text: noteContent, time: timeStr });
          mainWindow.webContents.send('log-message', { type: 'data', message: `Nota guardada en Desktop: ${noteContent.slice(0, 50)}...` });
        }
      } catch (err) {
        console.error('[Sync Engine] Error guardando nota:', err);
      }
      break;
    }

    case 'OPEN_DOWNLOADS': {
      const dl = path.join(os.homedir(), 'Downloads');
      shell.openPath(dl);
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
      shell.openPath(target);
      break;
    }
  }
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

function startTelemetryBroadcaster() {
  if (telemetryInterval) clearInterval(telemetryInterval);
  telemetryInterval = setInterval(() => {
    if (connectedCount() === 0) return;
    const freeMemGb = (os.freemem() / (1024 * 1024 * 1024)).toFixed(1);
    const totalMemGb = (os.totalmem() / (1024 * 1024 * 1024)).toFixed(1);
    const ramUsagePercent = Math.round(((os.totalmem() - os.freemem()) / os.totalmem()) * 100);
    const uptimeMin = Math.round(os.uptime() / 60);

    const payload = {
      freeRamGb: freeMemGb,
      totalRamGb: totalMemGb,
      ramPercent: ramUsagePercent,
      uptimeMin: uptimeMin,
      cpuCount: os.cpus().length,
      platform: os.platform()
    };

    broadcast({
      id: `telemetry-${Date.now()}`,
      type: 'HARDWARE_TELEMETRY',
      payload,
      timestamp: Date.now(),
      direction: 'INBOUND',
      status: 'RECEIVED'
    });
  }, 3000);
}

function notifyClientConnected(ipAddress) {
  if (mainWindow) {
    const cleanIp = (ipAddress || '').replace('::ffff:', '');
    const n = connectedCount();
    const label = n > 1 ? `${n} nodos (${cleanIp})` : `Celular (${cleanIp})`;
    mainWindow.webContents.send('status-update', { status: 'CONNECTED', device: label, clients: n });
  }
}

const lastIotLog = new Map();

function emitIot(payload, fromIp) {
  if (!mainWindow) return;
  mainWindow.webContents.send('iot-telemetry', payload);
  const source = (payload && (payload.source || payload.device)) || 'nodo';
  const key = String(source);
  const now = Date.now();
  if (!lastIotLog.has(key) || now - lastIotLog.get(key) > 15000) {
    lastIotLog.set(key, now);
    const ip = (fromIp || '').replace('::ffff:', '');
    mainWindow.webContents.send('log-message', {
      type: 'iot',
      message: `IoT [${source}] ${payload && payload.device ? payload.device : ''} ${ip}`.trim()
    });
  }
}

function handleIotMessage(msg, remoteAddress, exceptWs) {
  let payload = msg.payload;
  if (typeof payload === 'string') {
    try { payload = JSON.parse(payload); } catch (_e) { payload = { raw: payload }; }
  }
  if (!payload || typeof payload !== 'object') {
    payload = msg.sensors ? msg : {};
  }
  emitIot(payload, remoteAddress);
  broadcast({
    id: msg.id || `iot-${Date.now()}`,
    type: 'IOT_TELEMETRY',
    payload,
    timestamp: Date.now(),
    direction: 'INBOUND',
    status: 'RECEIVED'
  }, exceptWs);
  return payload;
}

function startServer() {
  const primaryIp = getPrimaryLocalIp();
  const wsUrl = `ws://${primaryIp}:${PORT}`;

  server = http.createServer((req, res) => {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

    if (req.method === 'OPTIONS') {
      res.writeHead(204);
      res.end();
      return;
    }

    if (req.url === '/ping' || req.url === '/status') {
      notifyClientConnected(req.socket.remoteAddress);
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ status: 'CONNECTED', hostName: os.hostname(), ip: primaryIp, clients: connectedCount() }));
      return;
    }

    if (req.method === 'POST' && (req.url === '/api/iot' || req.url === '/iot')) {
      let body = '';
      req.on('data', chunk => { body += chunk.toString(); });
      req.on('end', () => {
        try {
          const msg = JSON.parse(body);
          handleIotMessage(msg, req.socket.remoteAddress, null);
          res.writeHead(200, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ result: 'SUCCESS', action: 'IOT_TELEMETRY' }));
        } catch (e) {
          res.writeHead(400, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: e.message }));
        }
      });
      return;
    }

    if (req.method === 'POST' && (req.url === '/api/command' || req.url === '/command')) {
      let body = '';
      req.on('data', chunk => { body += chunk.toString(); });
      req.on('end', () => {
        try {
          const msg = JSON.parse(body);
          notifyClientConnected(req.socket.remoteAddress);
          if (msg.type === 'IOT_TELEMETRY' || (msg.payload && msg.payload.sensors)) {
            handleIotMessage(msg, req.socket.remoteAddress, null);
          } else if (!LIVE_TYPES.has(msg.type)) {
            executeCommand(msg.type, msg.payload);
          }

          if (mainWindow && msg.type !== 'IOT_TELEMETRY') {
            mainWindow.webContents.send('log-message', { type: 'data', message: `Comando REST [${msg.type}]: ${JSON.stringify(msg.payload)}` });
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
          if (mainWindow) {
            mainWindow.webContents.send('log-message', { type: 'data', message: `Archivo subido desde celular a: ${path.basename(destPath)}` });
          }
        });
        fileStream.on('error', (err) => {
          res.writeHead(500, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: err.message }));
        });
      } catch (e) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: e.message }));
      }
      return;
    }

    res.writeHead(404, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ error: 'Not found' }));
  });

  wss = new WebSocket.Server({ server });

  server.on('error', (err) => {
    if (err.code === 'EADDRINUSE') {
      console.warn(`[Sync Engine Warning] El puerto ${PORT} ya esta en uso. Continuando...`);
    } else {
      console.error('[Sync Engine Server Error]:', err);
    }
  });

  server.listen(PORT, '0.0.0.0', () => {
    console.log('====================================================');
    console.log(`[Sync Engine] Dual Protocol activo en: ${wsUrl}`);
    console.log(`[Sync Engine] IoT REST: http://${primaryIp}:${PORT}/api/iot`);
    console.log('====================================================');
    try {
      qrcode.generate(wsUrl, { small: true });
    } catch (_e) {}
    stopDiscovery = startDiscovery({
      getIp: getPrimaryLocalIp,
      port: PORT,
      onLog: (msg) => {
        console.log('[Sync Engine]', msg);
        if (mainWindow) mainWindow.webContents.send('log-message', { type: 'info', message: msg });
      }
    });
    if (mainWindow) {
      mainWindow.webContents.send('discovery-info', {
        udp: true,
        mdns: true,
        ble: process.platform === 'win32'
      });
    }
  });

  startTelemetryBroadcaster();

  wss.on('connection', (ws, req) => {
    clients.add(ws);
    const ip = req.socket.remoteAddress;
    console.log(`[Sync Engine WebSocket] Conexi\u00f3n desde: ${ip} (${connectedCount()} clientes)`);

    notifyClientConnected(ip);
    if (mainWindow) {
      mainWindow.webContents.send('log-message', { type: 'connect', message: `Cliente WebSocket conectado desde ${ip}` });
    }

    ws.send(JSON.stringify({
      id: 'init-handshake',
      type: 'CONNECTION_STATE',
      payload: { status: 'CONNECTED', hostName: os.hostname() },
      timestamp: Date.now(),
      direction: 'INBOUND',
      status: 'RECEIVED'
    }));

    ws.on('message', (data) => {
      try {
        const msg = JSON.parse(data.toString());
        notifyClientConnected(ip);

        if (msg.type === 'PING' || msg.payload === 'PING') {
          ws.send(JSON.stringify({
            id: msg.id || 'ack-ping',
            type: 'PONG',
            payload: 'PONG_OK',
            timestamp: Date.now(),
            direction: 'INBOUND',
            status: 'RECEIVED'
          }));
          return;
        }

        if (msg.type === 'IOT_TELEMETRY') {
          handleIotMessage(msg, ip, ws);
          return;
        }

        if (LIVE_TYPES.has(msg.type)) {
          return;
        }

        if (mainWindow) {
          mainWindow.webContents.send('log-message', { type: 'data', message: `Mensaje WebSocket [${msg.type}]: ${JSON.stringify(msg.payload)}` });
        }

        executeCommand(msg.type, msg.payload);
        sendAck(ws, msg.id, msg.type, 'SUCCESS');
      } catch (err) {
        console.error('[Sync Engine Error]: Mensaje corrupto recibido', err);
      }
    });

    ws.on('close', () => {
      clients.delete(ws);
      console.log(`[Sync Engine] Dispositivo desconectado. Quedan ${connectedCount()}.`);
      if (mainWindow) {
        if (connectedCount() === 0) {
          mainWindow.webContents.send('status-update', { status: 'DISCONNECTED' });
          mainWindow.webContents.send('log-message', { type: 'disconnect', message: 'Celular desconectado' });
        } else {
          notifyClientConnected(ip);
          mainWindow.webContents.send('log-message', { type: 'disconnect', message: `Nodo sali\u00f3. Quedan ${connectedCount()}.` });
        }
      }
    });
  });
}

function sendAck(ws, originalId, action, result) {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({
      id: originalId || 'ack',
      type: 'COMMAND_ACK',
      payload: { action, result },
      timestamp: Date.now(),
      direction: 'INBOUND',
      status: 'RECEIVED'
    }));
  }
}

ipcMain.on('send-clipboard-to-phone', (event, text) => {
  broadcast({
    id: `pc-${Date.now()}`,
    type: 'CLIPBOARD_RECEIVED_FROM_PC',
    payload: { text },
    timestamp: Date.now(),
    direction: 'INBOUND',
    status: 'RECEIVED'
  });
});

ipcMain.on('lock-pc', () => {
  executeCommand('LOCK_SCREEN', {});
});

ipcMain.on('open-downloads', () => {
  executeCommand('OPEN_DOWNLOADS', {});
});

ipcMain.on('open-notepad', () => {
  executeCommand('OPEN_NOTEPAD', {});
});

ipcMain.on('mute-pc', () => {
  executeCommand('VOLUME_MUTE', {});
});

app.whenReady().then(() => {
  createWindow();
  createTray();
  startServer();
});

app.on('before-quit', () => {
  try { if (stopDiscovery) stopDiscovery(); } catch (_e) {}
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    // Keep app running in system tray
  }
});
