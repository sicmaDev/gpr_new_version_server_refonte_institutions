module.exports = {
  apps: [{
    name: 'whatgpr',
    script: 'index.js',

    // Redémarrage automatique
    autorestart:   true,
    restart_delay: 12000,  // 12s : laisse Chrome le temps de s'arrêter complètement
    kill_timeout:  8000,   // attendre 8s avant de forcer la mort du processus
    max_restarts:  50,
    min_uptime:    '15s',

    // Variables d'environnement (surcharge le .env si besoin)
    env: {
      NODE_ENV: 'production',
      PORT:     '3001',
    },

    // Logs
    out_file:    './logs/out.log',
    error_file:  './logs/error.log',
    merge_logs:  true,
    log_date_format: 'YYYY-MM-DD HH:mm:ss',

    // Pas de watch (évite les redémarrages intempestifs)
    watch: false,
  }],
};
