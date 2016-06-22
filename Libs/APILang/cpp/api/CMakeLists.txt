include_directories(${PROJECT_SOURCE_DIR}/api/src/)
include_directories(${PROJECT_SOURCE_DIR}/api/src/headers/)

file(GLOB srcs "*.cpp")
file(GLOB gensrcs "generated/*.cpp")
file(GLOB genhdrs "generated/*.hpp")
file(GLOB hdrs "*.hpp")
file(GLOB mainfile "main.cpp")
list(REMOVE_ITEM srcs ${mainfile})

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")
set ( CMAKE_PREFIX_PATH ${CMAKE_PREFIX_PATH} /Users/alanmoore/Development/cpp-netlib)
find_package( cppnetlib 0.11.1 REQUIRED)
include_directories( ${CPPNETLIB_INCLUDE_DIRS})

add_library(rap_api OBJECT ${hdrs} ${srcs} ${genhdrs} ${gensrcs})
add_executable(raptest ${hdrs} ${genhdrs} ${mainfile} $<TARGET_OBJECTS:rap_api>)
target_link_libraries( raptest ${CPPNETLIB_LIBRARIES})
