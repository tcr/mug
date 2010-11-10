c[_] Mug: a JavaScript compiler for the JVM.
============================================

Mug statically compiles JavaScript into Java .class files.
It's currently in version crazy-alpha.

Compiling with mug.jar in your classpath:

	java -cp mug.jar mug.compiler com.example.input input.js [file2 file3...]
	
Resulting class files are in the newly created out/ directory.
Include this and mug-js.jar to run:

    java -cp mug-js.jar:out com.example.input.JSScript

Roadmap
-------

Short-term:

1. allow code to import other modules
2. get compilation working fully from java, i.e. from a string, from a file, etc
3. make it possible to compile/run a script entirely programmatically (interactive, test, etc)

Soon:

* improve ECMAScript implementation
  * currently unsupported constructs: labels, regex literals, `with`, `try/catch/throw`, `break/continue`, `do`, `for`...`in`, `switch/case/default`, `typeof`
  * also broken: native prototypes
* convert AST profiler from JS to Clojure, or at least self-host

Later:

* use ASM library+annotations for Java<->JS interfacing
* implement static type analysis

License
-------

Mug is copyright 2010 Tim Cameron Ryan.  
Released under the BSD license.

Credits:
--------

"parse-js" CL library originally by Marijn Haverbeke.  
Ported to JavaScript in 2010 by Mihai Bazon.  
Both released under the zlib license.
