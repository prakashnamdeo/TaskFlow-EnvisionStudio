const crypto = require('crypto');
const { mapTaskRow } = require('../db/database');

function createTaskController(database) {
  return {
    // GET /api/tasks
    async getAllTasks(req, res, next) {
      try {
        const { assignee, status } = req.query;

        let sql = 'SELECT * FROM tasks WHERE 1=1';
        const params = [];

        // Assignee filter
        if (assignee && typeof assignee === 'string' && assignee.trim() !== '') {
          sql += ' AND LOWER(assigneeId) = LOWER(?)';
          params.push(assignee.trim());
        }

        // Status filter
        if (status && typeof status === 'string') {
          const normalizedStatus = status.trim().toLowerCase();
          if (normalizedStatus === 'active') {
            sql += ' AND completed = 0';
          } else if (normalizedStatus === 'completed') {
            sql += ' AND completed = 1';
          }
        }

        // Order by createdAt ASC for predictable list order
        sql += ' ORDER BY createdAt ASC';

        const rows = await database.all(sql, params);
        const tasks = rows.map(mapTaskRow);

        res.json(tasks);
      } catch (err) {
        next(err);
      }
    },

    // GET /api/tasks/:id
    async getTaskById(req, res, next) {
      try {
        const { id } = req.params;
        const row = await database.get('SELECT * FROM tasks WHERE id = ?', [id]);

        if (!row) {
          return res.status(404).json({ error: 'Task not found' });
        }

        res.json(mapTaskRow(row));
      } catch (err) {
        next(err);
      }
    },

    // POST /api/tasks
    async createTask(req, res, next) {
      try {
        const { title, description, dueDate, priority, assigneeId, completed } = req.validatedBody;

        const id = crypto.randomUUID();
        const createdAt = new Date().toISOString();

        const insertSql = `
          INSERT INTO tasks (id, title, description, dueDate, priority, assigneeId, completed, createdAt)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        `;

        await database.run(insertSql, [
          id,
          title,
          description,
          dueDate,
          priority,
          assigneeId,
          completed ? 1 : 0,
          createdAt
        ]);

        const row = await database.get('SELECT * FROM tasks WHERE id = ?', [id]);
        res.status(201).json(mapTaskRow(row));
      } catch (err) {
        next(err);
      }
    },

    // PATCH /api/tasks/:id/toggle
    async toggleTask(req, res, next) {
      try {
        const { id } = req.params;
        const existing = await database.get('SELECT * FROM tasks WHERE id = ?', [id]);

        if (!existing) {
          return res.status(404).json({ error: 'Task not found' });
        }

        const newCompleted = existing.completed === 1 ? 0 : 1;

        await database.run('UPDATE tasks SET completed = ? WHERE id = ?', [newCompleted, id]);

        const updated = await database.get('SELECT * FROM tasks WHERE id = ?', [id]);
        res.json(mapTaskRow(updated));
      } catch (err) {
        next(err);
      }
    }
  };
}

module.exports = { createTaskController };
