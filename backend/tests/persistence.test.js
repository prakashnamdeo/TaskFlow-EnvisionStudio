const { test, describe, before, after } = require('node:test');
const assert = require('node:assert/strict');
const path = require('path');
const fs = require('fs');
const { Database } = require('../src/db/database');

describe('Database Persistence Across Restarts', () => {
  const testDbPath = path.resolve(__dirname, 'test_persistence.db');

  before(async () => {
    if (fs.existsSync(testDbPath)) {
      fs.unlinkSync(testDbPath);
    }
  });

  after(async () => {
    if (fs.existsSync(testDbPath)) {
      fs.unlinkSync(testDbPath);
    }
  });

  test('Seeds on first launch, retains custom data across restart without reseeding', async () => {
    // Session 1: First startup
    const db1 = new Database(testDbPath);
    await db1.init();
    const tasks1 = await db1.all('SELECT * FROM tasks');
    assert.equal(tasks1.length, 15, 'Should seed initial 15 tasks');

    // Add a custom task in session 1
    const customId = 'custom-user-task-1';
    await db1.run(
      'INSERT INTO tasks (id, title, description, dueDate, priority, assigneeId, completed, createdAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?)',
      [customId, 'Persistent User Task', 'Check persistence', '2026-11-25', 'high', 'priya', 0, new Date().toISOString()]
    );
    await db1.close();

    // Session 2: Second startup (restart)
    const db2 = new Database(testDbPath);
    await db2.init();
    const tasks2 = await db2.all('SELECT * FROM tasks');
    assert.equal(tasks2.length, 16, 'Should retain custom task (15 original + 1 added)');

    const customTask = await db2.get('SELECT * FROM tasks WHERE id = ?', [customId]);
    assert.ok(customTask, 'Custom task should persist across DB restart');
    assert.equal(customTask.title, 'Persistent User Task');

    await db2.close();
  });
});
