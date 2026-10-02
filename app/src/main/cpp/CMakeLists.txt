cmake_minimum_required(VERSION 3.22.1)
project("Han1meViewer")

add_library(
        chino
        SHARED
        chino.cpp
        kaffu.c
)

find_library(log-lib log)

if (NOT DEFINED HN1_H3_STATIC_LIB)
    message(FATAL_ERROR "HN1_H3_STATIC_LIB must point to libhn1_h3.a")
endif ()
if (NOT DEFINED HN1_H3_VERSION_SCRIPT)
    message(FATAL_ERROR "HN1_H3_VERSION_SCRIPT must point to the export map")
endif ()

target_link_libraries(
        chino
        PRIVATE
        ${log-lib}
        c++_shared
        -Wl,--whole-archive
        ${HN1_H3_STATIC_LIB}
        -Wl,--no-whole-archive
)
target_link_options(chino PRIVATE "-Wl,--version-script=${HN1_H3_VERSION_SCRIPT}")
