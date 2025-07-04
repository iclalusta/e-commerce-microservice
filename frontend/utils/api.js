// API helper for all requests
// Usage: api.get(url), api.post(url, data), api.del(url)
const api = (() => {
  const API_BASE = '';
  function getToken() {
    return localStorage.getItem('jwt_token');
  }
  function getUserId() {
    return localStorage.getItem('user_id');
  }
  async function request(method, url, data) {
    const headers = {
      'Content-Type': 'application/json',
    };
    const token = getToken();
    const userId = getUserId();
    if (token) headers['Authorization'] = `Bearer ${token}`;
    if (userId) headers['X-User-Id'] = userId;
    const options = {
      method,
      headers,
    };
    if (data) options.body = JSON.stringify(data);
    const res = await fetch(API_BASE + url, options);
    let body;
    try {
      body = await res.json();
    } catch {
      body = null;
    }
    if (!res.ok) {
      throw (body && body.message) || res.statusText || 'API Error';
    }
    return body;
  }
  return {
    get: (url) => request('GET', url),
    post: (url, data) => request('POST', url, data),
    del: (url) => request('DELETE', url),
  };
})();

// Export for use in other scripts
window.api = api; 