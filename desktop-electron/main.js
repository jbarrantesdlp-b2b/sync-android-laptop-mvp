const WebSocket = require('ws');
const port = 8123;
const wss = new WebSocket.Server({ port });

wss.on('connection', function connection(ws) {
  console.log('Client connected');
  ws.on('message', function incoming(message) {
    console.log('received: %s', message);
    ws.send(`echo: ${message}`);
  });
  ws.on('close', () => console.log('Client disconnected'));
});

console.log(`WebSocket server running on ws://localhost:${port}`);
