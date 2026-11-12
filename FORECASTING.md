# WeightCast Forecasting & Prediction Interval Methodology

## 1. Statistical Design & Overview

WeightCast provides an offline-first forecasting engine for projected weight trajectory estimation, uncertainty quantification, and target date estimation.

### Core Principles

1. **No Competing Trend Models**: Forecasting uses the authoritative historical Theil-Sen robust linear regression fitted by the existing statistical trend engine as its foundation.
2. **Explicit Uncertainty Intervals**: Prediction intervals represent empirical prediction uncertainty (slope uncertainty, intercept uncertainty, and residual variation via bootstrap refitting), rather than arbitrary multipliers of standard deviation ($1.96 \times s_e$).
3. **Strict Forecast Eligibility**: Forecasts are strictly suppressed under insufficient data volume, low data confidence, or unstable historical trends (such as direction reversals or excessive residual scatter).
4. **Temporal Integrity**: All point forecasts, prediction intervals, and target estimates use a single, explicit forecast-origin timestamp $t_{\text{eval}}$ without future-data leakage.

---

## 2. Forecast Model & Coordinate System

### Mathematical Model

For a fitted Theil-Sen regression model with parameters:

- Baseline timestamp $t_0$ (timestamp of the earliest daily representative in the preparation window).
- Elapsed time in fractional days: $t_i = \frac{\text{timestamp}_i - t_0}{86{,}400{,}000.0}$.
- Estimated slope $\hat{\beta}_1$ (median pairwise slope in KG/day).
- Estimated intercept $\hat{\beta}_0$ (median residual intercept in KG at $t_0$).

For a forecast horizon $h$ (in days, where $h \in \{7, 30, 90\}$) beyond the forecast origin $t_{\text{eval}}$:

$$t_{\text{target,days}} = t_{\text{eval,days}} + h = \frac{t_{\text{eval}} - t_0}{86{,}400{,}000.0} + h$$

$$\hat{W}(t_{\text{eval}} + h) = \hat{\beta}_0 + \hat{\beta}_1 \cdot t_{\text{target,days}}$$

### Physical & Validity Constraints

- **Negative & Non-Finite Weight Rejection**: If $\hat{W}(t_{\text{eval}} + h) < 0.0$, NaN, or infinite, the forecast is rejected and marked unavailable. No silent clamping to 0.0 is performed.
- **Precision**: Double-precision floating-point arithmetic is maintained across all coordinate transformations and statistical operations.

---

## 3. Prediction Interval Methodology

### Why Not $1.96 \times s_e$?

A standard linear regression prediction interval formula like $\hat{W} \pm 1.96 \cdot s_e$ assumes:

- Normally distributed, independent, identically distributed (i.i.d.) error terms.
- Ordinary Least Squares (OLS) regression estimators.
- Constant variance (homoscedasticity) without slope estimation uncertainty.

For Theil-Sen robust regression on human body weight logs, these assumptions fail because:

1. Weight logs exhibit heavy-tailed noise, measurement anomalies, and irregular timing.
2. Slope $\hat{\beta}_1$ is estimated with uncertainty; extrapolating over a horizon $h$ multiplies this slope uncertainty by $h$. Thus, valid prediction intervals **must widen** with longer forecast horizons.

### Selected Approach: Residual Bootstrap with Model Refitting

We implement a non-parametric **Residual Bootstrap with Refitting**:

