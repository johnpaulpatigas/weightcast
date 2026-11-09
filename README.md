# WeightCast — Native Android Weight Tracker & Predictive Analytics App

WeightCast is an offline-first, privacy-focused body weight tracking and statistical forecasting application for Android built with modern Android development standards.

## Core Features
- **Offline-First Storage**: Native Room SQLite database persisting all measurements securely on device.
- **Canonical Unit Integrity**: Internal calculations and database storage strictly use canonical Kilograms (KG).
- **Flexible Unit Preferences**: Live toggle and automatic user preference persistence for Kilograms (KG) and Pounds (LBS).
- **Interactive Dashboard**: Displays current weight, 7-day moving average trend weight, 7-day net weight change, goal progress percentage, and target date estimate.
- **Visual Weight History & Charts**: Interactive trend chart with time range filtering (`7D`, `30D`, `90D`, `6M`, `1Y`, `All Time`) distinguishing actual measurements from smoothed trend lines.
- **Weight Logging & Validation**: Full CRUD operations for body weight logs with robust validation against non-positive numbers, implausible measurements, future timestamps, and duplicate entry warnings.
- **Bottom Navigation**: Seamless navigation between Dashboard, History & Trends, and Log Management screens.

## Tech Stack
- **Language**: Kotlin 2.1
- **UI Framework**: Jetpack Compose & Material 3
- **Architecture**: Clean Architecture + MVVM
- **Database**: Room Database 2.8
- **Dependency Injection**: Hilt 2.60
- **Asynchrony**: Kotlin Coroutines & StateFlow
- **Preferences**: DataStore Preferences
- **Build System**: Gradle Kotlin DSL & KSP
