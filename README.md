# WeightCast — Native Android Weight Tracker & Predictive Analytics App

WeightCast is an offline-first, privacy-focused body weight tracking and statistical forecasting application for Android built with modern Android development standards.

## Core Features (Phase 1)
- **Offline-First Storage**: Native Room SQLite database persisting all measurements securely on device.
- **Canonical Unit Integrity**: Internal calculations and database storage strictly use canonical Kilograms (KG).
- **Flexible Unit Preferences**: Live toggle and automatic user preference persistence for Kilograms (KG) and Pounds (LBS).
- **Weight Logging & Validation**: Full CRUD operations for body weight logs with robust validation against non-positive numbers, implausible measurements, future timestamps, and duplicate entry warnings.
- **Historical Data Entry**: Native Material 3 date and time picker support for entering or modifying historical records.

## Tech Stack
- **Language**: Kotlin 2.1
- **UI Framework**: Jetpack Compose & Material 3
- **Architecture**: Clean Architecture + MVVM
- **Database**: Room Database 2.8
- **Dependency Injection**: Hilt 2.60
- **Asynchrony**: Kotlin Coroutines & StateFlow
- **Preferences**: DataStore Preferences
- **Build System**: Gradle Kotlin DSL & KSP
