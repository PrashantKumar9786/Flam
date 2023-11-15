# Flam Camera Processing App

This project demonstrates real-time camera processing with OpenCV, OpenGL ES rendering, and a TypeScript web viewer.

## Key Features

### Android App
- Camera feed integration using TextureView and CameraX
- Frame processing via JNI/C++ with OpenCV edge detection
- OpenGL ES 2.0 rendering of processed frames
- Toggle between raw camera and processed views
- FPS counter and measurement
- Save both raw and processed frames

### Web Viewer
- TypeScript + HTML implementation
- Toggle between raw and processed images
- Display frame stats (resolution, FPS)

## How to Run

### Android App
1. Open `android-code/` in Android Studio
2. Build and run on a device/emulator
3. Grant camera permission when prompted
4. Use the toggle button to switch between views
5. Tap "Save Frame" to save both raw and processed images

### Web Viewer
1. Copy `processed_sample.png` and `raw_sample.png` to `web-viewer/`
2. In terminal:
   ```
   cd web-viewer
   npm install
   npm run dev
   ```
3. Open http://localhost:3000
4. Use the toggle button to switch between images

## Implementation Details

### Android App
- **Camera Pipeline**: CameraX with TextureView preview and ImageAnalysis
- **Native Processing**: JNI with OpenCV Canny edge detection
  - Fallback implementation when OpenCV is not available
- **Rendering**: OpenGL ES 2.0 with texture rendering
- **File Structure**:
  - `app/src/main/java/com/example/flam/` — Kotlin sources: `MainActivity`, `NativeUtils`, `FlamGLSurfaceView`, `GLRenderer`
  - `app/src/main/cpp/` — Native sources: `flam_native.cpp` (OpenCV Canny) and `flam_native_fallback.cpp`
  - `app/src/main/cpp/CMakeLists.txt` — CMake configuration with optional OpenCV support

### Web Viewer
- **TypeScript + HTML**: Simple viewer with toggle functionality
- **File Structure**:
  - `index.html` — HTML structure with toggle between raw/processed views
  - `src/main.ts` — TypeScript implementation for view toggling and stats display
  - `dist/main.js` — Compiled TypeScript

## Requirements Checklist

### 1. Camera Feed Integration ✅
- [x] TextureView used for camera preview
- [x] CameraX for repeating image capture stream

### 2. Frame Processing via OpenCV (C++) ✅
- [x] JNI implementation to process frames
- [x] Canny Edge Detection filter applied
- [x] Return processed image to Java/Kotlin

### 3. Render Output with OpenGL ES ✅
- [x] OpenGL ES 2.0 rendering with textures
- [x] 10-15+ FPS performance achieved
- [x] Toggle between raw and processed views

### 4. Web Viewer (TypeScript) ✅
- [x] Minimal TypeScript + HTML implementation
- [x] Display static sample processed frame
- [x] Toggle between raw and processed images
- [x] Show frame stats (resolution, FPS)

## Bonus Features

- **Toggle View**: Switch between raw camera feed and processed output
- **FPS Counter**: Real-time FPS measurement and display
- **Dual Image Saving**: Save both raw and processed frames
- **Web Viewer Enhancements**: Toggle between raw and processed images

## Performance
- Achieves 10-15+ FPS on mid-range devices
- Configurable resolution (640x480 default, can be reduced to 320x240)

## Build Notes

### OpenCV Configuration
- The app is designed to work with or without OpenCV
- If OpenCV is available, set `OpenCV_ANDROID_SDK_PATH` in `CMakeLists.txt`
- If not, the app will use the fallback implementation

### Troubleshooting
- If FPS is low, reduce resolution in `MainActivity.kt` (line ~104): `val targetRes = Size(320, 240)`
- If build fails with OpenCV errors, the app will use the fallback path

---

## Contact
Prashant Kumar — GitHub: https://github.com/PrashantKumar9786
Email: prashantkushwah913@gmail.com
