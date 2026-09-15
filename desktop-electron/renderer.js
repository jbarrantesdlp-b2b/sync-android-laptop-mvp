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
const liveClock = document.getElementById('live-clock');
const liveDate = document.getElementById('live-date');

function tickClock() {
  const now = new Date();
  liveClock.textContent = now.toLocaleTimeString('es-PE', { hour: 'numeric', minute: '2-digit' });
  liveDate.textContent = now.toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'long' });
}
tickClock();
setInterval(tickClock, 10000);

ipcRenderer.on('server-info', (event, data) => {
  serverUrl.textContent = data.url;
  QRCode.toCanvas(qrCanvas, data.url, {
    width: 160,
    margin: 1,
    color: { dark: '#0F172A', light: '#FFFFFF' }
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

function sendClipboard() {
  const text = clipboardInput.value.trim();
  if (text) {
    ipcRenderer.send('send-clipboard-to-phone', text);
    appendLog('data', `Texto enviado al celular: "${text}"`);
    clipboardInput.value = '';
  }
}

sendClipboardBtn.addEventListener('click', sendClipboard);
clipboardInput.addEventListener('keydown', (e) => {
  if (e.key === 'Enter') sendClipboard();
});

lockBtn.addEventListener('click', () => {
  ipcRenderer.send('lock-pc');
  appendLog('info', 'Bloqueo de pantalla de PC\u2026');
});

clearLogBtn.addEventListener('click', () => {
  logContent.innerHTML = '';
  appendLog('info', 'Registro limpiado');
});
