coffee -cb ../src/mug/compiler.coffee
java -cp ../mug-runtime-0.2.0-jar-with-dependencies.jar:../asm-3.3.jar:../lib/mug-compiler-ringo.jar js.mug.compiler -j mug-compiler.jar -o ../lib -n mug ../src/mug/compiler.js -n jast ../src/jast/jast.js ../src/jast/walkers.js ../src/jast/nodes.js ../src/jast/parser.js ../src/jast/parser-base.js
