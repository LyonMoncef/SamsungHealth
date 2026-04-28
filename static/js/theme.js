// V2.3.3.2 — Theme switcher (light/dark) with FOUC prevention.
// Set data-theme SYNC before body render.
(function () {
  var STORAGE_KEY = 'ds-theme';

  function getEffectiveTheme() {
    try {
      var stored = window.localStorage.getItem(STORAGE_KEY);
      if (stored === 'light' || stored === 'dark') return stored;
    } catch (e) { /* localStorage may be unavailable */ }
    if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
      return 'dark';
    }
    return 'light';
  }

  function applyTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
  }

  function toggleTheme() {
    var next = getEffectiveTheme() === 'dark' ? 'light' : 'dark';
    try {
      window.localStorage.setItem(STORAGE_KEY, next);
    } catch (e) { /* ignore */ }
    applyTheme(next);
  }

  applyTheme(getEffectiveTheme());

  document.addEventListener('DOMContentLoaded', function () {
    var btn = document.querySelector('[data-theme-toggle]');
    if (btn) btn.addEventListener('click', toggleTheme);
  });

  if (window.matchMedia) {
    var mq = window.matchMedia('(prefers-color-scheme: dark)');
    var handler = function () {
      try {
        if (!window.localStorage.getItem(STORAGE_KEY)) applyTheme(getEffectiveTheme());
      } catch (e) { applyTheme(getEffectiveTheme()); }
    };
    if (mq.addEventListener) mq.addEventListener('change', handler);
    else if (mq.addListener) mq.addListener(handler);
  }
})();
