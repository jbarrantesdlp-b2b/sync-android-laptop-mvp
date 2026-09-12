const { app, BrowserWindow, Tray, Menu, ipcMain, nativeImage, shell } = require('electron');
const path = require('path');
const http = require('http');
const WebSocket = require('ws');
const { exec } = require('child_process');
const os = require('os');
const qrcode = require('qrcode-terminal');

const PORT = 8123;
let mainWindow = null;
let tray = null;
let server = null;
let wss = null;
let activeWs = null;
let telemetryInterval = null;

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
    width: 760,
    height: 540,
    resizable: true,
    autoHideMenuBar: true,
    title: 'SyncApp - Integración Local',
    backgroundColor: '#0B0F19',
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
            title: 'SyncApp',
            content: 'La app sigue ejecutándose en segundo plano en la bandeja.'
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
    const icon = nativeImage.createFromBitmap(Buffer.from([
      0, 255, 200, 255, 0, 255, 200, 255, 0, 255, 200, 255, 0, 255, 200, 255
    ]), { width: 2, height: 2 });

    tray = new Tray(icon);
    tray.setToolTip('SyncApp - Control de Celular & Laptop');

    const contextMenu = Menu.buildFromTemplate([
      {
        label: 'Abrir SyncApp',
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
  console.log(`[SyncApp Command Executed]: ${type}`, payload);
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

    case 'SYNC_CLIPBOARD':
      if (payload && payload.text) {
        const text = payload.text;
        const clean = text.replace(/"/g, '`"');
        exec(`powershell -c "Set-Clipboard -Value \\"${clean}\\""`);
        if (text.startsWith('http://') || text.startsWith('https://')) {
          shell.openExternal(text);
        }
      }
      break;
  }
}

function startTelemetryBroadcaster() {
  if (telemetryInterval) clearInterval(telemetryInterval);
  telemetryInterval = setInterval(() => {
    if (activeWs && activeWs.readyState === WebSocket.OPEN) {
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

      activeWs.send(JSON.stringify({
        id: `telemetry-${Date.now()}`,
        type: 'HARDWARE_TELEMETRY',
        payload: payload,
        timestamp: Date.now(),
        direction: 'INBOUND',
        status: 'RECEIVED'
      }));
    }
  }, 3000);
}

function notifyClientConnected(ipAddress) {
  if (mainWindow) {
    const cleanIp = (ipAddress || '').replace('::ffff:', '');
    mainWindow.webContents.send('status-update', { status: 'CONNECTED', device: `Celular (${cleanIp})` });
  }
}

function startServer() {
  const primaryIp = getPrimaryLocalIp();
  const wsUrl = `ws://${primaryIp}:${PORT}`;

  // HTTP Server for REST Fallback
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
      res.end(JSON.stringify({ status: 'CONNECTED', hostName: os.hostname(), ip: primaryIp }));
      return;
    }

    if (req.method === 'POST' && (req.url === '/api/command' || req.url === '/command')) {
      let body = '';
      req.on('data', chunk => { body += chunk.toString(); });
      req.on('end', () => {
        try {
          const msg = JSON.parse(body);
          notifyClientConnected(req.socket.remoteAddress);
          executeCommand(msg.type, msg.payload);

          if (mainWindow) {
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

    res.writeHead(404, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ error: 'Not found' }));
  });

  // Attach WebSocket Server to HTTP Server
  wss = new WebSocket.Server({ server });

  server.listen(PORT, '0.0.0.0', () => {
    console.log('====================================================');
    console.log(`[SyncApp Server] Dual Protocol activo en: ${wsUrl}`);
    console.log('====================================================');
    try {
      qrcode.generate(wsUrl, { small: true });
    } catch (_e) {}
  });

  startTelemetryBroadcaster();

  wss.on('connection', (ws, req) => {
    activeWs = ws;
    const ip = req.socket.remoteAddress;
    console.log(`[SyncApp WebSocket] Conexión establecida desde: ${ip}`);

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

        if (mainWindow && msg.type !== 'PING') {
          mainWindow.webContents.send('log-message', { type: 'data', message: `Mensaje WebSocket [${msg.type}]: ${JSON.stringify(msg.payload)}` });
        }

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

        executeCommand(msg.type, msg.payload);
        sendAck(ws, msg.id, msg.type, 'SUCCESS');
      } catch (err) {
        console.error('[SyncApp Error]: Mensaje corrupto recibido', err);
      }
    });

    ws.on('close', () => {
      if (activeWs === ws) {
        activeWs = null;
        console.log('[SyncApp] Dispositivo desconectado.');
        if (mainWindow) {
          mainWindow.webContents.send('status-update', { status: 'DISCONNECTED' });
          mainWindow.webContents.send('log-message', { type: 'disconnect', message: 'Celular desconectado' });
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

// IPC Handlers from Renderer
ipcMain.on('send-clipboard-to-phone', (event, text) => {
  if (activeWs && activeWs.readyState === WebSocket.OPEN) {
    activeWs.send(JSON.stringify({
      id: `pc-${Date.now()}`,
      type: 'CLIPBOARD_RECEIVED_FROM_PC',
      payload: { text },
      timestamp: Date.now(),
      direction: 'INBOUND',
      status: 'RECEIVED'
    }));
  }
});

ipcMain.on('lock-pc', () => {
  executeCommand('LOCK_SCREEN', {});
});

app.whenReady().then(() => {
  createWindow();
  createTray();
  startServer();
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    // Keep app running in system tray
  }
});
