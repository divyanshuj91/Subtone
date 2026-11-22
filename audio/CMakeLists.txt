cmake_minimum_required(VERSION 3.22.1)
project("subtone_audio")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(oboe REQUIRED CONFIG)

add_library(subtone_audio SHARED
    src/main/cpp/AudioEngine.cpp
    src/main/cpp/Metronome.cpp
    src/main/cpp/jni_bridge.cpp
)

target_include_directories(subtone_audio PRIVATE
    src/main/cpp
)

target_link_libraries(subtone_audio
    oboe::oboe
    log
    OpenSLES
)
