// ============================================
// QUIZARD BACKEND SERVER
// ============================================

const express = require('express');
const mysql = require('mysql2/promise');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const cors = require('cors');

const app = express();
app.use(express.json());
app.use(cors());

// ============================================
// DATABASE CONFIGURATION
// ============================================
const dbConfig = {
    host: 'localhost',
    user: 'root',              // Change this to your MySQL username
    password: '',              // Change this to your MySQL password
    database: 'quizard'     // Your database name
};

// JWT Secret Key (change this to something secure!)
const JWT_SECRET = 'quizard_secret_key_2024_change_this';

// Create database connection pool
const pool = mysql.createPool(dbConfig);

// Test database connection
pool.getConnection()
    .then(connection => {
        console.log('✅ Database connected successfully!');
        connection.release();
    })
    .catch(err => {
        console.error('❌ Database connection failed:', err.message);
    });

// ============================================
// AUTHENTICATION ENDPOINTS
// ============================================

// SIGNUP ENDPOINT
app.post('/api/signup', async (req, res) => {
    try {
        const { name, email, password } = req.body;

        // Validation
        if (!name || !email || !password) {
            return res.json({
                success: false,
                message: 'All fields are required'
            });
        }

        if (password.length < 6) {
            return res.json({
                success: false,
                message: 'Password must be at least 6 characters long'
            });
        }

        // Check if email already exists
        const [existingUsers] = await pool.query(
            'SELECT id FROM users WHERE email = ?',
            [email]
        );

        if (existingUsers.length > 0) {
            return res.json({
                success: false,
                message: 'Email already registered'
            });
        }

        // Hash password
        const hashedPassword = await bcrypt.hash(password, 10);

        // Insert new user
        const [result] = await pool.query(
            'INSERT INTO users (name, email, password) VALUES (?, ?, ?)',
            [name, email, hashedPassword]
        );

        console.log(`✅ New user registered: ${email}`);

        res.json({
            success: true,
            message: 'Account created successfully'
        });

    } catch (error) {
        console.error('Signup error:', error);
        res.json({
            success: false,
            message: 'Server error. Please try again later.'
        });
    }
});

// LOGIN ENDPOINT
app.post('/api/login', async (req, res) => {
    try {
        const { email, password } = req.body;

        // Validation
        if (!email || !password) {
            return res.json({
                success: false,
                message: 'Email and password are required'
            });
        }

        // Find user by email
        const [users] = await pool.query(
            'SELECT id, name, email, password, is_active FROM users WHERE email = ?',
            [email]
        );

        if (users.length === 0) {
            return res.json({
                success: false,
                message: 'Invalid email or password'
            });
        }

        const user = users[0];

        // Check if account is active
        if (!user.is_active) {
            return res.json({
                success: false,
                message: 'Account is disabled'
            });
        }

        // Verify password
        const isPasswordValid = await bcrypt.compare(password, user.password);

        if (!isPasswordValid) {
            return res.json({
                success: false,
                message: 'Invalid email or password'
            });
        }

        // Update last login
        await pool.query(
            'UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE id = ?',
            [user.id]
        );

        // Generate JWT token
        const token = jwt.sign(
            { userId: user.id, email: user.email },
            JWT_SECRET,
            { expiresIn: '7d' }
        );

        // Store session in database
        await pool.query(
            'INSERT INTO user_sessions (user_id, token, expires_at) VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 7 DAY))',
            [user.id, token]
        );

        console.log(`✅ User logged in: ${email}`);

        res.json({
            success: true,
            token: token,
            name: user.name,
            email: user.email,
            message: 'Login successful'
        });

    } catch (error) {
        console.error('Login error:', error);
        res.json({
            success: false,
            message: 'Server error. Please try again later.'
        });
    }
});

// LOGOUT ENDPOINT
app.post('/api/logout', async (req, res) => {
    try {
        const token = req.headers.authorization?.split(' ')[1];

        if (token) {
            // Delete session from database
            await pool.query('DELETE FROM user_sessions WHERE token = ?', [token]);
            console.log('✅ User logged out');
        }

        res.json({
            success: true,
            message: 'Logged out successfully'
        });

    } catch (error) {
        console.error('Logout error:', error);
        res.json({
            success: false,
            message: 'Logout failed'
        });
    }
});

