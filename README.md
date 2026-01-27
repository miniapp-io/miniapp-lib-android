# lib-miniapp-android

## Introduction

lib-miniapp-android is an SDK for integrating Web3 MiniApp capabilities into Android applications, supporting rapid integration, running, and interaction of DApps/WebApps. This SDK provides rich features such as MiniApp lifecycle management, message communication, cloud storage, QR code scanning, sharing, etc., suitable for various scenarios like wallets, IM, and utility applications.

## Main Modules
- **miniapp-core**: Core functionality module, including MiniApp runtime, plugin management, data storage, network communication, etc.
- **miniapp-bridge**: Bridge module, responsible for communication between native and WebView/JS.
- **miniapp-bom**: Dependency version management.
- **app**: Demo application showcasing SDK integration and usage.

## Integration

### Add Dependencies
```gradle
implementation(platform("io.openweb3:miniapp-bom:1.0.32"))
implementation("io.openweb3:miniapp-core")
implementation("io.openweb3:miniapp-bridge")
```

### Usage Guide
Please refer to the following core classes:
- **PluginsManager**: Plugin manager
- **OpenPlatformPlugin**: Open platform plugin
- **MiniAppService**: MiniApp service
- **MyApplication**: Application initialization example

## Examples

For more usage examples, please refer to the demo project.

## Contributing

For feedback or contributions, please submit Issues or Pull Requests.