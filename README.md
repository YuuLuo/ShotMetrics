# ShotMetrics

A precision shooting analysis tool for Android. Capture target images, mark impact points, and get detailed ballistic statistics including group size, CEP, mean radius, and turret adjustment recommendations.

## Features

- **Image-based target analysis** - Take a photo or import from gallery, then mark impacts directly on the image
- **Reference scaling** - Set a known distance on the target for accurate real-world measurements
- **Ballistic statistics** - Extreme Spread, CEP, Mean Radius, Radial/Vertical/Horizontal SD, MPI offset
- **ATZ (Adjust To Zero)** - Turret click recommendations to zero your rifle, with configurable click values
- **Visual overlays** - CEP circle, Mean Radius, Extreme Spread, MPI crosshair, POA-to-MPI offset line
- **Image export** - Export annotated target images with customizable data labels, legends, and crop tools
- **CSV export** - Export raw shot data for further analysis
- **Session management** - Save, load, and review past shooting sessions
- **Caliber database** - Comprehensive list of common rifle and pistol calibers
- **Auxiliary lines** - Draw guide lines on the target to help locate center points

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material Design 3
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt
- **Database**: Room
- **Camera**: CameraX
- **Image loading**: Coil
- **Preferences**: DataStore

## Building

1. Clone the repository
2. Open in Android Studio (Ladybug or later recommended)
3. Sync Gradle
4. Run on a device or emulator (minSdk 26)

```bash
./gradlew assembleDebug
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
