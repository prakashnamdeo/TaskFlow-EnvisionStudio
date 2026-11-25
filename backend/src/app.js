const express = require('express');
const cors = require('cors');
const morgan = require('morgan');
const { createTaskRoutes } = require('./routes/taskRoutes');
const { createTaskController } = require('./controllers/taskController');
const { errorHandler, notFoundHandler } = require('./middleware/errorHandler');

function createApp(database) {
  const app = express();

  // Middleware
  app.use(cors());
  app.use(express.json());

  // Logging (disabled in test)
  if (process.env.NODE_ENV !== 'test') {
    app.use(morgan('dev'));
  }

  // Root index endpoint
  app.get('/', (req, res) => {
    res.json({
      name: 'TaskFlow REST API',
      status: 'running',
      message: 'TaskFlow backend is operational',
      endpoints: {
        health: '/health',
        tasks: '/api/tasks',
        tasksByAssignee: '/api/tasks?assignee=aarav',
        tasksByStatus: '/api/tasks?status=active',
        combinedFilters: '/api/tasks?assignee=aarav&status=completed'
      }
    });
  });

  // Health check endpoint
  app.get('/health', (req, res) => {
    res.json({ status: 'ok', timestamp: new Date().toISOString() });
  });

  // Task API routes
  const taskController = createTaskController(database);
  app.use('/api/tasks', createTaskRoutes(taskController));

  // 404 handler
  app.use(notFoundHandler);

  // Centralized Error handler
  app.use(errorHandler);

  return app;
}

module.exports = { createApp };
