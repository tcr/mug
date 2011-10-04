coffee -cb ../src/mug/compiler.coffee
ringo ../src/mug/compiler.js -j mug-compiler-ringo.jar -o ../lib -n mug ../src/mug/compiler.js -n jast ../src/jast/jast.js ../src/jast/walkers.js ../src/jast/nodes.js ../src/jast/parser.js ../src/jast/parser-base.js
