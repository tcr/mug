Tests
=====

Tests can be run out of this folder as such:

    java -cp ../lib/mug-js.jar:out mug.modules.$TEST_ID

(Note that all `"-"` become `"_"` in class names)

Available test modules:

* *regression* - Tests various lanugage features.
* *fs-test* - Tests the `fs` filesystem module.

The following benchmarks borrowed from [The Computer Language Benchmark Game](http://shootout.alioth.debian.org/) to test speed and ECMAScript implementation:

* *binary-trees* - Binary trees benchmark
* *regex-dna* - DNA/RegExp benchmark
* *n-body* - N-body benchmark
* *Fannkuch-redux* - Fannkuch-Redux benchmark