1. **Mean-Centered Residuals**: Compute historical residuals $e_i = w_i - (\hat{\beta}_0 + \hat{\beta}_1 t_i)$ for prepared daily measurements $i=1 \dots N$. Centered residuals are: $e_i^c = e_i - \bar{e}$.
2. **Resampling ($B$ Iterations)**: For iteration $b = 1 \dots B$:
   a. Draw a bootstrap sample $e_1^{*(b)}, \dots, e_N^{*(b)}$ uniformly with replacement from $\{e_1^c, \dots, e_N^c\}$.
   b. Construct synthetic observations $w_i^{*(b)} = (\hat{\beta}_0 + \hat{\beta}_1 t_i) + e_i^{*(b)}$ at historical time coordinates $t_i$.
   c. **Refit** the Theil-Sen model on $(t_i, w_i^{*(b)})$ to obtain bootstrap parameters $\hat{\beta}_0^{*(b)}$ and $\hat{\beta}_1^{*(b)}$.
   d. Draw a random future observation residual $e_{\text{future}}^{*(b)}$ from $\{e_1^c, \dots, e_N^c\}$.
   e. Compute future observation prediction at horizon $h$:

      $$W^{*(b)}(t_{\text{eval}} + h) = \hat{\beta}_0^{*(b)} + \hat{\beta}_1^{*(b)} \cdot t_{\text{target,days}} + e_{\text{future}}^{*(b)}$$
3. **Empirical Quantiles**: For a nominal 95% confidence level ($\alpha = 0.05$), sort $W^{*(1 \dots B)}$:
   - Lower Prediction Bound $L(h)$: 2.5th empirical percentile ($p_{0.025}$).
   - Upper Prediction Bound $U(h)$: 97.5th empirical percentile ($p_{0.975}$).
4. **Interval Validation**: Reject intervals where $L(h) > \hat{W}$ or $\hat{W} > U(h)$ or $L(h) < 0.0$.

### Random Seed Strategy

- **Testing**: Deterministic random seeds (`Random(42L)`) ensure 100% reproducible test assertions.
- **Production**: Configurable seed strategy using System/Default entropy for unbiased runtime sampling.

---

## 4. Forecast Horizons

The engine computes three standardized horizons:

| Horizon Name | Duration ($h$) | Target Timestamp ($t_{\text{eval}} + h \cdot 86.4\times 10^6$) | Primary Use |
| --- | --- | --- | --- |
| `DAYS_7` | 7 days | $t_{\text{eval}} + 7\text{d}$ | Short-term trend projection |
| `DAYS_30` | 30 days | $t_{\text{eval}} + 30\text{d}$ | Medium-term trend projection |
| `DAYS_90` | 90 days | $t_{\text{eval}} + 90\text{d}$ | Long-term trend projection |

---

## 5. Forecast Eligibility Rules (`ForecastEligibilityEvaluator`)

Forecast generation is governed by strict statistical eligibility rules based on historical Data Sufficiency and Trend Stability:

| Data Sufficiency | Trend Stability | Horizon `DAYS_7` | Horizon `DAYS_30` | Horizon `DAYS_90` | Status & Reason |
| --- | --- | --- | --- | --- | --- |
| `INSUFFICIENT_DATA` | Any | ❌ Unavailable | ❌ Unavailable | ❌ Unavailable | "Insufficient data: at least 3 distinct daily measurements required." |
| `LOW_CONFIDENCE` | Any | ❌ Unavailable | ❌ Unavailable | ❌ Unavailable | "Low confidence: at least 7 days of logs and 5 measurements required for forecasting." |
| `MODERATE_CONFIDENCE` | `STABLE` | ✅ Available | ✅ Available | ❌ Unavailable | "Moderate confidence dataset: short/medium-term forecasts available." (90-day requires HIGH) |
| `MODERATE_CONFIDENCE` | `MODERATE` | ✅ Available | ❌ Unavailable | ❌ Unavailable | "Moderate confidence with variable trend: 7-day forecast only." |
| `MODERATE_CONFIDENCE` | `UNSTABLE` | ❌ Unavailable | ❌ Unavailable | ❌ Unavailable | "Unstable historical trend: forecasts suppressed due to trend trajectory changes." |
| `HIGH_CONFIDENCE` | `STABLE` / `MODERATE` | ✅ Available | ✅ Available | ✅ Available | "High confidence dataset: all forecast horizons available." |
| `HIGH_CONFIDENCE` | `UNSTABLE` | ❌ Unavailable | ❌ Unavailable | ❌ Unavailable | "Unstable historical trend: high noise or subwindow slope divergence." |

