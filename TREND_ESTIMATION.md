# WeightCast Trend Estimation Methodology

## 1. Overview
WeightCast uses a mathematically defensible, two-stage statistical approach to estimate body weight trends from raw weight logs:
1. **Trailing 7-Calendar-Date Moving Average**: Smooths raw measurements using daily representatives across 7 UTC calendar dates `[evalUtcDay - 6 days, evalUtcDay]`.
2. **Theil-Sen Robust Linear Regression**: Computes non-parametric linear regression slope and intercept using median pairwise slopes.
3. **Independent Data Sufficiency & Trend Stability Evaluation**: Evaluates whether measurement data volume/span is adequate and whether the trend direction is temporally consistent.

## 2. Time-Coordinate Normalization & Intercept Convention
- Baseline timestamp $t_0$ is established as the earliest valid measurement's timestamp in UTC epoch millis.
- Normalized elapsed time:
  $$t_i = \frac{\text{timestamp}_i - t_0}{86,400,000.0} \quad (\text{fractional days})$$
- The regression intercept $\hat{\beta}_0$ represents the estimated trend weight in KG at the baseline reference timestamp $t_0$.

## 3. Theil-Sen Robust Regression
For prepared daily measurements $(t_i, w_i)$:
1. **Pairwise Slopes**:
   $$s_{ij} = \frac{w_j - w_i}{t_j - t_i} \quad \text{for all } t_j > t_i$$
2. **Median Slope**:
   $$\hat{\beta}_1 = \text{median}(s_{ij})$$
3. **Median Intercept**:
   $$\hat{\beta}_0 = \text{median}(w_i - \hat{\beta}_1 \cdot t_i)$$
4. **Current Fitted Trend Weight at $t_{\text{eval}}$**:
   $$t_{\text{eval,days}} = \frac{t_{\text{eval}} - t_0}{86,400,000.0}$$
   $$W_{\text{trend,current}} = \hat{\beta}_0 + \hat{\beta}_1 \cdot t_{\text{eval,days}}$$
   *Note: If $W_{\text{trend,current}}$ is negative or non-finite, the estimation produces an unavailable `null` result (no silent zero-clamping).*
5. **Weekly Rate of Weight Change**:
   $$\Delta W_{\text{week}} = 7.0 \times \hat{\beta}_1$$
6. **Residual Standard Deviation**:
   $$s_e = \sqrt{\frac{\sum_{i=1}^{N} (w_i - (\hat{\beta}_0 + \hat{\beta}_1 \cdot t_i))^2}{N - 2}} \quad \text{for } N > 2, \text{ else } 0.0$$
   *Note: $s_e$ is a descriptive statistic measuring residual sample dispersion around the fitted trend line.*

## 4. Data Sufficiency Criteria (`DataSufficiencyEvaluator`)
| Classification | Daily Measurement Count ($N$) | Time Span ($\Delta t$) | Temporal Coverage Ratio |
|---|---|---|---|
| `INSUFFICIENT_DATA` | $N < 3$ | Any | Any |
| `LOW_CONFIDENCE` | $N \ge 3$ | $< 7$ days | Any (or coverage $< 40\%$) |
| `MODERATE_CONFIDENCE` | $N \ge 5$ | $\ge 7$ days | $\ge 40\%$ |
| `HIGH_CONFIDENCE` | $N \ge 10$ | $\ge 14$ days | $\ge 50\%$ |

- **Coverage Ratio Definition & Heuristic Scope**:
  $$\text{coverageRatio} = \frac{N}{\lfloor\text{timeSpanDays}\rfloor + 1}$$
  Coverage ratio is a temporal measurement-density heuristic (ratio of daily measurements present over total calendar days in span). It prevents datasets with sparse multi-month gaps from being misclassified as `HIGH_CONFIDENCE`. It is not a calibrated probability of forecast accuracy.
- **Evaluation Timestamp Windowing**: All sufficiency and trend evaluations strictly filter measurements to $t_i \le t_{\text{eval}}$ (zero future-data leakage).

## 5. Trend Stability Diagnostics (`TrendStabilityEvaluator`)
Stability evaluates trajectory consistency independently from data volume:
- **Subwindow Slope Comparison**: Prepared measurements are split into chronological halves. Theil-Sen regression is evaluated on each half ($\hat{\beta}_{\text{first}}$, $\hat{\beta}_{\text{second}}$).
- **Direction Reversal**: Flagged if $\hat{\beta}_{\text{first}} \cdot \hat{\beta}_{\text{second}} < 0$ and both $|\Delta W_{\text{sub}}| \ge 0.25$ kg/week.
- **Classification Rules**:
  - `UNSTABLE`: Reversal detected OR $|\Delta W_{\text{first}} - \Delta W_{\text{second}}| > 0.75$ kg/week OR $s_e > 1.2$ kg.
  - `MODERATE`: $|\Delta W_{\text{first}} - \Delta W_{\text{second}}| > 0.30$ kg/week OR $s_e > 0.6$ kg.
  - `STABLE`: Consistent direction, subwindow slope diff $\le 0.30$ kg/week, and $s_e \le 0.6$ kg.

## 6. Synthetic Noise Stress Testing Results
Empirical synthetic validation tests confirmed:
- Linear weight loss ($-0.50$ kg/week) with Gaussian noise $\sigma = 0.1, 0.3, 0.5, 1.0$ kg is estimated accurately within tolerance ($\pm 0.05$ to $0.15$ kg/week).
- Single extreme outliers ($+15.0$ kg) and double extreme outliers ($+20.0$ kg, $-15.0$ kg) do not distort the slope estimate.
- Abrupt direction reversals are flagged as `UNSTABLE` with `isDirectionReversal = true`.
