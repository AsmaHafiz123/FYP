const jwt = require('jsonwebtoken');
const secretKey = 'your-secret-key';  // Use your actual secret key

const jwtVerify = (req, res, next) => {
  const token = req.headers.authorization?.split(' ')[1];  // Extract token from Authorization header

  if (!token) {
    return res.status(403).json({ message: 'No token provided' });
  }

  jwt.verify(token, secretKey, (err, decoded) => {
    if (err) {
      return res.status(403).json({ message: 'Invalid or expired token' });
    }
    req.user = decoded;  // Store the decoded token in the request object
    next();  // Proceed to the next middleware or endpoint
  });
};

module.exports = jwtVerify;
