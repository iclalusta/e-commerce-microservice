// Header/navbar component
window.Header = {
  render: function (active) {
    const userName = localStorage.getItem('user_name') || 'User';
    setTimeout(() => {
      document.getElementById('logoutBtn').onclick = function(e) {
        e.preventDefault();
        window.Modal.show('Are you sure you want to logout?', {
          confirmText: 'Logout',
          cancelText: 'Cancel',
          onConfirm: () => {
            if(window.auth) window.auth.logout();
            window.location.href = 'Login.html';
          }
        });
      };
      // Notification polling
      function updateBadge(count) {
        const badge = document.getElementById('notificationBadge');
        if (badge) badge.textContent = count > 0 ? count : '';
      }
      async function pollNotifications() {
        const userId = localStorage.getItem('user_id');
        if (!userId) return;
        try {
          const notifications = await window.api.get(`/api/notification?userId=${userId}`);
          const unread = Array.isArray(notifications) ? notifications.filter(n => !n.read).length : 0;
          updateBadge(unread);
        } catch {
          updateBadge('');
        }
      }
      pollNotifications();
      if (!window._notificationPoller) {
        window._notificationPoller = setInterval(pollNotifications, 30000);
      }
    }, 0);
    return `
      <header class="navbar">
        <div class="logo" style="font-weight:bold;font-size:1.2rem;">E-Shop</div>
        <nav class="nav-links">
          <a href="Home.html" class="nav-link${active==='home' ? ' active' : ''}">Home</a>
          <a href="Cart.html" class="nav-link${active==='cart' ? ' active' : ''}">Cart</a>
          <a href="Orders.html" class="nav-link${active==='orders' ? ' active' : ''}">Orders</a>
          <a href="Profile.html" class="nav-link${active==='profile' ? ' active' : ''}">Profile</a>
          <span class="nav-link" id="notificationIcon" style="position:relative;">
            <span style="margin-right:0.5em;">🔔</span>
            <span class="notification-badge" id="notificationBadge"></span>
          </span>
          <a href="#" class="nav-link" id="logoutBtn">Logout</a>
        </nav>
      </header>
    `;
  }
}; 