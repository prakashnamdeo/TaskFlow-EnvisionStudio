const { createApp } = require('./app');
const { defaultDatabase } = require('./db/database');

const PORT = process.env.PORT || 3000;
const HOST = process.env.HOST || '0.0.0.0';

async function startServer() {
  try {
    console.log('[Server] Initializing database...');
    await defaultDatabase.init();

    const app = createApp(defaultDatabase);

    const server = app.listen(PORT, HOST, () => {
      console.log(`===============================================`);
      console.log(`TaskFlow Backend running at http://${HOST}:${PORT}`);
      console.log(`Android Emulator base URL: http://10.0.2.2:${PORT}`);
      console.log(`API Endpoint: http://${HOST}:${PORT}/api/tasks`);
      console.log(`===============================================`);
    });

    // Graceful shutdown
    const shutdown = async () => {
      console.log('\n[Server] Shutting down gracefully...');
      server.close(async () => {
        await defaultDatabase.close();
        console.log('[Server] Database connection closed.');
        process.exit(0);
      });
    };

    process.on('SIGINT', shutdown);
    process.on('SIGTERM', shutdown);

    return server;
  } catch (err) {
    console.error('[Server] Startup failed:', err);
    process.exit(1);
  }
}

if (require.main === module) {
  startServer();
}

module.exports = { startServer };
