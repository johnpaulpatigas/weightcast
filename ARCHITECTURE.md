# WeightCast Architecture

WeightCast follows Clean Architecture and MVVM principles to enforce separation of concerns, testability, and mathematical predictability.

## Layer Structure

```
app/src/main/java/com/johnpaulpatigas/weightcast/
├── core/                  # Core utilities, unit converters, preferences
│   ├── model/             # WeightUnit enum (KG)
│   ├── preferences/       # DataStore preferences repository
│   └── util/              # WeightConverter, DateTimeUtils, WeightValidationConstants
├── data/                  # Room entities, DAOs, Database, Repositories
│   ├── local/             # Room Database & Entities
│   ├── mapper/            # Entity <-> Domain mappers
│   └── repository/        # Repository implementations
├── domain/                # Business logic, domain models, use cases, statistical engine
│   ├── model/             # WeightEntry, WeightGoal, DashboardSummary, ChartPoint, TimeRange
│   ├── repository/        # Repository interfaces
│   ├── forecast/          # Forecasting engine, bootstrap prediction intervals, eligibility, backtesting
│   │   ├── backtest/      # Historical forecast backtester
│   │   ├── engine/        # ForecastingEngine, PredictionIntervalEstimator, ForecastEligibilityEvaluator, TargetAchievementEstimator
│   │   └── model/         # WeightForecast, ForecastHorizon, PredictionInterval, ForecastAvailability, TargetAchievementEstimate
│   ├── trend/             # Pure Kotlin Statistical Engine
│   │   ├── engine/        # DataPreparation, DoubleMedianTracker, IncrementalTheilSenEvaluator, TheilSenEstimator, Evaluators
│   │   └── model/         # WeightTrendEstimate, DataSufficiency, TrendStability, Diagnostics
│   └── usecase/           # Use cases (Validation, Add/Update/Delete, GetSummary, GetChartData, GetStatisticalTrend, GenerateWeightForecast)
├── feature/               # Feature UI components & ViewModels
│   ├── dashboard/         # Dashboard screen, components, and ViewModel
│   ├── weight_history/    # Weight History screen and ViewModel
│   └── weight_logging/    # Weight Logging screen, ViewModel, dialogs
├── navigation/            # Bottom Navigation Bar / Rail and NavHost setup
├── di/                    # Hilt Dependency Injection modules
└── WeightCastApp.kt       # Application class
```

## Data Flow
`UI (Compose Screen)` ➔ `ViewModel (StateFlow)` ➔ `Domain Use Cases (GetDashboardSummary, GetHistoricalChartData)` ➔ `GetStatisticalTrendUseCase` ➔ `StatisticalTrendEvaluator` ➔ `Repository` ➔ `Room DAO`
