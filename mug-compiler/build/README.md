# Build Scripts

So this folder's a bit janky atm. Presumably the compiler should be included
in a Maven project; I'm working toward that. Atm this is the poor man's 
Ant solution:

1. Install CoffeeScript on your system.
2. Install RingoJS on your system.
3. Run ./compile-with-ringo.sh to build the compiler in ../lib/mug-compiler-ringo.jar
4. Run ./compile-with-mug.sh to build the compiler` in ../lib/mug-compiler.jar

RingoJS can be omitted from this process, but it saves a lot of headaches
in this stage of the project to have a stable ECMAScript platform to test
against.