c[_] JavaScript compiler for the JVM
====================================

Mug statically compiles JavaScript into Java .class files.
It's currently in version crazy-alpha.

Compiling with mug.jar in your classpath:

	java -cp mug.jar mug.Compiler input input.js [namespace2 file2 namespace3 file3...]
	
Resulting class files are in the newly created out/ directory.
Include this and mug-js.jar to run your module:

    java -cp mug-js.jar:out mug.modules.input

You can also run a module programmatically in Java. It returns
the "exports" object:

    import mug.Modules;
    ...
    JSObject fs = Modules.getModule("fs").load();

Roadmap
-------

Short-term:

* improve ECMAScript implementation
  * currently unsupported constructs: labels, `with`, `try/catch/throw`, `break/continue`, `do`...`while`, `for`...`in`, `switch/case/default`
  * much of the standard library
  * more complete operator support
* make compilation programmatic (string, file, stream, etc.)
* make compilation/test cycle programmatic
* convert AST profiler from JS to Clojure, or at least self-host

Later:

* static type analysis, typed compilation
* better Java<->JS interfacing, perhaps using ASM library+annotations

License
-------

Mug is copyright 2010 Tim Cameron Ryan.  
Released under the BSD license.

**Credits:**
`parse-js` CL library originally by Marijn Haverbeke,  
ported to JavaScript in 2010 by Mihai Bazon.
Both released under the zlib license.
