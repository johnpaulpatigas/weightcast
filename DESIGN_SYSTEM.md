# Material 3 Expressive Design System

WeightCast is built using Material 3 Expressive design tokens to deliver a premium, tactile body weight tracking and predictive analytics experience.

## Design Foundations

### Color System

- **Primary**: Deep Teal (`#006A6B` Light / `#4CF2F3` Dark)
- **Secondary**: Slate Cyan (`#4A6363` Light / `#B0CCCB` Dark)
- **Tertiary**: Soft Steel Blue (`#4B607A` Light / `#B3C8E8` Dark)
- **Surface Containers**: Multi-tiered tonal surfaces (`SurfaceContainerLowest`, `SurfaceContainerLow`, `SurfaceContainerHigh`, `SurfaceContainerHighest`)
- **Dynamic Color**: Full support for Android 12+ Monet dynamic color themes with graceful fallback.

### Typography

- **Display Large**: 52sp ExtraBold for hero weight readouts.
- **Headline / Title**: Bold and SemiBold weights for structured screen sections.
- **Label / Body**: High-contrast, scalable text hierarchy.

### Shape System

- **Extra Small**: 8dp rounded corners for chips and badges.
- **Small**: 12dp rounded corners for buttons.
- **Medium**: 18dp rounded corners for items and chart tooltips.
- **Large**: 24dp rounded corners for cards and action buttons.
- **Extra Large**: 32dp rounded corners for hero cards and modal dialogs.

### Motion & Interaction

- Spring-based animated number transitions (`AnimatedMetricContent`) when values or units change.
- Interactive touch inspection on trend charts with tooltip overlays and highlight indicators.
- Extended Floating Action Button quick-add patterns.
- Adaptive Navigation Rail support for large screen / tablet devices.

### Accessibility Standards

- Non-color-only status communication (explicit directional labels "lost in 7d" / "gained in 7d" alongside icons).
- Minimum 48dp touch targets.
- Content descriptions for chart and metric elements.
