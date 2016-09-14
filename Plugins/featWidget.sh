: # An attempt to write an install script that works on both Unix and Windows
: # The following should make Windows jump to the end of the bash
: # In Windows CMD a colon is the comment delimeter but in bash it evaluates to true
: # Works in bash and sh, but not csh
:<<"::HIDECMD"
@ECHO OFF

SET host=localhost
SET port=8665
SET overlay=file

:loop
IF NOT "%1"=="" (
    IF "%1"=="-host" (
        SET host=%2
        SHIFT
    )
    IF "%1"=="-port" (
        SET port=%2
        SHIFT
    )
    IF "%1"=="-overlay" (
        SET overlay=%2
        SHIFT
    )
    SHIFT
    GOTO :loop
)

FOR %%f IN (EntityTest) DO (
        %%f\build\install\%%f\bin\%%f.bat -host http://%host%:%port%/rapture -overlay %overlay%
)
EXIT /B 0

: # End of windows CMD script. Start of Unix Shell script
::HIDECMD

# OS specific support (must be 'true' or 'false').

WRAPPER_START=""
WRAPPER_END=" $*"

cygwin=false
msys=false
darwin=false

case "`uname`" in
  CYGWIN* )
    cygwin=true
    ;;
  Darwin* )
    darwin=true
    ;;
  MINGW* )
    msys=true
    ;;
esac

if $msys ; then
    WRAPPER_START="cmd \"/C call "
    WRAPPER_END=".bat $*\"" 
fi

for f in 'Widgets'
do
   CALL="$f/build/install/$f/bin/$f"

   if ($msys) ; then
      CALL=$(echo $CALL | sed 's|/|\\|g')
   fi

   CALL="$WRAPPER_START$CALL$WRAPPER_END"

   eval $CALL
done
