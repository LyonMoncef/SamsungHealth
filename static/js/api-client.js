// V2.3.3.2 — Fetch wrapper with unified error handling (429/423/401).
// Uses credentials: 'include' so the httpOnly refresh cookie travels on /auth/refresh.
(function () {
  var API_BASE = '';

  async function _handleResponse(r) {
    var body = null;
    try { body = await r.json(); } catch (e) { /* not JSON */ }
    if (r.status === 429) {
      return {
        ok: false,
        status: 429,
        code: 'rate_limit_exceeded',
        retryAfter: r.headers.get('Retry-After'),
      };
    }
    if (r.status === 423) {
      return { ok: false, status: 423, code: 'account_locked' };
    }
    if (r.status >= 400) {
      var detail = (body && body.detail) ? body.detail : 'unknown_error';
      return { ok: false, status: r.status, code: detail };
    }
    return { ok: true, status: r.status, body: body };
  }

  async function apiPost(path, payload, opts) {
    opts = opts || {};
    var headers = { 'Content-Type': 'application/json' };
    if (opts.headers) {
      for (var k in opts.headers) {
        if (Object.prototype.hasOwnProperty.call(opts.headers, k)) {
          headers[k] = opts.headers[k];
        }
      }
    }
    if (opts.bearer) headers['Authorization'] = 'Bearer ' + opts.bearer;
    if (opts.regToken) headers['X-Registration-Token'] = opts.regToken;
    var r = await fetch(API_BASE + path, {
      method: 'POST',
      headers: headers,
      body: JSON.stringify(payload || {}),
      credentials: 'include',
    });
    return _handleResponse(r);
  }

  window.dsApiClient = { apiPost: apiPost };
})();
