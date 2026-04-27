// V2.3.3.2 — Auth form helpers: validation, FLASH_PARAMS whitelist,
// history.replaceState purge after URL token read, .textContent only.
(function () {
  var FLASH_PARAMS_WHITELIST = {
    registered: { '1': true, '0': true },
    expired: { '1': true },
    verified: { '1': true },
    reset: { '1': true },
  };

  function readFlashParam(name) {
    var params = new URLSearchParams(window.location.search);
    var raw = params.get(name);
    if (!raw) return null;
    var allowed = FLASH_PARAMS_WHITELIST[name];
    if (allowed && allowed[raw]) return raw;
    return null;
  }

  function readQueryToken() {
    var params = new URLSearchParams(window.location.search);
    return params.get('token');
  }

  function readFragmentParam(name) {
    var hash = window.location.hash || '';
    if (hash.charAt(0) === '#') hash = hash.substring(1);
    var params = new URLSearchParams(hash);
    return params.get(name);
  }

  function purgeUrlState() {
    try {
      window.history.replaceState({}, '', window.location.pathname);
    } catch (e) { /* ignore */ }
  }

  function setMessage(el, text, kind) {
    if (!el) return;
    el.textContent = text;
    el.className = 'auth-message ' + (kind === 'success' ? 'auth-message-success' : 'auth-message-error');
  }

  function validatePassword(pwd) {
    if (typeof pwd !== 'string') return false;
    return pwd.length >= 12;
  }

  window.dsAuthForm = {
    readFlashParam: readFlashParam,
    readQueryToken: readQueryToken,
    readFragmentParam: readFragmentParam,
    purgeUrlState: purgeUrlState,
    setMessage: setMessage,
    validatePassword: validatePassword,
  };
})();
