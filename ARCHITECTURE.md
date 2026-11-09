# WeightCast Architecture

WeightCast follows Clean Architecture and MVVM principles to enforce separation of concerns, testability, and mathematical predictability.

## Layer Structure

```
app/src/main/java/com/johnpaulpatigas/weightcast/
├── core/                  # Core utilities, unit converters, preferences, trend math
│   ├── model/             # WeightUnit enum
│   ├── preferences/       # DataStore preferences repository
│   └── util/              # WeightConverter, DateTimeUtils, TrendCalculator
├── data/                  # Room entities, DAOs, Database, Repositories
│   ├── local/             # Room Database & Entities
│   ├── mapper/            # Entity <-> Domain mappers
│   └── repository/        # Repository implementations
├── domain/                # Business logic, domain models, use cases
│   ├── model/             # WeightEntry, WeightGoal, DashboardSummary, ChartPoint, TimeRange
│   ├── repository/        # Repository interfaces
│   └── usecase/           # Use cases (Validation, Add/Update/Delete, GetSummary, GetChartData)
├── feature/               # Feature UI components & ViewModels
│   ├── dashboard/         # Dashboard screen, components, and ViewModel
│   ├── weight_history/    # Weight History screen and ViewModel
│   └── weight_logging/    # Weight Logging screen, ViewModel, dialogs
├── navigation/            # Bottom Navigation Bar and NavHost setup
├── di/                    # Hilt Dependency Injection modules
└── WeightCastApp.kt       # Application class
```

## Data Flow
`UI (Compose Screen)` ➔ `ViewModel (StateFlow)` ➔ `Use Case` ➔ `Repository` ➔ `Room DAO / DataStore`
