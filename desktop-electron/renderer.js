const { ipcRenderer } = require('electron');
const QRCode = require('qrcode');

// UI Elements
const liveFullDate = document.getElementById('live-full-date');
const heroHostname = document.getElementById('hero-hostname');
const heroLatency = document.getElementById('hero-latency');
const heroStatusLabel = document.getElementById('hero-status-label');
const serverUrlDisplay = document.getElementById('server-url-display');
const qrCanvas = document.getElementById('qr-canvas');
const pairingModal = document.getElementById('pairing-modal');
const btnCloseModal = document.getElementById('btn-close-modal');
const btnOpenPairing = document.getElementById('btn-open-pairing');
const btnSidebarDetails = document.getElementById('btn-sidebar-details');

const clipboardInput = document.getElementById('clipboard-input');
const sendClipboardBtn = document.getElementById('send-clipboard-btn');
const btnActionClipboard = document.getElementById('btn-action-clipboard');
const btnActionSync = document.getElementById('btn-action-sync');
const btnActionAi = document.getElementById('btn-action-ai');
const btnActionRules = document.getElementById('btn-action-rules');
const lockBtn = document.getElementById('lock-btn');
const clearLogBtn = document.getElementById('clear-log-btn');

// Phone indicator in devices list
const dotPhone = document.getElementById('dot-phone');
const labelPhoneName = document.getElementById('label-phone-name');
const pillPhoneStatus = document.getElementById('pill-phone-status');
const txtPhoneLatency = document.getElementById('txt-phone-latency');
const sidebarStatusSub = document.getElementById('sidebar-status-sub');
const dockLatencyNum = document.getElementById('dock-latency-num');
const statEventsCount = document.getElementById('stat-events-count');
const activityTimeline = document.getElementById('activity-timeline-container');

// Date Formatter (matches "JUEVES, 15 DE SEPTIEMBRE")
function updateDate() {
  const now = new Date();
  const options = { weekday: 'long', day: 'numeric', month: 'long' };
  const str = now.toLocaleDateString('es-ES', options).toUpperCase();
  if (liveFullDate) liveFullDate.textContent = str;
}
updateDate();
setInterval(updateDate, 60000);

// Modal Controls
function openModal() {
  if (pairingModal) pairingModal.style.display = 'flex';
}
function closeModal() {
  if (pairingModal) pairingModal.style.display = 'none';
}

if (btnOpenPairing) btnOpenPairing.addEventListener('click', openModal);
if (btnSidebarDetails) btnSidebarDetails.addEventListener('click', openModal);
if (btnCloseModal) btnCloseModal.addEventListener('click', closeModal);
if (pairingModal) {
  pairingModal.addEventListener('click', (e) => {
    if (e.target === pairingModal) closeModal();
  });
}

// Server URL & QR Code
let currentServerUrl = 'ws://127.0.0.1:8123';
ipcRenderer.on('server-info', (event, data) => {
  currentServerUrl = data.url;
  if (serverUrlDisplay) serverUrlDisplay.textContent = data.url;
  if (qrCanvas) {
    QRCode.toCanvas(qrCanvas, data.url, {
      width: 160,
      margin: 1,
      color: { dark: '#0F172A', light: '#FFFFFF' }
    }, (err) => {
      if (err) console.error('Error QR:', err);
    });
  }
});

// Status & Telemetry Updates
ipcRenderer.on('status-update', (event, data) => {
  const isConnected = data.status === 'CONNECTED';
  const device = data.device || 'Android';

  if (heroStatusLabel) heroStatusLabel.textContent = isConnected ? 'Conectado' : 'Buscando celular...';
  if (sidebarStatusSub) sidebarStatusSub.textContent = isConnected ? 'Todo en orden' : 'Sin clientes activos';

  if (pillPhoneStatus) {
    pillPhoneStatus.className = `badge-status-pill ${isConnected ? 'online' : 'offline'}`;
    pillPhoneStatus.textContent = isConnected ? 'Conectado' : 'Sin conexión';
  }
  if (dotPhone) {
    dotPhone.className = `device-status-dot ${isConnected ? 'online' : 'offline'}`;
  }
  if (labelPhoneName && isConnected) {
    labelPhoneName.textContent = device.split(' ')[0] || 'Galaxy S24';
  }
});

// Hardware telemetry (RAM, latency, stats)
ipcRenderer.on('iot-telemetry', (event, payload) => {
  // Real time latency or updates
});

ipcRenderer.on('log-message', (event, data) => {
  addActivityRow(data.type, data.message);
});

function addActivityRow(type, text) {
  if (!activityTimeline) return;
  const now = new Date();
  const time = now.toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' });

  const row = document.createElement('div');
  row.className = 'activity-row';
  row.innerHTML = `
    <div class="activity-icon-badge blue">
      <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
    </div>
    <div class="activity-row-content">
      <strong class="activity-row-title">${text}</strong>
      <span class="activity-row-meta">En vivo · ${time}</span>
    </div>
    <button class="item-more-btn">⋮</button>
  `;
  activityTimeline.insertBefore(row, activityTimeline.firstChild);
  if (activityTimeline.children.length > 6) {
    activityTimeline.removeChild(activityTimeline.lastChild);
  }
}

// Quick Actions
function sendClipboard(customText) {
  const text = customText || (clipboardInput ? clipboardInput.value.trim() : '');
  if (text) {
    ipcRenderer.send('send-clipboard-to-phone', text);
    addActivityRow('data', `Portapapeles enviado: "${text.substring(0, 35)}..."`);
    if (clipboardInput) clipboardInput.value = '';
    closeModal();
  }
}

if (sendClipboardBtn) sendClipboardBtn.addEventListener('click', () => sendClipboard());
if (clipboardInput) {
  clipboardInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') sendClipboard();
  });
}

if (btnActionClipboard) {
  btnActionClipboard.addEventListener('click', () => {
    openModal();
    if (clipboardInput) clipboardInput.focus();
  });
}

if (btnActionSync) {
  btnActionSync.addEventListener('click', () => {
    ipcRenderer.send('send-clipboard-to-phone', 'SYNC_NOW');
    addActivityRow('info', 'Sincronización manual de archivos solicitada');
  });
}

if (btnActionAi) {
  btnActionAi.addEventListener('click', () => {
    openModal();
  });
}

if (btnActionRules) {
  btnActionRules.addEventListener('click', () => {
    addActivityRow('info', 'Automatizaciones activas en segundo plano');
  });
}

if (lockBtn) {
  lockBtn.addEventListener('click', () => {
    ipcRenderer.send('lock-pc');
    addActivityRow('info', 'Bloqueo de PC ejecutado');
  });
}

if (clearLogBtn) {
  clearLogBtn.addEventListener('click', () => {
    if (activityTimeline) activityTimeline.innerHTML = '';
  });
}
