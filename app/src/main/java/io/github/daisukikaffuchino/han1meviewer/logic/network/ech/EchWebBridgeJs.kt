package io.github.daisukikaffuchino.han1meviewer.logic.network.ech

internal object EchWebBridgeJs {

    fun script(protectedHosts: Array<String>): String =
        TEMPLATE.replace(
            "__ECH_PROTECTED__",
            protectedHosts.joinToString(",", "[", "]") { "\"$it\"" },
        )

    private val TEMPLATE = """
(function () {
  if (window.__echBridgeInstalled || !window.EchBridge) return;
  window.__echBridgeInstalled = true;

  var PROTECTED = __ECH_PROTECTED__;
  var pending = {};
  var sequence = 0;

  function log(message) {
    try { window.EchBridge.log(String(message).slice(0, 300)); } catch (ignored) {}
  }

  function hostOf(url) {
    try { return new URL(url, location.href).hostname.toLowerCase(); }
    catch (ignored) { return ''; }
  }

  function isProtected(url) {
    var host = hostOf(url);
    for (var i = 0; i < PROTECTED.length; i++) {
      var domain = PROTECTED[i];
      if (host === domain || host.endsWith('.' + domain)) return true;
    }
    return false;
  }

  function bytesToBase64(buffer) {
    var bytes = new Uint8Array(buffer);
    var binary = '';
    for (var i = 0; i < bytes.length; i++) binary += String.fromCharCode(bytes[i]);
    return btoa(binary);
  }

  function textToBase64(text) {
    return bytesToBase64(new TextEncoder().encode(text).buffer);
  }

  function base64ToBytes(value) {
    var binary = atob(value || '');
    var bytes = new Uint8Array(binary.length);
    for (var i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
    return bytes;
  }

  function base64ToText(value) {
    try { return new TextDecoder('utf-8').decode(base64ToBytes(value)); }
    catch (ignored) { return ''; }
  }

  window.__echBridgeResolve = function (id, payloadJson) {
    var resolve = pending[id];
    if (!resolve) return;
    delete pending[id];
    try { resolve(JSON.parse(payloadJson)); }
    catch (ignored) { resolve({ ok: false, status: 502, error: 'Invalid bridge response' }); }
  };

  function nativeSend(method, url, headers, bodyBase64) {
    return new Promise(function (resolve) {
      var id = String(++sequence);
      var timeout = setTimeout(function () {
        if (pending[id]) {
          delete pending[id];
          resolve({ ok: false, status: 502, error: 'ECH bridge timeout' });
        }
      }, 30000);
      pending[id] = function (payload) {
        clearTimeout(timeout);
        resolve(payload);
      };
      try {
        window.EchBridge.send(
          id, method, url, JSON.stringify(headers || {}), bodyBase64 || '', location.href
        );
      } catch (error) {
        delete pending[id];
        clearTimeout(timeout);
        resolve({ ok: false, status: 502, error: String(error) });
      }
    });
  }

  function headersToObject(headers) {
    var result = {};
    try {
      if (!headers) return result;
      if (typeof headers.forEach === 'function') {
        headers.forEach(function (value, key) { result[key] = value; });
      } else {
        for (var key in headers) {
          if (Object.prototype.hasOwnProperty.call(headers, key)) result[key] = headers[key];
        }
      }
    } catch (ignored) {}
    return result;
  }

  function serializeBody(body) {
    try {
      if (body === null || body === undefined) return { data: '', contentType: null };
      if (typeof body === 'string') {
        return { data: textToBase64(body), contentType: 'text/plain;charset=UTF-8' };
      }
      if (typeof URLSearchParams !== 'undefined' && body instanceof URLSearchParams) {
        return {
          data: textToBase64(body.toString()),
          contentType: 'application/x-www-form-urlencoded;charset=UTF-8'
        };
      }
      if (typeof FormData !== 'undefined' && body instanceof FormData) {
        var hasFile = false;
        body.forEach(function (value) {
          if (typeof File !== 'undefined' && value instanceof File) hasFile = true;
        });
        if (hasFile) return null;
        return {
          data: textToBase64(new URLSearchParams(body).toString()),
          contentType: 'application/x-www-form-urlencoded;charset=UTF-8'
        };
      }
      if (body instanceof ArrayBuffer) {
        return { data: bytesToBase64(body), contentType: null };
      }
      if (body && typeof body.byteLength === 'number' && body.buffer instanceof ArrayBuffer) {
        return {
          data: bytesToBase64(
            body.buffer.slice(body.byteOffset, body.byteOffset + body.byteLength)
          ),
          contentType: null
        };
      }
    } catch (ignored) {}
    return null;
  }

  function applyContentType(headers, contentType) {
    if (!contentType) return;
    for (var key in headers) {
      if (Object.prototype.hasOwnProperty.call(headers, key) &&
          key.toLowerCase() === 'content-type') return;
    }
    headers['Content-Type'] = contentType;
  }

  var originalFetch = window.fetch;
  if (originalFetch) {
    window.fetch = function (input, init) {
      try {
        var url = typeof input === 'string' ? input : (input && input.url);
        var method = String(
          (init && init.method) || (input && input.method) || 'GET'
        ).toUpperCase();
        var headers = headersToObject((init && init.headers) || (input && input.headers));
        if (url && method !== 'GET' && method !== 'HEAD' && isProtected(url)) {
          var serialized = serializeBody(init ? init.body : null);
          if (serialized) {
            applyContentType(headers, serialized.contentType);
            return nativeSend(method, url, headers, serialized.data).then(function (result) {
              var body = result.ok ? base64ToBytes(result.bodyB64) : new Uint8Array();
              return new Response(body, {
                status: result.ok ? result.status : (result.status || 502),
                statusText: result.statusText || '',
                headers: result.headers || {}
              });
            });
          }
          log('Fetch left untouched because its body cannot be serialized');
        }
      } catch (error) {
        log('Fetch hook failed: ' + error);
      }
      return originalFetch.apply(this, arguments);
    };
  }

  var OriginalXhr = window.XMLHttpRequest;
  if (OriginalXhr && OriginalXhr.prototype && OriginalXhr.prototype.open) {
    var originalOpen = OriginalXhr.prototype.open;
    var originalSetHeader = OriginalXhr.prototype.setRequestHeader;
    var originalSend = OriginalXhr.prototype.send;
    var originalAbort = OriginalXhr.prototype.abort;

    OriginalXhr.prototype.open = function (method, url, async) {
      this.__echRequest = {
        method: String(method || 'GET').toUpperCase(),
        url: String(url),
        headers: {},
        async: async !== false,
        aborted: false
      };
      return originalOpen.apply(this, arguments);
    };

    OriginalXhr.prototype.setRequestHeader = function (key, value) {
      if (this.__echRequest) this.__echRequest.headers[key] = value;
      return originalSetHeader.apply(this, arguments);
    };

    OriginalXhr.prototype.abort = function () {
      if (this.__echRequest) this.__echRequest.aborted = true;
      return originalAbort.apply(this, arguments);
    };

    OriginalXhr.prototype.send = function (body) {
      var state = this.__echRequest;
      try {
        if (state && state.async && state.method !== 'GET' && state.method !== 'HEAD' &&
            isProtected(state.url)) {
          var serialized = serializeBody(body);
          if (serialized) {
            applyContentType(state.headers, serialized.contentType);
            var xhr = this;
            nativeSend(state.method, state.url, state.headers, serialized.data)
              .then(function (result) { fakeXhrResponse(xhr, result, state); });
            return;
          }
          log('XHR left untouched because its body cannot be serialized');
        }
      } catch (error) {
        log('XHR hook failed: ' + error);
      }
      return originalSend.apply(this, arguments);
    };
  }

  function define(object, key, value) {
    try {
      Object.defineProperty(object, key, {
        configurable: true,
        get: function () { return value; }
      });
    } catch (ignored) {}
  }

  function makeEvent(type) {
    try { return new ProgressEvent(type); }
    catch (ignored) { return new Event(type); }
  }

  function fakeXhrResponse(xhr, result, state) {
    if (state.aborted) return;
    var status = result.ok ? result.status : (result.status || 502);
    var text = result.ok ? base64ToText(result.bodyB64) : (result.error || 'ECH bridge failed');
    var responseType = '';
    try { responseType = xhr.responseType || ''; } catch (ignored) {}
    var response = text;
    if (responseType === 'json') response = text ? JSON.parse(text) : null;
    if (responseType === 'arraybuffer') response = base64ToBytes(result.bodyB64).buffer;
    if (responseType === 'blob') response = new Blob([base64ToBytes(result.bodyB64)]);

    define(xhr, 'readyState', 4);
    define(xhr, 'status', status);
    define(xhr, 'statusText', result.statusText || '');
    define(xhr, 'responseURL', result.url || state.url);
    define(xhr, 'responseText', text);
    define(xhr, 'response', response);
    define(xhr, 'getAllResponseHeaders', function () {
      var output = '';
      var headers = result.headers || {};
      for (var key in headers) {
        if (Object.prototype.hasOwnProperty.call(headers, key)) {
          output += key + ': ' + headers[key] + '\r\n';
        }
      }
      return output;
    });
    define(xhr, 'getResponseHeader', function (name) {
      var headers = result.headers || {};
      for (var key in headers) {
        if (Object.prototype.hasOwnProperty.call(headers, key) &&
            key.toLowerCase() === String(name).toLowerCase()) return headers[key];
      }
      return null;
    });
    try { xhr.dispatchEvent(makeEvent('readystatechange')); } catch (ignored) {}
    try { xhr.dispatchEvent(makeEvent('load')); } catch (ignored) {}
    try { xhr.dispatchEvent(makeEvent('loadend')); } catch (ignored) {}
  }

  document.addEventListener('submit', function (event) {
    try {
      var form = event.target;
      if (!form || form.tagName !== 'FORM') return;
      var action = form.action || location.href;
      if (!isProtected(action) || !form.querySelector('input[type=password]')) return;
      if (form.querySelector('input[type=file]')) return;
      var encoding = String(form.getAttribute('enctype') || '').toLowerCase();
      if (encoding.indexOf('multipart') >= 0) return;
      event.preventDefault();
      event.stopPropagation();
      var body = new URLSearchParams(new FormData(form)).toString();
      window.EchBridge.postForm(action, textToBase64(body), location.href);
    } catch (error) {
      log('Form hook failed: ' + error);
    }
  }, true);
})();
""".trimIndent()
}
