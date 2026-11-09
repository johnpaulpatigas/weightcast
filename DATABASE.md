# Database Design

WeightCast uses Room Database to store user measurements locally and offline.

## Schema Version: 1

### `weight_entries` Table
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | Unique log identifier |
| `weightInKg` | REAL | NOT NULL | Canonical weight value stored in Kilograms |
| `unit` | TEXT | NOT NULL | Original user input unit ("KG" or "LBS") |
| `timestamp` | INTEGER | NOT NULL (Indexed) | Epoch milliseconds of measurement time |
| `notes` | TEXT | NULLABLE | Optional text notes |
| `createdAt` | INTEGER | NOT NULL | Record creation epoch millis |
| `updatedAt` | INTEGER | NOT NULL | Last update epoch millis |

### `weight_goals` Table
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | INTEGER | PRIMARY KEY (id=1) | Single user target goal record |
| `targetWeightInKg` | REAL | NOT NULL | Canonical target weight in KG |
| `unit` | TEXT | NOT NULL | Target display unit |
| `goalType` | TEXT | NOT NULL | Goal strategy ("WEIGHT_LOSS", "WEIGHT_GAIN", "MAINTENANCE") |
| `createdAt` | INTEGER | NOT NULL | Creation epoch millis |
| `updatedAt` | INTEGER | NOT NULL | Last update epoch millis |

## Design Guarantees
- Single source of truth.
- Canonical storage in Kilograms (KG) prevents unit conversion drift.
- Offline and local to device.
