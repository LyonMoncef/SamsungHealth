// V2.3.3.3 — dsColor helper: lit --ds-* depuis CSS computed avec sanitizer.
(function (global) {
  'use strict';

  var COLOR_RE = /^(#[0-9a-fA-F]{3,8}|rgba?\([^)]+\)|hsla?\([^)]+\)|[a-z]+)$/;

  function dsColor(token) {
    var raw = '';
    try {
      raw = getComputedStyle(document.documentElement)
        .getPropertyValue('--ds-' + token)
        .trim();
    } catch (e) { /* ignore */ }
    if (!COLOR_RE.test(raw)) {
      if (typeof console !== 'undefined' && console.warn) {
        console.warn('[ds-colors] invalid value for --ds-' + token + ', falling back: ' + raw);
      }
      return '#000000';
    }
    return raw;
  }

  global.dsColor = dsColor;
})(typeof window !== 'undefined' ? window : this);
