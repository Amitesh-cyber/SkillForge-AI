document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('loginForm');
    const emailInput = document.getElementById('emailInput');
    const passwordInput = document.getElementById('passwordInput');
    const loginError = document.getElementById('loginError');
    const googleLoginBtn = document.getElementById('googleLoginBtn');

    // Create Modal for Google Account Selection
    const modalHtml = `
        <div id="googleModal" style="display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); z-index: 1000; align-items: center; justify-content: center;">
            <div class="glass" style="background: white; padding: 30px; border-radius: 24px; width: 340px; text-align: center;">
                <img src="https://www.svgrepo.com/show/355037/google.svg" width="40" style="margin-bottom: 20px;">
                <h3 style="margin-bottom: 10px; font-family: 'Outfit';">Choose an account</h3>
                <p style="font-size: 13px; color: #64748b; margin-bottom: 20px;">to continue to SkillForge AI</p>
                <div style="display: flex; flex-direction: column; gap: 10px;">
                    <button class="account-opt" data-user="Amitesh Kumar" data-email="amiteshkumar23@lpu.in" style="padding: 12px; border: 1px solid #e2e8f0; border-radius: 12px; background: none; cursor: pointer; display: flex; align-items: center; gap: 10px;">
                        <img src="https://api.dicebear.com/7.x/avataaars/svg?seed=Amitesh" width="30" style="border-radius: 50%;">
                        <div style="text-align: left;">
                            <div style="font-weight: 700; font-size: 13px;">Amitesh Kumar</div>
                            <div style="font-size: 11px; color: #64748b;">amiteshkumar23@lpu.in</div>
                        </div>
                    </button>
                    <button class="account-opt" data-user="Skill Explorer" data-email="explorer@skillforge.ai" style="padding: 12px; border: 1px solid #e2e8f0; border-radius: 12px; background: none; cursor: pointer; display: flex; align-items: center; gap: 10px;">
                        <img src="https://api.dicebear.com/7.x/avataaars/svg?seed=Felix" width="30" style="border-radius: 50%;">
                        <div style="text-align: left;">
                            <div style="font-weight: 700; font-size: 13px;">Skill Explorer</div>
                            <div style="font-size: 11px; color: #64748b;">explorer@skillforge.ai</div>
                        </div>
                    </button>
                    <button id="closeModal" style="margin-top: 10px; border: none; background: none; color: #6366f1; font-weight: 600; cursor: pointer;">Cancel</button>
                </div>
            </div>
        </div>
    `;
    document.body.insertAdjacentHTML('beforeend', modalHtml);

    // Handle Login
    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = emailInput.value;
        const password = passwordInput.value;

        try {
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password })
            });

            const data = await response.json();
            if (response.ok) {
                localStorage.setItem('isLoggedIn', 'true');
                localStorage.setItem('user', JSON.stringify(data));
                window.location.href = '/index.html';
            } else {
                loginError.textContent = data.error || 'Login failed';
                loginError.style.display = 'block';
            }
        } catch (err) {
            loginError.textContent = 'Server error. Please try again.';
            loginError.style.display = 'block';
        }
    });

    // Handle Google Login Modal
    if (googleLoginBtn) {
        googleLoginBtn.addEventListener('click', () => {
            document.getElementById('googleModal').style.display = 'flex';
        });
    }

    document.getElementById('closeModal').onclick = () => {
        document.getElementById('googleModal').style.display = 'none';
    };

    document.querySelectorAll('.account-opt').forEach(btn => {
        btn.onclick = () => {
            const user = {
                id: 1,
                username: btn.getAttribute('data-user'),
                email: btn.getAttribute('data-email')
            };
            localStorage.setItem('isLoggedIn', 'true');
            localStorage.setItem('user', JSON.stringify(user));
            
            btn.style.background = '#f8fafc';
            btn.innerHTML = '<i class="fas fa-spinner fa-spin" style="margin: auto;"></i>';
            
            setTimeout(() => {
                window.location.href = '/index.html';
            }, 1000);
        };
    });

    const toggleAuthBtn = document.getElementById('toggleAuthBtn');
    if (toggleAuthBtn) {
        toggleAuthBtn.addEventListener('click', (e) => {
            e.preventDefault();
            alert('Signup is coming soon! For now, use the Google login or dummy credentials.');
        });
    }
});
