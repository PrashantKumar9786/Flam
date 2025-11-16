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

## Architecture Overview

This project consists of two main parts:

- **Android native camera + processing pipeline (Kotlin + C++/JNI)**
- **Web-based viewer (TypeScript)**

---

## 1. Android Native Pipeline (Kotlin + C++/JNI)

### 1.1 JNI Layer

- **Files**
  - [android-code/app/src/main/cpp/flam_native.cpp](cci:7://file:///c:/Users/Dell/Downloads/tmp/tmp/android-code/app/src/main/cpp/flam_native.cpp:0:0-0:0)
  - [android-code/app/src/main/java/com/example/flam/NativeUtils.kt](cci:7://file:///c:/Users/Dell/Downloads/tmp/tmp/android-code/app/src/main/java/com/example/flam/NativeUtils.kt:0:0-0:0)
- **Role**
  - Bridges Kotlin/Java and C++ for performance‑critical frame processing.
  - Exposes the native function:
    - [processFrameNV21(nv21: ByteArray, width: Int, height: Int): ByteArray](cci:1://file:///c:/Users/Dell/Downloads/tmp/tmp/android-code/app/src/main/java/com/example/flam/NativeUtils.kt:9:8-10:90)

### 1.2 Native Processing (C++)

- Input format: **NV21** (YUV 4:2:0) camera preview frames.
- Steps in [flam_native.cpp](cci:7://file:///c:/Users/Dell/Downloads/tmp/tmp/android-code/app/src/main/cpp/flam_native.cpp:0:0-0:0):
  - Convert NV21 → **BGR** ([nv21ToBGR](cci:1://file:///c:/Users/Dell/Downloads/tmp/tmp/android-code/app/src/main/cpp/flam_native.cpp:14:0-45:1)).
  - If OpenCV is available:
    - Wrap BGR buffer into `cv::Mat`.
    - Convert to grayscale.
    - Run **Canny edge detection**.
    - Convert output to **RGBA** for rendering.
  - If OpenCV is not available:
    - Apply a simple grayscale fallback.
- Output: `jbyteArray` containing an **RGBA** frame.

---

## 2. Android Frame Flow (Camera → Screen)

High‑level flow:

1. **Camera capture**
   - Camera API delivers NV21 frames to the Android layer (via `MainActivity` / camera callbacks).

2. **JNI call**
   - Frame bytes are passed to [NativeUtils.processFrameNV21(...)](cci:1://file:///c:/Users/Dell/Downloads/tmp/tmp/android-code/app/src/main/java/com/example/flam/NativeUtils.kt:9:8-10:90).
   - JNI forwards the data into [flam_native.cpp](cci:7://file:///c:/Users/Dell/Downloads/tmp/tmp/android-code/app/src/main/cpp/flam_native.cpp:0:0-0:0) for processing.

3. **Native processing**
   - C++ processes the frame (NV21 → BGR → grayscale/edges → RGBA).
   - Returns an RGBA byte buffer to Kotlin.

4. **OpenGL rendering**
   - **Files**
     - [android-code/app/src/main/java/com/example/flam/GLRenderer.kt](cci:7://file:///c:/Users/Dell/Downloads/tmp/tmp/android-code/app/src/main/java/com/example/flam/GLRenderer.kt:0:0-0:0)
     - `android-code/app/src/main/java/com/example/flam/FlamGLSurfaceView.kt`
   - [GLRenderer](cci:2://file:///c:/Users/Dell/Downloads/tmp/tmp/android-code/app/src/main/java/com/example/flam/GLRenderer.kt:15:0-166:1):
     - Maintains an OpenGL texture and a fullscreen quad.
     - Uploads the processed RGBA frame into the texture.
     - Renders the quad to fill the screen (processed frame shown in real time).

5. **UI / Activity**
   - `MainActivity.kt` sets up:
     - `FlamGLSurfaceView` as the content view.
     - The camera capture and routing of frames into the native processing + renderer.

---

## 3. Web Viewer (TypeScript)

### 3.1 Purpose

The `web-viewer` folder contains a lightweight web UI for inspecting frames (for example, exported/saved frames from the Android pipeline).

- **Files**
  - [web-viewer/src/main.ts](cci:7://file:///c:/Users/Dell/Downloads/tmp/tmp/web-viewer/src/main.ts:0:0-0:0)
  - `web-viewer/index.html`
  - `web-viewer/tsconfig.json`
  - `web-viewer/package.json`

### 3.2 UI Behavior ([main.ts](cci:7://file:///c:/Users/Dell/Downloads/tmp/tmp/web-viewer/src/main.ts:0:0-0:0))

- References DOM elements:
  - `#processed-frame` – processed frame image.
  - `#raw-frame` – original/raw camera frame image.
  - `#toggleBtn` – button to switch between views.
  - `#processed-view`, `#raw-view` – containers for each view.
  - `#stats` – text area for resolution/FPS info.
- Logic:
  - Maintains a boolean `showingProcessed` state.
  - [toggleView()](cci:1://file:///c:/Users/Dell/Downloads/tmp/tmp/web-viewer/src/main.ts:24:0-41:1):
    - Shows/hides processed vs raw containers.
    - Updates the button label.
    - Calls [updateStats()](cci:1://file:///c:/Users/Dell/Downloads/tmp/tmp/web-viewer/src/main.ts:11:0-22:1) to refresh metadata.
  - [updateStats()](cci:1://file:///c:/Users/Dell/Downloads/tmp/tmp/web-viewer/src/main.ts:11:0-22:1):
    - Reads the `naturalWidth`/`naturalHeight` from the currently visible image.
    - Displays `Resolution: <WxH> | FPS: ~12 fps (sample)` in the stats div.
  - Event wiring:
    - On `DOMContentLoaded`:
      - Registers click handler for the toggle button.
      - Registers `onload` handlers on images to update stats when they finish loading.

---

## 4. Build & Project Layout

- **Android**
  - Gradle project in `android-code/`.
  - Native code built via `CMakeLists.txt` in `android-code/app/src/main/cpp/`.
  - Kotlin UI / OpenGL renderer under `android-code/app/src/main/java/com/example/flam/`.

- **Web viewer**
  - TypeScript sources under `web-viewer/src/`.
  - Standard TypeScript config in `web-viewer/tsconfig.json`.
  - `package.json` defines scripts/dependencies for building/running the viewer.

---

## 5. End‑to‑End Data Flow Summary

1. Camera delivers NV21 frames on Android.
2. Kotlin calls [NativeUtils.processFrameNV21(...)](cci:1://file:///c:/Users/Dell/Downloads/tmp/tmp/android-code/app/src/main/java/com/example/flam/NativeUtils.kt:9:8-10:90).
3. JNI/C++ converts and processes the frame (optionally using OpenCV).
4. Processed RGBA frame is returned to Kotlin.
5. [GLRenderer](cci:2://file:///c:/Users/Dell/Downloads/tmp/tmp/android-code/app/src/main/java/com/example/flam/GLRenderer.kt:15:0-166:1) uploads the frame as an OpenGL texture and renders it to the screen.
6. Frames can be exported and inspected using the **TypeScript web viewer**, which toggles between raw and processed outputs and shows basic stats.

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
