const sqlite3 = require('sqlite3').verbose();
const path = require('path');
const fs = require('fs');
const crypto = require('crypto');
const { seedTasks } = require('./seedData');

const DB_DIR = path.resolve(__dirname, '../../database');
const DEFAULT_DB_PATH = path.join(DB_DIR, 'taskflow.db');

class Database {
  constructor(dbPath = DEFAULT_DB_PATH) {
    this.dbPath = dbPath;
    this.db = null;
  }

  async connect() {
    if (this.dbPath !== ':memory:') {
      if (!fs.existsSync(DB_DIR)) {
        fs.mkdirSync(DB_DIR, { recursive: true });
      }
    }

    return new Promise((resolve, reject) => {
      this.db = new sqlite3.Database(this.dbPath, (err) => {
        if (err) {
          console.error(`[DB] Failed to connect to SQLite at ${this.dbPath}:`, err);
          return reject(err);
        }
        resolve(this.db);
      });
    });
  }

  async init() {
    if (!this.db) {
      await this.connect();
    }

    // Enable foreign keys and WAL mode for better concurrency
    await this.run('PRAGMA foreign_keys = ON');
    if (this.dbPath !== ':memory:') {
      await this.run('PRAGMA journal_mode = WAL');
    }

    // Create tasks table
    await this.run(`
      CREATE TABLE IF NOT EXISTS tasks (
        id TEXT PRIMARY KEY,
        title TEXT NOT NULL,
        description TEXT,
        dueDate TEXT,
        priority TEXT NOT NULL CHECK(priority IN ('low', 'medium', 'high')),
        assigneeId TEXT NOT NULL CHECK(assigneeId IN ('aarav', 'priya', 'rohan')),
        completed INTEGER NOT NULL DEFAULT 0,
        createdAt TEXT NOT NULL
      )
    `);

    // Check if tasks table is empty, and seed only if empty
    const countRow = await this.get('SELECT COUNT(*) as count FROM tasks');
    if (countRow && countRow.count === 0) {
      console.log('[DB] Tasks table is empty. Seeding initial 15 tasks from assessment...');
      await this.seed();
      console.log('[DB] Seeding completed successfully.');
    } else {
      console.log(`[DB] Database already contains ${countRow.count} tasks. Preserving existing data.`);
    }
  }

  async seed() {
    const insertStmt = `
      INSERT INTO tasks (id, title, description, dueDate, priority, assigneeId, completed, createdAt)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    `;

    for (let i = 0; i < seedTasks.length; i++) {
      const task = seedTasks[i];
      const id = crypto.randomUUID();
      const createdAt = new Date(Date.now() - (seedTasks.length - i) * 60000).toISOString();
      await this.run(insertStmt, [
        id,
        task.title,
        task.description,
        task.dueDate,
        task.priority,
        task.assigneeId,
        task.completed ? 1 : 0,
        createdAt
      ]);
    }
  }

  run(sql, params = []) {
    return new Promise((resolve, reject) => {
      this.db.run(sql, params, function (err) {
        if (err) return reject(err);
        resolve({ lastID: this.lastID, changes: this.changes });
      });
    });
  }

  get(sql, params = []) {
    return new Promise((resolve, reject) => {
      this.db.get(sql, params, (err, row) => {
        if (err) return reject(err);
        resolve(row);
      });
    });
  }

  all(sql, params = []) {
    return new Promise((resolve, reject) => {
      this.db.all(sql, params, (err, rows) => {
        if (err) return reject(err);
        resolve(rows);
      });
    });
  }

  close() {
    return new Promise((resolve, reject) => {
      if (!this.db) return resolve();
      this.db.close((err) => {
        if (err) return reject(err);
        this.db = null;
        resolve();
      });
    });
  }
}

// Map SQLite row to domain JSON Task
function mapTaskRow(row) {
  if (!row) return null;
  return {
    id: row.id,
    title: row.title,
    description: row.description,
    dueDate: row.dueDate,
    priority: row.priority,
    assigneeId: row.assigneeId,
    completed: row.completed === 1,
    createdAt: row.createdAt
  };
}

module.exports = {
  Database,
  mapTaskRow,
  defaultDatabase: new Database()
};
