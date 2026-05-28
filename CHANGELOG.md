# Changelog

## 1.0.42

- Added token expiration persistence and expiration check in session management
- Auto-clears expired local auth data (`token` / `expires_at`) on startup

## 1.0.41

- OAuth popup: auto-close on login complete; centered dialog (80% width, 50–80% height)

## 1.0.40

- Fixed WebView crash when opening deeplinks / external URLs
- Fixed WebView LRU cache removal (`removeCache` / `remove` by instance)

## 1.0.39

- Fixed some bugs

## 1.0.38

- Added automatic retry mechanism for non-success status codes (non-2xx responses) and connect timeout.