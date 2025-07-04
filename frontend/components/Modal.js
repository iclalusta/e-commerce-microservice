// Modal component
(function () {
  let modalEl = null;
  function show(message, { confirmText, cancelText, onConfirm, onCancel } = {}) {
    hide();
    modalEl = document.createElement('div');
    modalEl.className = 'modal-overlay';
    modalEl.innerHTML = `
      <div class="modal-box">
        <div class="modal-message">${message}</div>
        <div class="modal-actions">
          ${confirmText ? `<button id="modalConfirm">${confirmText}</button>` : ''}
          ${cancelText ? `<button id="modalCancel">${cancelText}</button>` : ''}
        </div>
      </div>
      <style>
        .modal-overlay {
          position: fixed;
          top: 0; left: 0; right: 0; bottom: 0;
          background: rgba(0,0,0,0.3);
          display: flex;
          align-items: center;
          justify-content: center;
          z-index: 1000;
        }
        .modal-box {
          background: #fff;
          padding: 2rem 1.5rem;
          border-radius: 8px;
          min-width: 250px;
          max-width: 90vw;
          box-shadow: 0 2px 16px rgba(0,0,0,0.12);
        }
        .modal-message {
          margin-bottom: 1.2rem;
          font-size: 1.1rem;
        }
        .modal-actions {
          display: flex;
          gap: 1rem;
          justify-content: flex-end;
        }
        .modal-actions button {
          padding: 0.5rem 1.2rem;
          border: none;
          border-radius: 5px;
          background: #3498db;
          color: #fff;
          font-size: 1rem;
          cursor: pointer;
        }
        .modal-actions button#modalCancel {
          background: #e74c3c;
        }
      </style>
    `;
    document.body.appendChild(modalEl);
    if (confirmText) {
      document.getElementById('modalConfirm').onclick = () => {
        hide();
        if (onConfirm) onConfirm();
      };
    }
    if (cancelText) {
      document.getElementById('modalCancel').onclick = () => {
        hide();
        if (onCancel) onCancel();
      };
    }
  }
  function hide() {
    if (modalEl) {
      modalEl.remove();
      modalEl = null;
    }
  }
  window.Modal = { show, hide };
})(); 