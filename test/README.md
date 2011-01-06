Tests
=====

Tests can be run out of this folder as such:

    java -cp ../lib/mug.jar mug.Compiler --package mug.test test-filename.js
    java -cp ../lib/mug-js.jar:bin mug.test.[test name]
    
Note that '-' is converted to '_' in the package name.

Available test modules:

* *hello-world* - The most basic demo.
* *regression* - Tests various lanugage features.
* *fs-test* - Tests the `fs` filesystem module.
* *java-test* - Tests Java integration module (with Swing).

The following benchmarks borrowed from [The Computer Language Benchmark Game](http://shootout.alioth.debian.org/) to test speed and ECMAScript implementation:

* *binary-trees* - Binary trees benchmark
* *regex-dna* - DNA/RegExp benchmark
* *n-body* - N-body benchmark
* *fannkuch-redux* - Fannkuch-Redux benchmark