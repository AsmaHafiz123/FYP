// Required Libraries and Middleware Setup
const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');
const mysql = require('mysql2/promise');
const bcrypt = require('bcrypt');    //Password encryptions
const http = require('http');
const { Server } = require('socket.io');
const nodemailer = require('nodemailer');
const crypto = require('crypto');
const cron = require('node-cron');
const { check, validationResult } = require('express-validator');
const rateLimit = require('express-rate-limit');
const router = express.Router();
const QRCode = require('qrcode');
const jwt = require('jsonwebtoken');
const jwtVerify = require('./middleware/jwtVerify');  // Import the JWT middleware
const multer = require('multer');
const path = require('path');


// Initialize App and Server
const app = express();
const server = http.createServer(app);
const io = new Server(server);

// Middleware
app.use(cors());
app.use(bodyParser.json());

app.use(express.json());
app.use(express.static('public'));
app.use(express.json()); // This will parse the incoming JSON data

// Port
const PORT = 3000;

// MySQL Connection Setup
const db = mysql.createPool({
    host: 'localhost',
    user: 'root',
    password: 'app2112106*#',
    database: 'parental_patrol',
    waitForConnections: true,
    connectionLimit: 10,    //max 10 connections to the sql at a time 
    queueLimit: 0,
});

// Nodemailer Configuration
const transporter = nodemailer.createTransport({
    service: 'gmail',
    auth: {
        user: 'parentalpatrolchildrensafety@gmail.com',
        pass: 'lece jpdt ingm iqfr', // Use your App Password for Gmail
    },
});



app.get('/api/test', (req, res) => {
    res.status(200).send('Backend connection successful');
});

// Routes
app.post('/role-selection', (req, res) => {
    const { role } = req.body;
    req.session = { role };
    res.status(200).send('Role saved in session');
});

app.get('/parent/checkEmail', async (req, res) => {
    const { email } = req.query;
    if (!email) return res.status(400).json({ message: 'Email is required.' });

    try {
        const [rows] = await db.query('SELECT * FROM parent WHERE email = ?', [email]);
        res.status(200).json(rows.length > 0);
    } catch (err) {
        console.error('Error checking email in parent:', err);
        res.status(500).json({ message: 'Server error during email check in parent.' });
    }
});

app.get('/child/checkEmail', async (req, res) => {
    const { email } = req.query;
    if (!email) return res.status(400).json({ message: 'Email is required.' });

    try {
        const [rows] = await db.query('SELECT * FROM child WHERE email = ?', [email]);
        res.status(200).json(rows.length > 0);
    } catch (err) {
        console.error('Error checking email in child:', err);
        res.status(500).json({ message: 'Server error during email check in child.' });
    }
});

// User login
app.post('/login', async (req, res) => {
    const { email, password, role } = req.body;

    // Ensure all required fields are provided
    if (!email || !password || !role) {
        return res.status(400).json({ success: false, message: 'Please provide email, password, and role.' });
    }

    try {
        // Check the appropriate table based on the role
        let checkUserQuery = '';
        if (role === 'parent') {
            checkUserQuery = 'SELECT * FROM parent WHERE email = ?';
        } else if (role === 'child') {
            checkUserQuery = 'SELECT * FROM child WHERE email = ?';
        } else {
            return res.status(400).json({ success: false, message: 'Invalid role provided.' });
        }

        // Query the correct table based on role
        const [rows] = await db.query(checkUserQuery, [email]);

        // If no user found, send a response
        if (rows.length === 0) {
            return res.status(404).json({ success: false, message: "User not found. Sign up first." });
        }

        const user = rows[0];

        // Compare the provided password with the stored password
        const isPasswordCorrect = await bcrypt.compare(password, user.password);
        
        if (!isPasswordCorrect) {
            return res.status(401).json({ success: false, message: 'Incorrect password.' });
        }

        // Respond with successful login
        return res.status(200).json({
            success: true,
            message: 'Login successful!',
            user: { name: user.name, email: user.email }
        });
    } catch (err) {
        console.error('Error during login: ', err);
        return res.status(500).json({ success: false, message: 'Login failed due to server error.' });
    }
});

