const express = require('express');
const { validateCreateTask } = require('../middleware/validator');

function createTaskRoutes(controller) {
  const router = express.Router();

  router.get('/', controller.getAllTasks);
  router.get('/:id', controller.getTaskById);
  router.post('/', validateCreateTask, controller.createTask);
  router.patch('/:id/toggle', controller.toggleTask);

  return router;
}

module.exports = { createTaskRoutes };
