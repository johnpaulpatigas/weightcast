# WeightCast Trend Estimation Methodology

## 1. Overview
WeightCast uses a mathematically defensible, two-stage statistical approach to estimate body weight trends from raw weight logs:
1. **Trailing 7-Calendar-Date Moving Average**: Smooths raw measurements using daily representatives across 7 UTC calendar dates `[evalUtcDay - 6 days, evalUtcDay]`.
2. **Theil-Sen Robust Linear Regression**: Computes non-parametric linear regression slope and intercept using median pairwise slopes.

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
   *Note: $s_e$ is a descriptive statistic measuring residual sample dispersion around the fitted trend line. It is not a prediction interval, confidence interval, or uncertainty guarantee.*

## 4. Theoretical Robustness & Performance
- **Theoretical Breakdown Point**: The asymptotic single-median Theil-Sen estimator has a theoretical breakdown point of $\approx 29.3\%$ under continuous independent sample assumptions. Empirical synthetic tests demonstrate robustness against isolated scale spikes.
- **Observed Execution Time**: For $N = 500$ daily measurements ($124,750$ pairwise slopes), local execution completes deterministically in $< 10$ ms.
