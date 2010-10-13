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

Soon:
* Convert AST profiler from JS to Clojure
* Improve ECMAScript implementation

Later:
* Use ASM library+annotations for Java<->JS interfacing
* Implement static variable/type analysis

License
-------
Mug is released under the BSD license.

Credits:
--------

"parse-js" CL library originally by Marijn Haverbeke.
Ported to JavaScript in 2010 by Mihai Bazon.
Both released under the zlib license.