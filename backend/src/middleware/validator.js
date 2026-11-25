const VALID_PRIORITIES = ['low', 'medium', 'high'];
const VALID_ASSIGNEES = ['aarav', 'priya', 'rohan'];

function isValidDateString(dateStr) {
  if (typeof dateStr !== 'string') return false;
  // Match YYYY-MM-DD
  const regex = /^\d{4}-\d{2}-\d{2}$/;
  if (!regex.test(dateStr)) return false;

  const [yearStr, monthStr, dayStr] = dateStr.split('-');
  const year = parseInt(yearStr, 10);
  const month = parseInt(monthStr, 10);
  const day = parseInt(dayStr, 10);

  if (month < 1 || month > 12) return false;
  if (day < 1 || day > 31) return false;

  const dateObj = new Date(Date.UTC(year, month - 1, day));
  return (
    dateObj.getUTCFullYear() === year &&
    dateObj.getUTCMonth() === month - 1 &&
    dateObj.getUTCDate() === day
  );
}

function validateCreateTask(req, res, next) {
  const errors = {};
  const { title, description, dueDate, priority, assigneeId } = req.body;

  // Title validation
  if (title === undefined || title === null) {
    errors.title = 'Title is required and cannot be empty';
  } else if (typeof title !== 'string') {
    errors.title = 'Title must be a text string';
  } else if (title.trim().length === 0) {
    errors.title = 'Title is required and cannot be empty';
  }

  // Priority validation
  if (!priority) {
    errors.priority = 'Priority is required';
  } else if (!VALID_PRIORITIES.includes(String(priority).toLowerCase())) {
    errors.priority = 'Priority must be one of: low, medium, high';
  }

  // Assignee validation
  if (!assigneeId) {
    errors.assigneeId = 'Assignee is required';
  } else if (!VALID_ASSIGNEES.includes(String(assigneeId).toLowerCase())) {
    errors.assigneeId = 'Assignee must be one of: aarav, priya, rohan';
  }

  // Due Date validation (optional)
  if (dueDate !== undefined && dueDate !== null && dueDate !== '') {
    if (!isValidDateString(dueDate)) {
      errors.dueDate = 'Due date must be a valid date in YYYY-MM-DD format or null';
    }
  }

  // Description validation (optional)
  if (description !== undefined && description !== null) {
    if (typeof description !== 'string') {
      errors.description = 'Description must be a text string';
    }
  }

  if (Object.keys(errors).length > 0) {
    return res.status(400).json({
      error: 'Validation failed',
      errors
    });
  }

  // Normalize cleaned fields onto req.validatedBody
  req.validatedBody = {
    title: title.trim(),
    description: (description !== undefined && description !== null && typeof description === 'string' && description.trim().length > 0)
      ? description.trim()
      : null,
    dueDate: (dueDate && isValidDateString(dueDate)) ? dueDate : null,
    priority: String(priority).toLowerCase(),
    assigneeId: String(assigneeId).toLowerCase(),
    completed: Boolean(req.body.completed)
  };

  next();
}

module.exports = {
  VALID_PRIORITIES,
  VALID_ASSIGNEES,
  isValidDateString,
  validateCreateTask
};
