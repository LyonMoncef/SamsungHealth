// V2.3.3.3 — Admin SPA: fetch wrapper, table render, action handlers, polling.
// All DOM mutations via .textContent / DOM API (Trusted Types compatible).
(function () {
  'use strict';

  var STORAGE_KEY = 'admin_token';
  var TOKEN_HEADER = 'X-' + 'Registration-Token';
  var POLL_MS = 30000;
  var pollHandle = null;

  function getToken() {
    try {
      return window.sessionStorage.getItem(STORAGE_KEY);
    } catch (e) {
      return null;
    }
  }

  function clearToken() {
    try { window.sessionStorage.removeItem(STORAGE_KEY); } catch (e) { /* ignore */ }
  }

  function redirectLogin() {
    var rt = encodeURIComponent(window.location.pathname);
    window.location.href = '/admin/login?return_to=' + rt;
  }

  function adminFetch(path, options) {
    options = options || {};
    var token = getToken();
    if (!token) {
      redirectLogin();
      return Promise.reject(new Error('no_token'));
    }
    var headers = options.headers || {};
    headers[TOKEN_HEADER] = token;
    headers['Accept'] = 'application/json';
    if (options.body && !headers['Content-Type']) {
      headers['Content-Type'] = 'application/json';
    }
    options.headers = headers;
    options.credentials = 'same-origin';
    return fetch(path, options).then(function (r) {
      if (r.status === 401) {
        clearToken();
        redirectLogin();
        throw new Error('unauthorized');
      }
      return r;
    });
  }

  function setMessage(id, text, isError) {
    var el = document.getElementById(id);
    if (!el) return;
    el.textContent = text;
    el.dataset.kind = isError ? 'error' : 'ok';
  }

  function clearChildren(node) {
    while (node.firstChild) node.removeChild(node.firstChild);
  }

  // ── Users list page ───────────────────────────────────────────────
  function renderUsers(users) {
    var tbody = document.getElementById('admin-users-tbody');
    var empty = document.getElementById('admin-users-empty');
    if (!tbody) return;
    clearChildren(tbody);
    if (!users || !users.length) {
      if (empty) empty.hidden = false;
      return;
    }
    if (empty) empty.hidden = true;
    users.forEach(function (u) {
      var tr = document.createElement('tr');
      tr.appendChild(textCell(u.email || ''));
      var statusCell = document.createElement('td');
      var statusText;
      var statusCls;
      if (u.locked_until) { statusText = 'locked'; statusCls = 'status-locked'; }
      else if (u.is_active === false) { statusText = 'disabled'; statusCls = 'status-disabled'; }
      else { statusText = 'active'; statusCls = 'status-active'; }
      statusCell.textContent = statusText;
      statusCell.className = statusCls;
      tr.appendChild(statusCell);
      tr.appendChild(textCell(String(u.failed_login_count != null ? u.failed_login_count : 0)));
      tr.appendChild(textCell(u.last_login_at || '—'));
      var actions = document.createElement('td');
      var detailBtn = document.createElement('button');
      detailBtn.type = 'button';
      detailBtn.textContent = 'Détails';
      detailBtn.addEventListener('click', function () {
        window.location.href = '/admin/users/' + encodeURIComponent(u.id);
      });
      actions.appendChild(detailBtn);
      tr.appendChild(actions);
      tbody.appendChild(tr);
    });
  }

  function textCell(value) {
    var td = document.createElement('td');
    td.textContent = value;
    return td;
  }

  function loadUsers() {
    adminFetch('/admin/users').then(function (r) {
      return r.json();
    }).then(function (data) {
      window.__adminUsers = data;
      renderUsers(data);
    }).catch(function () { /* handled in adminFetch */ });
  }

  function applyFilter(query) {
    var all = window.__adminUsers || [];
    if (!query) { renderUsers(all); return; }
    var q = query.toLowerCase();
    renderUsers(all.filter(function (u) {
      return (u.email || '').toLowerCase().indexOf(q) !== -1;
    }));
  }

  // ── User detail page ───────────────────────────────────────────────
  function uuidFromPath() {
    var m = window.location.pathname.match(/^\/admin\/users\/([0-9a-f-]+)/i);
    return m ? m[1] : null;
  }

  function renderUserDetail(data) {
    var info = document.getElementById('admin-user-info');
    if (info && data.user) {
      clearChildren(info);
      Object.keys(data.user).forEach(function (k) {
        var dt = document.createElement('dt');
        dt.textContent = k;
        var dd = document.createElement('dd');
        var v = data.user[k];
        dd.textContent = v == null ? '—' : String(v);
        info.appendChild(dt);
        info.appendChild(dd);
      });
    }
    var providers = document.getElementById('admin-user-providers');
    if (providers) {
      clearChildren(providers);
      (data.providers || []).forEach(function (p) {
        var li = document.createElement('li');
        li.textContent = (p.provider || '?') + ' — ' + (p.provider_email || p.provider_sub || '');
        providers.appendChild(li);
      });
      if (!(data.providers || []).length) {
        var li = document.createElement('li');
        li.textContent = 'Aucun provider lié.';
        providers.appendChild(li);
      }
    }
    var events = document.getElementById('admin-user-events');
    if (events) {
      clearChildren(events);
      (data.recent_events || []).forEach(function (ev) {
        var li = document.createElement('li');
        li.textContent = (ev.event_type || '?') + ' @ ' + (ev.created_at || '');
        events.appendChild(li);
      });
      if (!(data.recent_events || []).length) {
        var li = document.createElement('li');
        li.textContent = 'Aucun événement.';
        events.appendChild(li);
      }
    }
  }

  function loadUserDetail(uid) {
    adminFetch('/admin/users/' + encodeURIComponent(uid)).then(function (r) {
      if (r.status === 404) {
        setMessage('admin-user-message', 'Utilisateur introuvable.', true);
        return null;
      }
      return r.json();
    }).then(function (data) {
      if (data) renderUserDetail(data);
    }).catch(function () { /* handled */ });
  }

  function lockUser(uid, minutes) {
    return adminFetch('/admin/users/' + encodeURIComponent(uid) + '/lock', {
      method: 'POST',
      body: JSON.stringify({ duration_minutes: minutes, reason: 'admin-ui' })
    });
  }

  function unlockUser(uid) {
    return adminFetch('/admin/users/' + encodeURIComponent(uid) + '/unlock', {
      method: 'POST'
    });
  }

  // ── Pending verifications page ─────────────────────────────────────
  function renderPending(items) {
    var list = document.getElementById('admin-pending-list');
    var empty = document.getElementById('admin-pending-empty');
    if (!list) return;
    clearChildren(list);
    if (!items || !items.length) {
      if (empty) empty.hidden = false;
      return;
    }
    if (empty) empty.hidden = true;
    items.forEach(function (item) {
      var li = document.createElement('li');
      var meta = document.createElement('div');
      meta.textContent = (item.purpose || '?') + ' — ' + (item.user_email || '');
      li.appendChild(meta);
      var link = document.createElement('span');
      link.className = 'verify-link';
      link.textContent = item.verify_link || '';
      li.appendChild(link);
      var btn = document.createElement('button');
      btn.type = 'button';
      btn.textContent = 'Copier le lien';
      btn.addEventListener('click', function () {
        if (navigator.clipboard) {
          navigator.clipboard.writeText(item.verify_link || '').then(function () {
            btn.textContent = 'Copié !';
            setTimeout(function () { btn.textContent = 'Copier le lien'; }, 1500);
          });
        }
      });
      li.appendChild(btn);
      list.appendChild(li);
    });
  }

  function loadPending() {
    adminFetch('/admin/pending-verifications').then(function (r) {
      return r.json();
    }).then(function (data) {
      renderPending(data);
    }).catch(function () { /* handled */ });
  }

  function startPolling(fn) {
    if (pollHandle != null) return;
    pollHandle = window.setInterval(fn, POLL_MS);
  }

  function stopPolling() {
    if (pollHandle != null) {
      window.clearInterval(pollHandle);
      pollHandle = null;
    }
  }

  // ── Wire on DOM ready ───────────────────────────────────────────────
  document.addEventListener('DOMContentLoaded', function () {
    var logout = document.getElementById('admin-logout');
    if (logout) {
      logout.addEventListener('click', function () {
        clearToken();
        window.location.href = '/admin/login';
      });
    }

    var path = window.location.pathname;
    if (path === '/admin/users' || path === '/admin/users/') {
      loadUsers();
      var search = document.getElementById('admin-users-search');
      if (search) {
        search.addEventListener('input', function () { applyFilter(search.value); });
      }
    } else if (/^\/admin\/users\/[0-9a-f-]+/i.test(path)) {
      var uid = uuidFromPath();
      if (uid) loadUserDetail(uid);
      document.querySelectorAll('[data-admin-action]').forEach(function (btn) {
        btn.addEventListener('click', function () {
          var action = btn.dataset.adminAction;
          if (!uid) return;
          var p = null;
          if (action === 'lock-15') p = lockUser(uid, 15);
          else if (action === 'lock-60') p = lockUser(uid, 60);
          else if (action === 'unlock') p = unlockUser(uid);
          if (p) p.then(function () { loadUserDetail(uid); });
        });
      });
    } else if (path === '/admin/pending-verifications' || path === '/admin/pending-verifications/') {
      loadPending();
      startPolling(loadPending);
      document.addEventListener('visibilitychange', function () {
        if (document.hidden) stopPolling();
        else { loadPending(); startPolling(loadPending); }
      });
    }
  });
})();