// Signup route
app.post('/signup', async (req, res) => {
    console.log('Signup request received:', req.body);
    const { name, email, password, role } = req.body;
    const table = role === 'parent' ? 'parent' : 'child';
 
    try {
        // Check based on role
        if (role === 'parent') {
            const [parentRows] = await db.query('SELECT * FROM parent WHERE email = ?', [email]);
            if (parentRows.length > 0) {
                return res.status(400).json({
                    success: false,
                    message: 'Already have an account. Please login.'
                });
            }
 
            const [childRows] = await db.query('SELECT * FROM child WHERE email = ?', [email]);
            if (childRows.length > 0) {
                return res.status(400).json({
                    success: false, 
                    message: 'This email is already used as a child account'
                });
            }
        } else { // Child signup
            const [childRows] = await db.query('SELECT * FROM child WHERE email = ?', [email]);
            if (childRows.length > 0) {
                return res.status(400).json({
                    success: false,
                    message: 'Already have an account. Please login.'
                });
            }
 
            const [parentRows] = await db.query('SELECT * FROM parent WHERE email = ?', [email]);
            if (parentRows.length > 0) {
                return res.status(400).json({
                    success: false,
                    message: 'This email is already used as a parent account'
                });
            }
        }
 
        // Hash password and insert user
        const hashedPassword = await bcrypt.hash(password, 10);
        await db.query(
            `INSERT INTO ${table} (name, email, password) VALUES (?, ?, ?)`,
            [name, email, hashedPassword]
        );
 
        const token = jwt.sign(
            { email, role, name },
            'your_jwt_secret_key',
            { expiresIn: '24h' }
        );
 
        console.log('User signed up successfully:', email);
 
        res.status(200).json({
            success: true,
            message: 'Signup successful',
            token,
            user: { email, name, role }
        });
 
    } catch (err) {
        console.error('Error during signup:', err);
        res.status(500).json({
            success: false,
            message: 'Signup failed',
            error: err.message
        });
    }
 });

// Forgot Password Route
app.post('/forgot-password', async (req, res) => {
    const { email } = req.body;
    const resetCode = Math.floor(100000 + Math.random() * 900000);
    const expirationTime = new Date(Date.now() + 5 * 60 * 1000); // 5 minutes

    try {
        await db.query(
            `INSERT INTO reset_codes (email, reset_code, expiration_time) 
             VALUES (?, ?, ?) 
             ON DUPLICATE KEY UPDATE reset_code = ?, expiration_time = ?`,
            [email, resetCode, expirationTime, resetCode, expirationTime]
        );

        const mailOptions = {
            from: 'parentalpatrolchildrensafety@gmail.com',
            to: email,
            subject: 'Password Reset Code',
            text: `Your password reset code is: ${resetCode}`,
        };

        transporter.sendMail(mailOptions, (error) => {
            if (error) return res.status(500).json({ message: 'Error sending reset email.' });
            res.status(200).json({ message: 'Reset code sent to your email.' });
        });
    } catch (err) {
        console.error('Error during forgot password:', err);
        res.status(500).json({ message: 'Error saving reset code.' });
    }
});


// Endpoint to verify reset code
app.post('/verify-reset-code', async (req, res) => {
    const { email, code } = req.body;

    // Validate input
    if (!email || !code) {
        return res.status(400).json({ 
            success: false, 
            message: "Email and code are required." 
        });
    }

    try {
        // Query to check if the reset code is valid and not expired
        const [rows] = await db.query(
            `SELECT * 
             FROM reset_codes 
             WHERE email = ? AND reset_code = ? AND expiration_time > NOW()`,
            [email, code]
        );

        // Check if the reset code exists and is valid
        if (rows.length > 0) {
            return res.status(200).json({ 
                success: true, 
                message: "Code verified successfully. Redirecting to password reset page." 
            });
        } else {
            return res.status(401).json({ 
                success: false, 
                message: "Invalid or expired reset code. Please try again." 
            });
        }
    } catch (err) {
        // Handle any database or server errors
        console.error("Error verifying reset code:", err);
        return res.status(500).json({ 
            success: false, 
            message: "Internal server error. Please try again later." 
        });
    }
});

