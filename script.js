const API_BASE_URL = 'http://localhost:8080/api';  // Spring Boot for file operations
const AUTH_API_URL = 'http://localhost:3000/api';  // Node.js for authentication

let currentReviewerId = null;
let currentFlashcardIndex = 0;
let flashcards = [];
let questions = [];
let showingAnswer = false;
let backendConnected = false;

// Get auth token from localStorage
function getAuthToken() {
    return localStorage.getItem('userToken');
}

// Get auth headers
function getAuthHeaders() {
    const headers = { 'Content-Type': 'application/json' };
    const token = getAuthToken();
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    return headers;
}

async function testBackendConnection() {
    try {
        // Test auth server connection (Node.js on port 3000)
        const authResponse = await fetch(`${AUTH_API_URL}/users`, {
            method: 'GET',
            headers: getAuthHeaders()
        });

        if (authResponse.ok) {
            console.log('✅ Auth server connected successfully!');
            const users = await authResponse.json();
            displayUsers(users);
        } else {
            console.warn('⚠️ Auth server responded with:', authResponse.status);
        }

        // Test file server connection (Spring Boot on port 8080)
        const fileResponse = await fetch(`${API_BASE_URL}/quiz/test`, {
            method: 'GET'
        });

        if (fileResponse.ok) {
            console.log('✅ File server connected successfully!');
            backendConnected = true;
            return true;
        } else {
            console.warn('⚠️ File server responded with:', fileResponse.status);
            backendConnected = false;
            return false;
        }
    } catch (error) {
        console.error('❌ Backend connection failed:', error);
        backendConnected = false;
        return false;
    }
}

function displayUsers(users) {
    const userList = document.getElementById('userList');
    if (!userList) return;

    userList.innerHTML = users
        .map(user => `<li>${user.name} (${user.email})</li>`)
        .join('');
}

function showPage(pageId) {
    // Remove active from all pages except auth
    document.querySelectorAll('.page').forEach(page => {
        if (page.id !== 'auth') {
            page.classList.remove('active');
            page.style.display = 'none';
        }
    });
    
    // Remove active from all nav buttons
    document.querySelectorAll('.nav-btn').forEach(btn => btn.classList.remove('active'));

    // Show the target page
    const targetPage = document.getElementById(pageId);
    if (targetPage) {
        targetPage.classList.add('active');
        targetPage.style.display = 'block';
    }

    // Find and activate the corresponding nav button
    const navButtons = document.querySelectorAll('.nav-btn');
    navButtons.forEach(btn => {
        const btnText = btn.textContent.trim().toLowerCase();
        if (btnText.includes(pageId.toLowerCase())) {
            btn.classList.add('active');
        }
    });

    // Load content if needed
    if (pageId === 'flashcard' && flashcards.length > 0) loadFlashcards();
    if (pageId === 'quiz' && questions.length > 0) startQuiz();
}

async function handleFileUpload(event) {
    const file = event.target.files[0];
    if (!file) return;

    const formData = new FormData();
    formData.append('file', file);

    const uploadArea = document.querySelector('.upload-area');
    uploadArea.innerHTML = '<div class="loading active"><div class="spinner"></div><p>Uploading and processing file...</p></div>';

    try {
        // Add Authorization header for file upload
        const headers = {};
        const token = getAuthToken();
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        console.log('Uploading to:', `${API_BASE_URL}/reviewers/upload`);
        console.log('With token:', token ? 'Yes' : 'No');

        const response = await fetch(`${API_BASE_URL}/reviewers/upload`, { 
            method: 'POST', 
            headers: headers,
            body: formData 
        });
        
        console.log('Upload response status:', response.status);
        
        if (response.status === 401) {
            throw new Error('Session expired - please login again');
        }
        
        if (!response.ok) {
            const errorText = await response.text();
            console.error('Upload error response:', errorText);
            throw new Error(`Upload failed with status: ${response.status}`);
        }

        const data = await response.json();
        console.log('Upload response data:', data);
        
        // Handle different response formats
        currentReviewerId = data.id || data.reviewerId || data.reviewer?.id;
        
        if (!currentReviewerId) {
            console.error('No reviewer ID in response:', data);
            throw new Error('Invalid response from server');
        }

        uploadArea.innerHTML = `
            <div class="upload-success" style="background: linear-gradient(135deg, #10b981 0%, #059669 100%); color: white; padding: 2rem; border-radius: 1.5rem; text-align: center;">
                <div class="success-icon" style="font-size: 3rem; margin-bottom: 1rem;">✅</div>
                <h2>File uploaded successfully!</h2>
                <p>File: ${data.fileName || data.filename || file.name}</p>
                <p>Generating summary...</p>
            </div>
        `;
        
        // Remove the onclick handler from upload area to prevent errors
        uploadArea.onclick = null;
        uploadArea.style.cursor = 'default';
        
        // Load summary and show prompt after
        await loadSummary(currentReviewerId);
        showConversionPrompt();

    } catch (error) {
        console.error('Upload error:', error);
        
        if (error.message.includes('Session expired')) {
            uploadArea.innerHTML = `
                <div class="upload-error" style="background: #fee2e2; padding: 2rem; border-radius: 1.5rem; text-align: center;">
                    <div class="error-icon" style="font-size: 3rem; margin-bottom: 1rem;">🔐</div>
                    <h2 style="color: #dc2626;">Session Expired</h2>
                    <p style="color: #991b1b;">Please logout and login again</p>
                    <button class="btn" onclick="logout()" style="margin-top: 1rem;">Logout</button>
                </div>
            `;
        } else {
            // Always try local processing as fallback
            uploadArea.innerHTML = `
                <div class="upload-error" style="background: #fef3c7; padding: 2rem; border-radius: 1.5rem; text-align: center;">
                    <div class="error-icon" style="font-size: 3rem; margin-bottom: 1rem;">⚠️</div>
                    <h2 style="color: #92400e;">Using Demo Mode</h2>
                    <p style="color: #78350f;">Processing file locally...</p>
                </div>
            `;
            
            readFileLocally(file);
        }
    }
}

