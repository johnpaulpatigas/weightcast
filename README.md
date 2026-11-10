# WeightCast — Native Android Weight Tracker & Predictive Analytics App

WeightCast is an offline-first, privacy-focused body weight tracking and statistical forecasting application for Android built with modern Android development standards and **Material 3 Expressive design principles**.

## Core Features
- **Offline-First Storage**: Native Room SQLite database persisting all measurements securely on device.
- **Kilogram Standardization**: All measurements, calculations, and displays operate strictly in Kilograms (KG).
- **Authoritative Statistical Engine**: Employs non-parametric **Theil-Sen Robust Linear Regression** and 7-calendar-date moving averages for underlying trend estimation.
- **Metric Hierarchy & Transparency**: Primary focus on latest recorded weight, secondary focus on estimated trend weight, weekly rate of change ($kg/week$), and human-readable data sufficiency and trend stability status badges.
- **Interactive Weight History & Charts**: Interactive trend chart with touch inspection tooltips, smooth Bezier curves, and time range filtering (`7D`, `30D`, `90D`, `6M`, `1Y`, `All Time`).
- **Weight Logging & Validation**: Full CRUD operations for body weight logs with robust validation against non-positive numbers, implausible measurements, future timestamps, and duplicate entry warnings.
- **Adaptive Navigation**: Seamless transition between Compact Bottom Navigation Bar and Tablet Navigation Rail.

## Tech Stack
- **Language**: Kotlin 2.1
- **UI Framework**: Jetpack Compose & Material 3 Expressive
- **Architecture**: Clean Architecture + MVVM
- **Database**: Room Database 2.8
- **Dependency Injection**: Hilt 2.60
- **Asynchrony**: Kotlin Coroutines & StateFlow
- **Preferences**: DataStore Preferences
- **Build System**: Gradle Kotlin DSL & KSP
