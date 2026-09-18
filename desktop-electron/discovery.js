const dgram = require('dgram');
const os = require('os');
const path = require('path');
const { spawn } = require('child_process');

const UDP_PORT = 41234;
const MAGIC = 'SYNC_ENGINE';
const MULTICAST = '239.55.55.1';

function beacon(ip, port) {
  return Buffer.from(JSON.stringify({
    v: 1,
    app: MAGIC,
    name: os.hostname(),
    ip,
    port,
    url: `ws://${ip}:${port}`
  }));
}

function startUdpBeacon(getIp, port, onLog) {
  const sock = dgram.createSocket({ type: 'udp4', reuseAddr: true });
  sock.on('error', (err) => onLog && onLog(`UDP discovery: ${err.message}`));
  sock.bind(UDP_PORT, () => {
    try { sock.setBroadcast(true); } catch (_e) {}
    try { sock.addMembership(MULTICAST); } catch (_e) {}
    onLog && onLog(`Anuncio Wi-Fi UDP :${UDP_PORT}`);
  });

  const tick = () => {
    const ip = getIp();
    const buf = beacon(ip, port);
    try { sock.send(buf, UDP_PORT, '255.255.255.255'); } catch (_e) {}
    try { sock.send(buf, UDP_PORT, MULTICAST); } catch (_e) {}
  };
  const timer = setInterval(tick, 2000);
  tick();
  return () => {
    clearInterval(timer);
    try { sock.close(); } catch (_e) {}
  };
}

function startMdns(getIp, port, onLog) {
  let bonjour;
  let service;
  try {
    const { Bonjour } = require('bonjour-service');
    bonjour = new Bonjour();
    const ip = getIp();
    service = bonjour.publish({
      name: `SYNC ENGINE ${os.hostname()}`,
      type: 'syncengine',
      protocol: 'tcp',
      port,
      txt: { url: `ws://${ip}:${port}`, app: MAGIC, name: os.hostname() }
    });
    onLog && onLog('Anuncio mDNS _syncengine._tcp');
  } catch (e) {
    onLog && onLog(`mDNS opcional no cargado (${e.message}). UDP sigue activo.`);
  }
  return () => {
    try { service && service.stop(); } catch (_e) {}
    try { bonjour && bonjour.destroy(); } catch (_e) {}
  };
}

function startBle(getIp, port, onLog) {
  if (process.platform !== 'win32') return () => {};
  const script = path.join(__dirname, 'ble-advertise.ps1');
  let child;
  try {
    child = spawn('powershell.exe', [
      '-NoProfile', '-ExecutionPolicy', 'Bypass',
      '-File', script,
      '-Ip', getIp(),
      '-Port', String(port)
    ], { windowsHide: true, stdio: 'ignore' });
    child.on('error', () => {});
    child.on('exit', (code) => {
      if (code && code !== 0) onLog && onLog('Bluetooth LE no disponible en este Windows');
    });
    onLog && onLog('Anuncio Bluetooth LE (si el adaptador lo permite)');
  } catch (_e) {
    onLog && onLog('Bluetooth LE omitido');
  }
  const refresh = setInterval(() => {}, 60000);
  return () => {
    clearInterval(refresh);
    try { if (child) child.kill(); } catch (_e) {}
  };
}

function defaultGetIp() {
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

function startDiscovery(opts) {
  let getIp = defaultGetIp;
  let port = 8123;
  let onLog = console.log;

  if (typeof opts === 'number') {
    port = opts;
  } else if (typeof opts === 'function') {
    getIp = opts;
  } else if (opts && typeof opts === 'object') {
    if (typeof opts.getIp === 'function') getIp = opts.getIp;
    if (opts.port) port = opts.port;
    if (typeof opts.onLog === 'function') onLog = opts.onLog;
  }

  const stopUdp = startUdpBeacon(getIp, port, onLog);
  const stopMdns = startMdns(getIp, port, onLog);
  const stopBle = startBle(getIp, port, onLog);
  return () => {
    stopUdp();
    stopMdns();
    stopBle();
  };
}

module.exports = { startDiscovery, UDP_PORT };
