cmake_minimum_required(VERSION 3.10.2)
project(flam_native)

# Check if OpenCV_ANDROID_SDK_PATH is defined
if(DEFINED ENV{OpenCV_ANDROID_SDK_PATH})
    set(OpenCV_DIR $ENV{OpenCV_ANDROID_SDK_PATH}/sdk/native/jni)
    find_package(OpenCV QUIET)
endif()

# If OpenCV was found, use it
if(OpenCV_FOUND)
    add_definitions(-DHAVE_OPENCV)
    include_directories(${OpenCV_INCLUDE_DIRS})
    message(STATUS "OpenCV found, building with OpenCV support")
    
    # Build the main native library with OpenCV
    add_library(flam_native SHARED
                flam_native.cpp)
    
    # Link against OpenCV libraries
    target_link_libraries(flam_native
                          ${OpenCV_LIBS}
                          android
                          log)
else()
    # OpenCV not found, build without it
    message(STATUS "OpenCV not found, building without OpenCV support")
    
    # Build the fallback native library
    add_library(flam_native SHARED
                flam_native.cpp)
    
    # Link against Android libraries only
    target_link_libraries(flam_native
                          android
                          log)
endif()
