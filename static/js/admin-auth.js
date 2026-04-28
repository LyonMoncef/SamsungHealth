// V2.3.3.3 — Admin login flow. Sessionstorage token, probe endpoint validation.
(function () {
  'use strict';

  var STORAGE_KEY = 'admin_token';
  var TOKEN_HEADER = 'X-' + 'Registration-Token';

  function showMessage(text, isError) {
    var el = document.getElementById('admin-login-message');
    if (!el) return;
    el.textContent = text;
    el.dataset.kind = isError ? 'error' : 'ok';
  }

  function validateReturnTo(raw) {
    if (!raw) return null;
    if (/^\/admin\/[a-z0-9/-]+$/.test(raw)) return raw;
    return null;
  }

  function nextUrl() {
    var params = new URLSearchParams(window.location.search);
    return validateReturnTo(params.get('return_to')) || '/admin/users';
  }

  document.addEventListener('DOMContentLoaded', function () {
    var form = document.getElementById('admin-login-form');
    if (!form) return;
    form.addEventListener('submit', function (ev) {
      ev.preventDefault();
      var input = form.querySelector('input[name="admin-token"]');
      var token = input ? input.value.trim() : '';
      if (!token) {
        showMessage('Token requis.', true);
        return;
      }
      var headers = { 'Content-Type': 'application/json' };
      headers[TOKEN_HEADER] = token;
      fetch('/admin/probe', {
        method: 'POST',
        headers: headers,
        credentials: 'same-origin',
        body: '{}'
      }).then(function (r) {
        if (r.status === 200) {
          try {
            window.sessionStorage.setItem(STORAGE_KEY, token);
          } catch (e) { /* ignore */ }
          window.location.href = nextUrl();
        } else if (r.status === 401) {
          showMessage('Token invalide.', true);
        } else if (r.status === 429) {
          showMessage('Trop de tentatives. Réessayez plus tard.', true);
        } else {
          showMessage('Erreur (' + r.status + ').', true);
        }
      }).catch(function () {
        showMessage('Erreur réseau.', true);
      });
    });
  });
})();
