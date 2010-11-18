c[_] JavaScript Compiler for the JVM
====================================

Mug statically compiles JavaScript into Java .class files.
It's currently in version crazy-alpha.

Compiling with mug.jar in your classpath:

	java -cp mug.jar mug.Compiler test test.js [module-id file2 module-id file3...]
	
Resulting class files are in the newly created out/ directory.
Include this and mug-js.jar to run a module:

    java -cp mug-js.jar:out mug.modules.test

You can also load a module programmatically in Java. It returns
the "exports" object:

    import mug.Modules;
    ...
    JSObject fs = Modules.getModule("fs").load();

Why?
----

* Faster than Rhino, sacrificing ECMAScript conformity/Java interoperability.
* Compiled JavaScript has little overhead as possible compared to a full engine to support eval() constructs). Preferrable for embedded devices.
* It's a neat party trick.

Roadmap
-------

To-dos, in some particular order.

*ECMAScript Implementation*

* Currently unsupported constructs: labels, `with`, `void`, `try/catch/throw`, `break/continue`, `do`...`while`, `for`...`in`, `switch/case/default`
* Much of the standard library
* More complete type cases for operator

*Mug API*

* Compilation should be programmatic (string, file, stream, etc.)
* Allow compilation/test cycle from source
* Convert AST profiler from JS to Clojure/self-host

*Java Interop*

* Some method for Java<->JS interfacing (Rhino-style)
* `import('java.package.Class');` imports a Java class 

*Debug/Logging*

* Compile line numbers/filenames into bytecode for debugging

*Optimizations*

* Static type analysis and compilation
* Byte-code optimization of utilities

License
-------

Mug is copyright 2010 Tim Cameron Ryan.  
Released under the BSD license.

**Credits:**
`parse-js` CL library originally by Marijn Haverbeke,  
ported to JavaScript in 2010 by Mihai Bazon.
Both released under the zlib license.