function readFileLocally(file) {
    const reader = new FileReader();
    reader.onload = function(e) {
        const content = e.target.result;
        window.localContent = content;
        generateLocalSummary(content);
        
        setTimeout(() => showConversionPrompt(), 500);
    };
    reader.readAsText(file);
}

function generateLocalSummary(content) {
    const sentences = content.split(/[.!?]+/).filter(s => s.trim().length > 20);
    const keyPoints = sentences.slice(0, Math.min(5, sentences.length));
    const summary = '📋 KEY POINTS:\n\n' + keyPoints.map((s, i) => `${i + 1}. ${s.trim()}.`).join('\n\n');

    const summaryBox = document.getElementById('summaryBox');
    if (summaryBox) {
        summaryBox.innerHTML = `<pre>${summary}</pre>`;
        
        // Auto-navigate to summary page
        showPageById('summary');
    }
}

async function loadSummary(reviewerId) {
    const summaryBox = document.getElementById('summaryBox');
    const loading = document.querySelector('#summary .loading');
    if (loading) loading.style.display = 'block';

    try {
        console.log('Loading summary for reviewer ID:', reviewerId);

        const response = await fetch(`${API_BASE_URL}/reviewers/${reviewerId}/summary`, {
            method: 'GET',
            headers: getAuthHeaders()
        });
        
        console.log('Summary response status:', response.status);
        
        if (!response.ok) {
            const errorText = await response.text();
            console.error('Summary error:', errorText);
            throw new Error('Failed to load summary');
        }

        const data = await response.json();
        console.log('Summary data received:', data);
        
        // Handle different response formats
        const summaryText = data.summary || data.content || data.text || JSON.stringify(data, null, 2);
        
        summaryBox.innerHTML = `<pre>${summaryText}</pre>`;
        
        // Auto-navigate to summary page
        showPageById('summary');
        
    } catch (error) {
        console.error('Error loading summary:', error);
        
        // If we have local content, use it
        if (window.localContent) {
            generateLocalSummary(window.localContent);
        } else {
            summaryBox.innerHTML = `
                <div style="text-align: center; padding: 2rem; color: #dc2626;">
                    <h3>Failed to load summary</h3>
                    <p>${error.message}</p>
                    <button class="btn" onclick="location.reload()">Try Again</button>
                </div>
            `;
        }
    } finally {
        if (loading) loading.style.display = 'none';
    }
}

// Helper function to show page without event dependency
function showPageById(pageId) {
    console.log('Navigating to page:', pageId);
    
    // Hide auth page if visible
    const authPage = document.getElementById('auth');
    if (authPage) {
        authPage.classList.remove('active');
        authPage.style.display = 'none';
    }

    // Hide all other pages
    document.querySelectorAll('.page').forEach(page => {
        if (page.id !== 'auth') {
            page.classList.remove('active');
            page.style.display = 'none';
        }
    });
    
    // Remove active from all nav buttons
    document.querySelectorAll('.nav-btn').forEach(btn => btn.classList.remove('active'));

    // Show the target page
    const targetPage = document.getElementById(pageId);
    if (targetPage) {
        targetPage.classList.add('active');
        targetPage.style.display = 'block';
        console.log('Page activated:', pageId);
    } else {
        console.error('Page not found:', pageId);
    }
    
    // Activate corresponding nav button
    const pageMap = { 'home': 0, 'summary': 1, 'flashcard': 2, 'quiz': 3 };
    const navButtons = document.querySelectorAll('.nav-btn:not(.logout-btn)');
    if (navButtons[pageMap[pageId]]) {
        navButtons[pageMap[pageId]].classList.add('active');
    }
    
    // Scroll to top
    window.scrollTo(0, 0);
}

