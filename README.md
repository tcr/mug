c[_] JavaScript Compiler for the JVM
====================================

Mug statically compiles JavaScript into Java .class files.

Compiling with mug.jar in your classpath:

	java -cp mug.jar mug.Compiler test.js [module2.js module3.js ...]
	
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
    var JButton = java.import("javax.swing.JButton")
    var frame = new JFrame("Window")
    frame.add(new JButton("Click me"))
    frame.setSize(200, 200)
    frame.setVisible(true)

Why?
----

* Faster than Rhino, sacrificing a full interpreter for the speed of static compilation.
* Minimal overhead. Standard library `mug-js.jar` is < 75kb.
* Mug's goal is that compiled code be as similar to Java as possible, and easily debuggable.
* It's a neat party trick.

The Mug compiler is written in Clojure. However, compiled JavaScript has no Clojure dependencies and only requires the Java `mug-js.jar` archive.

Development
-----------

Mug is developed as an Eclipse project using the Counterclockwise extension.

If you're interesting in helping out:

* Submit bug reports/feature requests!
* Help refine the API (see Issues)
* Write JavaScript testcases (obscure language features, edge cases)
* Write CommonJS modules
* Any items on the TODO list

TODO
----

*ECMAScript Implementation*

* Implement unsupported constructs: `try/catch/throw`, `switch/case/default`, labels, `void`, `delete`
* Complete the standard library
* Complete operator support for some combinations of types
* Normalize DontDelete, DontEnum, ReadOnly patterns
* Replace RegExp implementation (no java.util.regex)

*Compiler*

* Compilation/testing should be programmatic (string, file, stream, etc.)
* Static Java compilation into modules
* Switch to `GeneratorAdapter` in ASM library (can use Clojure's built-in)
* Bytecode inlining of JS utilities and conversions
* Overall polishing of everything

License
-------

Mug is copyright 2010-2011 Tim Cameron Ryan.  
Released under the BSD license.

**Credits:**
`parse-js` CL library originally by Marijn Haverbeke,  
ported to JavaScript in 2010 by Mihai Bazon,
released under the BSD license.