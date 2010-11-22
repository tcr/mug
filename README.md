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

To interface with java in Mug, use the `java` module.

    var java = require("java")
    var JFrame = java.import("javax.swing.JFrame")
    var frame = new JFrame("Window")
    frame.add(new JButton("Click me"))
    frame.setVisible(true)

Why?
----

* Faster than Rhino, sacrificing ECMAScript conformity for speed.
* Compiled JavaScript has as little overhead as possible compared to a full interpreter. Preferrable for embedded devices.
* It's a neat party trick.

Development
-----------

Mug is an Eclipse project developed using the Counterclockwise extension.

Submitting bug reports/feature requests would be excellent. If you're interested in writing code, I'd love your help with: JavaScript testcases (language features you find are broken), writing modules (see source for mug.modules.fs), and then any items on the TODO list.

Roadmap
-------

To-dos, in some particular order.

*ECMAScript Implementation*

* Currently unsupported constructs: `try/catch/throw`, `switch/case/default`, labels, `with`, `void`
* Complete the standard library
* Complete operator cases for some combinations of types
* Replace regexp implementation (java.util.regex) 

*Mug API*

* Compilation should be programmatic (string, file, stream, etc.)
* Allow compilation/test cycle from source
* Convert AST profiler from JS to Clojure/self-host

*Java Interop*

* Java compilation into modules
* Better Java interfacing

*Debug/Logging*

* Compile line numbers/filenames into bytecode for debugging

*ASM*

* Switch to `GeneratorAdapter` in ASM library (can use Clojure's built-in)

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