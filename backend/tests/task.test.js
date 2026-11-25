const { test, describe, before, after } = require('node:test');
const assert = require('node:assert/strict');
const request = require('supertest');
const { createApp } = require('../src/app');
const { Database } = require('../src/db/database');

describe('TaskFlow Backend API Tests', () => {
  let db;
  let app;

  before(async () => {
    // Isolated in-memory database with seeded data
    db = new Database(':memory:');
    await db.init();
    app = createApp(db);
  });

  after(async () => {
    await db.close();
  });

  test('GET /health returns 200 ok', async () => {
    const res = await request(app).get('/health');
    assert.equal(res.status, 200);
    assert.equal(res.body.status, 'ok');
  });

  test('GET /api/tasks returns all 15 seeded tasks', async () => {
    const res = await request(app).get('/api/tasks');
    assert.equal(res.status, 200);
    assert.ok(Array.isArray(res.body));
    assert.equal(res.body.length, 15);

    // Verify task structure
    const first = res.body[0];
    assert.ok(first.id);
    assert.ok(first.title);
    assert.equal(typeof first.completed, 'boolean');
    assert.ok(['low', 'medium', 'high'].includes(first.priority));
    assert.ok(['aarav', 'priya', 'rohan'].includes(first.assigneeId));
  });

  test('GET /api/tasks?assignee=aarav filters by assignee', async () => {
    const res = await request(app).get('/api/tasks?assignee=aarav');
    assert.equal(res.status, 200);
    assert.ok(res.body.length > 0);
    res.body.forEach(task => {
      assert.equal(task.assigneeId, 'aarav');
    });
  });

  test('GET /api/tasks?status=active returns only active tasks', async () => {
    const res = await request(app).get('/api/tasks?status=active');
    assert.equal(res.status, 200);
    assert.ok(res.body.length > 0);
    res.body.forEach(task => {
      assert.equal(task.completed, false);
    });
  });

  test('GET /api/tasks?status=completed returns only completed tasks', async () => {
    const res = await request(app).get('/api/tasks?status=completed');
    assert.equal(res.status, 200);
    assert.ok(res.body.length > 0);
    res.body.forEach(task => {
      assert.equal(task.completed, true);
    });
  });

  test('GET /api/tasks?assignee=aarav&status=completed combines both filters', async () => {
    const res = await request(app).get('/api/tasks?assignee=aarav&status=completed');
    assert.equal(res.status, 200);
    res.body.forEach(task => {
      assert.equal(task.assigneeId, 'aarav');
      assert.equal(task.completed, true);
    });
  });

  test('GET /api/tasks with filters yielding no results returns empty array (Edge Case 3)', async () => {
    // Aarav has only 1 completed task initially. If we filter by rohan and a specific state or non-existent
    const res = await request(app).get('/api/tasks?assignee=nonexistent&status=completed');
    assert.equal(res.status, 200);
    assert.deepEqual(res.body, []);
  });

  test('POST /api/tasks creates a valid task with 201', async () => {
    const newTask = {
      title: 'Implement unit tests',
      description: 'Write comprehensive backend and Android tests',
      dueDate: '2026-11-30',
      priority: 'high',
      assigneeId: 'aarav'
    };

    const res = await request(app)
      .post('/api/tasks')
      .send(newTask);

    assert.equal(res.status, 201);
    assert.ok(res.body.id);
    assert.equal(res.body.title, 'Implement unit tests');
    assert.equal(res.body.description, 'Write comprehensive backend and Android tests');
    assert.equal(res.body.dueDate, '2026-11-30');
    assert.equal(res.body.priority, 'high');
    assert.equal(res.body.assigneeId, 'aarav');
    assert.equal(res.body.completed, false);
  });

  test('POST /api/tasks creates task without due date (Edge Case 1)', async () => {
    const newTask = {
      title: 'Task without due date',
      description: null,
      dueDate: null,
      priority: 'low',
      assigneeId: 'priya'
    };

    const res = await request(app)
      .post('/api/tasks')
      .send(newTask);

    assert.equal(res.status, 201);
    assert.equal(res.body.dueDate, null);
  });

  test('POST /api/tasks rejects empty or missing title with 400 (Edge Case 4)', async () => {
    const resEmpty = await request(app)
      .post('/api/tasks')
      .send({
        title: '   ',
        priority: 'medium',
        assigneeId: 'rohan'
      });

    assert.equal(resEmpty.status, 400);
    assert.ok(resEmpty.body.errors);
    assert.ok(resEmpty.body.errors.title);

    const resMissing = await request(app)
      .post('/api/tasks')
      .send({
        priority: 'medium',
        assigneeId: 'rohan'
      });

    assert.equal(resMissing.status, 400);
    assert.ok(resMissing.body.errors.title);
  });

  test('POST /api/tasks rejects invalid priority with 400', async () => {
    const res = await request(app)
      .post('/api/tasks')
      .send({
        title: 'Valid title',
        priority: 'urgent', // not low, medium, or high
        assigneeId: 'rohan'
      });

    assert.equal(res.status, 400);
    assert.ok(res.body.errors.priority);
  });

  test('POST /api/tasks rejects invalid assignee with 400', async () => {
    const res = await request(app)
      .post('/api/tasks')
      .send({
        title: 'Valid title',
        priority: 'medium',
        assigneeId: 'john' // not aarav, priya, or rohan
      });

    assert.equal(res.status, 400);
    assert.ok(res.body.errors.assigneeId);
  });

  test('POST /api/tasks rejects invalid due date format with 400', async () => {
    const res = await request(app)
      .post('/api/tasks')
      .send({
        title: 'Valid title',
        dueDate: '15-10-2026', // wrong format, must be YYYY-MM-DD
        priority: 'medium',
        assigneeId: 'aarav'
      });

    assert.equal(res.status, 400);
    assert.ok(res.body.errors.dueDate);
  });

  test('PATCH /api/tasks/:id/toggle toggles completion status', async () => {
    // Fetch a task
    const listRes = await request(app).get('/api/tasks');
    const targetTask = listRes.body[0];
    const initialStatus = targetTask.completed;

    // First toggle
    const toggleRes1 = await request(app).patch(`/api/tasks/${targetTask.id}/toggle`);
    assert.equal(toggleRes1.status, 200);
    assert.equal(toggleRes1.body.completed, !initialStatus);

    // Second toggle (revert back)
    const toggleRes2 = await request(app).patch(`/api/tasks/${targetTask.id}/toggle`);
    assert.equal(toggleRes2.status, 200);
    assert.equal(toggleRes2.body.completed, initialStatus);
  });

  test('PATCH /api/tasks/:id/toggle returns 404 for unknown task', async () => {
    const res = await request(app).patch('/api/tasks/non-existent-id/toggle');
    assert.equal(res.status, 404);
    assert.ok(res.body.error);
  });
});
