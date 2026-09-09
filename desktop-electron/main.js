const { app, BrowserWindow, Tray, Menu, ipcMain, nativeImage, shell } = require('electron');
const path = require('path');
const WebSocket = require('ws');
const { exec } = require('child_process');
const os = require('os');
const qrcode = require('qrcode-terminal');

const PORT = 8123;
let mainWindow = null;
let tray = null;
let wss = null;
let activeWs = null;
let telemetryInterval = null;

function getLocalIp() {
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

  // Minimize to tray on close
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

  // Send server info to UI once ready
  mainWindow.webContents.on('did-finish-load', () => {
    const localIp = getLocalIp();
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

function startWebSocketServer() {
  const localIp = getLocalIp();
  const wsUrl = `ws://${localIp}:${PORT}`;

  try {
    wss = new WebSocket.Server({ port: PORT, host: '0.0.0.0' });
  } catch (e) {
    console.error('Failed to create WebSocket Server:', e);
    return;
  }

  wss.on('error', (err) => {
    console.error('WebSocket Server Error:', err);
  });

  console.log('====================================================');
  console.log(`[SyncApp Server] Escuchando en: ${wsUrl}`);
  console.log('====================================================');
  try {
    qrcode.generate(wsUrl, { small: true });
  } catch (_e) {}

  startTelemetryBroadcaster();

  wss.on('connection', (ws, req) => {
    activeWs = ws;
    const ip = req.socket.remoteAddress;
    console.log(`[SyncApp] Conexión establecida desde: ${ip}`);

    if (mainWindow) {
      mainWindow.webContents.send('status-update', { status: 'CONNECTED', device: `Xiaomi (${ip})` });
      mainWindow.webContents.send('log-message', { type: 'connect', message: `Cliente conectado desde ${ip}` });
    }

    ws.send(JSON.stringify({
      id: 'init-handshake',
      type: 'CONNECTION_STATE',
      payload: { status: 'CONNECTED', hostName: 'Laptop-Host' },
      timestamp: Date.now(),
      direction: 'INBOUND',
      status: 'RECEIVED'
    }));

    ws.on('message', (data) => {
      try {
        const msg = JSON.parse(data.toString());

        if (mainWindow && msg.type !== 'PING') {
          mainWindow.webContents.send('log-message', { type: 'data', message: `Mensaje de celular [${msg.type}]: ${JSON.stringify(msg.payload)}` });
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

        switch (msg.type) {
          case 'OPEN_URL':
            if (msg.payload && msg.payload.url) {
              shell.openExternal(msg.payload.url);
              sendAck(ws, msg.id, 'OPEN_URL', 'SUCCESS');
            }
            break;

          case 'VOLUME_MUTE':
            exec('powershell -c "$w = New-Object -ComObject wscript.shell; $w.SendKeys([char]173)"');
            sendAck(ws, msg.id, 'VOLUME_MUTE', 'SUCCESS');
            break;

          case 'VOLUME_UP':
            exec('powershell -c "$w = New-Object -ComObject wscript.shell; $w.SendKeys([char]175)"');
            sendAck(ws, msg.id, 'VOLUME_UP', 'SUCCESS');
            break;

          case 'VOLUME_DOWN':
            exec('powershell -c "$w = New-Object -ComObject wscript.shell; $w.SendKeys([char]174)"');
            sendAck(ws, msg.id, 'VOLUME_DOWN', 'SUCCESS');
            break;

          case 'MEDIA_PLAY_PAUSE':
            exec('powershell -c "$w = New-Object -ComObject wscript.shell; $w.SendKeys([char]179)"');
            sendAck(ws, msg.id, 'MEDIA_PLAY_PAUSE', 'SUCCESS');
            break;

          case 'LOCK_SCREEN':
            exec('rundll32.exe user32.dll,LockWorkStation');
            sendAck(ws, msg.id, 'LOCK_SCREEN', 'SUCCESS');
            break;

          case 'PRESENTATION_NEXT':
            exec('powershell -c "$w = New-Object -ComObject wscript.shell; $w.SendKeys(\'{RIGHT}\')"');
            sendAck(ws, msg.id, 'PRESENTATION_NEXT', 'SUCCESS');
            break;

          case 'PRESENTATION_PREV':
            exec('powershell -c "$w = New-Object -ComObject wscript.shell; $w.SendKeys(\'{LEFT}\')"');
            sendAck(ws, msg.id, 'PRESENTATION_PREV', 'SUCCESS');
            break;

          case 'SYNC_CLIPBOARD':
            if (msg.payload && msg.payload.text) {
              const text = msg.payload.text;
              const clean = text.replace(/"/g, '`"');
              exec(`powershell -c "Set-Clipboard -Value \\"${clean}\\""`);
              sendAck(ws, msg.id, 'SYNC_CLIPBOARD', 'SUCCESS');

              // Auto-open URL if payload text starts with http/https
              if (text.startsWith('http://') || text.startsWith('https://')) {
                shell.openExternal(text);
              }
            }
            break;

          default:
            ws.send(JSON.stringify({
              id: msg.id || 'ack-echo',
              type: 'ECHO_RESPONSE',
              payload: msg.payload,
              timestamp: Date.now(),
              direction: 'INBOUND',
              status: 'RECEIVED'
            }));
            break;
        }
      } catch (err) {
        console.error('[SyncApp Error]: Mensaje corrupto recibido', err);
      }
    });

    ws.on('close', () => {
      activeWs = null;
      console.log('[SyncApp] Dispositivo desconectado.');
      if (mainWindow) {
        mainWindow.webContents.send('status-update', { status: 'DISCONNECTED' });
        mainWindow.webContents.send('log-message', { type: 'disconnect', message: 'Celular desconectado' });
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
  exec('rundll32.exe user32.dll,LockWorkStation');
});

app.whenReady().then(() => {
  createWindow();
  createTray();
  startWebSocketServer();
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    // Keep app running in system tray
  }
});
