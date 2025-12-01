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
                <p>Navigate to Summary, Flashcards, or Quiz to get started!</p>
            </div>
        `;
        loadSummary(currentReviewerId);

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
    };
    reader.readAsText(file);
}

function generateLocalSummary(content) {
    const sentences = content.split(/[.!?]+/).filter(s => s.trim().length > 20);
    const keyPoints = sentences.slice(0, Math.min(5, sentences.length));
    const summary = '📋 KEY POINTS:\n\n' + keyPoints.map((s, i) => `${i + 1}. ${s.trim()}.`).join('\n\n');

    const summaryBox = document.getElementById('summaryBox');
    if (summaryBox) summaryBox.innerHTML = `<pre>${summary}</pre>`;
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
    } catch (error) {
        console.error('Error loading summary:', error);
        generateLocalSummary(window.localContent || 'No content available');
    } finally {
        if (loading) loading.style.display = 'none';
    }
}

document.addEventListener('DOMContentLoaded', async () => {
    console.log('Quizard initialized!');
    console.log('Backend URL:', API_BASE_URL);

    await testBackendConnection();
});
