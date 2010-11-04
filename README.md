c[_] Mug: a JavaScript compiler for the JVM.
============================================

Mug statically compiles JavaScript into Java .class files.
It's currently in version crazy-alpha.

Compiling with mug.jar in your classpath:

	java -cp mug.jar mug.Compiler input.js
	
Resulting class files are in the out/ directory. Include this
and mug-util.jar to run:

    java -cp mug-js.jar;out mug.compiled.JSScript

Roadmap
-------

Short-term:
1. allow chooseable namespaces for compiled code
2. fix jar compilation setup
3. allow code to import other modules
4. get compilation working fully from java, i.e. from a string, from a file, etc
5. make it possible to compile and run a script entirely programmatically (interactive, test, etc)

Soon:
* improve ECMAScript implementation
* convert AST profiler from JS to Clojure

Later:
* use ASM library+annotations for Java<->JS interfacing
* implement static type analysis

License
-------
Mug is released under the BSD license.

Credits:
--------

"parse-js" CL library originally by Marijn Haverbeke.
Ported to JavaScript in 2010 by Mihai Bazon.
Both released under the zlib license.