async function deleteExpiredResetCodes() {
    try {
        await db.query(
            `DELETE FROM reset_codes WHERE expiration_time < NOW()`
        );
        console.log("Expired reset codes deleted.");
    } catch (err) {
        console.error("Error deleting expired reset codes:", err);
    }
}

// Call the function where necessary
deleteExpiredResetCodes();

// Endpoint to update password
app.post('/update-password', async (req, res) => {
    const { email, newPassword } = req.body;
    if (!email || !newPassword) {
        return res.status(400).json({ success: false, message: 'Please provide both email and new password.' });
    }

    try {
        const hashedPassword = await bcrypt.hash(newPassword, 10);

        // First try to update in the 'parent' table
        let updateResult = await db.query('UPDATE parent SET password = ? WHERE email = ?', [hashedPassword, email]);
        
        // If no rows are updated in the parent table, try updating in the 'child' table
        if (updateResult[0].affectedRows === 0) {
            updateResult = await db.query('UPDATE child SET password = ? WHERE email = ?', [hashedPassword, email]);
        }

        if (updateResult[0].affectedRows === 0) {
            return res.status(404).json({ success: false, message: 'User not found.' });
        }

        return res.status(200).json({ success: true, message: 'Password updated successfully!' });
    } catch (err) {
        console.error('Error updating password: ', err);
        return res.status(500).json({ success: false, message: 'Password update failed due to server error.' });
    }
});

// Remove Expired Reset Codes
cron.schedule('* * * * *', async () => {
    try {
        await db.query('DELETE FROM reset_codes WHERE expiration_time < NOW()');
    } catch (err) {
        console.error('Error removing expired reset codes:', err);
    }
});


app.get('/ParentEmailQrCode', async (req, res) => {
    const email = req.query.email;

    if (!email) {
        return res.status(400).json({ success: false, message: "Email is required" });
    }

    try {
        const [rows] = await db.query('SELECT COUNT(*) AS count FROM parent WHERE email = ?', [email]);

        if (rows[0].count > 0) {
            res.json({ success: true, exists: true });
        } else {
            res.json({ success: true, exists: false });
        }
    } catch (error) {
        console.error('Database query error:', error);
        res.status(500).json({ success: false, message: 'Database error' });
    }
});
  
  const qrCode = require('qrcode');
  const { v4: uuidv4 } = require('uuid'); // Import UUID
 
// Route to generate the QR Code for the given parent and child emails
app.get('/generateQRCode', (req, res) => {
    const combinedEmails = req.query.emails;
    console.log("Generating QR Code for emails:", combinedEmails);

    if (!combinedEmails) {
        return res.status(400).json({ success: false, message: "Emails are required" });
    }

    const [parentEmail, childEmail] = combinedEmails.split(',');

    if (!parentEmail || !childEmail) {
        return res.status(400).json({ success: false, message: "Both parentEmail and childEmail are required" });
    }

    // Change format to simple JSON object
    const qrData = JSON.stringify({
        parent_email: parentEmail,
        child_email: childEmail
    });

    qrCode.toDataURL(qrData, (err, url) => {
        if (err) {
            console.error("QR Code generation error:", err);
            return res.status(500).json({ success: false, message: "Error generating QR code" });
        }
        return res.json({ success: true, qrCodeUrl: url });
    });
});
  
// Add this endpoint in your server.js file along with other endpoints
app.post('/add-device-connection', async (req, res) => {
    const { parent_email, child_email } = req.body;
    
    if (!parent_email || !child_email) {
        return res.status(400).json({ message: 'Both emails are required.' });
    }
    
    try {
        // Check if connection already exists
        const [existingConnection] = await db.query(
            'SELECT * FROM device_connections WHERE parent_email = ? AND child_email = ?',
            [parent_email, child_email]
        );
        
        if (existingConnection.length > 0) {
            // Update existing connection
            await db.query(
                `UPDATE device_connections 
                 SET status = 'disconnected', updated_at = NOW() 
                 WHERE parent_email = ? AND child_email = ?`,
                [parent_email, child_email]
            );
        } else {
            // Create new connection
            await db.query(
                `INSERT INTO device_connections 
                (parent_email, child_email, status, created_at, updated_at) 
                VALUES (?, ?, 'disconnected', NOW(), NOW())`,
                [parent_email, child_email]
            );
        }
        
        res.status(200).json({ 
            success: true, 
            message: 'Device connection recorded successfully' 
        });
        
    } catch (err) {
        console.error('Error recording device connection:', err);
        res.status(500).json({ 
            message: 'Server error during device connection recording.' 
        });
    }
});


