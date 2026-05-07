# 📱 TeleDrive

<div align="center">

### Transform Your Telegram Into Unlimited Cloud Storage

**Stop paying for cloud storage. Start using what you already have.**

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-Open%20Source-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/Version-0.1.0-orange.svg)](https://github.com/ruru1901/TeleDrive)

[Features](#-features) • [Why TeleDrive?](#-why-teledrive) • [Getting Started](#-getting-started) • [Screenshots](#-how-it-works) • [Tech Stack](#-tech-stack)

</div>

---

## 🎯 What is TeleDrive?

TeleDrive is a **native Android app** that turns Telegram into your personal cloud storage. Upload, organize, and access your files anywhere—all for **free**, with **unlimited storage**, and **military-grade encryption**.

Your **Saved Messages** becomes your root drive. Your **private channels** become folders. It's that simple.

---

## 🚀 Why TeleDrive?

### ✅ What Makes It The Best

| Feature | TeleDrive | Google Drive | Dropbox | OneDrive |
|---------|-----------|--------------|---------|----------|
| **Storage Limit** | ✅ Unlimited* | ❌ 15GB free | ❌ 2GB free | ❌ 5GB free |
| **File Size Limit** | ✅ 2GB per file | ❌ Limited | ❌ Limited | ❌ Limited |
| **Monthly Cost** | ✅ $0 Forever | ❌ $1.99+/mo | ❌ $9.99+/mo | ❌ $1.99+/mo |
| **End-to-End Encryption** | ✅ Yes | ⚠️ Partial | ⚠️ Partial | ⚠️ Partial |
| **Cross-Platform** | ✅ Via Telegram | ✅ Yes | ✅ Yes | ✅ Yes |
| **No Account Needed** | ✅ Use Telegram | ❌ New account | ❌ New account | ❌ New account |
| **Open Source** | ✅ Yes | ❌ No | ❌ No | ❌ No |
| **Ads** | ✅ None | ⚠️ Some plans | ⚠️ Some plans | ⚠️ Some plans |

*Telegram's infrastructure allows unlimited files up to 2GB each

---

## 💎 Key Advantages

### 🎁 **Completely Free**
- No subscription fees, ever
- No hidden costs or premium tiers
- Leverage Telegram's free infrastructure
- Save $120+ per year compared to paid cloud services

### 🔒 **Military-Grade Security**
- **Master password protection** with Argon2 hashing
- **SQLCipher database encryption** for local storage
- **Libsodium cryptography** for secure operations
- **Biometric authentication** (fingerprint/face unlock)
- **Encrypted preferences** for API credentials

### ⚡ **Lightning Fast**
- Native Android performance with Kotlin
- Telegram's CDN for blazing-fast downloads
- Efficient file caching with Coil
- Background uploads/downloads with WorkManager

### 🎨 **Beautiful & Modern**
- Material Design 3 with dynamic theming
- Smooth animations with Jetpack Compose
- Dark mode support
- Intuitive, gesture-based interface

### 🌍 **Access Anywhere**
- Files stored in Telegram = accessible on any device
- Web, desktop, mobile—all platforms supported
- No need to install TeleDrive on other devices
- Just open Telegram and your files are there

### 🛡️ **Privacy First**
- Files stored in YOUR private channels
- No third-party servers
- No data mining or tracking
- You control everything

---

## 📊 Pros & Cons

### ✅ Pros

- **Unlimited storage** (as long as Telegram allows)
- **Zero cost** - completely free forever
- **Large file support** - up to 2GB per file
- **Cross-platform access** through Telegram
- **Strong encryption** with master password
- **Open source** - audit the code yourself
- **No ads or tracking**
- **Fast uploads/downloads** via Telegram's CDN
- **Automatic backups** with scheduled workers
- **Modern UI** with Material Design 3
- **Biometric security** for quick access
- **Bulk operations** - move/delete multiple files at once
- **Smart search** across all folders
- **Native Android performance**

### ⚠️ Cons

- **Requires Telegram account** (but you probably already have one)
- **2GB file size limit** (Telegram's restriction)
- **Android only** (for now - iOS version planned)
- **Requires API credentials** from my.telegram.org (one-time setup)
- **Not official Telegram app** (independent project)
- **Depends on Telegram's infrastructure** (if Telegram changes policies, app may be affected)

---

## ✨ Features

### 📁 File Management
- ✅ **Upload any file type** - documents, images, videos, archives
- ✅ **Download with progress tracking** - see real-time download status
- ✅ **Organize in folders** - each folder is a private Telegram channel
- ✅ **Multi-select operations** - bulk delete, move, or download
- ✅ **Smart search** - find files instantly across all folders
- ✅ **File metadata preview** - view details before downloading

### 🔐 Security Features
- ✅ **Master password** - protect your entire drive
- ✅ **Encrypted local database** - SQLCipher protection
- ✅ **Secure credential storage** - AndroidX Security Crypto
- ✅ **Biometric unlock** - fingerprint or face recognition
- ✅ **Auto-lock** - secure your drive when inactive

### 🎯 Smart Features
- ✅ **Automatic backups** - scheduled with WorkManager
- ✅ **Transfer queue** - manage multiple uploads/downloads
- ✅ **Cache management** - intelligent storage optimization
- ✅ **Dark mode** - easy on the eyes
- ✅ **Material You** - dynamic color theming (Android 12+)

### 🚀 Coming Soon
- 🎯 **Rich media preview** - view images and videos in-app
- 🎯 **PDF viewer** - read documents without downloading
- 🎯 **Audio/video player** - stream media files
- 🎯 **Share to other apps** - export files easily
- 🎯 **Background notifications** - track long uploads/downloads
- 🎯 **Multi-account support** - manage multiple Telegram accounts
- 🎯 **iOS version** - bring TeleDrive to iPhone

---

## 🏗️ Tech Stack

TeleDrive is built with cutting-edge Android technologies:

### 🎨 UI Layer
- **Jetpack Compose** - Modern declarative UI
- **Material Design 3** - Latest design system
- **Coil** - Efficient image loading

### 💾 Data Layer
- **Room Database** - Local data persistence
- **SQLCipher** - Database encryption
- **Kotlin Coroutines** - Async operations
- **Flow** - Reactive data streams

### 🔐 Security Layer
- **AndroidX Security Crypto** - Encrypted SharedPreferences
- **Argon2kt** - Password hashing
- **Libsodium** - Cryptographic operations
- **Biometric API** - Fingerprint/face authentication

### 🌐 Network Layer
- **TDLib** - Official Telegram Database Library
- **TDLibX** - Android-optimized bindings
- **Reflection adapter** - Flexible TDLib integration

### ⚙️ Background Work
- **WorkManager** - Reliable background tasks
- **Foreground Services** - Long-running operations

---

## 🚀 Getting Started

### Prerequisites

Before you begin, ensure you have:

- ✅ **Android Studio** (Hedgehog or newer)
- ✅ **Android SDK 26+** (Android 8.0 Oreo or higher)
- ✅ **Telegram account**
- ✅ **API credentials** from [my.telegram.org](https://my.telegram.org)

### 📥 Installation

#### Option 1: Build from Source

```bash
# Clone the repository
git clone https://github.com/ruru1901/TeleDrive.git
cd TeleDrive

# Set up Android SDK (Windows PowerShell)
$env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android\Sdk'

# Build the project
./gradlew assembleDebug

# Install on connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

#### Option 2: Download APK

Download the latest APK from [Releases](https://github.com/ruru1901/TeleDrive/releases) and install on your device.

---

## 📱 How It Works

### 🔧 First Time Setup

#### Step 1: Get Telegram API Credentials

1. Visit [my.telegram.org](https://my.telegram.org)
2. Log in with your phone number
3. Navigate to **"API development tools"**
4. Click **"Create new application"**
5. Fill in the form:
   - **App title**: TeleDrive (or any name)
   - **Short name**: teledrive
   - **Platform**: Android
6. Copy your `api_id` and `api_hash`

#### Step 2: Launch TeleDrive

1. Open the app
2. Enter your `api_id` and `api_hash`
3. Tap **"Continue"**

#### Step 3: Authenticate

1. Enter your **phone number** (with country code)
2. Enter the **verification code** sent to Telegram
3. If you have 2FA enabled, enter your **password**

#### Step 4: Set Master Password

1. Create a **strong master password**
2. Confirm the password
3. (Optional) Enable **biometric unlock**

🎉 **You're all set!** Start uploading files.

---

### 📂 Using TeleDrive

#### Upload Files
1. Tap the **➕ Upload** button
2. Select files from your device
3. Watch the upload progress
4. Files appear in your current folder

#### Create Folders
1. Tap the **📁 Folders** button
2. Tap **"Create New Folder"**
3. Enter a folder name
4. A private Telegram channel is created automatically

#### Search Files
1. Tap the **🔍 Search** bar
2. Type your query (minimum 3 characters)
3. Results appear instantly from all folders

#### Download Files
1. Tap on any file card
2. View file metadata
3. Tap **"Download"**
4. Track download progress

#### Move Files
1. **Long-press** a file to enter selection mode
2. Select multiple files
3. Tap **"Move"**
4. Choose destination folder

#### Delete Files
1. **Long-press** to select files
2. Tap **"Delete"**
3. Confirm deletion
4. Files are removed from Telegram

---

## 🏛️ Architecture

TeleDrive follows **Clean Architecture** principles with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────┐
│                      UI Layer                           │
│  (Jetpack Compose + Material 3 + ViewModels)           │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                   Domain Layer                          │
│     (Use Cases + Business Logic + Gateway Interface)    │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                    Data Layer                           │
│  (Room Database + TDLib Gateway + Encrypted Settings)   │
└─────────────────────────────────────────────────────────┘
```

### 📦 Project Structure

```
app/src/main/java/com/teledrive/android/
│
├── 📱 ui/                          # Jetpack Compose UI
│   ├── auth/                       # Authentication screens
│   ├── drive/                      # Main drive interface
│   ├── security/                   # Security settings
│   └── theme/                      # Material 3 theming
│
├── 💾 data/                        # Data persistence
│   ├── TeleDriveDatabase.kt        # Room database
│   ├── Entities.kt                 # Data models
│   └── TeleDriveDao.kt             # Database queries
│
├── 🌐 telegram/                    # Telegram integration
│   ├── TelegramGateway.kt          # Interface contract
│   ├── TdLibTelegramGateway.kt     # Real TDLib implementation
│   └── InMemoryTelegramGateway.kt  # Mock for testing
│
├── 🔐 crypto/                      # Encryption utilities
│   ├── MasterPasswordService.kt    # Password management
│   └── GhostCrypto.kt              # Cryptographic operations
│
├── 💼 backup/                      # Backup system
│   ├── BackupManager.kt            # Backup orchestration
│   ├── BackupWorker.kt             # Background worker
│   └── ManifestManager.kt          # Backup metadata
│
├── 🔒 secure/                      # Secure storage
│   └── SecureSettings.kt           # Encrypted preferences
│
├── 📊 queue/                       # Transfer management
│   └── TransferReducer.kt          # Upload/download state
│
└── 🗂️ repository/                  # Data repositories
    └── KeystoreRepository.kt       # Keystore management
```

---

## 🧪 Testing

TeleDrive includes comprehensive unit tests:

```bash
# Run all tests
./gradlew test

# Run with detailed output
./gradlew :app:testDebugUnitTest --stacktrace
```

### Test Coverage

- ✅ **Folder marker parsing** - Validates `[TD]` prefix handling
- ✅ **File metadata mapping** - Tests Telegram message to file conversion
- ✅ **Transfer state transitions** - Verifies upload/download state machine
- ✅ **Preview cache policy** - Tests cache eviction logic
- ✅ **Backup manifest management** - Validates backup metadata
- ✅ **TDLib availability checks** - Tests reflection-based detection

---

## 🗺️ Roadmap

### ✅ Version 0.1.0 (Current)
- ✅ Complete authentication flow
- ✅ Folder management (create, delete, navigate)
- ✅ File upload with progress tracking
- ✅ File download with progress tracking
- ✅ Search functionality
- ✅ Master password encryption
- ✅ Biometric authentication
- ✅ Automatic backups
- ✅ TDLib integration
- ✅ Material Design 3 UI
- ✅ Dark mode support

### 🎯 Version 0.2.0 (Next)
- 🎯 Rich media preview (images, videos)
- 🎯 In-app PDF viewer
- 🎯 Audio/video player with streaming
- 🎯 Share files to other apps
- 🎯 Background upload/download notifications
- 🎯 Cache management settings

### 🚀 Version 0.3.0 (Future)
- 🚀 Multi-account support
- 🚀 File sharing with other TeleDrive users
- 🚀 Folder sharing (collaborative folders)
- 🚀 File versioning
- 🚀 Trash/recycle bin
- 🚀 iOS version

### 💡 Ideas & Suggestions
- 💡 Desktop app (Windows, macOS, Linux)
- 💡 Web interface
- 💡 File compression before upload
- 💡 Automatic photo backup from camera
- 💡 File tagging and labels
- 💡 Advanced search filters

---

## 🤝 Contributing

We welcome contributions! Here's how you can help:

### 🐛 Report Bugs
Found a bug? [Open an issue](https://github.com/ruru1901/TeleDrive/issues) with:
- Device model and Android version
- Steps to reproduce
- Expected vs actual behavior
- Screenshots if applicable

### 💡 Suggest Features
Have an idea? [Start a discussion](https://github.com/ruru1901/TeleDrive/discussions) and tell us:
- What problem does it solve?
- How should it work?
- Why is it useful?

### 🔧 Submit Pull Requests

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Write/update tests
5. Commit your changes (`git commit -m 'Add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

### 📝 Development Guidelines

- **Follow Kotlin conventions** - Use official style guide
- **Write tests** - Cover new features with unit tests
- **Use Jetpack Compose** - All UI should be declarative
- **Keep gateway clean** - Don't break the `TelegramGateway` abstraction
- **Document complex code** - Add comments for tricky logic
- **Update README** - Document new features

---

## 📚 Documentation

- **[ARCHITECTURE.md](ARCHITECTURE.md)** - Detailed architecture notes
- **[TDLIB_SETUP.md](TDLIB_SETUP.md)** - TDLib integration guide
- **[BACKUP_VERIFICATION.md](BACKUP_VERIFICATION.md)** - Backup system details

---

## 🙏 Acknowledgments

This project wouldn't be possible without:

- **[Telegram](https://telegram.org)** - For the amazing platform and TDLib
- **[TDLibX](https://github.com/tdlibx/td)** - For Android-optimized TDLib bindings
- **[Android Jetpack](https://developer.android.com/jetpack)** - For modern Android development tools
- **[SQLCipher](https://www.zetetic.net/sqlcipher/)** - For database encryption
- **[Argon2](https://github.com/lambdapioneer/argon2kt)** - For secure password hashing
- **[Libsodium](https://github.com/terl/lazysodium-android)** - For cryptographic operations

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).

```
MIT License

Copyright (c) 2025 TeleDrive

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
```

---

## 📞 Support & Contact

### 💬 Get Help
- **GitHub Issues** - [Report bugs](https://github.com/ruru1901/TeleDrive/issues)
- **GitHub Discussions** - [Ask questions](https://github.com/ruru1901/TeleDrive/discussions)
- **Telegram** - [@ruru1901](https://t.me/ruru1901)

### 🌟 Show Your Support
If you find TeleDrive useful, please:
- ⭐ **Star this repository**
- 🐦 **Share on social media**
- 📝 **Write a blog post**
- 💬 **Tell your friends**

---

## ⚠️ Disclaimer

**Important Notes:**

- TeleDrive is an **independent project** and is **not affiliated** with Telegram
- This app uses Telegram's infrastructure in compliance with their [Terms of Service](https://telegram.org/tos)
- **Use at your own risk** - Always keep backups of important files
- The developers are **not responsible** for any data loss or account issues
- Telegram may change their policies at any time, which could affect this app
- This is a **community project** maintained by volunteers

---

## 🎉 Fun Facts

- 📊 **Lines of Code**: ~5,000+ lines of Kotlin
- ⚡ **Build Time**: ~30 seconds on modern hardware
- 🎨 **UI Components**: 100% Jetpack Compose
- 🧪 **Test Coverage**: Growing with every release
- 💾 **APK Size**: ~15MB (includes native libraries)
- 🚀 **Minimum Android Version**: Android 8.0 (API 26)
- 🎯 **Target Android Version**: Android 14 (API 36)

---

<div align="center">

### 🌟 Star History

[![Star History Chart](https://api.star-history.com/svg?repos=ruru1901/TeleDrive&type=Date)](https://star-history.com/#ruru1901/TeleDrive&Date)

---

**Made with ❤️ by the Android community**

**Stop paying for cloud storage. Start using TeleDrive today.**

[⬆ Back to Top](#-teledrive)

</div>
