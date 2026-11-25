function errorHandler(err, req, res, next) {
  console.error('[Error]', err.stack || err);

  const status = err.status || err.statusCode || 500;
  const message = err.message || 'Internal Server Error';

  res.status(status).json({
    error: message,
    ...(process.env.NODE_ENV === 'development' && { stack: err.stack })
  });
}

function notFoundHandler(req, res) {
  res.status(404).json({
    error: `Cannot ${req.method} ${req.originalUrl} - Route not found`
  });
}

module.exports = {
  errorHandler,
  notFoundHandler
};