// Show conversion prompt modal
function showConversionPrompt() {
    // Remove existing modal if any
    closeModal();
    
    const modal = document.createElement('div');
    modal.className = 'modal-overlay';
    modal.innerHTML = `
        <div class="modal-content">
            <div class="modal-icon">✨</div>
            <h2>Great! Your summary is ready!</h2>
            <p>Would you like to convert this into study materials?</p>
            <div class="modal-buttons">
                <button class="btn btn-purple" onclick="convertToFlashcards()">
                    <i class="fa-solid fa-clone"></i> Create Flashcards
                </button>
                <button class="btn btn-success" onclick="convertToQuiz()">
                    <i class="fa-solid fa-pen"></i> Create Quiz
                </button>
                <button class="btn btn-secondary" onclick="closeModal()">
                    Maybe Later
                </button>
            </div>
        </div>
    `;
    document.body.appendChild(modal);
}

// Close modal
function closeModal() {
    const modal = document.querySelector('.modal-overlay');
    if (modal) {
        modal.remove();
    }
}

// Convert to flashcards
async function convertToFlashcards() {
    closeModal();
    showPageById('flashcard');
    
    const flashcardBox = document.getElementById('flashcardBox');
    flashcardBox.innerHTML = '<h2>Generating flashcards...</h2>';
    
    try {
        if (!currentReviewerId) throw new Error('No reviewer ID available');
        
        console.log('Requesting flashcards for reviewer:', currentReviewerId);
        
        const response = await fetch(`${API_BASE_URL}/reviewers/${currentReviewerId}/flashcards`, {
            method: 'POST',
            headers: getAuthHeaders()
        });
        
        console.log('Flashcard response status:', response.status);
        
        if (response.ok) {
            const data = await response.json();
            console.log('Flashcard data received:', data);
            
            // Handle different response formats
            if (Array.isArray(data)) {
                flashcards = data;
            } else if (data.flashcards && Array.isArray(data.flashcards)) {
                flashcards = data.flashcards;
            } else if (data.data && Array.isArray(data.data)) {
                flashcards = data.data;
            } else {
                console.warn('Unexpected flashcard format:', data);
                flashcards = [];
            }
            
            console.log('Flashcards loaded:', flashcards.length, 'cards');
        } else {
            console.warn('Flashcard generation failed, using local mode');
            flashcards = generateLocalFlashcards(window.localContent || '');
        }
        
        if (flashcards.length === 0) {
            console.warn('No flashcards generated, using demo data');
            flashcards = generateLocalFlashcards(window.localContent || '');
        }
        
        currentFlashcardIndex = 0;
        loadFlashcards();
        
    } catch (error) {
        console.error('Error generating flashcards:', error);
        flashcards = generateLocalFlashcards(window.localContent || '');
        currentFlashcardIndex = 0;
        loadFlashcards();
    }
}

// Convert to quiz
async function convertToQuiz() {
    closeModal();
    showPageById('quiz');
    
    const quizContainer = document.getElementById('quizContainer');
    quizContainer.innerHTML = '<p style="text-align: center; padding: 2rem;">Generating quiz questions...</p>';
    
    try {
        if (!currentReviewerId) throw new Error('No reviewer ID available');
        
        console.log('Requesting quiz for reviewer:', currentReviewerId);
        
        const response = await fetch(`${API_BASE_URL}/reviewers/${currentReviewerId}/quiz`, {
            method: 'POST',
            headers: getAuthHeaders()
        });
        
        console.log('Quiz response status:', response.status);
        
        if (response.ok) {
            const data = await response.json();
            console.log('Quiz data received:', data);
            
            // Handle different response formats
            if (Array.isArray(data)) {
                questions = data;
            } else if (data.questions && Array.isArray(data.questions)) {
                questions = data.questions;
            } else if (data.data && Array.isArray(data.data)) {
                questions = data.data;
            } else {
                console.warn('Unexpected quiz format:', data);
                questions = [];
            }
            
            console.log('Questions loaded:', questions.length, 'questions');
        } else {
            console.warn('Quiz generation failed, using local mode');
            questions = generateLocalQuiz(window.localContent || '');
        }
        
        if (questions.length === 0) {
            console.warn('No questions generated, using demo data');
            questions = generateLocalQuiz(window.localContent || '');
        }
        
        startQuiz();
        
    } catch (error) {
        console.error('Error generating quiz:', error);
        questions = generateLocalQuiz(window.localContent || '');
        startQuiz();
    }
}