// Store connected clients
const connectedClients = new Map();

// Socket connection handler
io.on('connection', (socket) => {
    console.log('Client connected');

    // Register child device
    socket.on('register_child', (childEmail) => {
        console.log('Child registered:', childEmail);
        connectedClients.set(childEmail, socket);
    });
    // Add this new event handler for screen data
    socket.on('screen-data', (data) => {
        // Get parent's socket based on connection
        const { child_email, screenshot, currentApp } = data;
        
        // Find parent email from device_connections
        db.query(
            'SELECT parent_email FROM device_connections WHERE child_email = ? AND status = "CONNECTED"',
            [child_email],
            (err, results) => {
                if (err || results.length === 0) return;
                
                const parentEmail = results[0].parent_email;
                const parentSocket = connectedClients.get(parentEmail);
                
                if (parentSocket) {
                    parentSocket.emit('screen-update', {
                        screenshot,
                        currentApp,
                        timestamp: new Date()
                    });
                }
            }
        );
    });
    // Handle disconnect
    socket.on('disconnect', () => {
        console.log('Client disconnected');
        // Remove from connected clients
        for (const [email, sock] of connectedClients.entries()) {
            if (sock === socket) {
                connectedClients.delete(email);
                break;
            }
        }
    });
});

// Express route to handle the connection verification
app.post('/verify-connection', async (req, res) => {
    console.log('Scanned data received:', req.body);
    const { parent_email, child_email } = req.body;

    try {
        // Extract the JWT token from Authorization header
        const authHeader = req.headers['authorization'];
        const token = authHeader && authHeader.split(' ')[1];  // Get the token after 'Bearer'
        
        if (!token) {
            return res.status(403).json({ 
                success: false, 
                message: 'Token is missing' 
            });
        }

        // Verify the JWT token and decode it to extract the email
        const decoded = jwt.verify(token, 'your_jwt_secret_key');  // Replace with your actual secret key
        const signupEmail = decoded.email;  // Extract email from the token

        console.log('Token verification:', { signupEmail, qrParentEmail: parent_email });

        // Compare the email from the QR code with the email in the token
        if (parent_email !== signupEmail) {
            console.log('Email mismatch: QR email does not match signup email');
            return res.status(403).json({ 
                success: false,
                message: 'Please use the same email as used during signup'
            });
        }

        console.log(`Processing connection request for Parent: ${parent_email}, Child: ${child_email}`);

        // If email matches, proceed with your database logic (for connecting devices)
        // Example: Checking the device_connections table
        const [rows] = await db.query(
            'SELECT * FROM device_connections WHERE parent_email = ? AND child_email = ? AND status = "DISCONNECTED"',
            [parent_email, child_email]
        );

        if (rows.length === 0) {
            console.log('No pending connection found');
            return res.status(404).json({ 
                success: false, 
                message: 'No pending connection request found' 
            });
        }

        console.log('Connection found, updating status to Connected');

        // Update the connection status in the database
        await db.query(
            'UPDATE device_connections SET status = ?, updated_at = NOW() WHERE parent_email = ? AND child_email = ?',
            ['CONNECTED', parent_email, child_email]
        );

        console.log('Connection status updated successfully');
        // Add this WebSocket notification code here
        const childSocket = connectedClients.get(child_email);
        if (childSocket) {
               console.log('Notifying child device about successful connection');
               childSocket.emit('connection_success');
            }
        return res.status(200).json({ 
            success: true,
            message: 'Connection established successfully'
        });

    } catch (error) {
        // Handle specific errors
        if (error.name === 'JsonWebTokenError') {
            console.error('Invalid token:', error);
            return res.status(401).json({
                success: false,
                message: 'Invalid token, please delete your account and signup again'
            });
        }
        if (error.name === 'TokenExpiredError') {
            console.error('Token expired:', error);
            return res.status(401).json({
                success: false,
                message: 'Signup session expired, please delete your account and signup again'
            });
        }

        console.error('Server error:', error);
        return res.status(500).json({ 
            success: false,
            message: 'Server error, please try again'
        });
    }
});


