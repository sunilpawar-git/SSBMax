# 🎯 SSBMax - SSB Preparation App

[![Android](https://img.shields.io/badge/Android-API%2030+-brightgreen.svg)](https://android-arsenal.com/api?level=30)
[![Kotlin](https://img.shields.io/badge/Kotlin-100%25-blue.svg)](https://kotlinlang.org)
[![Material Design 3](https://img.shields.io/badge/Material%20Design-3-orange.svg)](https://m3.material.io/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**SSBMax** is a comprehensive Android application designed for SSB (Services Selection Board) preparation, helping candidates prepare for Indian Armed Forces selection processes with authentic test simulations, study materials, and expert guidance.

## 🚀 Features

### 📱 Enhanced Navigation
- **User Profile Integration**: Complete profile management with PIQ form access
- **Custom Sidebar**: Military-themed navigation with proper status bar handling
- **Intuitive UI**: Material Design 3 with navy blue and military gold color scheme

### 🎯 Stage I Tests
- **OIR Test (Officer Intelligence Rating)**: 
  - Verbal and Non-verbal reasoning
  - Numerical ability assessment
  - 40 minutes duration with 120 questions
- **PPDT (Picture Perception & Discussion Test)**:
  - Picture observation and story writing
  - Group discussion simulation
  - Leadership assessment

### 🎖️ Stage II Tests
- **Psychology Tests**:
  - TAT (Thematic Apperception Test)
  - WAT (Word Association Test)
  - SRT (Situation Reaction Test)
  - SDT (Self Description Test)
- **GTO Tests (Group Testing Officer)**:
  - Group Discussion (GD)
  - Group Planning Exercise (GPE)
  - Progressive Group Task (PGT)
  - Individual Obstacles
  - Command Tasks
- **IO Test (Interview Officer)**:
  - Personal interview preparation
  - Current affairs assessment
  - Service knowledge evaluation

### 📚 Additional Features
- **Study Materials**: Comprehensive content for all SSB stages
- **Practice Tests**: Authentic test simulations
- **Progress Tracking**: Detailed analytics and performance monitoring
- **Tips & Tricks**: Expert guidance and strategies

## 🛠️ Technical Stack

- **Architecture**: MVVM (Model-View-ViewModel) with Repository pattern
- **Navigation**: Android Navigation Component with single Activity
- **UI Framework**: Material Design 3 with ViewBinding
- **Language**: Kotlin 100%
- **Minimum SDK**: API 30 (Android 11)
- **Target SDK**: API 36
- **Build System**: Gradle with Kotlin DSL

## 📋 Requirements

- Android 11 (API level 30) or higher
- 100MB+ free storage space
- Internet connection for updates and additional content

## 🏗️ Architecture

```
app/
├── data/
│   ├── local/          # Room database, SharedPreferences
│   ├── remote/         # API services, DTOs
│   ├── repository/     # Data repositories
│   └── models/         # Data classes, entities
├── domain/
│   ├── usecase/        # Business logic use cases
│   └── repository/     # Repository interfaces
├── presentation/
│   ├── ui/
│   │   ├── auth/       # Login, registration
│   │   ├── dashboard/  # Home, progress tracking
│   │   ├── tests/      # All SSB test modules
│   │   ├── study/      # Study materials
│   │   └── profile/    # User profile, settings
│   ├── viewmodel/      # ViewModels
│   └── adapter/        # RecyclerView adapters
└── utils/              # Extensions, constants, helpers
```

## 🎨 Design System

### Color Palette
- **Primary**: Navy Blue (`#1B3A4B`) - Represents discipline and professionalism
- **Secondary**: Olive Green (`#6B8E23`) - Military heritage
- **Accent**: Military Gold (`#D4AF37`) - Excellence and achievement
- **Surface**: Military Gray (`#708090`) - Modern and clean

### Typography
- **Headlines**: Bold, clear hierarchy
- **Body Text**: Readable and accessible
- **Buttons**: Action-oriented with proper contrast

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 17 or higher
- Android SDK with API 30+

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/sunilpawar-git/SSBMax.git
   cd SSBMax
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the cloned directory

3. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   ```

## 📱 Screenshots

*Screenshots will be added in future updates*

## 🤝 Contributing

We welcome contributions to SSBMax! Please read our [Contributing Guidelines](CONTRIBUTING.md) before submitting pull requests.

### Development Setup
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Indian Armed Forces for inspiration
- SSB candidates and veterans for valuable feedback
- Material Design team for excellent design guidelines
- Android development community for continuous support

## 📞 Support

For support, email support@ssbmax.com or create an issue in this repository.

## 🔄 Version History

### v1.0.0 (Current)
- Initial release with comprehensive SSB preparation features
- Enhanced sidebar navigation with user profile integration
- Complete Stage I and Stage II test modules
- Material Design 3 implementation
- Status bar compatibility fixes

---

**Made with ❤️ for SSB aspirants**

*Disclaimer: This app is designed to assist in SSB preparation and is not officially affiliated with the Indian Armed Forces or SSB.*
