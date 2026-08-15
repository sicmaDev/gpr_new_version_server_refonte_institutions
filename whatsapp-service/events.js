const { EventEmitter } = require('events');

const bus = new EventEmitter();
bus.setMaxListeners(100);

/** Liste des clients SSE connectés */
const sseClients = new Set();

function addSseClient(res) {
  sseClients.add(res);
  res.on('close', () => sseClients.delete(res));
}

/**
 * Diffuse un événement SSE à tous les clients connectés.
 * @param {string} event  - nom de l'événement (wa_status | new_message | survey_response)
 * @param {object} data   - payload JSON
 */
function broadcast(event, data) {
  const payload = `event: ${event}\ndata: ${JSON.stringify(data)}\n\n`;
  for (const res of sseClients) {
    try { res.write(payload); } catch (_) { sseClients.delete(res); }
  }
}

module.exports = { bus, addSseClient, broadcast };