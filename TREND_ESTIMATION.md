# WeightCast Trend Estimation Methodology

## 1. Overview
WeightCast uses an authoritative, non-parametric statistical approach to estimate body weight trends from raw weight logs:
1. **Theil-Sen Robust Linear Regression**: Computes non-parametric linear regression slope and intercept using median pairwise slopes.
2. **Independent Data Sufficiency & Trend Stability Evaluation**: Evaluates whether measurement data volume/span is adequate and whether the trend direction is temporally consistent.
3. **No Moving-Average Fallback**: Historical charts do not substitute 7-day moving averages as statistical trends. When $N < 3$ distinct daily representatives exist, actual measurements are plotted without a statistical trend line.
4. **Single Domain Pathway**: All domain features route statistical evaluations through `GetStatisticalTrendUseCase`.

## 2. Production Domain Integration & Metric Hierarchy
- **Primary Metric**: Latest Recorded Weight (Authoritative actual measurement; never replaced with a regression estimate).
- **Secondary Metric**: Current Statistical Trend Weight (Fitted value $\hat{W}(t_{\text{eval}})$ from Theil-Sen regression).
- **Supporting Rate**: Weekly Rate of Change ($\Delta W_{\text{week}} = 7.0 \times \hat{\beta}_1$ in kg/week).
- **Sufficiency & Stability Badges**: Concise human-readable status indicators ("Sufficient data", "Limited data", "Not enough data", "Stable trend", "Variable trend", "Unstable trend").
- **Insufficient Data Behavior**: If $N < 3$ or data is insufficient, trend weight and weekly change are omitted, showing "Not enough data to estimate trend" while preserving the primary recorded weight readout.

## 3. Time-Coordinate Normalization & Intercept Convention
- Baseline timestamp $t_0$ is established as the earliest valid measurement's timestamp in UTC epoch millis.
- Normalized elapsed time:
  $$t_i = \frac{\text{timestamp}_i - t_0}{86,400,000.0} \quad (\text{fractional days})$$
- The regression intercept $\hat{\beta}_0$ represents the estimated trend weight in KG at the baseline reference timestamp $t_0$.

## 4. Theil-Sen Robust Regression
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

## 5. Data Sufficiency Criteria (`DataSufficiencyEvaluator`)
| Classification | Daily Measurement Count ($N$) | Time Span ($\Delta t$) | Temporal Coverage Ratio |
|---|---|---|---|
| `INSUFFICIENT_DATA` | $N < 3$ | Any | Any |
| `LOW_CONFIDENCE` | $N \ge 3$ | $< 7$ days | Any (or coverage $< 40\%$) |
| `MODERATE_CONFIDENCE` | $N \ge 5$ | $\ge 7$ days | $\ge 40\%$ |
| `HIGH_CONFIDENCE` | $N \ge 10$ | $\ge 14$ days | $\ge 50\%$ |

- **Coverage Ratio Definition**:
  $$\text{coverageRatio} = \frac{N}{\lfloor\text{timeSpanDays}\rfloor + 1}$$
  Coverage ratio is a temporal measurement-density heuristic. It prevents datasets with sparse multi-month gaps from being misclassified as `HIGH_CONFIDENCE`.

## 6. Trend Stability Diagnostics (`TrendStabilityEvaluator`)
- **Subwindow Slope Comparison**: Prepared measurements are split into chronological halves ($\hat{\beta}_{\text{first}}$, $\hat{\beta}_{\text{second}}$).
- **Direction Reversal**: Flagged if $\hat{\beta}_{\text{first}} \cdot \hat{\beta}_{\text{second}} < 0$ and both $|\Delta W_{\text{sub}}| \ge 0.25$ kg/week.
- **Classification Rules**:
  - `UNSTABLE`: Reversal detected OR $|\Delta W_{\text{first}} - \Delta W_{\text{second}}| > 0.75$ kg/week OR $s_e > 1.2$ kg.
  - `MODERATE`: $|\Delta W_{\text{first}} - \Delta W_{\text{second}}| > 0.30$ kg/week OR $s_e > 0.6$ kg.
  - `STABLE`: Consistent direction, subwindow slope diff $\le 0.30$ kg/week, and $s_e \le 0.6$ kg.

## 7. Historical Evaluation Performance & Algorithmic Complexity
Evaluating a single point's Theil-Sen regression requires calculating $\frac{N(N-1)}{2}$ pairwise slopes, resulting in $O(N^2 \log N)$ time complexity due to median sorting.

Generating a full historical trend line series requires evaluating independent regression models for each timestamp $t_k \le t_N$, yielding an overall algorithmic complexity of:
$$\sum_{k=1}^{N} O(k^2 \log k) \approx O(N^3 \log N)$$

### Observed Benchmark Times (Complete Historical Chart Data Generation):
- **30 measurements**: ~4.5 ms
- **100 measurements**: ~106.5 ms
- **250 measurements**: ~618.3 ms
- **500 measurements**: ~4,978.4 ms (~4.98 seconds)

**Primary Bottleneck**: Pairwise slope computation and array allocations across all historical sub-windows ($\sim 20.8$ million pairwise slope operations for $N=500$). Strict mathematical correctness and temporal isolation are preserved without unverified caching or approximations.
