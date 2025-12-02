const API_BASE_URL = 'http://localhost:8080/api';

let currentReviewerId = null;
let currentFlashcardIndex = 0;
let flashcards = [];
let questions = [];
let showingAnswer = false;
let backendConnected = false;

async function testBackendConnection() {
    try {
        const response = await fetch(`${API_BASE_URL}/users`, {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' }
        });

        if (response.ok) {
            console.log('✅ Backend connected successfully!');
            backendConnected = true;
            hideBackendWarning();

            const users = await response.json();
            displayUsers(users);

            return true;
        } else {
            console.error('❌ Backend responded with error:', response.status);
            backendConnected = false;
            showBackendWarning();
            return false;
        }
    } catch (error) {
        console.error('❌ Backend connection failed:', error);
        backendConnected = false;
        showBackendWarning();
        return false;
    }
}

function showBackendWarning() {
    const uploadArea = document.querySelector('.upload-area');
    if (!uploadArea) return;

    if (!document.getElementById('backendWarning')) {
        const warning = document.createElement('div');
        warning.id = 'backendWarning';
        warning.style.cssText = `
            background: #fff3cd;
            border: 2px solid #ffc107;
            border-radius: 10px;
            padding: 15px;
            margin: 15px 0;
            color: #856404;
            text-align: center;
        `;
        warning.innerHTML = `
            <strong>⚠️ Backend not connected</strong><br>
            <small>Using demo mode with local processing</small>
        `;
        uploadArea.insertAdjacentElement('beforebegin', warning);
    }
}

function hideBackendWarning() {
    const warning = document.getElementById('backendWarning');
    if (warning) warning.remove();
}

function displayUsers(users) {
    const userList = document.getElementById('userList');
    if (!userList) return;

    userList.innerHTML = users
        .map(user => `<li>${user.name} (${user.email})</li>`)
        .join('');
}

function showPage(pageId) {
    document.querySelectorAll('.page').forEach(page => page.classList.remove('active'));
    document.querySelectorAll('.nav-btn').forEach(btn => btn.classList.remove('active'));

    document.getElementById(pageId).classList.add('active');
    event.currentTarget.classList.add('active');

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
        if (!backendConnected) throw new Error('Backend not connected');

        const response = await fetch(`${API_BASE_URL}/reviewers/upload`, { 
            method: 'POST', 
            body: formData 
        });
        
        if (!response.ok) throw new Error('Upload failed');

        const data = await response.json();
        currentReviewerId = data.id;

        uploadArea.innerHTML = `
            <div class="upload-success">
                <div class="success-icon">✅</div>
                <h2>File uploaded successfully!</h2>
                <p>File: ${data.fileName}</p>
                <p>Generating summary...</p>
            </div>
        `;
        
        // Load summary and show prompt after
        await loadSummary(currentReviewerId);
        showConversionPrompt();

    } catch (error) {
        console.error('Upload error:', error);
        backendConnected = false;
        uploadArea.innerHTML = `
            <div class="upload-error" style="background: #fff3cd; padding: 20px; border-radius: 10px; text-align: center;">
                <div class="error-icon" style="font-size: 3rem;">⚠️</div>
                <h2 style="color: #856404;">Backend not connected</h2>
                <p style="color: #856404;">Using demo mode with local processing</p>
                <button class="btn" onclick="location.reload()">Try Again</button>
            </div>
        `;
        readFileLocally(file);
    }
}

function readFileLocally(file) {
    const reader = new FileReader();
    reader.onload = function(e) {
        const content = e.target.result;
        window.localContent = content;
        generateLocalSummary(content);
        
        // Show prompt after local summary generation
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
        if (!backendConnected) throw new Error('Backend not connected');

        const response = await fetch(`${API_BASE_URL}/reviewers/${reviewerId}/summary`);
        if (!response.ok) throw new Error('Failed to load summary');

        const data = await response.json();
        summaryBox.innerHTML = `<pre>${data.summary}</pre>`;
        
        // Auto-navigate to summary page
        showPageById('summary');
        
    } catch (error) {
        console.error('Error loading summary:', error);
        generateLocalSummary(window.localContent || 'No content available');
    } finally {
        if (loading) loading.style.display = 'none';
    }
}

// Helper function to show page without event dependency
function showPageById(pageId) {
    document.querySelectorAll('.page').forEach(page => page.classList.remove('active'));
    document.querySelectorAll('.nav-btn').forEach(btn => btn.classList.remove('active'));

    document.getElementById(pageId).classList.add('active');
    
    // Activate corresponding nav button
    const pageMap = { 'home': 0, 'summary': 1, 'flashcard': 2, 'quiz': 3 };
    const navButtons = document.querySelectorAll('.nav-btn');
    if (navButtons[pageMap[pageId]]) {
        navButtons[pageMap[pageId]].classList.add('active');
    }
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
        if (backendConnected && currentReviewerId) {
            // Call backend API to generate flashcards
            const response = await fetch(`${API_BASE_URL}/reviewers/${currentReviewerId}/flashcards`);
            if (!response.ok) throw new Error('Failed to generate flashcards');
            
            const data = await response.json();
            flashcards = data.flashcards || [];
        } else {
            // Generate flashcards locally
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
        if (backendConnected && currentReviewerId) {
            // Call backend API to generate quiz
            const response = await fetch(`${API_BASE_URL}/reviewers/${currentReviewerId}/quiz`);
            if (!response.ok) throw new Error('Failed to generate quiz');
            
            const data = await response.json();
            questions = data.questions || [];
        } else {
            // Generate quiz locally
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
    
    flashcardBox.className = 'flashcard';
    flashcardBox.innerHTML = `<h2>${showingAnswer ? card.answer : card.question}</h2>`;
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
                ${q.options.map((option, optIndex) => `
                    <label class="quiz-option">
                        <input type="radio" name="question${index}" value="${optIndex}">
                        <span>${option}</span>
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
        if (selected && parseInt(selected.value) === q.correctAnswer) {
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