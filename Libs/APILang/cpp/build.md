# Instructions for building the C++ Rapture API

The build system for the C++ API uses CMake to generate a makefile. Before running cmake you need
to generate the API cpp files. This is done by executing "gradle genCpp" in the Libs folder of 
the Rapture project. "gradle install" at that level will by default also run the Cpp generation task.

# Dependencies

The C++ API depends on cpp-netlib for its network code, and cpp-netlib depends on boost. Both need to
be present on the build system for the build to work (and for even cmake to succesfully generate
the makefiles for the project).

## Build system

On the Mac you must of the XCode tools installed and then install cmake. A convenient way of installing
cmake is to use brew - brew install cmake.

On Linux you will need a number of packages on a completely clean system:

    sudo apt install default-jdk
    sudo apt install gradle
    sudo apt install build-essential
    sudo apt install cmake

## Boost

Boost is available at http://boost.org.

On the Mac, download the latest version, unpack it and then run:

    ./bootstrap.sh
    ./b2.sh

On Linux you can install through apt-get :

    sudo apt-get install libboost-all-dev.

## cppnetlib

Cpp-netlib is available at http://cpp-netlib.org. There is an apt on Linux but it isn't compiled with
the correct flags - you must build it manually. 

On the Mac, you can provide the location of BOOST by passing in the variable BOOST_ROOT on the cmake
command line, e.g.

    cmake -DBOOST_ROOT=/Users/someone/boost_1_16_0 .

# Rapture build

If all the dependencies are built you can simply run

    cmake .

in the cpp folder of this project, and then run make. As can be seen from the CMakeLists.txt file, the project builds two components - 
a static library for linking with your project and a short test application that uses that library. Feel free to take and adapt the project
to build the library into your client side application.



