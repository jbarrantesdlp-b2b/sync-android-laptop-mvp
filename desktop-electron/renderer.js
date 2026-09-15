const { ipcRenderer } = require('electron');
const QRCode = require('qrcode');

const statusPill = document.getElementById('status-pill');
const statusText = document.getElementById('status-text');
const deviceName = document.getElementById('device-name');
const serverUrl = document.getElementById('server-url');
const logContent = document.getElementById('log-content');
const qrCanvas = document.getElementById('qr-canvas');
const clipboardInput = document.getElementById('clipboard-input');
const sendClipboardBtn = document.getElementById('send-clipboard-btn');
const lockBtn = document.getElementById('lock-btn');
const clearLogBtn = document.getElementById('clear-log-btn');
const clockEl = document.getElementById('clock');
const clockDate = document.getElementById('clock-date');
const heroDevice = document.getElementById('hero-device');

function tickClock() {
  const now = new Date();
  if (clockEl) {
    clockEl.textContent = now.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit', hour12: false });
  }
  if (clockDate) {
    const raw = now.toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'long' });
    clockDate.textContent = raw.charAt(0).toUpperCase() + raw.slice(1);
  }
}
tickClock();
setInterval(tickClock, 10000);

ipcRenderer.on('server-info', (event, data) => {
  serverUrl.textContent = data.url;
  QRCode.toCanvas(qrCanvas, data.url, {
    width: 148,
    margin: 1,
    color: { dark: '#050811', light: '#FFFFFF' }
  }, (error) => {
    if (error) console.error('Error al generar QR en Canvas:', error);
  });
  appendLog('info', `Servidor activo en: ${data.url}`);
});

ipcRenderer.on('status-update', (event, data) => {
  if (data.status === 'CONNECTED') {
    statusPill.className = 'status-pill connected';
    statusText.textContent = 'Conectado';
    deviceName.textContent = data.device || 'Xiaomi 2312';
    if (heroDevice) heroDevice.textContent = data.device || 'Xiaomi 2312';
    appendLog('connect', `Dispositivo conectado: ${data.device || 'Android'}`);
  } else {
    statusPill.className = 'status-pill disconnected';
    statusText.textContent = 'Desconectado';
    deviceName.textContent = 'Buscando celular\u2026';
    appendLog('disconnect', 'Dispositivo desconectado');
  }
});

ipcRenderer.on('log-message', (event, data) => {
  appendLog(data.type || 'info', data.message);
});

function appendLog(type, text) {
  const time = new Date().toLocaleTimeString('es-ES', { hour12: false });
  const div = document.createElement('div');
  div.className = `log-item ${type}`;
  div.textContent = `[${time}] ${text}`;
  logContent.appendChild(div);
  logContent.scrollTop = logContent.scrollHeight;
}

sendClipboardBtn.addEventListener('click', () => {
  const text = clipboardInput.value.trim();
  if (text) {
    ipcRenderer.send('send-clipboard-to-phone', text);
    appendLog('data', `Texto enviado al celular: "${text}"`);
    clipboardInput.value = '';
  }
});

lockBtn.addEventListener('click', () => {
  ipcRenderer.send('lock-pc');
  appendLog('info', 'Ejecutando bloqueo de pantalla de PC...');
});

clearLogBtn.addEventListener('click', () => {
  logContent.innerHTML = '';
  appendLog('info', 'Registro de eventos limpiado');
});