// ============================================
// TOKEN VERIFICATION MIDDLEWARE
// ============================================
const verifyToken = async (req, res, next) => {
    try {
        const token = req.headers.authorization?.split(' ')[1];

        if (!token) {
            return res.status(401).json({
                success: false,
                message: 'Access token required'
            });
        }

        // Verify JWT
        const decoded = jwt.verify(token, JWT_SECRET);

        // Check if session exists in database
        const [sessions] = await pool.query(
            'SELECT user_id FROM user_sessions WHERE token = ? AND expires_at > NOW()',
            [token]
        );

        if (sessions.length === 0) {
            return res.status(401).json({
                success: false,
                message: 'Invalid or expired token'
            });
        }

        req.userId = decoded.userId;
        next();

    } catch (error) {
        return res.status(401).json({
            success: false,
            message: 'Invalid token'
        });
    }
};

// ============================================
// USER ENDPOINTS
// ============================================

// Get all users
app.get('/api/users', async (req, res) => {
    try {
        const [users] = await pool.query(
            'SELECT id, name, email, created_at FROM users WHERE is_active = TRUE ORDER BY created_at DESC'
        );
        res.json(users);
    } catch (error) {
        console.error('Error fetching users:', error);
        res.status(500).json({ error: 'Failed to fetch users' });
    }
});

// Get user profile (protected route example)
app.get('/api/profile', verifyToken, async (req, res) => {
    try {
        const [users] = await pool.query(
            'SELECT id, name, email, created_at, last_login FROM users WHERE id = ?',
            [req.userId]
        );

        if (users.length === 0) {
            return res.status(404).json({
                success: false,
                message: 'User not found'
            });
        }

        res.json({
            success: true,
            user: users[0]
        });

    } catch (error) {
        console.error('Profile error:', error);
        res.status(500).json({
            success: false,
            message: 'Server error'
        });
    }
});

// ============================================
// PLACEHOLDER ENDPOINTS FOR FUTURE FEATURES
// (These are for your existing script.js functionality)
// ============================================

// Upload reviewer file (placeholder)
app.post('/api/reviewers/upload', verifyToken, async (req, res) => {
    // TODO: Implement file upload logic
    res.json({
        success: true,
        id: 1,
        fileName: 'sample.pdf',
        message: 'File upload endpoint - to be implemented'
    });
});

// Get summary (placeholder)
app.get('/api/reviewers/:id/summary', verifyToken, async (req, res) => {
    // TODO: Implement summary generation
    res.json({
        summary: 'This is a sample summary. Implement your AI summary generation here.'
    });
});

// Generate flashcards (placeholder)
app.post('/api/reviewers/:id/flashcards', verifyToken, async (req, res) => {
    // TODO: Implement flashcard generation
    res.json({
        flashcards: [
            { term: 'Sample Term 1', definition: 'Sample Definition 1' },
            { term: 'Sample Term 2', definition: 'Sample Definition 2' }
        ]
    });
});

// Generate quiz (placeholder)
app.post('/api/reviewers/:id/quiz', verifyToken, async (req, res) => {
    // TODO: Implement quiz generation
    res.json({
        questions: [
            {
                question: 'Sample Question 1?',
                options: ['Answer A', 'Answer B', 'Answer C', 'Answer D'],
                correctAnswer: 'Answer A'
            }
        ]
    });
});

// ============================================
// START SERVER
// ============================================
const PORT = 3000; // Auth server on port 3000 (Spring Boot uses 8080)
app.listen(PORT, () => {
    console.log(`
╔════════════════════════════════════════╗
║   🚀 QUIZARD AUTH SERVER RUNNING      ║
║   📍 http://localhost:${PORT}           ║
║   ✅ Ready to accept connections       ║
║   📝 Spring Boot runs on port 8080     ║
╚════════════════════════════════════════╝
    `);
});

// Handle graceful shutdown
process.on('SIGINT', async () => {
    console.log('\n⏳ Shutting down server...');
    await pool.end();
    console.log('✅ Database connections closed');
    process.exit(0);
});