// Socket connection handler
io.on('connection', (socket) => {
    console.log('Client connected');

    // Register child device
    socket.on('register_child', (childEmail) => {
        console.log('Child registered:', childEmail);
        connectedClients.set(childEmail, socket);
    });

    // Handle screen data from child
    socket.on('screen-data', (data) => {
        const { child_email, screenshot, currentApp } = data;

        // Save to database and notify parent
        db.query(
            'SELECT parent_email FROM device_connections WHERE child_email = ? AND status = "CONNECTED"',
            [child_email],
            (err, results) => {
                if (err || results.length === 0) return;

                const parentEmail = results[0].parent_email;
                const parentSocket = connectedClients.get(parentEmail);

                if (parentSocket) {
                    parentSocket.emit('screen-update', {
                        screenshot,
                        currentApp,
                        timestamp: new Date()
                    });
                }
            }
        );
    });

    // Handle disconnect
    socket.on('disconnect', () => {
        console.log('Client disconnected');
        // Remove from connected clients
        for (const [email, sock] of connectedClients.entries()) {
            if (sock === socket) {
                connectedClients.delete(email);
                break;
            }
        }
    });
});


// Configure multer for file uploads
const fs = require('fs').promises;
const uploadsDir = path.join(__dirname, 'Uploads');
fs.mkdir(uploadsDir, { recursive: true }).catch(err => {
  console.error('Error creating uploads directory:', err);
});

app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ limit: '10mb', extended: true }));

app.post('/screen-data', async (req, res) => {
  const { id, child_email, screenshot, current_app, created_at } = req.body;

  if (!child_email || !screenshot || !current_app) {
    return res.status(400).json({ 
      success: false, 
      message: 'Required fields (child_email, screenshot, current_app) are missing' 
    });
  }

  try {
    const buffer = Buffer.from(screenshot, 'base64');
    const fileName = `screenshot_${child_email}_${Date.now()}.jpg`;
    const filePath = path.join(uploadsDir, fileName);

    await fs.writeFile(filePath, buffer);
    console.log(`Screenshot saved to disk for ${child_email}: ${filePath}`);

    const [result] = await db.query(
      `INSERT INTO screenshots (id, child_email, current_app, created_at, file_path) 
       VALUES (?, ?, ?, ?, ?)`,
      [id || null, child_email, current_app, created_at || new Date().toISOString(), filePath]
    );
    console.log(`Screenshot record saved for ${child_email}, ID: ${result.insertId}`);

    res.status(200).json({ 
      success: true, 
      message: 'Screenshot saved successfully',
      insertId: result.insertId
    });
  } catch (err) {
    console.error('Error saving screenshot:', {
      message: err.message,
      stack: err.stack,
      sqlError: err.sqlMessage || 'N/A'
    });
    res.status(500).json({ 
      success: false, 
      message: 'Server error occurred', 
      error: err.message 
    });
  }
});

// Schedule cleanup job to delete old screenshots (runs daily at midnight)
cron.schedule('0 0 * * *', async () => {
  try {
    const [rows] = await db.query('SELECT file_path FROM screenshots WHERE created_at < NOW() - INTERVAL 7 DAY');
    for (const row of rows) {
      await fs.unlink(row.file_path).catch(err => console.error('Error deleting file:', err));
    }
    await db.query('DELETE FROM screenshots WHERE created_at < NOW() - INTERVAL 7 DAY');
    console.log('Old screenshots deleted');
  } catch (err) {
    console.error('Error in cleanup job:', err);
  }
});
















                                                                                                                                                                                                                                                                                              // Example IMEI validation function

// Start Server
server.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
