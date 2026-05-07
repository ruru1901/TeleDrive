# TeleDrive

> Turn your Telegram into a personal cloud drive

TeleDrive is a native Android app that transforms Telegram into a fully functional cloud storage solution. Use your Saved Messages as the root drive and create private channels as folders—all with end-to-end encryption and unlimited storage potential.

## 🌟 What Makes TeleDrive Special

Instead of paying for cloud storage, TeleDrive leverages Telegram's infrastructure to give you:

- **Unlimited Storage**: Upload files up to 2GB each
- **End-to-End Encryption**: Your files are secured with master password protection
- **Cross-Platform Access**: Access your files from any device with Telegram
- **No Monthly Fees**: Use Telegram's free infrastructure
- **Privacy First**: Files stored in your private channels and Saved Messages

## ✨ Features

### Core Functionality
- 📁 **Folder Management**: Create, delete, and organize folders as private Telegram channels
- 📤 **File Upload**: Pick and upload any file from your device
- 📥 **File Download**: Download files with progress tracking
- 🔍 **Smart Search**: Find files quickly across all folders
- 🗑️ **Bulk Operations**: Multi-select files for delete or move operations
- 📊 **File Preview**: View metadata for images, videos, PDFs, and documents

### Security & Privacy
- 🔐 **Master Password**: Protect your drive with a master password
- 🔒 **Encrypted Storage**: Local database encryption with SQLCipher
- 🛡️ **Secure Settings**: API credentials stored in encrypted preferences
- 🔑 **Biometric Support**: Unlock with fingerprint or face recognition

### Advanced Features
- 💾 **Automatic Backup**: Scheduled backups with WorkManager
- 🎨 **Material Design 3**: Modern, beautiful UI with dark mode support
- ⚡ **Transfer Queue**: Manage multiple uploads/downloads simultaneously
- 📱 **Native Performance**: Built with Kotlin and Jetpack Compose

## 🏗️ Architecture

TeleDrive is built with modern Android development best practices:

- **UI Layer**: Jetpack Compose with Material 3 theming
- **Data Layer**: Room database with encrypted SQLCipher backend
- **Network Layer**: TDLib (Telegram Database Library) integration
- **Security**: AndroidX Security Crypto + Argon2 + Libsodium
- **Background Work**: WorkManager for reliable background operations

### Key Components

```
app/
├── telegram/          # TDLib gateway and Telegram API integration
├── ui/               # Compose screens (auth, drive, security)
├── data/             # Room entities, DAOs, and database
├── backup/           # Backup manager and manifest handling
├── crypto/           # Encryption utilities and master password service
├── queue/            # Transfer state management
└── secure/           # Encrypted settings storage
```

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog or newer
- Android SDK 26+ (Android 8.0 Oreo)
- Telegram account
- API credentials from [my.telegram.org](https://my.telegram.org)

### Building the App

1. **Clone the repository**
   ```bash
   git clone https://github.com/ruru1901/TeleDrive.git
   cd TeleDrive
   ```

2. **Set up Android SDK**
   ```powershell
   $env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
   ```

3. **Build the project**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Install on device**
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

### Running Tests

```powershell
$env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
./gradlew :app:testDebugUnitTest --stacktrace
```

## 📱 How to Use

### First Time Setup

1. **Get Telegram API Credentials**
   - Visit [my.telegram.org](https://my.telegram.org)
   - Log in with your phone number
   - Go to "API development tools"
   - Create an app and note your `api_id` and `api_hash`

2. **Launch TeleDrive**
   - Enter your API credentials
   - Provide your phone number
   - Enter the verification code sent to Telegram
   - If enabled, enter your 2FA password

3. **Set Master Password**
   - Create a strong master password
   - This encrypts your local database and settings

### Using the Drive

- **Upload Files**: Tap the upload button and select files from your device
- **Create Folders**: Tap the folder icon and create a new folder (creates a private Telegram channel)
- **Search Files**: Use the search bar to find files across all folders
- **Download Files**: Tap a file to view details, then download
- **Move Files**: Long-press to select multiple files, then use the move option
- **Delete Files**: Select files and tap delete (removes from Telegram)

## 🔧 Technical Details

### Dependencies

| Library | Purpose |
|---------|---------|
| Jetpack Compose | Modern declarative UI framework |
| Material 3 | Google's latest design system |
| Room | Local database with SQLCipher encryption |
| TDLibX | Telegram Database Library for Android |
| WorkManager | Reliable background task scheduling |
| Coil | Image loading and caching |
| Argon2kt | Password hashing |
| Libsodium | Cryptographic operations |

### TDLib Integration

TeleDrive uses TDLib (Telegram Database Library) for all Telegram operations. The app includes a reflection-based adapter that supports both standard TDLib and TDLibX packages.

**Gateway Pattern**: All Telegram operations go through `TelegramGateway` interface, making the codebase testable and allowing fallback to `InMemoryTelegramGateway` for development.

### Storage Strategy

- **Saved Messages**: Acts as the root drive
- **Private Channels**: Each folder is a private Telegram channel with `[TD]` prefix
- **File Metadata**: Stored in local Room database for fast access
- **Encryption**: Master password encrypts local database and settings

## 🧪 Testing

The project includes comprehensive unit tests:

- ✅ Folder marker parsing
- ✅ File metadata mapping
- ✅ Transfer state transitions
- ✅ Preview cache policy
- ✅ Backup manifest management
- ✅ TDLib availability checks

Run tests with:
```bash
./gradlew test
```

## 📋 Roadmap

### Current Status (v0.1.0)
- ✅ Complete authentication flow
- ✅ Folder management
- ✅ File upload/download
- ✅ Search functionality
- ✅ Master password encryption
- ✅ Automatic backups
- ✅ TDLib integration

### Planned Features
- 🎯 Rich media preview (images, videos)
- 🎯 PDF viewer
- 🎯 Audio/video player
- 🎯 Share files to other apps
- 🎯 Background upload/download with notifications
- 🎯 Cache management settings
- 🎯 Multi-account support

## 🤝 Contributing

Contributions are welcome! This project is designed to be maintainable and extensible.

### Development Guidelines

- Follow Kotlin coding conventions
- Use Jetpack Compose for all UI
- Write unit tests for business logic
- Keep the `TelegramGateway` abstraction clean
- Document complex algorithms

### Project Structure

For detailed architecture notes, see [`ARCHITECTURE.md`](ARCHITECTURE.md).  
For TDLib setup details, see [`TDLIB_SETUP.md`](TDLIB_SETUP.md).

## 📄 License

This project is open source. Feel free to use, modify, and distribute.

## 🙏 Acknowledgments

- **Telegram**: For providing the TDLib and infrastructure
- **TDLibX**: For Android-optimized TDLib bindings
- **Android Community**: For excellent libraries and tools

## 📞 Support

- **Issues**: Report bugs via GitHub Issues
- **Discussions**: Join GitHub Discussions for questions
- **Telegram**: Contact [@ruru1901](https://t.me/ruru1901)

## ⚠️ Disclaimer

TeleDrive is an independent project and is not affiliated with Telegram. Use at your own risk. Always keep backups of important files.

---

**Made with ❤️ for the Android community**
