c[_] Mug 0.2, easy JavaScript for your JVM
==========================================

Mug is a simple, static JavaScript compiler for the JVM, written in CoffeeScript (and self-hosted!). Mug targets Java 6 and focuses on simplicity and file size.

### Getting Started

    $ git clone git://github.com/timcameronryan/mug.git mug
    $ cd mug/mug-compiler/test
    $ export CLASSPATH=../mug-runtime-0.2.0-jar-with-dependencies.jar:../asm-3.3.jar:../lib/mug-compiler.jar 
    $ java js.mug.compiler hello-world.js
    $ java -cp $CLASSPATH:out js.hello_world
    Hello world!

### Changelog

*    **Version 0.2 (2011/10/04)** Aaaand we're back. Mug compiler rewritten in
     CoffeeScript, and now self-hosting! Speed/API improvements, better
     ECMAScript support. The old Clojure version has been tagged and locked away.

     Started mug-nodelib project to emulate the Node API.

*    **Version 0.1 (2011/01/08)** Initial release.

The Compiler
------------

Compile modules with mug-compiler.jar and asm-3.3.jar:

	java -cp lib/mug-compiler.jar:asm-3.3.jar js.mug.compiler module.js [path/to/module2.js path/to/module3.js ...]

    Options
      --output, -o <arg>    Output directory (default "out")
      --jar, -j <arg>       Create jar archive in output directory
      --namespace, -n <arg>  Namespace to use for subsequent modules (default "")

In your output directory, your class files or a jar archive are written out.

Mug takes the basename of each file and compiles it into the "js" package. For
example, the file "module.js", becomes the class "js.module". You can specify
namespace parameters with the "-n" parameter, i.e. "module.js" with namespace
"storefront" becomes "js.storefront.module". The "-n" parameter can be used
multiple times and applies to all subsequent files specified.

The Runtime
-----------

Mug includes a small runtime (180kb) to bootstrap compiled modules. Mug modules
can be run directly or included from other scripts. Include the mug-runtime.jar
to run your module:

    java -cp mug-compiler.jar:my-module.jar js.my_module

Mug has built-in support for CommonJS-style `require()`. All modules loaded 
are either relative to the current package ("js.storefront.module" calls
require("./models"), loading "js.storefront.models"), or loaded from Mug's
global package (ie "java", mug-nodelib, etc. See Libraries below).

You can use Mug's "java" module to interact with Java classes directly from
JavaScript:

    var java = require("java")
    var swing = java.import("javax.swing") // get a Package object
    var frame = new swing.JFrame("Window")
    frame.add(new swing.JButton("Click me"))
    frame.setSize(new java.lang.Integer(200), 200) // java.lang/io/util needs no import
    frame.setVisible(true)

You can also run a compiled module programmatically in Java. It returns
the "exports" object:

    import js.my_module;
    import mug.runtime.*;
    ...
    JSObject fs = js.my_module.load(); // returns the exports object
    String id = JSUtils.asString(fs.get("id")); // JSUtils has functions to coerce types
    ((JSFunction) fs.get("open")).invoke(null, "somefile.txt"); // use invoke(thsObj, args...) to call functions

For authoring Mug modules in Java, please see the Wiki.

Where's my standard library? CommonJS support?
----------------------------------------------

Libraries between Java implementations differ drastically. As such, Mug by
default only includes the "java" module, leaving the choice of standard library
up to the user:

* If you're interested in Node.js API compatibility, see my other hosted GitHub
  project, "mug-nodelib".
* If you're interested in CommonJS support, feel free to start a project! I'll
  work with you to get your code up and running.

Mug? Why? How?
--------------

* Compared to Rhino, favors static compilation and small runtime vs. large
  interpreter.
* Minimal overhead. Runtime library `mug-compile.jar` is ~180kb.
* Consistent, easily debuggable compiled output.
* It's a neat party trick.

The Mug compiler is written in CoffeeScript and self-hosted, and is easily
extensible.

The Mug runtime is written in Java and developed in Eclipse.

**NOTE:** Mug is still alpha-quality software, and bug reports are a Good Thing.
Support for ECMAScript features is generally added incrementally. Mug is stable
enough to run one major application (the Mug compiler itself), but there is no
assurance it will run everything. File a bug and support will be added ASAP.

License
-------

Mug is copyright 2010-2011 Tim Cameron Ryan.  
Released under the MIT/BSD license.

**Compiler libraries:**
`parse-js` CL library ported to JavaScript in 2010 by Mihai Bazon, released
under the BSD license. See UglifyJS on Github.  
`ASM Bytecode Manipulation Framework` copyright (c) OW2 Consoritum, released
under the BSD license.  

**Runtime libraries:**
`json-simple` JSON parsing library copyright (c) fangyidong, released under
Apache 2.0 license.  
`jakarta-oro` Java text-processing classes (c) The Apache Software Foundation,
released under the Apache 2.0 license.