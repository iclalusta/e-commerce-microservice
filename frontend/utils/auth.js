// Auth logic for login, register, validate, logout
async function login(email, password) {
  const res = await api.post('/auth/login', { email, password });
  localStorage.setItem('jwt_token', res.token);
  const user = await validate();
  return user;
}
async function register(name, email, password) {
  await api.post('/auth/register', { name, email, password });
  // Auto-login after register
  return await login(email, password);
}
async function validate() {
  const res = await api.post('/auth/validate', {});
  localStorage.setItem('user_id', res.id);
  localStorage.setItem('user_name', res.name);
  localStorage.setItem('user_email', res.email);
  return res;
}
function logout() {
  localStorage.removeItem('jwt_token');
  localStorage.removeItem('user_id');
  localStorage.removeItem('user_name');
  localStorage.removeItem('user_email');
}
window.auth = { login, register, validate, logout };

// Attach form handlers after DOM loaded
window.addEventListener('DOMContentLoaded', () => {
  const loginForm = document.getElementById('loginForm');
  const registerForm = document.getElementById('registerForm');
  const loginError = document.getElementById('loginError');
  const registerError = document.getElementById('registerError');
  const spinnerContainer = document.getElementById('spinnerContainer');

  function showSpinner() {
    spinnerContainer.innerHTML = window.Spinner.render();
  }
  function hideSpinner() {
    spinnerContainer.innerHTML = '';
  }

  if (loginForm) {
    loginForm.onsubmit = async (e) => {
      e.preventDefault();
      loginError.textContent = '';
      showSpinner();
      try {
        const email = document.getElementById('loginEmail').value;
        const password = document.getElementById('loginPassword').value;
        await window.auth.login(email, password);
        hideSpinner();
        window.location.href = 'Home.html';
      } catch (err) {
        hideSpinner();
        loginError.textContent = err;
      }
    };
  }
  if (registerForm) {
    registerForm.onsubmit = async (e) => {
      e.preventDefault();
      registerError.textContent = '';
      showSpinner();
      try {
        const name = document.getElementById('registerName').value;
        const email = document.getElementById('registerEmail').value;
        const password = document.getElementById('registerPassword').value;
        await window.auth.register(name, email, password);
        hideSpinner();
        window.location.href = 'Home.html';
      } catch (err) {
        hideSpinner();
        registerError.textContent = err;
      }
    };
  }
}); 