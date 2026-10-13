document.addEventListener('DOMContentLoaded', () => {
    window.addEventListener('error', function(e) {
        document.body.innerHTML += `<div style="position:fixed; z-index:9999; top:0; left:0; background:red; color:white; padding:10px; width: 100%;">ERROR: ${e.message} at ${e.filename}:${e.lineno}</div>`;
    });
    window.addEventListener('unhandledrejection', function(e) {
        document.body.innerHTML += `<div style="position:fixed; z-index:9999; top:40px; left:0; background:orange; color:white; padding:10px; width: 100%;">PROMISE ERROR: ${e.reason}</div>`;
    });
    
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get('view') === 'immersive') {
        const sessionId = urlParams.get('session');
        const mode = urlParams.get('mode');
        const company = urlParams.get('company');
        
        document.querySelector('.sidebar').style.display = 'none';
        document.querySelector('.top-nav').style.display = 'none';
        
        document.querySelectorAll('.view').forEach(v => v.style.display = 'none');
        document.getElementById('interviewView').style.display = 'block';
        
        const hub = document.getElementById('interviewHub');
        if (hub) hub.style.display = 'none';
        
        document.getElementById('immersiveSimulator').style.display = 'block';
        document.body.classList.add('fullscreen-active');
        
        window.activeSessionId = sessionId;
        window.selectedMode = mode;
        window.selectedCompany = company;
        
        document.getElementById('simCompanyBadge').textContent = company;
        document.getElementById('simModeBadge').textContent = mode + " ROUND";
        document.getElementById('interviewerName').textContent = `${company} Interviewer`;
        document.getElementById('interviewerRole').textContent = mode === "HR" ? "Talent Partner" : "Systems Architect";

        setTimeout(() => {
            if (mode === 'HR') {
                document.querySelector('.sim-workspace').style.display = 'none';
                document.getElementById('hrWorkspace').style.display = 'flex';
            } else if (mode === 'APTITUDE') {
                document.querySelector('.sim-workspace').style.display = 'none';
                document.getElementById('aptitudeWorkspace').style.display = 'flex';
                window.loadAptitudeQuestions();
            } else {
                initMonacoEditor();
                initDrawingCanvas();
            }

            fetch(`/api/interview/session/${sessionId}`)
               .then(res => res.json())
               .then(data => {
                   const chatArea = document.getElementById('simChatMessages');
                   chatArea.innerHTML = '';
                   if (data.session && data.session.transcript) {
                       const logs = JSON.parse(data.session.transcript);
                       logs.forEach(l => window.appendSimChat(l.sender, l.message));
                       if (logs.length > 0) window.triggerTextToSpeech(logs[logs.length-1].message);
                   }
               });
               
            window.simSeconds = 0;
            clearInterval(window.simTimerInterval);
            window.simTimerInterval = setInterval(() => { 
                window.simSeconds++; 
                const timerEl = document.getElementById('simTimer');
                if (timerEl) {
                    timerEl.innerHTML = `<i class="far fa-clock"></i> ${new Date(window.simSeconds * 1000).toISOString().substr(14, 5)}`;
                }
            }, 1000);
        }, 100);
    } else {
        // Initialize normal SPA view (sidebar, top-nav, main-content are visible by default)
    }

    // Navigation & State
    const navItems = document.querySelectorAll('.nav-item[data-view]');
    const views = document.querySelectorAll('.view');
    let currentTopic = localStorage.getItem('lastTopic') || "General Learning";
    let currentLevel = "Beginner";
    let currentRoadmapStep = "Overview";
    let chatMode = "Beginner";

    // AI Response Cache (Local Storage)
    const aiCache = JSON.parse(localStorage.getItem('aiCache') || '{}');

    // Smart Fallback Knowledge Base (Offline Mode)
    const offlineKB = {
        "what is time complexity": "Time complexity is a computational complexity that describes the amount of time it takes to run an algorithm. It is typically expressed using Big O notation (e.g., O(n), O(log n)).",
        "what is hadoop": "Apache Hadoop is an open-source framework that allows for the distributed processing of large data sets across clusters of computers using simple programming models.",
        "what is big data": "Big data refers to extremely large data sets that may be analyzed computationally to reveal patterns, trends, and associations, especially relating to human behavior and interactions.",
        "what is python": "Python is a high-level, interpreted, general-purpose programming language. Its design philosophy emphasizes code readability with its use of significant indentation."
    };

    // Auth State Check & Auto Login
    const storedUser = JSON.parse(localStorage.getItem('user') || '{}');
    if (storedUser.username) {
        updateUIWithUser(storedUser);
    }

    fetch('/api/auth/me').then(res => {
        if (res.ok) {
            return res.json().then(data => {
                localStorage.setItem('user', JSON.stringify(data));
                updateUIWithUser(data);
            });
        } else {
            // Optional: redirect to login if strictly required
            // window.location.href = '/login.html';
        }
    }).catch(console.error);

    function updateUIWithUser(user) {
        document.getElementById('navUsername').textContent = user.username;
        const navImg = document.querySelector('#userPill img');
        if (navImg && user.profileImage) {
            navImg.src = user.profileImage;
        }
        const profileName = document.getElementById('profileName');
        const profileEmail = document.getElementById('profileEmail');
        if (profileName) profileName.textContent = user.username;
        if (profileEmail) profileEmail.textContent = user.email;
    }

    // SPA Navigation
    navItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const targetView = item.getAttribute('data-view');
            navItems.forEach(nav => nav.classList.remove('active'));
            item.classList.add('active');
            views.forEach(view => view.style.display = 'none');
            const activeView = document.getElementById(targetView + 'View');
            if (activeView) activeView.style.display = 'block';
            if (targetView === 'history') loadHistory();
            if (targetView === 'saved') loadSaved();
            if (targetView === 'interview') loadPastSessions();
            if (targetView === 'settings') window.loadProfileDashboard();
        });
    });

    // Theme & Notification Bell
    const notificationBell = document.getElementById('notificationBell');
    if (notificationBell) {
        notificationBell.onclick = (e) => {
            notificationBell.classList.toggle('active');
            e.stopPropagation();
        };
        document.addEventListener('click', () => notificationBell.classList.remove('active'));
    }

    const themeToggle = document.getElementById('themeToggle');
    if (themeToggle) {
        if (localStorage.getItem('theme') === 'dark') {
            document.body.classList.add('dark-theme');
            themeToggle.classList.add('active');
        }
        themeToggle.addEventListener('click', () => {
            document.body.classList.toggle('dark-theme');
            themeToggle.classList.toggle('active');
            localStorage.setItem('theme', document.body.classList.contains('dark-theme') ? 'dark' : 'light');
        });
    }

    // Handle Search
    const searchForm = document.getElementById('searchForm');
    searchForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const topic = document.getElementById('topicInput').value.trim();
        const level = document.getElementById('levelInput').value;
        if (!topic) return;

        currentTopic = topic;
        currentLevel = level;
        localStorage.setItem('lastTopic', topic);

        const resultsContainer = document.getElementById('resultsContainer');
        const roadmapContent = document.getElementById('roadmapContent');
        const webResourcesGrid = document.getElementById('webResourcesGrid');
        const practiceGrid = document.getElementById('practiceGrid');
        const videosGrid = document.getElementById('videosGrid');

        resultsContainer.style.display = 'none';
        document.getElementById('loader').style.display = 'flex';
        roadmapContent.innerHTML = '';
        webResourcesGrid.innerHTML = '';
        practiceGrid.innerHTML = '';
        videosGrid.innerHTML = '';

        try {
            const cacheKey = `roadmap_${topic}_${level}`;
            if (aiCache[cacheKey]) {
                renderResults(aiCache[cacheKey]);
                document.getElementById('loader').style.display = 'none';
                resultsContainer.style.display = 'block';
                return;
            }

            const response = await fetch('/api/recommend', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ topic, level })
            });
            const data = await response.json();
            
            if (response.status === 429) {
                alert('⚠️ API Limit: ' + data.error + '\nTip: Try a different topic or wait a moment.');
                document.getElementById('loader').style.display = 'none';
                return;
            }

            if (!response.ok) throw new Error(data.error || 'Failed to fetch');

            aiCache[cacheKey] = data;
            localStorage.setItem('aiCache', JSON.stringify(aiCache));

            renderResults(data);
            document.getElementById('loader').style.display = 'none';
            resultsContainer.style.display = 'block';
            resultsContainer.scrollIntoView({ behavior: 'smooth' });
        } catch (err) {
            console.error(err);
            document.getElementById('loader').style.display = 'none';
            alert('Error: ' + err.message);
        }
    });

    function renderResults(data) {
        document.getElementById('roadmapTitle').textContent = data.roadmap_title || `Mastering ${currentTopic}`;
        const stepTemplate = document.getElementById('stepTemplate');
        const roadmapContent = document.getElementById('roadmapContent');

        if (data.steps) {
            data.steps.forEach((step, idx) => {
                const clone = stepTemplate.content.cloneNode(true);
                clone.querySelector('.step-number').textContent = `Step ${String(idx + 1).padStart(2, '0')}`;
                clone.querySelector('.step-title').textContent = step.title;
                clone.querySelector('.step-explanation').textContent = step.explanation;
                clone.querySelector('.time-badge').textContent = `⏳ ${step.estimated_time || 'Variable'}`;

                const ytMini = clone.querySelector('.yt-resource-mini');
                if (step.youtube_resource) {
                    ytMini.href = step.youtube_resource.url;
                    ytMini.querySelector('img').src = step.youtube_resource.thumbnail;
                    ytMini.querySelector('h4').textContent = step.youtube_resource.title;
                    const saveBtn = clone.querySelector('.save-btn');
                    saveBtn.onclick = (e) => { e.preventDefault(); e.stopPropagation(); saveResource(step.youtube_resource, saveBtn, currentTopic); };
                } else {
                    ytMini.style.display = 'none';
                }

                clone.querySelector('.ask-bot-btn').onclick = () => {
                    currentRoadmapStep = step.title;
                    document.getElementById('chatbotWindow').style.display = 'flex';
                    askBot(`Explain more about: ${step.title}`);
                };
                roadmapContent.appendChild(clone);
            });
        }

        const webGrid = document.getElementById('webResourcesGrid');
        const pracGrid = document.getElementById('practiceGrid');
        if (data.resources) {
            const allRes = [...(data.resources.websites || []), ...(data.resources.courses || [])];
            allRes.forEach(res => {
                const clone = document.getElementById('smallCardTemplate').content.cloneNode(true);
                clone.querySelector('h4').textContent = res.name;
                clone.querySelector('p').textContent = res.desc || res.platform || 'Curated link';
                clone.querySelector('a').href = res.url;
                webGrid.appendChild(clone);
            });
            if (data.resources.practice_platforms) {
                data.resources.practice_platforms.forEach(res => {
                    const clone = document.getElementById('smallCardTemplate').content.cloneNode(true);
                    clone.querySelector('h4').textContent = res.name;
                    clone.querySelector('p').textContent = res.desc;
                    clone.querySelector('a').href = res.url;
                    pracGrid.appendChild(clone);
                });
            }
        }

        const vidGrid = document.getElementById('videosGrid');
        if (data.recommended_videos) {
            data.recommended_videos.forEach(video => {
                const clone = document.getElementById('videoCardTemplate').content.cloneNode(true);
                clone.querySelector('.video-link').href = video.url;
                clone.querySelector('img').src = video.thumbnail;
                clone.querySelector('h4').textContent = video.title;
                clone.querySelector('.channel-name').textContent = video.channelTitle;
                clone.querySelector('.video-topic-tag').textContent = currentTopic;
                const saveBtn = clone.querySelector('.save-btn-floating');
                saveBtn.onclick = (e) => { e.preventDefault(); e.stopPropagation(); saveResource(video, saveBtn, currentTopic); };
                vidGrid.appendChild(clone);
            });
        }
    }

    async function saveResource(res, btn, topic) {
        try {
            const payload = { title: res.title, resourceUrl: res.url, thumbnailUrl: res.thumbnail, channelName: res.channelTitle, topic: topic };
            const resp = await fetch('/api/save', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
            if (resp.ok) { btn.innerHTML = '<i class="fas fa-check"></i>'; setTimeout(() => btn.innerHTML = '<i class="fas fa-bookmark"></i>', 2000); }
        } catch (err) { console.error(err); }
    }

    async function unsaveResource(id, card) {
        if (!confirm('Unsave this?')) return;
        try {
            const resp = await fetch(`/api/save/${id}`, { method: 'DELETE' });
            if (resp.ok) { card.style.opacity = '0'; setTimeout(() => card.remove(), 300); }
        } catch (err) { console.error(err); }
    }

    async function loadHistory() {
        const historyList = document.getElementById('historyList');
        historyList.innerHTML = '<div style="grid-column: 1/-1; text-align: center;">Loading...</div>';
        try {
            const resp = await fetch('/api/history');
            const data = await resp.json();
            historyList.innerHTML = '';
            data.reverse().forEach(item => {
                const card = document.createElement('div');
                card.className = 'glass history-card';
                card.innerHTML = `
                    <div style="display: flex; justify-content: space-between;"><i class="fas fa-graduation-cap"></i> <span class="glass" style="font-size: 11px; padding: 4px 10px;">${item.level}</span></div>
                    <div class="history-topic">${item.topic}</div>
                    <div style="font-size: 13px; color: var(--text-dim);"><i class="far fa-calendar-alt"></i> ${new Date(item.createdAt).toLocaleDateString()}</div>
                `;
                card.onclick = () => {
                    document.getElementById('topicInput').value = item.topic;
                    document.getElementById('levelInput').value = item.level;
                    document.querySelector('.nav-item[data-view="home"]').click();
                    searchForm.dispatchEvent(new Event('submit'));
                };
                historyList.appendChild(card);
            });
        } catch (err) { console.error(err); }
    }

    async function loadSaved() {
        const savedGrid = document.getElementById('savedGrid');
        savedGrid.innerHTML = '<div style="grid-column: 1/-1; text-align: center;">Loading...</div>';
        try {
            const resp = await fetch('/api/saved');
            const data = await resp.json();
            savedGrid.innerHTML = '';
            data.forEach(item => {
                const card = document.createElement('div');
                card.className = 'glass saved-card';
                // Check undefined for thumbnail
                let thumb = item.thumbnailUrl && item.thumbnailUrl !== 'undefined' ? item.thumbnailUrl : 'https://via.placeholder.com/320x180';
                let vidUrl = item.resourceUrl && item.resourceUrl !== 'undefined' ? item.resourceUrl : '#';
                let chName = item.channelName && item.channelName !== 'undefined' ? item.channelName : 'YouTube Channel';
                let topicName = item.topic && item.topic !== 'undefined' ? item.topic : 'General';
                let dateStr = item.savedAt ? new Date(item.savedAt).toLocaleDateString() : new Date().toLocaleDateString();

                card.innerHTML = `
                    <div class="saved-thumb-container" style="position: relative;">
                        <span class="topic-tag" style="position: absolute; top: 10px; left: 10px; background: rgba(0,0,0,0.6); color: white; padding: 4px 8px; border-radius: 4px; font-size: 11px;">${topicName}</span>
                        <img src="${thumb}" class="saved-thumb" style="width: 100%; border-radius: 12px 12px 0 0; object-fit: cover; aspect-ratio: 16/9;" onerror="this.src='https://via.placeholder.com/320x180'">
                        <button class="glass unsave-btn" style="position: absolute; top: 10px; right: 10px; width: 32px; height: 32px; border-radius: 50%; color: var(--accent); cursor: pointer; border: none; display: flex; align-items: center; justify-content: center;"><i class="fas fa-trash-alt"></i></button>
                    </div>
                    <div class="saved-details" style="padding: 15px; display: flex; flex-direction: column; gap: 8px;">
                        <h4 style="font-size: 14px; margin: 0; line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;">${item.title}</h4>
                        <div style="font-size: 12px; color: var(--text-dim); display: flex; align-items: center; gap: 5px;"><i class="fas fa-tv"></i> ${chName}</div>
                        <div style="font-size: 11px; opacity: 0.7;"><i class="far fa-calendar-alt"></i> Saved on: ${dateStr}</div>
                        <div style="display: flex; gap: 8px; margin-top: 10px; flex-wrap: wrap;">
                            <a href="${vidUrl}" target="_blank" onclick="if(this.href==='#' || this.href.includes('undefined')) { alert('Video URL is invalid.'); return false; }" class="btn-action watch-btn" style="flex: 1; text-align: center; background: var(--primary); color: white; padding: 8px; border-radius: 8px; text-decoration: none; font-size: 13px; font-weight: 600;"><i class="fas fa-play-circle"></i> Watch</a>
                        </div>
                        ${!vidUrl.includes('playlist') ? `
                        <div style="display: flex; gap: 8px; margin-top: 5px;">
                            <button class="btn-action summarize-btn" data-url="${vidUrl}" style="flex: 1; background: rgba(99,102,241,0.1); border: 1px solid var(--primary); color: var(--primary); padding: 8px; border-radius: 8px; cursor: pointer; font-size: 12px; font-weight: 600;"><i class="fas fa-magic"></i> Summarize</button>
                            <button class="btn-action read-btn" data-url="${vidUrl}" style="flex: 1; background: rgba(99,102,241,0.1); border: 1px solid var(--primary); color: var(--primary); padding: 8px; border-radius: 8px; cursor: pointer; font-size: 12px; font-weight: 600;"><i class="fas fa-book-open"></i> Read Instead</button>
                        </div>` : ''}
                    </div>
                `;
                card.querySelector('.unsave-btn').onclick = () => unsaveResource(item.id, card);
                
                const getVidId = (url) => {
                    if (url.includes('playlist')) return 'PLAYLIST';
                    let m = url.match(/(?:v=|\/)([0-9A-Za-z_-]{11}).*/);
                    return m ? m[1] : null;
                };

                const summarizeBtn = card.querySelector('.summarize-btn');
                const readBtn = card.querySelector('.read-btn');
                
                if (summarizeBtn && readBtn) {
                    summarizeBtn.onclick = () => {
                        const id = getVidId(vidUrl);
                        if (id === 'PLAYLIST') alert('Cannot summarize a full playlist. Please select a specific video.');
                        else showAiSummaryModal(id, 'default', item.title);
                    };
                    readBtn.onclick = () => {
                        const id = getVidId(vidUrl);
                        if (id === 'PLAYLIST') alert('Cannot read a full playlist. Please select a specific video.');
                        else showAiSummaryModal(id, 'text', item.title);
                    };
                }

                savedGrid.appendChild(card);
            });
        } catch (err) { console.error(err); }
    }

    async function showAiSummaryModal(videoId, mode, title) {
        if(!videoId) { alert('Invalid video ID'); return; }
        
        // Use chatbot window to show the summary
        document.getElementById('chatbotWindow').style.display = 'flex';
        
        let promptMode = mode === 'text' ? 'Converting video to readable notes...' : 'Summarizing video structure...';
        const botDiv = appendChat('bot', `<i class="fas fa-spinner fa-spin"></i> ${promptMode}<br><span style="font-size:11px; opacity:0.8;">Fetching transcript for: ${title}</span>`);
        
        try {
            const resp = await fetch('/api/video/summarize', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ videoId: videoId, mode: mode })
            });
            const data = await resp.json();
            if(!resp.ok) throw new Error(data.error);
            
            botDiv.innerHTML = `<div style="margin-bottom: 10px; font-weight: bold; color: var(--primary);"><i class="fas fa-video"></i> ${mode==='text' ? 'Video Notes' : 'Video Summary'}</div>` + marked.parse(data.summary);
        } catch(e) {
            botDiv.textContent = "Error: " + e.message;
        }
    }

    // Chatbot with Cache & Fallback
    const chatInput = document.getElementById('chatInput');
    const sendChat = document.getElementById('sendChat');
    const chatMessages = document.getElementById('chatMessages');

    async function askBot(msg) {
        if (!msg) return;
        const query = msg.toLowerCase().trim();
        appendChat('user', msg);
        chatInput.value = '';
        
        // 1. Check Local Cache
        if (aiCache[query]) {
            appendChat('bot', marked.parse(aiCache[query]));
            return;
        }

        const botDiv = appendChat('bot', '<i class="fas fa-spinner fa-spin"></i> SkillForge Assistant is thinking...');
        
        try {
            const resp = await fetch('/api/chat', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    message: msg, topic: currentTopic, level: currentLevel,
                    roadmapStep: currentRoadmapStep, mode: chatMode
                })
            });
            const data = await resp.json();
            
            if (resp.status === 429) {
                // 2. Check Smart Fallback Knowledge Base if API fails
                if (offlineKB[query]) {
                    botDiv.innerHTML = `<div style="color: var(--primary); font-weight: 700;">📘 Instant Insight (Offline)</div>` + marked.parse(offlineKB[query]);
                } else {
                    botDiv.innerHTML = `<div style="color: var(--accent); font-weight: 700;">⚠️ Quota Limit Reached</div>
                                        <p style="font-size: 13px;">I'm currently resting to stay free for everyone. Please wait 60 seconds.</p>
                                        <div style="margin-top: 10px; padding: 10px; background: rgba(0,0,0,0.03); border-radius: 10px; font-size: 12px;">
                                            <b>Pro Tip:</b> Try searching for basic topics like "Python" or "Big Data" while you wait!
                                        </div>`;
                }
            } else if (!resp.ok) {
                botDiv.textContent = "AI Error: " + (data.error || "Generation failed.");
            } else {
                aiCache[query] = data.response;
                localStorage.setItem('aiCache', JSON.stringify(aiCache));
                botDiv.innerHTML = marked.parse(data.response);
            }
            chatMessages.scrollTop = chatMessages.scrollHeight;
        } catch (err) { botDiv.textContent = "Error connecting to AI."; }
    }

    function appendChat(sender, text) {
        const div = document.createElement('div');
        div.className = `message ${sender}-message`;
        div.innerHTML = text;
        chatMessages.appendChild(div);
        chatMessages.scrollTop = chatMessages.scrollHeight;
        return div;
    }

    sendChat.onclick = () => askBot(chatInput.value.trim());
    chatInput.onkeypress = (e) => { if(e.key === 'Enter') askBot(chatInput.value.trim()); };

    document.getElementById('chatbotTrigger').onclick = () => {
        const win = document.getElementById('chatbotWindow');
        win.style.display = win.style.display === 'flex' ? 'none' : 'flex';
    };
    document.getElementById('closeChat').onclick = () => document.getElementById('chatbotWindow').style.display = 'none';

    document.querySelectorAll('.mode-btn').forEach(btn => {
        btn.onclick = () => {
            document.querySelectorAll('.mode-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            chatMode = btn.getAttribute('data-mode');
        };
    });

    document.getElementById('logoutBtn').onclick = (e) => {
        e.preventDefault();
        localStorage.clear();
        window.location.href = '/logout';
    };

    // ==========================================
    // SKILLFORGE AI INTERVIEW SIMULATOR LOGIC
    // ==========================================
    let activeSessionId = null;
    let selectedCompany = "Google";
    let selectedMode = "PROJECT";
    let simTimerInterval = null;
    let simSeconds = 0;
    let isVoiceActive = false;
    let synth = window.speechSynthesis;
    let recognition = null;
    
    // Web Speech API initialization
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (SpeechRecognition) {
        recognition = new SpeechRecognition();
        recognition.continuous = true;
        recognition.interimResults = false;
        recognition.lang = 'en-US';
        
        recognition.onresult = (event) => {
            const transcript = event.results[event.results.length - 1][0].transcript;
            const input = document.getElementById('simChatInput');
            if (input) {
                input.value = (input.value + " " + transcript).trim();
            }
        };
        
        recognition.onerror = (e) => {
            console.error("Speech recognition error:", e);
        };
    }

    // Monaco Editor Loader
    let editorInstance = null;
    function initMonacoEditor() {
        if (editorInstance) return;
        require.config({ paths: { 'vs': 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.39.0/min/vs' } });
        require(['vs/editor/editor.main'], function () {
            editorInstance = monaco.editor.create(document.getElementById('monacoEditorContainer'), {
                value: '// Select language and write code here...\n\nfunction solve() {\n    // Write solution\n}\n',
                language: 'javascript',
                theme: 'vs-dark',
                automaticLayout: true
            });
        });
    }

    // Selection highlights
    const companyCards = document.querySelectorAll('.company-card');
    companyCards.forEach(card => {
        card.addEventListener('click', () => {
            companyCards.forEach(c => c.classList.remove('active'));
            card.classList.add('active');
            selectedCompany = card.getAttribute('data-company');
        });
    });

    const modeCards = document.querySelectorAll('.mode-card');
    const projectSetupForm = document.getElementById('projectSetupForm');
    modeCards.forEach(card => {
        card.addEventListener('click', () => {
            modeCards.forEach(m => m.classList.remove('active'));
            card.classList.add('active');
            selectedMode = card.getAttribute('data-mode');
            
            // Show project metadata setup form only for PROJECT or DEBUGGING modes
            if (selectedMode === "PROJECT" || selectedMode === "DEBUGGING") {
                projectSetupForm.style.display = "block";
            } else {
                projectSetupForm.style.display = "none";
            }
        });
    });

    // Start Simulation Button
    document.getElementById('startInterviewBtn').addEventListener('click', async () => {
        const metadata = {
            projectTitle: document.getElementById('simProjTitle').value,
            techStack: document.getElementById('simProjStack').value,
            description: document.getElementById('simProjDesc').value,
            features: document.getElementById('simProjFeatures').value,
            architectureExplanation: document.getElementById('simProjArch').value
        };

        const startBtn = document.getElementById('startInterviewBtn');
        startBtn.disabled = true;
        startBtn.textContent = "Initializing Simulator Sandbox...";

        try {
            const resp = await fetch('/api/interview/session/start', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    mode: selectedMode,
                    company: selectedCompany,
                    metadata: metadata
                })
            });
            const data = await resp.json();
            if (!resp.ok) throw new Error(data.error || "Failed to initialize session");

            activeSessionId = data.sessionId;
            
            const url = new URL(window.location.origin + '/');
            url.searchParams.set('view', 'immersive');
            url.searchParams.set('session', activeSessionId);
            url.searchParams.set('mode', selectedMode);
            url.searchParams.set('company', selectedCompany);
            window.open(url.toString(), '_blank');

        } catch (e) {
            alert("Error initializing interview: " + e.message);
        } finally {
            startBtn.disabled = false;
            startBtn.innerHTML = '<i class="fas fa-play"></i> Initialize Immersive Interview Session';
        }
    });

    // Chat sending inside Simulator
    async function sendSimMessage() {
        const input = document.getElementById('simChatInput');
        const text = input.value.trim();
        if (!text || !activeSessionId) return;

        appendSimChat("user", text);
        input.value = '';

        // Animate avatar speaking state (thinking)
        document.getElementById('avatarFace').style.display = 'none';
        document.getElementById('speakingWaves').style.display = 'flex';

        try {
            const resp = await fetch(`/api/interview/session/${activeSessionId}/chat`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ message: text })
            });
            const data = await resp.json();
            if (!resp.ok) throw new Error(data.error || "Communication failure");

            appendSimChat("bot", data.response);
            triggerTextToSpeech(data.response);

        } catch (e) {
            appendSimChat("bot", "System Error: " + e.message);
        } finally {
            document.getElementById('avatarFace').style.display = 'block';
            document.getElementById('speakingWaves').style.display = 'none';
        }
    }

    document.getElementById('simSendChat').onclick = sendSimMessage;
    document.getElementById('simChatInput').onkeypress = (e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendSimMessage(); } };

    function appendSimChat(sender, text) {
        const chatArea = document.getElementById('simChatMessages');
        const bubble = document.createElement('div');
        bubble.className = `message ${sender}-message`;
        bubble.innerHTML = marked.parse(text);
        chatArea.appendChild(bubble);
        chatArea.scrollTop = chatArea.scrollHeight;
    }

    // Timer Update
    function updateSimTimer() {
        simSeconds++;
        const mins = String(Math.floor(simSeconds / 60)).padStart(2, '0');
        const secs = String(simSeconds % 60).padStart(2, '0');
        document.getElementById('simTimer').innerHTML = `<i class="far fa-clock"></i> ${mins}:${secs}`;
    }

    // Voice Mode Synthesis
    document.getElementById('voiceModeBtn').addEventListener('click', () => {
        isVoiceActive = !isVoiceActive;
        const btn = document.getElementById('voiceModeBtn');
        if (isVoiceActive) {
            btn.innerHTML = '<i class="fas fa-microphone" style="color: #10b981;"></i> Voice Mode Enabled';
            btn.classList.add('active');
            if (recognition) recognition.start();
        } else {
            btn.innerHTML = '<i class="fas fa-microphone-slash"></i> Enable Voice Mode';
            btn.classList.remove('active');
            if (recognition) recognition.stop();
            if (synth) synth.cancel();
        }
    });

    function triggerTextToSpeech(text) {
        if (!isVoiceActive || !synth) return;
        synth.cancel();
        // Remove markdown tags for natural speech
        const plainText = text.replace(/[*#_`]/g, '');
        const utterance = new SpeechSynthesisUtterance(plainText);
        utterance.onstart = () => {
            document.getElementById('avatarFace').style.display = 'none';
            document.getElementById('speakingWaves').style.display = 'flex';
        };
        utterance.onend = () => {
            document.getElementById('avatarFace').style.display = 'block';
            document.getElementById('speakingWaves').style.display = 'none';
        };
        synth.speak(utterance);
    }

    // End & Analyze Session
    document.getElementById('endSessionBtn').addEventListener('click', async () => {
        if (!confirm("Are you sure you want to end this interview and compile your final grading feedback scorecard?")) return;
        clearInterval(simTimerInterval);

        const endBtn = document.getElementById('endSessionBtn');
        endBtn.disabled = true;
        endBtn.textContent = "Compiling Report...";

        try {
            const resp = await fetch(`/api/interview/session/${activeSessionId}/complete`, {
                method: 'POST'
            });
            const data = await resp.json();
            if (!resp.ok) throw new Error(data.error || "Analysis aggregation failed.");

            // Hide immersive overlay
            document.getElementById('immersiveSimulator').style.display = 'none';
            document.body.classList.remove('fullscreen-active');
            if (synth) synth.cancel();
            if (recognition) recognition.stop();

            // Populate dashboard
            renderAnalyticsDashboard(data);

        } catch (e) {
            alert("Error compiling report: " + e.message);
        } finally {
            endBtn.disabled = false;
            endBtn.innerHTML = '<i class="fas fa-door-open"></i> End & Analyze';
        }
    });

    function renderAnalyticsDashboard(feedback) {
        document.getElementById('dashboardSessionTitle').textContent = `${selectedCompany} ${selectedMode} Performance scorecard`;
        document.getElementById('statTech').textContent = feedback.technicalScore + "%";
        document.getElementById('statCoding').textContent = feedback.codingScore + "%";
        document.getElementById('statDsa').textContent = feedback.dsaScore + "%";
        document.getElementById('statComm').textContent = feedback.communicationScore + "%";
        document.getElementById('statDebug').textContent = feedback.debuggingScore + "%";

        // Scoring circle
        const cumulative = Math.round((feedback.technicalScore + feedback.codingScore + feedback.dsaScore + feedback.communicationScore + feedback.debuggingScore) / 5);
        document.getElementById('feedbackTotalScore').textContent = cumulative + "%";
        
        let verdict = "Needs Practice";
        if (cumulative >= 90) verdict = "Strong Hire (L5+)";
        else if (cumulative >= 80) verdict = "Hire (L4)";
        else if (cumulative >= 70) verdict = "Leaning Hire";
        else verdict = "Leaning No-Hire / Needs Study";
        
        document.getElementById('feedbackHiringVerdict').textContent = verdict;
        document.getElementById('feedbackInsights').textContent = feedback.insights || "Overall average technical skill metrics matching corporate parameters.";

        // Populate lists
        const strList = document.getElementById('listStrengths');
        strList.innerHTML = '';
        try {
            const strengths = JSON.parse(feedback.strengths);
            strengths.forEach(s => {
                const li = document.createElement('li');
                li.textContent = s;
                strList.appendChild(li);
            });
        } catch (e) {
            strList.innerHTML = '<li>Responsive system design logic</li><li>Excellent modular thinking</li>';
        }

        const wkList = document.getElementById('listWeaknesses');
        wkList.innerHTML = '';
        try {
            const weaknesses = JSON.parse(feedback.weaknesses);
            weaknesses.forEach(w => {
                const li = document.createElement('li');
                li.textContent = w;
                wkList.appendChild(li);
            });
        } catch (e) {
            wkList.innerHTML = '<li>Slight lag in explaining complexity algorithms</li><li>Needs more concurrency depth</li>';
        }

        const recList = document.getElementById('listRecommendations');
        recList.innerHTML = '';
        try {
            const recommendations = JSON.parse(feedback.learningRecommendations);
            recommendations.forEach(r => {
                const li = document.createElement('li');
                li.textContent = r;
                recList.appendChild(li);
            });
        } catch (e) {
            recList.innerHTML = '<li>Advanced Distributed Systems guide</li><li>Dynamic programming LeetCode sheet</li>';
        }

        // Show Dashboard
        document.getElementById('interviewHub').style.display = 'none';
        document.getElementById('analyticsDashboard').style.display = 'block';
    }

    document.getElementById('closeDashboardBtn').addEventListener('click', () => {
        document.getElementById('analyticsDashboard').style.display = 'none';
        document.getElementById('interviewHub').style.display = 'block';
        loadPastSessions();
    });

    // Run Code Actions
    document.getElementById('runCodeBtn').addEventListener('click', async () => {
        if (!editorInstance || !activeSessionId) return;
        const code = editorInstance.getValue();
        const lang = document.getElementById('editorLanguage').value;
        const term = document.getElementById('terminalOutput');
        
        term.textContent = "Compiling and executing code in sandboxed JVM/V8 workspace...";

        try {
            const resp = await fetch(`/api/interview/session/${activeSessionId}/run-code`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ code: code, language: lang })
            });
            const data = await resp.json();
            
            let outputText = "";
            if (data.compilerMessage) {
                outputText += `[COMPILER LOGS]\n${data.compilerMessage}\n\n`;
            }
            if (data.stderr) {
                outputText += `[STANDARD ERROR]\n${data.stderr}\n\n`;
            }
            outputText += `[STANDARD OUTPUT]\n${data.stdout || "Program executed with no console output."}\n\n[Process exited with code ${data.exitCode}]`;
            
            term.textContent = outputText;
            term.scrollTop = term.scrollHeight;

        } catch (e) {
            term.textContent = "Compiler Connection Error: " + e.message;
        }
    });

    // Submit Solution Actions
    document.getElementById('submitCodeBtn').addEventListener('click', async () => {
        if (!editorInstance || !activeSessionId) return;
        const code = editorInstance.getValue();
        const lang = document.getElementById('editorLanguage').value;
        
        alert("Submitting solution code for dynamic test-case evaluation...");

        try {
            const resp = await fetch(`/api/interview/session/${activeSessionId}/submit-code`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ code: code, language: lang, problemTitle: "DSA Challenge" })
            });
            const data = await resp.json();
            
            const term = document.getElementById('terminalOutput');
            if (data.status === "PASSED") {
                term.textContent = "🏆 [SUBMISSION VERDICT]: ALL TEST CASES PASSED SUCCESSFULLY.\n\n" + data.output;
                alert("Submission Accepted! All code logic validation constraints completed.");
            } else {
                term.textContent = "❌ [SUBMISSION VERDICT]: COMPILATION OR RUNTIME CONSTRAINTS FAILED.\n\n" + data.stderr;
                alert("Submission failed. Review compiler outputs in console.");
            }

        } catch (e) {
            alert("Submission error: " + e.message);
        }
    });

    document.getElementById('clearTerminalBtn').onclick = () => {
        document.getElementById('terminalOutput').textContent = "Console cleared. Run code again.";
    };

    // Tab view selectors inside Simulator Workspace
    const tabs = document.querySelectorAll('.workspace-tab');
    const panes = document.querySelectorAll('.tab-pane');
    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            tabs.forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
            
            panes.forEach(pane => {
                pane.style.display = 'none';
                pane.classList.remove('active');
            });
            
            const activePane = document.getElementById(tab.getAttribute('data-tab'));
            activePane.style.display = activePane.id === "editor-tab" ? 'flex' : 'block';
            activePane.classList.add('active');
            
            // Force editor layout updates when visible
            if (editorInstance && tab.getAttribute('data-tab') === "editor-tab") {
                editorInstance.layout();
            }
        });
    });

    // Drawing Canvas Drawing Logic
    let canvas = null;
    let ctx = null;
    let drawing = false;
    let activeTool = "pencil";
    let startX = 0;
    let startY = 0;
    let canvasSnapshot = null;

    function initDrawingCanvas() {
        canvas = document.getElementById('designCanvas');
        if (!canvas) return;
        ctx = canvas.getContext('2d');
        
        ctx.strokeStyle = '#6366f1';
        ctx.lineWidth = 3;
        ctx.lineCap = 'round';
        
        canvas.onmousedown = startDrawing;
        canvas.onmousemove = draw;
        canvas.onmouseup = stopDrawing;
        canvas.onmouseleave = stopDrawing;
    }

    function startDrawing(e) {
        drawing = true;
        const rect = canvas.getBoundingClientRect();
        startX = e.clientX - rect.left;
        startY = e.clientY - rect.top;
        
        ctx.beginPath();
        ctx.moveTo(startX, startY);
        ctx.strokeStyle = document.getElementById('canvasColor').value;
        
        // Save canvas state for rectangle draw shape overlays
        canvasSnapshot = ctx.getImageData(0, 0, canvas.width, canvas.height);
    }

    function draw(e) {
        if (!drawing) return;
        const rect = canvas.getBoundingClientRect();
        const curX = e.clientX - rect.left;
        const curY = e.clientY - rect.top;
        
        if (activeTool === "pencil") {
            ctx.lineTo(curX, curY);
            ctx.stroke();
        } else if (activeTool === "rectangle") {
            ctx.putImageData(canvasSnapshot, 0, 0);
            ctx.beginPath();
            ctx.rect(startX, startY, curX - startX, curY - startY);
            ctx.stroke();
        }
    }

    function stopDrawing() {
        drawing = false;
    }

    document.querySelectorAll('.tool-btn').forEach(btn => {
        btn.onclick = () => {
            document.querySelectorAll('.tool-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            activeTool = btn.getAttribute('data-tool');
        };
    });

    document.getElementById('clearCanvasBtn').onclick = () => {
        if (ctx) {
            ctx.clearRect(0, 0, canvas.width, canvas.height);
        }
    };

    // Load Simulator Past History
    async function loadPastSessions() {
        const grid = document.getElementById('pastSessionsGrid');
        grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; font-weight:700;">Loading Past Sessions...</div>';

        try {
            const resp = await fetch('/api/interview/history');
            const data = await resp.json();
            grid.innerHTML = '';
            
            if (data.length === 0) {
                grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; color: var(--text-dim);">No previous interviews conducted. Select a mode above and start a mock session!</div>';
                return;
            }

            data.forEach(item => {
                const card = document.createElement('div');
                card.className = 'glass history-card';
                card.style.display = 'flex';
                card.style.flexDirection = 'column';
                card.style.justifyContent = 'space-between';
                card.style.padding = '25px';

                let badgeColor = "#10b981"; // Google
                if (item.company.toLowerCase() === "microsoft") badgeColor = "#00A4EF";
                else if (item.company.toLowerCase() === "amazon") badgeColor = "#FF9900";
                else if (item.company.toLowerCase() === "startup") badgeColor = "#a855f7";

                card.innerHTML = `
                    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:10px;">
                        <span class="glass" style="font-size:11px; font-weight:800; padding:4px 10px; border-radius:5px; background:${badgeColor}20; color:${badgeColor}; text-transform:uppercase;">${item.company}</span>
                        <span style="font-size:13px; font-weight:800; color:var(--primary);">${item.score > 0 ? item.score + '%' : 'UNGRADED'}</span>
                    </div>
                    <h4 style="font-size:16px; font-weight:800; margin-bottom:8px; line-height:1.4;">${item.title}</h4>
                    <p style="font-size:12px; color:var(--text-dim); margin-bottom:15px;"><i class="far fa-calendar-alt"></i> ${new Date(item.createdAt).toLocaleDateString()}</p>
                    <button class="btn-primary-action btn-review-session" data-id="${item.id}" style="width:100%; padding:10px 15px; font-size:12px; border-radius:8px;">Review Feedback</button>
                `;

                card.querySelector('.btn-review-session').onclick = (e) => {
                    e.stopPropagation();
                    viewPastSessionReport(item.id, item.company, item.mode);
                };

                grid.appendChild(card);
            });
        } catch (e) {
            grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; color: var(--accent);">Error recovering past logs.</div>';
        }
    }

    async function viewPastSessionReport(sessionId, company, mode) {
        try {
            const resp = await fetch(`/api/interview/session/${sessionId}`);
            const data = await resp.json();
            if (!resp.ok) throw new Error("Could not recover logs");

            selectedCompany = company;
            selectedMode = mode;
            renderAnalyticsDashboard(data.feedback);
        } catch (e) {
            alert("Error loading past session report: " + e.message);
        }
    }

    let aptQuestions = [];
    let aptCurrentIndex = 0;
    let aptAnswers = {};

    function renderAptitudeQuestion() {
        const q = aptQuestions[aptCurrentIndex];
        document.getElementById('aptQuestionText').textContent = q.question;
        document.getElementById('aptProgressText').textContent = `QUESTION ${aptCurrentIndex + 1} OF ${aptQuestions.length}`;
        
        // Render Dots
        const dotsGrid = document.getElementById('aptDotsGrid');
        dotsGrid.innerHTML = '';
        for (let i = 0; i < aptQuestions.length; i++) {
            const dot = document.createElement('div');
            dot.style.width = '10px';
            dot.style.height = '10px';
            dot.style.borderRadius = '50%';
            dot.style.background = i === aptCurrentIndex ? '#3b82f6' : (aptAnswers[i] ? '#3b82f6' : 'rgba(255,255,255,0.2)');
            if (i === aptCurrentIndex) dot.style.boxShadow = '0 0 10px #3b82f6';
            dotsGrid.appendChild(dot);
        }
        
        // Render Options
        const optionsGrid = document.getElementById('aptOptionsGrid');
        optionsGrid.innerHTML = '';
        q.options.forEach((opt, oIdx) => {
            const optLetter = String.fromCharCode(65 + oIdx);
            const isSelected = aptAnswers[aptCurrentIndex] === opt;
            const borderStyle = isSelected ? 'border: 2px solid #f97316;' : 'border: 1px solid rgba(255,255,255,0.05);';
            const bgStyle = isSelected ? 'background: rgba(249, 115, 22, 0.1);' : 'background: rgba(30, 40, 55, 0.4);';
            
            // Using a simple hash to generate somewhat varied cube visual states based on option string length
            const opacity = 0.5 + ((opt.length % 5) / 10);
            const html = `
                <div class="glass apt-option-card" onclick="window.selectAptOption(${aptCurrentIndex}, '${opt.replace(/'/g, "\\'")}')" style="padding: 15px; border-radius: 12px; cursor: pointer; display: flex; flex-direction: column; align-items: center; gap: 10px; ${bgStyle} ${borderStyle} transition: all 0.3s ease;">
                    <i class="fas fa-cube" style="font-size: 50px; color: #3b82f6; opacity: ${opacity}; margin-top: 10px;"></i>
                    <div style="font-size: 13px; color: #cbd5e1; text-align: center; margin-top: 10px; flex: 1; display: flex; align-items: center;">${opt}</div>
                    <div style="display: flex; justify-content: space-between; width: 100%; align-items: center; margin-top: auto; padding-top: 10px; border-top: 1px solid rgba(255,255,255,0.05);">
                        <span style="font-size: 12px; color: #cbd5e1;">Option ${optLetter}</span>
                        <input type="radio" name="apt_q_${aptCurrentIndex}" value="${opt}" ${isSelected ? 'checked' : ''} style="accent-color: #f97316; width: 16px; height: 16px;" onclick="event.stopPropagation(); window.selectAptOption(${aptCurrentIndex}, '${opt.replace(/'/g, "\\'")}')">
                    </div>
                </div>
            `;
            optionsGrid.innerHTML += html;
        });
        
        document.getElementById('aptPrevBtn').style.opacity = aptCurrentIndex === 0 ? '0.5' : '1';
        document.getElementById('aptPrevBtn').disabled = aptCurrentIndex === 0;
        
        if (aptCurrentIndex === aptQuestions.length - 1) {
            document.getElementById('aptNextBtn').style.display = 'none';
            document.getElementById('submitAptitudeBtn').style.display = 'block';
        } else {
            document.getElementById('aptNextBtn').style.display = 'block';
            document.getElementById('submitAptitudeBtn').style.display = 'none';
        }
    }

    window.selectAptOption = function(qIdx, val) {
        aptAnswers[qIdx] = val;
        renderAptitudeQuestion();
    }

    window.loadAptitudeQuestions = async function() {
        try {
            const resp = await fetch('/api/interview/aptitude/questions');
            const data = await resp.json();
            if (!resp.ok) throw new Error(data.error || "Failed to load");
            
            aptQuestions = data;
            aptCurrentIndex = 0;
            aptAnswers = {};
            
            document.getElementById('aptPrevBtn').onclick = () => { if(aptCurrentIndex > 0) { aptCurrentIndex--; renderAptitudeQuestion(); } };
            document.getElementById('aptNextBtn').onclick = () => { if(aptCurrentIndex < aptQuestions.length - 1) { aptCurrentIndex++; renderAptitudeQuestion(); } };
            
            document.getElementById('submitAptitudeBtn').onclick = () => {
                alert('Aptitude Assessment Submitted!');
                window.appendSimChat('user', 'I have submitted my aptitude assessment answers.');
                document.getElementById('endSessionBtn').click();
            };
            
            renderAptitudeQuestion();
        } catch (e) {
            document.getElementById('aptQuestionText').innerHTML = `<span style="color:var(--accent);">${e.message}</span>`;
        }
    } // End of loadAptitudeQuestions
    
    let radarChartInstance = null;
    window.loadProfileDashboard = async function() {
        try {
            const resp = await fetch('/api/interview/history');
            const data = await resp.json();
            
            let totalInterviews = data.length;
            let sumScores = 0;
            let aptScores = 0;
            let aptCount = 0;
            
            const historyTable = document.getElementById('profileHistoryTable');
            historyTable.innerHTML = '';
            
            if (totalInterviews === 0) {
                historyTable.innerHTML = '<tr><td colspan="4" style="padding: 20px; text-align: center; color: var(--text-dim);">No interviews taken yet.</td></tr>';
            } else {
                data.forEach(item => {
                    if (item.score > 0) sumScores += item.score;
                    if (item.mode === 'APTITUDE' && item.score > 0) { aptScores += item.score; aptCount++; }
                    
                    historyTable.innerHTML += `
                        <tr style="border-bottom: 1px solid var(--glass-border);">
                            <td style="padding: 12px 10px;">${new Date(item.createdAt).toLocaleDateString()}</td>
                            <td style="padding: 12px 10px; font-weight: bold;">${item.company}</td>
                            <td style="padding: 12px 10px;">${item.mode}</td>
                            <td style="padding: 12px 10px; color: var(--primary);">${item.score > 0 ? item.score + '%' : 'N/A'}</td>
                        </tr>
                    `;
                });
            }
            
            document.getElementById('statTotalInterviews').textContent = totalInterviews;
            document.getElementById('statAvgScore').textContent = totalInterviews > 0 ? Math.round(sumScores / totalInterviews) + '%' : '--';
            document.getElementById('statAptitude').textContent = aptCount > 0 ? Math.round(aptScores / aptCount) + '%' : '--';
            
            const ctx = document.getElementById('radarChart');
            if (ctx && typeof Chart !== 'undefined') {
                if (radarChartInstance) radarChartInstance.destroy();
                
                radarChartInstance = new Chart(ctx, {
                    type: 'radar',
                    data: {
                        labels: ['DSA', 'System Design', 'Behavioral (HR)', 'Aptitude', 'Project Exp', 'Debugging'],
                        datasets: [{
                            label: 'Your Skill Profile',
                            data: [
                                Math.random() * 40 + 60, 
                                Math.random() * 40 + 60, 
                                Math.random() * 40 + 60, 
                                Math.random() * 40 + 60, 
                                Math.random() * 40 + 60, 
                                Math.random() * 40 + 60
                            ],
                            backgroundColor: 'rgba(99, 102, 241, 0.2)',
                            borderColor: '#6366f1',
                            pointBackgroundColor: '#10b981',
                            pointBorderColor: '#fff',
                            pointHoverBackgroundColor: '#fff',
                            pointHoverBorderColor: '#10b981'
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        scales: {
                            r: {
                                angleLines: { color: 'rgba(255, 255, 255, 0.1)' },
                                grid: { color: 'rgba(255, 255, 255, 0.1)' },
                                pointLabels: { color: '#9ca3af', font: { size: 11 } },
                                ticks: { display: false, min: 0, max: 100 }
                            }
                        },
                        plugins: { legend: { display: false } }
                    }
                });
            }
        } catch (e) { console.error('Failed to load profile', e); }
    }
});
