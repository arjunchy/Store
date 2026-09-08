// Node 25+ exposes a stub global `localStorage` without getItem/setItem
// which breaks `typeof localStorage !== 'undefined'` checks in Next/webpack.
// This polyfill MUST be loaded before any other code (require -r).
if (typeof globalThis.localStorage === "undefined" || typeof globalThis.localStorage.getItem !== "function") {
  const store = new Map();
  const mock = {
    getItem(key) {
      return store.has(String(key)) ? store.get(String(key)) : null;
    },
    setItem(key, value) {
      store.set(String(key), String(value));
    },
    removeItem(key) {
      store.delete(String(key));
    },
    clear() {
      store.clear();
    },
    key(n) {
      return Array.from(store.keys())[n] ?? null;
    },
    get length() {
      return store.size;
    },
  };
  try {
    // Node 25 defines localStorage as a getter on globalThis — override it
    // @ts-ignore
    delete globalThis.localStorage;
  } catch {}
  // @ts-ignore
  globalThis.localStorage = mock;
  // @ts-ignore
  globalThis.sessionStorage = mock;
  // Also patch global for older code
  try {
    // @ts-ignore
    if (typeof global !== "undefined" && global.localStorage !== mock) {
      // @ts-ignore
      delete global.localStorage;
      // @ts-ignore
      global.localStorage = mock;
      // @ts-ignore
      global.sessionStorage = mock;
    }
  } catch {}
}
