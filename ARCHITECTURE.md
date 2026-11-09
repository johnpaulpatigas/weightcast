# WeightCast Architecture

WeightCast follows Clean Architecture and MVVM principles to enforce separation of concerns, testability, and mathematical predictability.

## Layer Structure

```
app/src/main/java/com/johnpaulpatigas/weightcast/
├── core/                  # Core utilities, unit converters, preferences
│   ├── model/             # WeightUnit enum
│   ├── preferences/       # DataStore preferences repository
│   └── util/              # WeightConverter, DateTimeUtils
├── data/                  # Room entities, DAOs, Database, Repositories
│   ├── local/             # Room Database & Entities
│   ├── mapper/            # Entity <-> Domain mappers
│   └── repository/        # Repository implementations
├── domain/                # Business logic, domain models, use cases
│   ├── model/             # WeightEntry, WeightGoal
│   ├── repository/        # Repository interfaces
│   └── usecase/           # Use cases (Validation, Add, Update, Delete, Get)
├── feature/               # Feature UI components & ViewModels
│   └── weight_logging/    # Weight Logging screen, ViewModel, dialogs
├── di/                    # Hilt Dependency Injection modules
└── WeightCastApp.kt       # Application class
```

## Data Flow
`UI (Compose Screen)` ➔ `ViewModel (StateFlow)` ➔ `Use Case` ➔ `Repository` ➔ `Room DAO / DataStore`
