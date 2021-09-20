# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.10.2)

# Declares and names the project.

project("lib_ffmpeg")

# 支持gnu++11
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")

# 1. 定义so库和头文件所在目录，方面后面使用
set(ffmpeg_lib_dir ${CMAKE_SOURCE_DIR}/../../../libs/${ANDROID_ABI})
set(ffmpeg_head_dir ${CMAKE_SOURCE_DIR})
set(native_head_dir ${CMAKE_SOURCE_DIR})

#2. 设置生成的so动态库最后输出的路径
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/../../../libs/${ANDROID_ABI})

# 3. 添加头文件目录
include_directories(${ffmpeg_head_dir}/include)

message("project source dir=${PROJECT_SOURCE_DIR}")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

# 4. 添加ffmpeg相关的so库
add_library(avutil
        SHARED
        IMPORTED)
set_target_properties(avutil
        PROPERTIES IMPORTED_LOCATION
        ${ffmpeg_lib_dir}/libavutil.so)

add_library(swresample
        SHARED
        IMPORTED)
set_target_properties(swresample
        PROPERTIES IMPORTED_LOCATION
        ${ffmpeg_lib_dir}/libswresample.so)

add_library(avcodec
        SHARED
        IMPORTED)
set_target_properties(avcodec
        PROPERTIES IMPORTED_LOCATION
        ${ffmpeg_lib_dir}/libavcodec.so)

add_library(avfilter
        SHARED
        IMPORTED)
set_target_properties(avfilter
        PROPERTIES IMPORTED_LOCATION
        ${ffmpeg_lib_dir}/libavfilter.so)

add_library(swscale
        SHARED
        IMPORTED)
set_target_properties(swscale
        PROPERTIES IMPORTED_LOCATION
        ${ffmpeg_lib_dir}/libswscale.so)

add_library(avformat
        SHARED
        IMPORTED)
set_target_properties(avformat
        PROPERTIES IMPORTED_LOCATION
        ${ffmpeg_lib_dir}/libavformat.so)

add_library(avdevice
        SHARED
        IMPORTED)
set_target_properties(avdevice
        PROPERTIES IMPORTED_LOCATION
        ${ffmpeg_lib_dir}/libavdevice.so)

add_library(avresample
        SHARED
        IMPORTED)
set_target_properties(avresample
        PROPERTIES IMPORTED_LOCATION
        ${ffmpeg_lib_dir}/libavresample.so)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

######
# 5. 引入其他模块的libyuv库，后期再说
# 5.1 获取和定义路径 libyuv
get_filename_component(ROOT_PROJECT_DIR ${CMAKE_SOURCE_DIR}/../../../../ PATH)

# 5.2 使用单个yuv库
set(LIBYUV_DIR ${ROOT_PROJECT_DIR}/libyuv-single)

add_library(libyuv
        SHARED
        IMPORTED)

set_target_properties(libyuv
        PROPERTIES IMPORTED_LOCATION
        ${LIBYUV_DIR}/libs/${ANDROID_ABI}/libyuv.so
        )

# 5.3 libyuv库头文件
include_directories(${LIBYUV_DIR}/src/main/cpp/include)


# 6. 查找代码中使用到的系统库
find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

# 7. 配置目标so库编译信息
add_library(ffmpeg
        SHARED

        ${native_head_dir}/encode/native_code.h
        ${native_head_dir}/encode/native_code.cpp
        ${native_head_dir}/encode/encode_video.h
        ${native_head_dir}/encode/encode_video.cpp
        ${native_head_dir}/encode/encode_mp4.h
        ${native_head_dir}/encode/encode_mp4.cpp
        ${native_head_dir}/encode/encode_jpeg.h
        ${native_head_dir}/encode/encode_jpeg.cpp

        ${native_head_dir}/ffplay_demo.cpp
        )


# 8. 指定编译目标库时，cmake要链接的库
target_link_libraries(

        # 8.1 指定目标库，native-encode 是在上面 add_library 中配置的目标库
        ffmpeg
        # libyuv 后期放开

        # 8.2 连接 FFmpeg 相关的库
        avutil
        avresample
        swresample
        avcodec
        avfilter
        swscale
        avformat
        avdevice
        -landroid
        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})