// Generate local flashcards (fallback)
function generateLocalFlashcards(content) {
    const sentences = content.split(/[.!?]+/).filter(s => s.trim().length > 20);
    const cards = [];
    
    for (let i = 0; i < Math.min(5, sentences.length); i++) {
        const sentence = sentences[i].trim();
        cards.push({
            question: `What is covered in point ${i + 1}?`,
            answer: sentence
        });
    }
    
    return cards.length > 0 ? cards : [
        { question: 'Sample Question 1', answer: 'Sample Answer 1' },
        { question: 'Sample Question 2', answer: 'Sample Answer 2' }
    ];
}

// Generate local quiz (fallback)
function generateLocalQuiz(content) {
    const sentences = content.split(/[.!?]+/).filter(s => s.trim().length > 20);
    const quizQuestions = [];
    
    for (let i = 0; i < Math.min(3, sentences.length); i++) {
        quizQuestions.push({
            question: `Question ${i + 1}: What does the content discuss?`,
            options: [
                sentences[i]?.trim() || 'Option A',
                'Incorrect option B',
                'Incorrect option C',
                'Incorrect option D'
            ],
            correctAnswer: 0
        });
    }
    
    return quizQuestions.length > 0 ? quizQuestions : [
        {
            question: 'Sample question about the content?',
            options: ['Correct answer', 'Wrong answer 1', 'Wrong answer 2', 'Wrong answer 3'],
            correctAnswer: 0
        }
    ];
}

// Load and display flashcards
function loadFlashcards() {
    if (flashcards.length === 0) {
        document.getElementById('flashcardBox').innerHTML = '<h2>No flashcards available</h2>';
        return;
    }
    
    showingAnswer = false;
    displayCurrentFlashcard();
}

function displayCurrentFlashcard() {
    const flashcardBox = document.getElementById('flashcardBox');
    const card = flashcards[currentFlashcardIndex];
    
    // Support backend format (term/definition) and local format (question/answer)
    const front = card.term || card.question || 'Loading...';
    const back = card.definition || card.answer || 'Loading...';
    
    flashcardBox.className = 'flashcard';
    flashcardBox.innerHTML = `<h2>${showingAnswer ? back : front}</h2>`;
}

function flipFlashcard() {
    showingAnswer = !showingAnswer;
    displayCurrentFlashcard();
}

function nextFlashcard() {
    currentFlashcardIndex = (currentFlashcardIndex + 1) % flashcards.length;
    showingAnswer = false;
    displayCurrentFlashcard();
}

function prevFlashcard() {
    currentFlashcardIndex = (currentFlashcardIndex - 1 + flashcards.length) % flashcards.length;
    showingAnswer = false;
    displayCurrentFlashcard();
}

// Start and display quiz
function startQuiz() {
    const quizContainer = document.getElementById('quizContainer');
    const scoreDisplay = document.getElementById('scoreDisplay');
    
    if (scoreDisplay) scoreDisplay.style.display = 'none';
    
    if (questions.length === 0) {
        quizContainer.innerHTML = '<p style="text-align: center;">No quiz questions available</p>';
        return;
    }
    
    quizContainer.innerHTML = questions.map((q, index) => `
        <div class="quiz-question">
            <h3>Question ${index + 1}</h3>
            <p>${q.question}</p>
            <div class="quiz-options">
                ${(q.choices || q.options || []).map((choice, optIndex) => `
                    <label class="quiz-option">
                        <input type="radio" name="question${index}" value="${choice}">
                        <span>${choice}</span>
                    </label>
                `).join('')}
            </div>
        </div>
    `).join('');
}

function submitQuiz() {
    let score = 0;
    questions.forEach((q, index) => {
        const selected = document.querySelector(`input[name="question${index}"]:checked`);
        // correctAnswer is a string value (the actual correct option text)
        if (selected && selected.value === q.correctAnswer) {
            score++;
        }
    });
    
    const scoreDisplay = document.getElementById('scoreDisplay');
    const scoreText = document.getElementById('scoreText');
    
    if (scoreText) scoreText.textContent = `You scored ${score} out of ${questions.length}!`;
    if (scoreDisplay) scoreDisplay.style.display = 'block';
}

document.addEventListener('DOMContentLoaded', async () => {
    console.log('Quizard initialized!');
    console.log('Backend URL:', API_BASE_URL);

    await testBackendConnection();
});