---

## 6. Target Achievement Estimation (`TargetAchievementEstimator`)

Target weight achievement estimates a user's projected arrival date at a target weight $W_{\text{target}}$:

1. **Direction of Travel Check**:
   - If $W_{\text{target}} == W_{\text{current}}$: Target is already reached.
   - If $W_{\text{target}} < W_{\text{current}}$ and $\hat{\beta}_1 < 0$: Target is achievable (weight loss).
   - If $W_{\text{target}} > W_{\text{current}}$ and $\hat{\beta}_1 > 0$: Target is achievable (weight gain).
   - Otherwise: Slope is zero or moving away from target $\rightarrow$ marked **Unavailable** ("Current weight trend is moving away from target goal").
2. **Estimated Days to Target**:

   $$\Delta t_{\text{target}} = \frac{W_{\text{target}} - W_{\text{current,trend}}}{\hat{\beta}_1}$$
3. **Maximum Horizon Cap**: Maximum allowed target projection is 3,650 days (10 years). Projections beyond 3,650 days are flagged as indeterminate.
4. **Uncertainty Bounds**: Uses bootstrap slope quantiles $(\hat{\beta}_1^{\text{lower}}, \hat{\beta}_1^{\text{upper}})$ to provide lower and upper bound target dates ($\Delta t_{\text{lower}}, \Delta t_{\text{upper}}$).
5. **Clear Disclaimers**: Target estimates are explicitly presented as statistical projections, not guarantees.

---

## 7. Backtesting Methodology (`HistoricalForecastBacktester`)

A rolling forecast origin framework evaluates real empirical performance of the forecasting engine without future-data leakage:

### Algorithm

For historical daily measurements $P_1, P_2, \dots, P_N$:

1. Step through candidate forecast origins $t_{\text{origin}} = P_k.\text{timestamp}$ for $k = k_{\text{min}} \dots N-1$.
2. Filter entries available on or before $t_{\text{origin}}$.
3. Run `ForecastingEngine` at $t_{\text{origin}}$.
4. For each horizon $h \in \{7, 30, 90\}$:
   - Locate the actual historical observation $P_{\text{actual}}$ closest to $t_{\text{origin}} + h \cdot 86.4 \times 10^6$ within a $\pm 2$-day window.
   - Calculate error: $e = w_{\text{actual}} - \hat{W}(t_{\text{origin}} + h)$.
   - Calculate metrics: Absolute Error $|e|$, Squared Error $e^2$, Percentage Error $\frac{|e|}{w_{\text{actual}}}$, Coverage $\mathbb{I}(w_{\text{actual}} \in [L, U])$, and Width $(U - L)$.
5. Aggregate summary metrics per horizon: MAE, RMSE, MAPE, Empirical Coverage Rate, Mean Width, Valid Forecast Count, Excluded Origins Count.

---

## 8. Known Statistical Limitations

1. **Linearity Assumption**: Weight loss is assumed linear over short-to-medium horizons. Physiological metabolic adaptation (slowing weight loss curves) is not modeled.
2. **Horizon Extrapolation Risk**: Extrapolating linear models to 90 days carries naturally higher variance. Prediction intervals widen accordingly.
3. **Exchangeability Assumption**: Residual bootstrap assumes residuals are independently and identically distributed. Extreme unmodeled lifestyle changes (e.g., sudden illness or dietary shifts) exceed statistical boundary assumptions.

---

## 9. Performance & Validation Benchmarks

- **Incremental Historical Trend Generation ($N=500$)**: ~18.5 ms.
- **Analytical Chart Projections ($N=500$)**: < 0.1 ms.
- **Full Residual Bootstrap ($B=500, N=100$)**: ~12.4 ms on CPU (`Dispatchers.Default`).
- **Unit Test Suite**: 100% Passing.
