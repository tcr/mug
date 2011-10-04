var AnnotationVisitor, ClassWriter, ClosureContextCompiler, CodeInput, ConstantsCompiler, Context, ContextCompiler, FieldVisitor, Label, MethodCompiler, MethodVisitor, Opcodes, ScriptContextCompiler, compiler, i, input, jast, java, jvm, log, mug, start, _ref, _ref2, _ref3;
var __slice = Array.prototype.slice, __indexOf = Array.prototype.indexOf || function(item) {
  for (var i = 0, l = this.length; i < l; i++) {
    if (this[i] === item) return i;
  }
  return -1;
}, __hasProp = Object.prototype.hasOwnProperty, __extends = function(child, parent) {
  for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; }
  function ctor() { this.constructor = child; }
  ctor.prototype = parent.prototype;
  child.prototype = new ctor;
  child.__super__ = parent.prototype;
  return child;
};
start = new Date();
log = {
  verbose: function() {
    var args;
    args = 1 <= arguments.length ? __slice.call(arguments, 0) : [];
  },
  notify: function() {
    var args;
    args = 1 <= arguments.length ? __slice.call(arguments, 0) : [];
    return console.log.apply(console, args);
  },
  error: function() {
    var args;
    args = 1 <= arguments.length ? __slice.call(arguments, 0) : [];
    return console.error.apply(console, args);
  }
};
jast = require('../jast/jast');
if (this.addToClasspath) {
  this.addToClasspath(this.module.resolve("../asm-3.3.jar"));
  arguments = this.system.args.slice(1);
  _ref = this.Packages.org.objectweb.asm, Opcodes = _ref.Opcodes, ClassWriter = _ref.ClassWriter, FieldVisitor = _ref.FieldVisitor, MethodVisitor = _ref.MethodVisitor, AnnotationVisitor = _ref.AnnotationVisitor, Label = _ref.Label;
} else {
  java = require('java');
  _ref2 = java["import"]('org.objectweb.asm'), Opcodes = _ref2.Opcodes, ClassWriter = _ref2.ClassWriter, FieldVisitor = _ref2.FieldVisitor, MethodVisitor = _ref2.MethodVisitor, AnnotationVisitor = _ref2.AnnotationVisitor, Label = _ref2.Label;
}
jvm = {};
jvm.qn = {
  object: "java/lang/Object",
  boolean: "java/lang/Boolean",
  number: "java/lang/Number",
  double: "java/lang/Double",
  string: "java/lang/String",
  exception: "java/lang/Exception",
  pattern: "java/util/regex/Pattern"
};
jvm.sig = {
  "void": "V",
  double: "D",
  integer: "I",
  boolean: "Z",
  obj: function(x) {
    return "L" + x + ";";
  },
  call: function() {
    var args, ret, _i;
    args = 2 <= arguments.length ? __slice.call(arguments, 0, _i = arguments.length - 1) : (_i = 0, []), ret = arguments[_i++];
    return "(" + args.join('') + ")" + (ret || "V");
  },
  array: function(x) {
    return "[" + x;
  }
};
mug = {};
mug.qn = {};
mug.qn.pkg = "mug/runtime/";
mug.qn["null"] = mug.qn.pkg + "JSNull";
mug.qn.boolean = mug.qn.pkg + "JSBoolean";
mug.qn.string = mug.qn.pkg + "JSString";
mug.qn.number = mug.qn.pkg + "JSNumber";
mug.qn.utils = mug.qn.pkg + "JSUtils";
mug.qn["function"] = mug.qn.pkg + "JSFunction";
mug.qn.object = mug.qn.pkg + "JSObject";
mug.qn.array = mug.qn.pkg + "JSArray";
mug.qn.regexp = mug.qn.pkg + "JSRegExp";
mug.qn.module = mug.qn.pkg + "JSModule";
mug.qn.exception = mug.qn.pkg + "JSException";
mug.qn.valueException = mug.qn.pkg + "JSValueException";
mug.qn.timers = mug.qn.pkg + "JSConcurrency";
mug.qn.toplevel = mug.qn.pkg + "JSEnvironment";
compiler = {};
compiler.globals = ["exports", "require", "print", "console", "arguments", "parseInt", "parseFloat", "isNaN", "isFinite", "Math", "JSON", "Object", "Array", "Number", "String", "Boolean", "Function", "Date", "Error", "SyntaxError", "RegExp", "setTimeout", "setInterval", "clearTimeout", "clearInterval"];
compiler.closureArgCount = 8;
compiler.qn = {};
compiler.qn.script = function() {
  return "js/script";
};
compiler.qn.pkg = function() {
  return compiler.qn.script + "$";
};
compiler.qn.constants = function() {
  return compiler.qn.pkg() + "constants";
};
compiler.qn.scriptscope = function() {
  return compiler.qn.pkg() + "scope$script";
};
compiler.qn.scope = function(i) {
  if (i === 0) {
    return compiler.qn.scriptscope();
  } else {
    return compiler.qn.pkg() + "scope$" + i;
  }
};
compiler.qn.context = function(i) {
  if (i === 0) {
    return compiler.qn.script;
  } else {
    return compiler.qn.pkg() + "closure$" + i;
  }
};
compiler.ident = {
  num: function(n) {
    return "NUM_" + n;
  },
  str: function(n) {
    return "STR_" + n;
  },
  regex: function(n) {
    return "REGEX_" + n;
  },
  scope: function(n) {
    return "SCOPE_" + n;
  },
  env: 'ENV'
};
compiler.sig = {};
compiler.sig.instantiate = (_ref3 = jvm.sig).call.apply(_ref3, [jvm.sig.integer].concat(__slice.call((function() {
  var _ref3, _results;
  _results = [];
  for (i = 0, _ref3 = compiler.closureArgCount; 0 <= _ref3 ? i < _ref3 : i > _ref3; 0 <= _ref3 ? i++ : i--) {
    _results.push(jvm.sig.obj(jvm.qn.object));
  }
  return _results;
})()), [jvm.sig.array(jvm.sig.obj(jvm.qn.object))], [jvm.sig.obj(jvm.qn.object)]));
compiler.sig.invoke = function(c) {
  var i, _ref4;
  if (c == null) {
    c = compiler.closureArgCount;
  }
  return (_ref4 = jvm.sig).call.apply(_ref4, [jvm.sig.obj(jvm.qn.object), jvm.sig.integer].concat(__slice.call((function() {
    var _results;
    _results = [];
    for (i = 0; 0 <= c ? i < c : i > c; 0 <= c ? i++ : i--) {
      _results.push(jvm.sig.obj(jvm.qn.object));
    }
    return _results;
  })()), [jvm.sig.array(jvm.sig.obj(jvm.qn.object))], [jvm.sig.obj(jvm.qn.object)]));
};
compiler.regs = {};
compiler.regs.thisObj = 1;
compiler.regs.argc = 2;
compiler.regs.offset = 3;
compiler.regs.scope = compiler.regs.offset + compiler.closureArgCount + 1;
compiler.regs.ref = compiler.regs.scope + 1;
compiler.stringArray = function() {
  var arr, i, strs, _ref4;
  strs = 1 <= arguments.length ? __slice.call(arguments, 0) : [];
  arr = java.lang.reflect.Array.newInstance(java.lang.String, strs.length);
  for (i = 0, _ref4 = strs.length; 0 <= _ref4 ? i < _ref4 : i > _ref4; 0 <= _ref4 ? i++ : i--) {
    arr[i] = strs[i];
  }
  return arr;
};
ConstantsCompiler = (function() {
  function ConstantsCompiler() {}
  ConstantsCompiler.prototype.compileClass = function() {
    var cw, expr, flags, i, mv, num, nums, regexp, regexps, _len, _len2, _len3, _len4, _ref4;
    nums = input.numbers;
    regexps = input.regexps;
    cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    cw.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, compiler.qn.constants(), null, jvm.qn.object, null);
    for (i = 0, _len = nums.length; i < _len; i++) {
      num = nums[i];
      cw.visitField(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, compiler.ident.num(i), jvm.sig.obj(jvm.qn.double), null, null).visitEnd();
    }
    for (i = 0, _len2 = regexps.length; i < _len2; i++) {
      regexp = regexps[i];
      cw.visitField(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, compiler.ident.regex(i), jvm.sig.obj(jvm.qn.pattern), null, null).visitEnd();
    }
    mv = cw.visitMethod(Opcodes.ACC_STATIC, "<clinit>", jvm.sig.call(jvm.sig["void"]), null, null);
    mv.visitCode();
    for (i = 0, _len3 = nums.length; i < _len3; i++) {
      num = nums[i];
      mv.visitTypeInsn(Opcodes.NEW, jvm.qn.double);
      mv.visitInsn(Opcodes.DUP);
      if (num > 0x7fffffff) {
        num = new java.lang.Long(num).intValue();
      }
      mv.visitLdcInsn(new java.lang.Double(num));
      mv.visitMethodInsn(Opcodes.INVOKESPECIAL, jvm.qn.double, "<init>", jvm.sig.call(jvm.sig.double, jvm.sig["void"]));
      mv.visitFieldInsn(Opcodes.PUTSTATIC, compiler.qn.constants(), compiler.ident.num(i), jvm.sig.obj(jvm.qn.double));
    }
    for (i = 0, _len4 = regexps.length; i < _len4; i++) {
      _ref4 = regexps[i], expr = _ref4[0], flags = _ref4[1];
      mv.visitLdcInsn(expr);
      mv.visitLdcInsn(flags);
      mv.visitMethodInsn(Opcodes.INVOKESTATIC, mug.qn.utils, "compilePattern", jvm.sig.call(jvm.sig.obj(jvm.qn.string), jvm.sig.obj(jvm.qn.string), jvm.sig.obj(jvm.qn.pattern)));
      mv.visitFieldInsn(Opcodes.PUTSTATIC, compiler.qn.constants(), compiler.ident.regex(i), jvm.sig.obj(jvm.qn.pattern));
    }
    mv.visitInsn(Opcodes.RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd;
    cw.visitEnd();
    log.verbose('...Compiled constants struct ' + compiler.qn.constants());
    return [compiler.qn.constants() + '.class', cw.toByteArray()];
  };
  return ConstantsCompiler;
})();
MethodCompiler = (function() {
  function MethodCompiler(ctx) {
    this.ctx = ctx;
    this.lineNumber = -1;
    this.labels = {};
    this.vars = ctx.localVars;
    this.enclosedVars = ctx.enclosedVars;
    this.unenclosedVars = ctx.unenclosedVars;
    log.verbose('Context unenclosed vars:', this.unenclosedVars);
  }
  MethodCompiler.prototype.getNodeCompileType = function(n) {
    switch (n.type) {
      case "null-literal":
        return "null";
      case "boolean-literal":
        return "boolean";
      case "num-literal":
        return "number";
      case "undef-literal":
        return "Object";
      case "str-literal":
        return "String";
      case "regexp-literal":
        return "JSRegExp";
      case "array-literal":
        return "JSArray";
      case "obj-literal":
        return "JSObject";
      case "func-literal":
        return "JSFunction";
      case "add-op-expr":
        return "Object";
      case "sub-op-expr":
      case "div-op-expr":
      case "mul-op-expr":
      case "mod-op-expr":
      case "lsh-op-expr":
      case "neg-op-expr":
      case "bit-and-op-expr":
      case "bit-or-op-expr":
      case "bit-xor-op-expr":
      case "bit-not-op-expr":
        return "number";
      case "eq-op-expr":
      case "neq-op-expr":
      case "not-op-expr":
      case "eqs-op-expr":
      case "neqs-op-expr":
      case "lt-op-expr":
      case "lte-op-expr":
      case "gt-op-expr":
      case "gte-op-expr":
      case "instanceof-op-expr":
      case "in-op-expr":
        return "boolean";
      case "or-op-expr":
      case "and-op-expr":
      case "if-expr":
        return "Object";
      case "typeof-op-expr":
        return "String";
      case "void-op-expr":
        return "Object";
      case "seq-op-expr":
        return this.getNodeCompileType(n.right);
      case "this-expr":
      case "scope-ref-expr":
      case "static-ref-expr":
      case "dyn-ref-expr":
      case "static-method-call-expr":
      case "call-expr":
      case "new-expr":
        return "Object";
      case "scope-assign-expr":
      case "static-assign-expr":
      case "dyn-assign-expr":
      case "if-expr":
        return "Object";
      case "scope-inc-expr":
      case "static-inc-expr":
      case "dyn-inc-expr":
        return "number";
      case "scope-delete-expr":
      case "static-delete-expr":
      case "dyn-delete-expr":
        return "boolean";
      default:
        throw new Error("Couldn't match compile type " + n.type);
    }
  };
  MethodCompiler.prototype.pushLabel = function(label, cont, brk) {
    if (label) {
      this.pushLabel("", cont, brk);
    } else {
      label = "";
    }
    this.labels[label] = this.labels[label] || [];
    this.labels[label].push({
      cont: cont,
      brk: brk
    });
  };
  MethodCompiler.prototype.popLabel = function(label) {
    var _ref4;
    if (label) {
      this.popLabel("");
    } else {
      label = "";
    }
    return (_ref4 = this.labels[label]) != null ? _ref4.pop() : void 0;
  };
  MethodCompiler.prototype.getLabel = function(label) {
    var _ref4;
    label = label || "";
    return (_ref4 = this.labels[label]) != null ? _ref4[this.labels[label].length - 1] : void 0;
  };
  MethodCompiler.prototype.compilePop = function(mv, node) {
    if (this.getNodeCompileType(node) === 'number') {
      mv.visitInsn(Opcodes.POP2);
    } else {
      mv.visitInsn(Opcodes.POP);
    }
  };
  MethodCompiler.prototype.compileLoadInt = function(mv, i) {
    if (i >= -128 && i <= 127) {
      mv.visitIntInsn(Opcodes.BIPUSH, i);
    } else {
      mv.visitLdcInsn(new java.lang.Integer(i));
    }
  };
  MethodCompiler.prototype.compileLoadEnvironment = function(mv) {
    mv.visitVarInsn(Opcodes.ALOAD, 0);
    mv.visitFieldInsn(Opcodes.GETFIELD, compiler.qn.context(this.ctx.id), compiler.ident.env, jvm.sig.obj(mug.qn.toplevel));
  };
  MethodCompiler.prototype.compileScopeRef = function(mv, name, ln) {
    var parent, _i, _len, _ref4;
    log.verbose("Seeking var " + name + " in current scope (" + this.vars + ")");
    if (__indexOf.call(this.vars, name) >= 0 || (this.ctx.id === 0 && __indexOf.call(compiler.globals, name) >= 0)) {
      log.verbose(' . Found in current scope.');
      mv.visitVarInsn(Opcodes.ALOAD, compiler.regs.scope);
      return compiler.qn.scope(this.ctx.id);
    }
    _ref4 = this.ctx.getParents().reverse();
    for (_i = 0, _len = _ref4.length; _i < _len; _i++) {
      parent = _ref4[_i];
      if (__indexOf.call(parent.localVars, name) >= 0 || (parent.id === 0 && __indexOf.call(compiler.globals, name) >= 0)) {
        log.verbose(" . Found in parent scope \#" + parent.id + ".");
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, compiler.qn.context(this.ctx.id), "SCOPE_" + parent.id, jvm.sig.obj(compiler.qn.scope(parent.id)));
        return compiler.qn.scope(parent.id);
      }
    }
    throw new Error("Identifier not found in any scope: " + name + " (line " + ln + ")");
  };
  MethodCompiler.prototype.compileAsObject = function(mv, expr, dupPrimitive) {
    var nums;
    if (dupPrimitive == null) {
      dupPrimitive = false;
    }
    if (expr.type === 'num-literal') {
      nums = input.numbers;
      if (nums.indexOf(expr.value) !== -1) {
        log.verbose(' . Num literal optimization.');
        mv.visitFieldInsn(Opcodes.GETSTATIC, compiler.qn.constants(), compiler.ident.num(nums.indexOf(expr.value)), jvm.sig.obj(jvm.qn.double));
        return;
      }
    }
    switch (this.getNodeCompileType(expr)) {
      case "boolean":
        mv.visitTypeInsn(Opcodes.NEW, jvm.qn.boolean);
        mv.visitInsn(Opcodes.DUP);
        break;
      case "number":
        mv.visitTypeInsn(Opcodes.NEW, jvm.qn.double);
        mv.visitInsn(Opcodes.DUP);
    }
    this.compileNode(mv, expr);
    switch (this.getNodeCompileType(expr)) {
      case 'boolean':
        if (dupPrimitive) {
          mv.visitInsn(Opcodes.DUP_X2);
        }
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, jvm.qn.boolean, "<init>", jvm.sig.call(jvm.sig.boolean, jvm.sig["void"]));
        break;
      case 'number':
        if (dupPrimitive) {
          mv.visitInsn(Opcodes.DUP_X2);
        }
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, jvm.qn.double, "<init>", jvm.sig.call(jvm.sig.double, jvm.sig["void"]));
        break;
      default:
        if (dupPrimitive) {
          mv.visitInsn(Opcodes.DUP_X2);
        }
    }
  };
  MethodCompiler.prototype.compileJSObject = function(mv, expr) {
    switch (this.getNodeCompileType(expr)) {
      case 'boolean':
        mv.visitTypeInsn(Opcodes.NEW, mug.qn.boolean);
        mv.visitInsn(Opcodes.DUP);
        this.compileLoadEnvironment(mv);
        this.compileNode(mv, expr);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, mug.qn.boolean, "<init>", jvm.sig.call(jvm.sig.obj(mug.qn.toplevel), jvm.sig.boolean, jvm.sig["void"]));
        break;
      case 'number':
        mv.visitTypeInsn(Opcodes.NEW, mug.qn.number);
        mv.visitInsn(Opcodes.DUP);
        this.compileLoadEnvironment(mv);
        this.compileNode(mv, expr);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, mug.qn.number, "<init>", jvm.sig.call(jvm.sig.obj(mug.qn.toplevel), jvm.sig.double, jvm.sig["void"]));
        break;
      case 'String':
        mv.visitTypeInsn(Opcodes.NEW, mug.qn.string);
        mv.visitInsn(Opcodes.DUP);
        this.compileLoadEnvironment(mv);
        this.compileNode(mv, expr);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, mug.qn.string, "<init>", jvm.sig.call(jvm.sig.obj(mug.qn.toplevel), jvm.sig.obj(jvm.qn.string), jvm.sig["void"]));
        break;
      default:
        this.compileLoadEnvironment(mv);
        this.compileNode(mv, expr);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, mug.qn.utils, "asJSObject", jvm.sig.call(jvm.sig.obj(mug.qn.toplevel), jvm.sig.obj(jvm.qn.object), jvm.sig.obj(mug.qn.object)));
    }
  };
  MethodCompiler.prototype.compileAsNumber = function(mv, type) {
    switch (type) {
      case "number":
        break;
      default:
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, mug.qn.utils, "asNumber", jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig.double));
    }
  };
  MethodCompiler.prototype.compileAsBoolean = function(mv, type) {
    switch (type) {
      case "boolean":
        break;
      default:
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, mug.qn.utils, "asBoolean", jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig.boolean));
    }
  };
  MethodCompiler.prototype.compileInvokeArgs = function(mv, args) {
    var i, _ref4, _ref5, _ref6;
    mv.visitLdcInsn(new java.lang.Integer(args.length));
    for (i = 0, _ref4 = compiler.closureArgCount; 0 <= _ref4 ? i < _ref4 : i > _ref4; 0 <= _ref4 ? i++ : i--) {
      if (args[i]) {
        this.compileAsObject(mv, args[i]);
      } else {
        mv.visitInsn(Opcodes.ACONST_NULL);
      }
    }
    if (args.length > compiler.closureArgCount) {
      this.compileLoadInt(mv, args.length - compiler.closureArgCount);
      mv.visitTypeInsn(Opcodes.ANEWARRAY, jvm.qn.object);
      for (i = _ref5 = compiler.closureArgCount, _ref6 = args.length; _ref5 <= _ref6 ? i < _ref6 : i > _ref6; _ref5 <= _ref6 ? i++ : i--) {
        mv.visitInsn(Opcodes.DUP);
        this.compileLoadInt(mv, i - compiler.closureArgCount);
        this.compileAsObject(mv, args[i]);
        mv.visitInsn(Opcodes.AASTORE);
      }
    } else {
      mv.visitInsn(Opcodes.ACONST_NULL);
    }
  };
  MethodCompiler.prototype.compileComparison = function(mv, op, left, right) {
    var falseCase, trueCase;
    falseCase = new Label();
    trueCase = new Label();
    this.compileNode(mv, left);
    this.compileAsNumber(mv, this.getNodeCompileType(left));
    this.compileNode(mv, right);
    this.compileAsNumber(mv, this.getNodeCompileType(right));
    mv.visitInsn(Opcodes.DCMPG);
    mv.visitJumpInsn(op, trueCase);
    mv.visitInsn(Opcodes.ICONST_0);
    mv.visitJumpInsn(Opcodes.GOTO, falseCase);
    mv.visitLabel(trueCase);
    mv.visitInsn(Opcodes.ICONST_1);
    mv.visitLabel(falseCase);
  };
  MethodCompiler.prototype.getRegister = function(name) {
    if (__indexOf.call(this.unenclosedVars, name) < 0) {
      return -1;
    }
    if (this.ctx.id !== 0) {
      if (__indexOf.call(this.ctx.node.args, name) >= 0) {
        return compiler.regs.offset + this.ctx.node.args.indexOf(name);
      }
      if (name === this.ctx.node.name) {
        return 0;
      }
    }
    if (this.ctx.id === 0 && __indexOf.call(compiler.globals, name) >= 0) {
      return -1;
    }
    return compiler.regs.ref + this.unenclosedVars.indexOf(name);
  };
  MethodCompiler.prototype.compileNode = function(mv, n) {
    var breakLabel, catchEndLabel, catchLabel, checkLabel, child, continueLabel, ctx, doubleThrowLabel, endCase, endLabel, exceptionLabel, expr, falseCase, finallyLabel, i, label, labels, match, n2, num, parent, qn, qn_parentA, qn_parentB, qn_parentC, qn_parentD, qn_parentE, reg, regex, startLabel, stat, statLabel, trueCase, tryLabel, value, _, _i, _j, _k, _l, _len, _len2, _len3, _len4, _len5, _len6, _len7, _len8, _ref10, _ref11, _ref12, _ref13, _ref14, _ref15, _ref16, _ref17, _ref18, _ref4, _ref5, _ref6, _ref7, _ref8, _ref9;
    if (this.lineNumber !== n.ln) {
      label = new Label();
      mv.visitLabel(label);
      mv.visitLineNumber(n.ln, label);
      this.lineNumber = n.ln;
    }
    switch (n.type) {
      case 'null-literal':
        this.compileLoadEnvironment(mv);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, mug.qn.toplevel, "getNullObject", jvm.sig.call(jvm.sig.obj(mug.qn["null"])));
        break;
      case 'boolean-literal':
        if (n.value) {
          mv.visitInsn(Opcodes.ICONST_1);
        } else {
          mv.visitInsn(Opcodes.ICONST_0);
        }
        break;
      case 'undef-literal':
        mv.visitInsn(Opcodes.ACONST_NULL);
        break;
      case 'num-literal':
        num = n.value;
        if (num > 0x7fffffff) {
          num = new java.lang.Long(num).intValue();
        }
        mv.visitLdcInsn(new java.lang.Double(num));
        break;
      case 'str-literal':
        mv.visitLdcInsn(n.value);
        break;
      case 'regexp-literal':
        _ref4 = input.regexps;
        for (i = 0, _len = _ref4.length; i < _len; i++) {
          regex = _ref4[i];
          if (regex[0] === n.expr && regex[1] === n.flags) {
            break;
          }
        }
        mv.visitTypeInsn(Opcodes.NEW, mug.qn.regexp);
        mv.visitInsn(Opcodes.DUP);
        this.compileLoadEnvironment(mv);
        mv.visitFieldInsn(Opcodes.GETSTATIC, compiler.qn.constants(), compiler.ident.regex(i), jvm.sig.obj(jvm.qn.pattern));
        mv.visitLdcInsn(n.flags);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, mug.qn.utils, "isPatternGlobal", jvm.sig.call(jvm.sig.obj(jvm.qn.string), jvm.sig.boolean));
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, mug.qn.regexp, "<init>", jvm.sig.call(jvm.sig.obj(mug.qn.toplevel), jvm.sig.obj(jvm.qn.pattern), jvm.sig.boolean, jvm.sig["void"]));
        break;
      case 'array-literal':
        mv.visitTypeInsn(Opcodes.NEW, mug.qn.array);
        mv.visitInsn(Opcodes.DUP);
        this.compileLoadEnvironment(mv);
        this.compileLoadInt(mv, n.exprs.length);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, mug.qn.array, "<init>", jvm.sig.call(jvm.sig.obj(mug.qn.toplevel), jvm.sig.integer, jvm.sig["void"]));
        mv.visitInsn(Opcodes.DUP);
        this.compileLoadInt(mv, n.exprs.length);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, jvm.qn.object);
        for (i = 0, _ref5 = n.exprs.length; 0 <= _ref5 ? i < _ref5 : i > _ref5; 0 <= _ref5 ? i++ : i--) {
          mv.visitInsn(Opcodes.DUP);
          this.compileLoadInt(mv, i);
          this.compileAsObject(mv, n.exprs[i]);
          mv.visitInsn(Opcodes.AASTORE);
        }
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, mug.qn.array, "load", jvm.sig.call(jvm.sig.array(jvm.sig.obj(jvm.qn.object)), jvm.sig["void"]));
        break;
      case 'obj-literal':
        mv.visitTypeInsn(Opcodes.NEW, mug.qn.object);
        mv.visitInsn(Opcodes.DUP);
        this.compileLoadEnvironment(mv);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, mug.qn.object, "<init>", jvm.sig.call(jvm.sig.obj(mug.qn.toplevel), jvm.sig["void"]));
        _ref6 = n.props;
        for (_i = 0, _len2 = _ref6.length; _i < _len2; _i++) {
          _ref7 = _ref6[_i], value = _ref7.value, expr = _ref7.expr;
          mv.visitInsn(Opcodes.DUP);
          mv.visitLdcInsn(value);
          this.compileAsObject(mv, expr);
          mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, mug.qn.object, "set", jvm.sig.call(jvm.sig.obj(jvm.qn.string), jvm.sig.obj(jvm.qn.object), jvm.sig["void"]));
        }
        break;
      case 'func-literal':
        child = input.getContext(n.closure);
        qn = compiler.qn.context(child.id);
        mv.visitTypeInsn(Opcodes.NEW, qn);
        mv.visitInsn(Opcodes.DUP);
        this.compileLoadEnvironment(mv);
        _ref8 = this.ctx.getParents();
        for (_j = 0, _len3 = _ref8.length; _j < _len3; _j++) {
          parent = _ref8[_j];
          mv.visitVarInsn(Opcodes.ALOAD, 0);
          mv.visitFieldInsn(Opcodes.GETFIELD, compiler.qn.context(this.ctx.id), "SCOPE_" + parent.id, jvm.sig.obj(compiler.qn.scope(parent.id)));
        }
        mv.visitVarInsn(Opcodes.ALOAD, compiler.regs.scope);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, qn, "<init>", (_ref9 = jvm.sig).call.apply(_ref9, [jvm.sig.obj(mug.qn.toplevel)].concat(__slice.call((function() {
          var _k, _len4, _ref9, _results;
          _ref9 = this.ctx.getLineage();
          _results = [];
          for (_k = 0, _len4 = _ref9.length; _k < _len4; _k++) {
            ctx = _ref9[_k];
            _results.push(jvm.sig.obj(compiler.qn.scope(ctx.id)));
          }
          return _results;
        }).call(this)), [jvm.sig["void"]])));
        break;
      case 'add-op-expr':
        this.compileAsObject(mv, n.left);
        this.compileAsObject(mv, n.right);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, mug.qn.utils, "add", jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig.obj(jvm.qn.object), jvm.sig.obj(jvm.qn.object)));
        break;
      case 'sub-op-expr':
        this.compileNode(mv, n.left);
        this.compileAsNumber(mv, this.getNodeCompileType(n.left));
        this.compileNode(mv, n.right);
        this.compileAsNumber(mv, this.getNodeCompileType(n.right));
        mv.visitInsn(Opcodes.DSUB);
        break;
      case 'div-op-expr':
        this.compileNode(mv, n.left);
        this.compileAsNumber(mv, this.getNodeCompileType(n.left));
        this.compileNode(mv, n.right);
        this.compileAsNumber(mv, this.getNodeCompileType(n.right));
        mv.visitInsn(Opcodes.DDIV);
        break;
      case 'mul-op-expr':
        this.compileNode(mv, n.left);
        this.compileAsNumber(mv, this.getNodeCompileType(n.left));
        this.compileNode(mv, n.right);
        this.compileAsNumber(mv, this.getNodeCompileType(n.right));
        mv.visitInsn(Opcodes.DMUL);
        break;
      case 'mod-op-expr':
        this.compileNode(mv, n.left);
        this.compileAsNumber(mv, this.getNodeCompileType(n.left));
        this.compileNode(mv, n.right);
        this.compileAsNumber(mv, this.getNodeCompileType(n.right));
        mv.visitInsn(Opcodes.DREM);
        break;
      case 'lsh-op-expr':
        this.compileNode(mv, n.left);
        this.compileAsNumber(mv, this.getNodeCompileType(n.left));
        mv.visitInsn(Opcodes.D2I);
        this.compileNode(mv, n.right);
        this.compileAsNumber(mv, this.getNodeCompileType(n.right));
        mv.visitInsn(Opcodes.D2I);
        mv.visitInsn(Opcodes.ISHL);
        mv.visitInsn(Opcodes.I2D);
        break;
      case 'eq-op-expr':
        this.compileAsObject(mv, n.left);
        this.compileAsObject(mv, n.right);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, mug.qn.utils, "testEquality", jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig.obj(jvm.qn.object), jvm.sig.boolean));
        break;
      case 'neq-op-expr':
        this.compileNode(mv, {
          type: 'not-op-expr',
          ln: n.ln,
          expr: {
            type: 'eq-op-expr',
            ln: n.ln,
            left: n.left,
            right: n.right
          }
        });
        break;
      case 'not-op-expr':
        falseCase = new Label();
        trueCase = new Label();
        this.compileNode(mv, n.expr);
        this.compileAsBoolean(mv, this.getNodeCompileType(n.expr));
        mv.visitJumpInsn(Opcodes.IFNE, falseCase);
        mv.visitInsn(Opcodes.ICONST_1);
        mv.visitJumpInsn(Opcodes.GOTO, trueCase);
        mv.visitLabel(falseCase);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitLabel(trueCase);
        break;
      case 'eqs-op-expr':
        this.compileAsObject(mv, n.left);
        this.compileAsObject(mv, n.right);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, mug.qn.utils, "testStrictEquality", jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig.obj(jvm.qn.object), jvm.sig.boolean));
        break;
      case 'neqs-op-expr':
        this.compileNode(mv, {
          type: 'not-op-expr',
          ln: n.ln,
          expr: {
            type: 'eqs-op-expr',
            ln: n.ln,
            left: n.left,
            right: n.right
          }
        });
        break;
      case 'lt-op-expr':
        this.compileComparison(mv, Opcodes.IFLT, n.left, n.right);
        break;
      case 'lte-op-expr':
        this.compileComparison(mv, Opcodes.IFLE, n.left, n.right);
        break;
      case 'gt-op-expr':
        this.compileComparison(mv, Opcodes.IFGT, n.left, n.right);
        break;
      case 'gte-op-expr':
        this.compileComparison(mv, Opcodes.IFGE, n.left, n.right);
        break;
      case 'lt-op-expr':
        this.compileComparison(mv, Opcodes.IFLT, n.left, n.right);
        break;
      case 'neg-op-expr':
        this.compileNode(mv, n.expr);
        this.compileAsNumber(mv, this.getNodeCompileType(n.expr));
        mv.visitInsn(Opcodes.DNEG);
        break;
      case 'or-op-expr':
        this.compileAsObject(mv, n.left);
        mv.visitInsn(Opcodes.DUP);
        this.compileAsBoolean(mv, 'Object');
        trueCase = new Label();
        mv.visitJumpInsn(Opcodes.IFNE, trueCase);
        mv.visitInsn(Opcodes.POP);
        this.compileAsObject(mv, n.right);
        mv.visitLabel(trueCase);
        break;
      case 'and-op-expr':
        this.compileAsObject(mv, n.left);
        mv.visitInsn(Opcodes.DUP);
        this.compileAsBoolean(mv, 'Object');
        falseCase = new Label();
        mv.visitJumpInsn(Opcodes.IFEQ, falseCase);
        mv.visitInsn(Opcodes.POP);
        this.compileAsObject(mv, n.right);
        mv.visitLabel(falseCase);
        break;
      case 'bit-and-op-expr':
        this.compileNode(mv, n.left);
        this.compileAsNumber(mv, this.getNodeCompileType(n.left));
        mv.visitInsn(Opcodes.D2L);
        this.compileNode(mv, n.right);
        this.compileAsNumber(mv, this.getNodeCompileType(n.right));
        mv.visitInsn(Opcodes.D2L);
        mv.visitInsn(Opcodes.LAND);
        mv.visitInsn(Opcodes.L2D);
        break;
      case 'bit-or-op-expr':
        this.compileNode(mv, n.left);
        this.compileAsNumber(mv, this.getNodeCompileType(n.left));
        mv.visitInsn(Opcodes.D2L);
        this.compileNode(mv, n.right);
        this.compileAsNumber(mv, this.getNodeCompileType(n.right));
        mv.visitInsn(Opcodes.D2L);
        mv.visitInsn(Opcodes.LOR);
        mv.visitInsn(Opcodes.L2D);
        break;
      case 'bit-xor-op-expr':
        this.compileNode(mv, n.left);
        this.compileAsNumber(mv, this.getNodeCompileType(n.left));
        mv.visitInsn(Opcodes.D2L);
        this.compileNode(mv, n.right);
        this.compileAsNumber(mv, this.getNodeCompileType(n.right));
        mv.visitInsn(Opcodes.D2L);
        mv.visitInsn(Opcodes.LXOR);
        mv.visitInsn(Opcodes.L2D);
        break;
      case 'instanceof-op-expr':
        switch (this.getNodeCompileType(n.left)) {
          case 'number':
          case 'boolean':
            mv.visitInsn(Opcodes.ICONST_0);
            break;
          default:
            switch (this.getNodeCompileType(n.right)) {
              case 'number':
              case 'boolean':
              case 'String':
                throw new Error("Objects cannot be instances of primitives (line " + this.ln + ")");
                break;
              default:
                this.compileAsObject(mv, n.right);
                mv.visitTypeInsn(Opcodes.CHECKCAST, mug.qn.object);
                this.compileAsObject(mv, n.left);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, mug.qn.object, "hasInstance", jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig.boolean));
            }
        }
        break;
      case 'in-op-expr':
        throw new Error("TODO: support 'in' operator (line " + this.ln);
        break;
      case 'typeof-op-expr':
        this.compileNode(mv, n.expr);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, mug.qn.utils, "typeof", jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig.obj(jvm.qn.string)));
        break;
      case 'void-op-expr':
        this.compileNode(mv, n.expr);
        this.compilePop(mv, n.expr);
        mv.visitInsn(Opcodes.ACONST_NULL);
        break;
      case 'seq-op-expr':
        this.compileNode(mv, n.left);
        this.compilePop(mv, n.left);
        this.compileNode(mv, n.right);
        break;
      case 'this-expr':
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        break;
      case 'if-expr':
        this.compileNode(mv, n.expr);
        this.compileAsBoolean(mv, this.getNodeCompileType(n.expr));
        falseCase = new Label();
        trueCase = new Label();
        mv.visitJumpInsn(Opcodes.IFEQ, falseCase);
        this.compileAsObject(mv, n.thenExpr);
        mv.visitJumpInsn(Opcodes.GOTO, trueCase);
        mv.visitLabel(falseCase);
        this.compileAsObject(mv, n.elseExpr);
        mv.visitLabel(trueCase);
        break;
      case 'scope-ref-expr':
        if ((reg = this.getRegister(n.value)) !== -1) {
          mv.visitVarInsn(Opcodes.ALOAD, reg);
        } else {
          qn_parentC = this.compileScopeRef(mv, n.value, n.ln);
          mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, qn_parentC, "get_" + n.value, jvm.sig.call(jvm.sig.obj(jvm.qn.object)));
        }
        break;
      case 'static-ref-expr':
        this.compileJSObject(mv, n.base);
        mv.visitLdcInsn(n.value);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, mug.qn.object, "get", jvm.sig.call(jvm.sig.obj(jvm.qn.string), jvm.sig.obj(jvm.qn.object)));
        break;
      case 'dyn-ref-expr':
        this.compileJSObject(mv, n.base);
        switch (this.getNodeCompileType(n.index)) {
          case 'number':
            this.compileNode(mv, n.index);
            mv.visitInsn(Opcodes.D2I);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, mug.qn.object, "get", jvm.sig.call(jvm.sig.integer, jvm.sig.obj(jvm.qn.object)));
            break;
          default:
            this.compileAsObject(mv, n.index);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, mug.qn.object, "get", jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig.obj(jvm.qn.object)));
        }
        break;
      case 'static-method-call-expr':
        this.compileJSObject(mv, n.base);
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn(n.value);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, mug.qn.object, "get", jvm.sig.call(jvm.sig.obj(jvm.qn.string), jvm.sig.obj(jvm.qn.object)));
        mv.visitTypeInsn(Opcodes.CHECKCAST, mug.qn.object);
        mv.visitInsn(Opcodes.SWAP);
        this.compileInvokeArgs(mv, n.args);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, mug.qn.object, "invoke", compiler.sig.invoke());
        break;
      case 'call-expr':
        this.compileJSObject(mv, n.expr);
        mv.visitInsn(Opcodes.ACONST_NULL);
        this.compileInvokeArgs(mv, n.args);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, mug.qn.object, "invoke", compiler.sig.invoke());
        break;
      case 'new-expr':
        this.compileNode(mv, n.constructor);
        mv.visitTypeInsn(Opcodes.CHECKCAST, mug.qn.object);
        this.compileInvokeArgs(mv, n.args);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, mug.qn.object, "instantiate", compiler.sig.instantiate);
        break;
      case 'scope-assign-expr':
        this.compileAsObject(mv, n.expr);
        mv.visitInsn(Opcodes.DUP);
        if ((reg = this.getRegister(n.value)) !== -1) {
          mv.visitVarInsn(Opcodes.ASTORE, reg);
        } else {
          qn_parentD = this.compileScopeRef(mv, n.value, n.ln);
          mv.visitInsn(Opcodes.SWAP);
          mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, qn_parentD, "set_" + n.value, jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig["void"]));
        }
        break;
      case 'static-assign-expr':
        this.compileJSObject(mv, n.base);
        mv.visitLdcInsn(n.value);
        this.compileAsObject(mv, n.expr);
        mv.visitInsn(Opcodes.DUP_X2);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, mug.qn.object, "set", jvm.sig.call(jvm.sig.obj(jvm.qn.string), jvm.sig.obj(jvm.qn.object), jvm.sig["void"]));
        break;
      case 'dyn-assign-expr':
        this.compileJSObject(mv, n.base);
        switch (this.getNodeCompileType(n.index)) {
          case 'number':
            this.compileNode(mv, n.index);
            mv.visitInsn(Opcodes.D2I);
            this.compileAsObject(mv, n.expr);
            mv.visitInsn(Opcodes.DUP_X2);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, mug.qn.object, "set", jvm.sig.call(jvm.sig.integer, jvm.sig.obj(jvm.qn.object), jvm.sig["void"]));
            break;
          default:
            this.compileAsObject(mv, n.index);
            this.compileAsObject(mv, n.expr);
            mv.visitInsn(Opcodes.DUP_X2);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, mug.qn.object, "set", jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig.obj(jvm.qn.object), jvm.sig["void"]));
        }
        break;
      case 'scope-inc-expr':
        if (n.pre) {
          n2 = {
            type: 'scope-assign-expr',
            ln: n.ln,
            value: n.value,
            expr: {
              type: 'add-op-expr',
              ln: n.ln,
              left: {
                type: 'scope-ref-expr',
                ln: n.ln,
                value: n.value
              },
              right: {
                type: 'num-literal',
                ln: n.ln,
                value: n.inc
              }
            }
          };
          this.compileNode(mv, n2);
          this.compileAsNumber(mv, this.getNodeCompileType(n2));
        } else {
          if ((reg = this.getRegister(n.value)) !== -1) {
            mv.visitVarInsn(Opcodes.ALOAD, reg);
            this.compileAsNumber(mv, 'Object');
            mv.visitInsn(Opcodes.DUP2);
          } else {
            qn_parentE = this.compileScopeRef(mv, n.value, n.ln);
            mv.visitInsn(Opcodes.DUP);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, qn_parentE, "get_" + n.value, jvm.sig.call(jvm.sig.obj(jvm.qn.object)));
            this.compileAsNumber(mv, 'Object');
            mv.visitInsn(Opcodes.DUP2_X1);
          }
          mv.visitTypeInsn(Opcodes.NEW, jvm.qn.double);
          mv.visitInsn(Opcodes.DUP_X2);
          mv.visitInsn(Opcodes.DUP_X2);
          mv.visitInsn(Opcodes.POP);
          mv.visitLdcInsn(new java.lang.Double(n.inc));
          mv.visitInsn(Opcodes.DADD);
          mv.visitMethodInsn(Opcodes.INVOKESPECIAL, jvm.qn.double, "<init>", jvm.sig.call(jvm.sig.double, jvm.sig["void"]));
          if (reg !== -1) {
            mv.visitVarInsn(Opcodes.ASTORE, reg);
          } else {
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, qn_parentE, "set_" + n.value, jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig["void"]));
          }
        }
        break;
      case 'static-inc-expr':
        if (n.pre) {
          n2 = {
            type: 'static-assign-expr',
            ln: n.ln,
            base: n.base,
            value: n.value,
            expr: {
              type: 'add-op-expr',
              ln: n.ln,
              left: {
                type: 'static-ref-expr',
                ln: n.ln,
                base: n.base,
                value: n.value
              },
              right: {
                type: 'num-literal',
                ln: n.ln,
                value: n.inc
              }
            }
          };
          this.compileNode(mv, n2);
          this.compileAsNumber(mv, this.getNodeCompileType(n2));
        } else {
          this.compileJSObject(mv, n.base);
          mv.visitInsn(Opcodes.DUP);
          mv.visitLdcInsn(n.value);
          mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, mug.qn.object, "get", jvm.sig.call(jvm.sig.obj(jvm.qn.string), jvm.sig.obj(jvm.qn.object)));
          this.compileAsNumber(mv, 'Object');
          mv.visitInsn(Opcodes.DUP2_X1);
          mv.visitTypeInsn(Opcodes.NEW, jvm.qn.double);
          mv.visitInsn(Opcodes.DUP_X2);
          mv.visitInsn(Opcodes.DUP_X2);
          mv.visitInsn(Opcodes.POP);
          mv.visitLdcInsn(new java.lang.Double(n.inc));
          mv.visitInsn(Opcodes.DADD);
          mv.visitMethodInsn(Opcodes.INVOKESPECIAL, jvm.qn.double, "<init>", jvm.sig.call(jvm.sig.double, jvm.sig["void"]));
          mv.visitLdcInsn(n.value);
          mv.visitInsn(Opcodes.SWAP);
          mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, mug.qn.object, "set", jvm.sig.call(jvm.sig.obj(jvm.qn.string), jvm.sig.obj(jvm.qn.object), jvm.sig["void"]));
        }
        break;
      case 'dyn-inc-expr':
        if (true) {
          n2 = {
            type: 'dyn-assign-expr',
            ln: n.ln,
            base: n.base,
            index: n.index,
            expr: {
              type: 'add-op-expr',
              ln: n.ln,
              left: {
                type: 'dyn-ref-expr',
                ln: n.ln,
                base: n.base,
                index: n.index
              },
              right: {
                type: 'num-literal',
                ln: n.ln,
                value: n.inc
              }
            }
          };
          this.compileNode(mv, n2);
          this.compileAsNumber(mv, this.getNodeCompileType(n2));
        } else {
          throw new Error('No support for dyn-inc-expr(pre)');
        }
        break;
      case 'scope-delete-expr':
        mv.visitInsn(Opcodes.ICONST_0);
        break;
      case 'static-delete-expr':
        this.compileJSObject(mv, n.base);
        mv.visitLdcInsn(n.value);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, mug.qn.object, "delete", jvm.sig.call(jvm.sig.obj(jvm.qn.string), jvm.sig.boolean));
        break;
      case 'dyn-delete-expr':
        this.compileJSObject(mv, n.base);
        this.compileAsObject(mv, n.index);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, mug.qn.object, "delete", jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig.boolean));
        break;
      case 'block-stat':
        _ref10 = n.stats;
        for (_k = 0, _len4 = _ref10.length; _k < _len4; _k++) {
          stat = _ref10[_k];
          this.compileNode(mv, stat);
        }
        break;
      case 'expr-stat':
        this.compileNode(mv, n.expr);
        this.compilePop(mv, n.expr);
        break;
      case 'ret-stat':
        if (!n.expr) {
          mv.visitInsn(Opcodes.ACONST_NULL);
        } else {
          this.compileAsObject(mv, n.expr);
        }
        mv.visitInsn(Opcodes.ARETURN);
        break;
      case 'while-stat':
        trueCase = new Label();
        falseCase = new Label();
        this.pushLabel(null, trueCase, falseCase);
        mv.visitLabel(trueCase);
        this.compileNode(mv, n.expr);
        this.compileAsBoolean(mv, this.getNodeCompileType(n.expr));
        mv.visitJumpInsn(Opcodes.IFEQ, falseCase);
        if (n.stat) {
          this.compileNode(mv, n.stat);
        }
        mv.visitJumpInsn(Opcodes.GOTO, trueCase);
        mv.visitLabel(falseCase);
        this.popLabel(null);
        break;
      case 'do-while-stat':
        trueCase = new Label();
        falseCase = new Label();
        this.pushLabel(null, trueCase, falseCase);
        mv.visitLabel(trueCase);
        if (n.stat) {
          this.compileNode(mv, n.stat);
        }
        this.compileNode(mv, n.expr);
        this.compileAsBoolean(mv, this.getNodeCompileType(n.expr));
        mv.visitJumpInsn(Opcodes.IFNE, trueCase);
        mv.visitLabel(falseCase);
        this.popLabel(null);
        break;
      case 'for-stat':
        if (n.init) {
          this.compileNode(mv, n.init);
          if (jast.isExpr(n.init)) {
            this.compilePop(mv, n.init);
          }
        }
        startLabel = new Label();
        continueLabel = new Label();
        breakLabel = new Label();
        this.pushLabel(null, continueLabel, breakLabel);
        mv.visitLabel(startLabel);
        if (n.expr) {
          this.compileNode(mv, n.expr);
          this.compileAsBoolean(mv, this.getNodeCompileType(n.expr));
          mv.visitJumpInsn(Opcodes.IFEQ, breakLabel);
        }
        if (n.stat) {
          this.compileNode(mv, n.stat);
        }
        mv.visitLabel(continueLabel);
        if (n.step) {
          this.compileNode(mv, n.step);
          this.compilePop(mv, n.step);
        }
        mv.visitJumpInsn(Opcodes.GOTO, startLabel);
        mv.visitLabel(breakLabel);
        this.popLabel(null);
        break;
      case 'if-stat':
        this.compileNode(mv, n.expr);
        this.compileAsBoolean(mv, this.getNodeCompileType(n.expr));
        falseCase = new Label();
        trueCase = new Label();
        mv.visitJumpInsn(Opcodes.IFEQ, falseCase);
        this.compileNode(mv, n.thenStat);
        mv.visitJumpInsn(Opcodes.GOTO, trueCase);
        mv.visitLabel(falseCase);
        if (n.elseStat) {
          this.compileNode(mv, n.elseStat);
        }
        mv.visitLabel(trueCase);
        break;
      case 'switch-stat':
        endCase = new Label();
        labels = (function() {
          var _l, _len5, _ref11, _results;
          _ref11 = n.cases;
          _results = [];
          for (_l = 0, _len5 = _ref11.length; _l < _len5; _l++) {
            _ = _ref11[_l];
            _results.push(new Label);
          }
          return _results;
        })();
        this.pushLabel(null, null, endCase);
        this.compileAsObject(mv, n.expr);
        _ref11 = n.cases;
        for (i = 0, _len5 = _ref11.length; i < _len5; i++) {
          _ref12 = _ref11[i], match = _ref12.match, stat = _ref12.stat;
          if (match) {
            mv.visitInsn(Opcodes.DUP);
            this.compileAsObject(mv, match);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, mug.qn.utils, "testEquality", jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig.obj(jvm.qn.object), jvm.sig.boolean));
            mv.visitJumpInsn(Opcodes.IFNE, labels[i]);
          }
        }
        _ref13 = n.cases;
        for (i = 0, _len6 = _ref13.length; i < _len6; i++) {
          _ref14 = _ref13[i], match = _ref14.match, stat = _ref14.stat;
          if (!match) {
            mv.visitJumpInsn(Opcodes.GOTO, labels[i]);
          }
        }
        mv.visitJumpInsn(Opcodes.GOTO, endCase);
        _ref15 = n.cases;
        for (i = 0, _len7 = _ref15.length; i < _len7; i++) {
          _ref16 = _ref15[i], match = _ref16.match, stat = _ref16.stat;
          mv.visitLabel(labels[i]);
          this.compileNode(mv, stat);
        }
        mv.visitLabel(endCase);
        mv.visitInsn(Opcodes.POP);
        this.popLabel(null);
        break;
      case 'throw-stat':
        mv.visitTypeInsn(Opcodes.NEW, mug.qn.valueException);
        mv.visitInsn(Opcodes.DUP);
        this.compileAsObject(mv, n.expr);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, mug.qn.valueException, "<init>", jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig["void"]));
        mv.visitInsn(Opcodes.ATHROW);
        break;
      case 'try-stat':
        tryLabel = new Label;
        catchLabel = new Label;
        finallyLabel = new Label;
        doubleThrowLabel = new Label;
        endLabel = new Label;
        mv.visitLabel(tryLabel);
        this.compileNode(mv, n.tryStat);
        mv.visitJumpInsn(Opcodes.GOTO, finallyLabel);
        mv.visitLabel(catchLabel);
        if (n.catchBlock) {
          exceptionLabel = new Label;
          catchEndLabel = new Label;
          mv.visitInsn(Opcodes.DUP);
          mv.visitTypeInsn(Opcodes.INSTANCEOF, mug.qn.valueException);
          mv.visitJumpInsn(Opcodes.IFEQ, exceptionLabel);
          mv.visitTypeInsn(Opcodes.CHECKCAST, mug.qn.valueException);
          mv.visitFieldInsn(Opcodes.GETFIELD, mug.qn.valueException, "value", jvm.sig.obj(jvm.qn.object));
          mv.visitJumpInsn(Opcodes.GOTO, catchEndLabel);
          mv.visitLabel(exceptionLabel);
          mv.visitInsn(Opcodes.DUP);
          mv.visitTypeInsn(Opcodes.INSTANCEOF, mug.qn.exception);
          mv.visitJumpInsn(Opcodes.IFNE, catchEndLabel);
          mv.visitTypeInsn(Opcodes.NEW, mug.qn.exception);
          mv.visitInsn(Opcodes.DUP_X1);
          mv.visitInsn(Opcodes.SWAP);
          this.compileLoadEnvironment(mv);
          mv.visitInsn(Opcodes.SWAP);
          mv.visitMethodInsn(Opcodes.INVOKESPECIAL, mug.qn.exception, "<init>", jvm.sig.call(jvm.sig.obj(mug.qn.toplevel), jvm.sig.obj(jvm.qn.exception), jvm.sig["void"]));
          mv.visitLabel(catchEndLabel);
          if ((reg = this.getRegister(n.catchBlock.value)) !== -1) {
            mv.visitVarInsn(Opcodes.ASTORE, reg);
          } else {
            qn_parentA = this.compileScopeRef(mv, n.catchBlock.value, n.catchBlock.ln);
            mv.visitInsn(Opcodes.SWAP);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, qn_parentA, "set_" + n.catchBlock.value, jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig["void"]));
          }
          if (n.catchBlock.stat) {
            this.compileNode(mv, n.catchBlock.stat);
          }
        } else {
          mv.visitInsn(Opcodes.POP);
        }
        mv.visitLabel(finallyLabel);
        if (n.finallyStat) {
          this.compileNode(mv, n.finallyStat);
        }
        mv.visitJumpInsn(Opcodes.GOTO, endLabel);
        mv.visitLabel(doubleThrowLabel);
        if (n.finallyStat) {
          this.compileNode(mv, n.finallyStat);
        }
        mv.visitInsn(Opcodes.ATHROW);
        mv.visitLabel(endLabel);
        mv.visitTryCatchBlock(tryLabel, catchLabel, catchLabel, jvm.qn.exception);
        mv.visitTryCatchBlock(tryLabel, finallyLabel, doubleThrowLabel, null);
        break;
      case 'break-stat':
        if (!(label = this.getLabel(null))) {
          throw new Error("Cannot break outside of loop");
        }
        mv.visitJumpInsn(Opcodes.GOTO, label.brk);
        break;
      case 'continue-stat':
        if (!(label = this.getLabel(null))) {
          throw new Error("Cannot continue outside of loop");
        }
        mv.visitJumpInsn(Opcodes.GOTO, label.cont);
        break;
      case 'var-stat':
        _ref17 = n.vars;
        for (_l = 0, _len8 = _ref17.length; _l < _len8; _l++) {
          _ref18 = _ref17[_l], value = _ref18.value, expr = _ref18.expr;
          if (expr) {
            this.compileNode(mv, {
              type: 'expr-stat',
              ln: expr.ln,
              expr: {
                type: 'scope-assign-expr',
                ln: expr.ln,
                value: value,
                expr: expr
              }
            });
          }
        }
        break;
      case 'defn-stat':
        this.compileNode(mv, {
          type: 'expr-stat',
          ln: n.ln,
          expr: {
            type: 'scope-assign-expr',
            ln: n.ln,
            value: n.closure.name,
            expr: {
              type: 'func-literal',
              ln: n.ln,
              closure: n.closure
            }
          }
        });
        break;
      case 'for-in-stat':
        checkLabel = new Label();
        statLabel = new Label();
        this.pushLabel(null, statLabel, checkLabel);
        this.compileJSObject(mv, n.expr);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, mug.qn.object, "getKeys", jvm.sig.call(jvm.sig.array(jvm.sig.obj(jvm.qn.string))));
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitJumpInsn(Opcodes.GOTO, checkLabel);
        mv.visitLabel(statLabel);
        mv.visitInsn(Opcodes.DUP2);
        mv.visitInsn(Opcodes.AALOAD);
        if ((reg = this.getRegister(n.value)) !== -1) {
          mv.visitVarInsn(Opcodes.ASTORE, reg);
        } else {
          qn_parentB = this.compileScopeRef(mv, n.value, n.ln);
          mv.visitInsn(Opcodes.SWAP);
          mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, qn_parentB, "set_" + n.value, jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig["void"]));
        }
        this.compileNode(mv, n.stat);
        mv.visitInsn(Opcodes.ICONST_1);
        mv.visitInsn(Opcodes.IADD);
        mv.visitLabel(checkLabel);
        mv.visitInsn(Opcodes.DUP2);
        mv.visitInsn(Opcodes.SWAP);
        mv.visitInsn(Opcodes.ARRAYLENGTH);
        mv.visitJumpInsn(Opcodes.IF_ICMPLT, statLabel);
        mv.visitInsn(Opcodes.POP2);
        this.popLabel(null);
        break;
      default:
        throw new Error('Unrecognized type ' + JSON.stringify(n.type));
    }
  };
  return MethodCompiler;
})();
ContextCompiler = (function() {
  function ContextCompiler(path, ctx) {
    this.path = path;
    this.ctx = ctx;
  }
  ContextCompiler.prototype.compileScopeClass = function() {
    var cw, mv, name, qn, scope, x, _i, _j, _k, _l, _len, _len2, _len3, _len4, _ref4, _ref5;
    qn = compiler.qn.scope(this.ctx.id);
    scope = this.ctx.localVars.slice();
    if (this.ctx.id === 0) {
      _ref4 = compiler.globals;
      for (_i = 0, _len = _ref4.length; _i < _len; _i++) {
        x = _ref4[_i];
        if (__indexOf.call(scope, x) < 0) {
          scope.push(x);
        }
      }
    }
    cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    cw.visit(Opcodes.V1_6, Opcodes.ACC_SUPER + Opcodes.ACC_PUBLIC, qn, null, jvm.qn.object, null);
    for (_j = 0, _len2 = scope.length; _j < _len2; _j++) {
      name = scope[_j];
      cw.visitField(0, "_" + name, jvm.sig.obj(jvm.qn.object), null, null).visitEnd();
    }
    for (_k = 0, _len3 = scope.length; _k < _len3; _k++) {
      name = scope[_k];
      mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "get_" + name, jvm.sig.call(jvm.sig.obj(jvm.qn.object)), null, null);
      mv.visitCode();
      mv.visitVarInsn(Opcodes.ALOAD, 0);
      mv.visitFieldInsn(Opcodes.GETFIELD, qn, "_" + name, jvm.sig.obj(jvm.qn.object));
      mv.visitInsn(Opcodes.ARETURN);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
      mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "set_" + name, jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig["void"]), null, null);
      mv.visitCode();
      mv.visitVarInsn(Opcodes.ALOAD, 0);
      mv.visitVarInsn(Opcodes.ALOAD, 1);
      mv.visitFieldInsn(Opcodes.PUTFIELD, qn, "_" + name, jvm.sig.obj(jvm.qn.object));
      mv.visitInsn(Opcodes.RETURN);
      mv.visitMaxs(2, 2);
      mv.visitEnd();
    }
    if (this.ctx.id === 0) {
      mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", jvm.sig.call(jvm.sig.obj(mug.qn.toplevel), jvm.sig["void"]), null, null);
      mv.visitCode();
      mv.visitVarInsn(Opcodes.ALOAD, 0);
      mv.visitMethodInsn(Opcodes.INVOKESPECIAL, jvm.qn.object, "<init>", jvm.sig.call(jvm.sig["void"]));
      _ref5 = compiler.globals;
      for (_l = 0, _len4 = _ref5.length; _l < _len4; _l++) {
        name = _ref5[_l];
        if (name !== 'exports') {
          mv.visitVarInsn(Opcodes.ALOAD, 0);
          mv.visitVarInsn(Opcodes.ALOAD, 1);
          mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, mug.qn.toplevel, "get_" + name, jvm.sig.call(jvm.sig.obj(jvm.qn.object)));
          mv.visitFieldInsn(Opcodes.PUTFIELD, qn, "_" + name, jvm.sig.obj(jvm.qn.object));
        }
      }
      mv.visitInsn(Opcodes.RETURN);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    } else {
      mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", jvm.sig.call(jvm.sig["void"]), null, null);
      mv.visitCode();
      mv.visitVarInsn(Opcodes.ALOAD, 0);
      mv.visitMethodInsn(Opcodes.INVOKESPECIAL, jvm.qn.object, "<init>", jvm.sig.call(jvm.sig["void"]));
      mv.visitInsn(Opcodes.RETURN);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }
    cw.visitEnd();
    log.verbose('...Compiled scope ' + qn);
    return [qn + '.class', cw.toByteArray()];
  };
  return ContextCompiler;
})();
ScriptContextCompiler = (function() {
  __extends(ScriptContextCompiler, ContextCompiler);
  function ScriptContextCompiler() {
    ScriptContextCompiler.__super__.constructor.apply(this, arguments);
  }
  ScriptContextCompiler.prototype.compileClass = function() {
    var cw, mv, qn;
    cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    qn = compiler.qn.context(this.ctx.id);
    cw.visit(Opcodes.V1_6, Opcodes.ACC_SUPER + Opcodes.ACC_PUBLIC, qn, null, mug.qn.module, null);
    cw.visitSource(this.path, null);
    this.compileInit(cw);
    this.compileMethods(cw);
    this.compileFields(cw);
    mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "main", jvm.sig.call(jvm.sig.array(jvm.sig.obj(jvm.qn.string)), jvm.sig["void"]), null, compiler.stringArray(jvm.qn.exception));
    mv.visitCode();
    mv.visitTypeInsn(Opcodes.NEW, compiler.qn.script);
    mv.visitInsn(Opcodes.DUP);
    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, compiler.qn.script, "<init>", jvm.sig.call(jvm.sig["void"]));
    mv.visitTypeInsn(Opcodes.NEW, mug.qn.toplevel);
    mv.visitInsn(Opcodes.DUP);
    mv.visitVarInsn(Opcodes.ALOAD, 0);
    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, mug.qn.toplevel, "<init>", jvm.sig.call(jvm.sig.array(jvm.sig.obj(jvm.qn.string)), jvm.sig["void"]));
    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, compiler.qn.script, "load", jvm.sig.call(jvm.sig.obj(mug.qn.toplevel), jvm.sig.obj(mug.qn.object)));
    mv.visitInsn(Opcodes.POP);
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, mug.qn.timers, "awaitTaskPool", jvm.sig.call(jvm.sig["void"]));
    mv.visitInsn(Opcodes.RETURN);
    mv.visitMaxs(1, 1);
    mv.visitEnd;
    cw.visitEnd();
    log.verbose('...Compiled script ' + qn);
    return [qn + '.class', cw.toByteArray()];
  };
  ScriptContextCompiler.prototype.compileInit = function(cw) {
    var mv, sig;
    sig = jvm.sig.call(jvm.sig["void"]);
    mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", sig, null, null);
    mv.visitCode();
    mv.visitVarInsn(Opcodes.ALOAD, 0);
    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, mug.qn.module, "<init>", sig);
    mv.visitInsn(Opcodes.RETURN);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  };
  ScriptContextCompiler.prototype.compileFields = function(cw) {
    cw.visitField(0, compiler.ident.env, jvm.sig.obj(mug.qn.toplevel), null, null).visitEnd();
  };
  ScriptContextCompiler.prototype.compileMethods = function(cw) {
    var asm, mv, name, reg, stat, _i, _j, _k, _len, _len2, _len3, _ref4, _ref5, _ref6;
    mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "load", jvm.sig.call(jvm.sig.obj(mug.qn.toplevel), jvm.sig.obj(mug.qn.object)), null, compiler.stringArray(jvm.qn.exception));
    asm = new MethodCompiler(this.ctx);
    mv.visitTypeInsn(Opcodes.NEW, compiler.qn.scriptscope());
    mv.visitInsn(Opcodes.DUP);
    mv.visitVarInsn(Opcodes.ALOAD, 1);
    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, compiler.qn.scriptscope(), "<init>", jvm.sig.call(jvm.sig.obj(mug.qn.toplevel), jvm.sig["void"]));
    mv.visitVarInsn(Opcodes.ASTORE, compiler.regs.scope);
    mv.visitTypeInsn(Opcodes.NEW, mug.qn.object);
    mv.visitInsn(Opcodes.DUP);
    mv.visitVarInsn(Opcodes.ALOAD, 1);
    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, mug.qn.object, "<init>", jvm.sig.call(jvm.sig.obj(mug.qn.toplevel), jvm.sig["void"]));
    mv.visitInsn(Opcodes.DUP);
    mv.visitVarInsn(Opcodes.ALOAD, compiler.regs.scope);
    mv.visitInsn(Opcodes.SWAP);
    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, compiler.qn.scriptscope(), "set_exports", jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig["void"]));
    mv.visitVarInsn(Opcodes.ALOAD, 0);
    mv.visitVarInsn(Opcodes.ALOAD, 1);
    mv.visitFieldInsn(Opcodes.PUTFIELD, compiler.qn.context(this.ctx.id), compiler.ident.env, jvm.sig.obj(mug.qn.toplevel));
    mv.visitVarInsn(Opcodes.ASTORE, compiler.regs.thisObj);
    _ref4 = asm.unenclosedVars;
    for (_i = 0, _len = _ref4.length; _i < _len; _i++) {
      name = _ref4[_i];
      if ((reg = asm.getRegister(name)) !== -1) {
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitVarInsn(Opcodes.ASTORE, reg);
      }
    }
    _ref5 = this.ctx.node.stats;
    for (_j = 0, _len2 = _ref5.length; _j < _len2; _j++) {
      stat = _ref5[_j];
      if (stat.type === 'defn-stat') {
        asm.compileNode(mv, stat);
      }
    }
    _ref6 = this.ctx.node.stats;
    for (_k = 0, _len3 = _ref6.length; _k < _len3; _k++) {
      stat = _ref6[_k];
      if (stat.type !== 'defn-stat') {
        asm.compileNode(mv, stat);
      }
    }
    mv.visitVarInsn(Opcodes.ALOAD, compiler.regs.thisObj);
    mv.visitInsn(Opcodes.ARETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd;
  };
  return ScriptContextCompiler;
})();
ClosureContextCompiler = (function() {
  __extends(ClosureContextCompiler, ContextCompiler);
  function ClosureContextCompiler() {
    ClosureContextCompiler.__super__.constructor.apply(this, arguments);
  }
  ClosureContextCompiler.prototype.compileClass = function() {
    var cw, qn;
    cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    qn = compiler.qn.context(this.ctx.id);
    cw.visit(Opcodes.V1_6, Opcodes.ACC_SUPER + Opcodes.ACC_PUBLIC, qn, null, mug.qn["function"], null);
    cw.visitSource(this.path, null);
    this.compileInit(cw);
    this.compileMethods(cw);
    this.compileFields(cw);
    cw.visitEnd();
    log.verbose('...Compiled closure ' + qn);
    return [qn + '.class', cw.toByteArray()];
  };
  ClosureContextCompiler.prototype.compileInit = function(cw) {
    var i, mv, parent, qn, sig, _len, _ref4, _ref5;
    sig = (_ref4 = jvm.sig).call.apply(_ref4, [jvm.sig.obj(mug.qn.toplevel)].concat(__slice.call((function() {
      var _i, _len, _ref4, _results;
      _ref4 = this.ctx.getParents();
      _results = [];
      for (_i = 0, _len = _ref4.length; _i < _len; _i++) {
        parent = _ref4[_i];
        _results.push(jvm.sig.obj(compiler.qn.scope(parent.id)));
      }
      return _results;
    }).call(this)), [jvm.sig["void"]]));
    mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", sig, null, null);
    qn = compiler.qn.context(this.ctx.id);
    mv.visitCode();
    mv.visitVarInsn(Opcodes.ALOAD, 0);
    mv.visitVarInsn(Opcodes.ALOAD, 1);
    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, mug.qn["function"], "<init>", jvm.sig.call(jvm.sig.obj(mug.qn.toplevel), jvm.sig["void"]));
    mv.visitVarInsn(Opcodes.ALOAD, 0);
    mv.visitVarInsn(Opcodes.ALOAD, 1);
    mv.visitFieldInsn(Opcodes.PUTFIELD, qn, compiler.ident.env, jvm.sig.obj(mug.qn.toplevel));
    _ref5 = this.ctx.getParents();
    for (i = 0, _len = _ref5.length; i < _len; i++) {
      parent = _ref5[i];
      mv.visitVarInsn(Opcodes.ALOAD, 0);
      mv.visitVarInsn(Opcodes.ALOAD, i + 2);
      mv.visitFieldInsn(Opcodes.PUTFIELD, qn, compiler.ident.scope(parent.id), jvm.sig.obj(compiler.qn.scope(parent.id)));
    }
    mv.visitInsn(Opcodes.RETURN);
    mv.visitMaxs(1, 1);
    mv.visitEnd;
  };
  ClosureContextCompiler.prototype.compileFields = function(cw) {
    var parent, _i, _len, _ref4;
    cw.visitField(0, compiler.ident.env, jvm.sig.obj(mug.qn.toplevel), null, null).visitEnd();
    _ref4 = this.ctx.getParents();
    for (_i = 0, _len = _ref4.length; _i < _len; _i++) {
      parent = _ref4[_i];
      cw.visitField(0, compiler.ident.scope(parent.id), jvm.sig.obj(compiler.qn.scope(parent.id)), null, null).visitEnd();
    }
  };
  ClosureContextCompiler.prototype.compileMethods = function(cw) {
    var asm, i, mv, name, reg, stat, _i, _j, _k, _len, _len2, _len3, _len4, _ref4, _ref5, _ref6, _ref7, _ref8, _ref9;
    mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "invoke", compiler.sig.invoke(), null, compiler.stringArray(jvm.qn.exception));
    mv.visitCode();
    asm = new MethodCompiler(this.ctx);
    mv.visitTypeInsn(Opcodes.NEW, compiler.qn.scope(this.ctx.id));
    mv.visitInsn(Opcodes.DUP);
    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, compiler.qn.scope(this.ctx.id), "<init>", jvm.sig.call(jvm.sig["void"]));
    mv.visitVarInsn(Opcodes.ASTORE, compiler.regs.scope);
    if (this.ctx.usesArguments) {
      asm.compileLoadEnvironment(mv);
      mv.visitVarInsn(Opcodes.ILOAD, compiler.regs.argc);
      for (i = _ref4 = compiler.regs.offset, _ref5 = compiler.regs.scope; _ref4 <= _ref5 ? i < _ref5 : i > _ref5; _ref4 <= _ref5 ? i++ : i--) {
        mv.visitVarInsn(Opcodes.ALOAD, i);
      }
      mv.visitMethodInsn(Opcodes.INVOKESTATIC, mug.qn.utils, "arguments", jvm.sig.call(jvm.sig.integer, jvm.sig.obj(jvm.qn.object), jvm.sig.obj(jvm.qn.object), jvm.sig.obj(jvm.qn.object), jvm.sig.obj(jvm.qn.object), jvm.sig.obj(jvm.qn.object), jvm.sig.obj(jvm.qn.object), jvm.sig.obj(jvm.qn.object), jvm.sig.obj(jvm.qn.object), jvm.sig.array(jvm.sig.obj(jvm.qn.object)), jvm.sig.array(jvm.sig.obj(jvm.qn.object))));
      mv.visitMethodInsn(Opcodes.INVOKESTATIC, mug.qn.utils, "createArgumentsObject", jvm.sig.call(jvm.sig.obj(mug.qn.toplevel), jvm.sig.array(jvm.sig.obj(jvm.qn.object)), jvm.sig.obj(mug.qn.object)));
      if ((reg = asm.getRegister("arguments")) !== -1) {
        mv.visitVarInsn(Opcodes.ASTORE, reg);
      } else {
        mv.visitVarInsn(Opcodes.ALOAD, compiler.regs.scope);
        mv.visitInsn(Opcodes.SWAP);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, compiler.qn.scope(this.ctx.id), "set_arguments", jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig["void"]));
      }
    }
    _ref6 = this.ctx.node.args;
    for (i = 0, _len = _ref6.length; i < _len; i++) {
      name = _ref6[i];
      if (asm.getRegister(name) === -1) {
        mv.visitVarInsn(Opcodes.ALOAD, compiler.regs.scope);
        mv.visitVarInsn(Opcodes.ALOAD, i + 3);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, compiler.qn.scope(this.ctx.id), "set_" + name, jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig["void"]));
      }
    }
    _ref7 = asm.unenclosedVars;
    for (_i = 0, _len2 = _ref7.length; _i < _len2; _i++) {
      name = _ref7[_i];
      if (__indexOf.call(this.ctx.node.args, name) < 0 && (name !== this.ctx.node.name && name !== 'arguments')) {
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitVarInsn(Opcodes.ASTORE, asm.getRegister(name));
      }
    }
    if (this.ctx.node.name && asm.getRegister(this.ctx.node.name) === -1) {
      mv.visitVarInsn(Opcodes.ALOAD, compiler.regs.scope);
      mv.visitVarInsn(Opcodes.ALOAD, 0);
      mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, compiler.qn.scope(this.ctx.id), "set_" + this.ctx.node.name, jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig["void"]));
    }
    _ref8 = this.ctx.node.stats;
    for (_j = 0, _len3 = _ref8.length; _j < _len3; _j++) {
      stat = _ref8[_j];
      if (stat.type === 'defn-stat') {
        asm.compileNode(mv, stat);
      }
    }
    _ref9 = this.ctx.node.stats;
    for (_k = 0, _len4 = _ref9.length; _k < _len4; _k++) {
      stat = _ref9[_k];
      if (stat.type !== 'defn-stat') {
        asm.compileNode(mv, stat);
      }
    }
    mv.visitInsn(Opcodes.ACONST_NULL);
    mv.visitInsn(Opcodes.ARETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  };
  return ClosureContextCompiler;
})();
Context = (function() {
  function Context(node) {
    this.node = node;
    this.usesArguments = this.node.type === 'script-context' ? false : jast.usesArguments(this.node);
    this.childNodes = jast.childContexts(this.node);
    this.localUndefinedRefs = jast.localUndefinedRefs(this.node);
    this.localVars = jast.localVars(this.node);
    this.children = [];
  }
  Context.prototype.pushChild = function(ctx) {
    this.children.push(ctx);
    ctx.parent = this;
  };
  Context.prototype.getLineage = function() {
    var cur, line;
    line = [];
    cur = this;
    while (cur) {
      line.push(cur);
      cur = cur.parent;
    }
    line.reverse();
    return line;
  };
  Context.prototype.getParents = function() {
    return this.getLineage().slice(0, -1);
  };
  Context.prototype.postAnalyze = function() {
    var child, getEnclosedVars, v, _i, _len, _ref4;
    getEnclosedVars = function(ctx, vars) {
      var child, ref, ret, v, _i, _len, _ref4, _ref5;
      ret = [];
      _ref4 = ctx.children;
      for (_i = 0, _len = _ref4.length; _i < _len; _i++) {
        child = _ref4[_i];
        ret.push((function() {
          var _j, _len2, _ref5, _results;
          _ref5 = child.localUndefinedRefs;
          _results = [];
          for (_j = 0, _len2 = _ref5.length; _j < _len2; _j++) {
            ref = _ref5[_j];
            if (__indexOf.call(vars, ref) >= 0) {
              _results.push(ref);
            }
          }
          return _results;
        })());
        ret.push(getEnclosedVars(child, (function() {
          var _j, _len2, _results;
          _results = [];
          for (_j = 0, _len2 = vars.length; _j < _len2; _j++) {
            v = vars[_j];
            if (__indexOf.call(child.localVars, v) < 0) {
              _results.push(v);
            }
          }
          return _results;
        })()));
      }
      return (_ref5 = []).concat.apply(_ref5, ret);
    };
    this.enclosedVars = getEnclosedVars(this, this.localVars);
    this.unenclosedVars = (function() {
      var _i, _len, _ref4, _results;
      _ref4 = this.localVars;
      _results = [];
      for (_i = 0, _len = _ref4.length; _i < _len; _i++) {
        v = _ref4[_i];
        if (__indexOf.call(this.enclosedVars, v) < 0) {
          _results.push(v);
        }
      }
      return _results;
    }).call(this);
    _ref4 = this.children;
    for (_i = 0, _len = _ref4.length; _i < _len; _i++) {
      child = _ref4[_i];
      child.postAnalyze();
    }
  };
  return Context;
})();
CodeInput = (function() {
  function CodeInput(code) {
    this.code = code;
    this.ast = jast.parse(code);
    this.numbers = jast.numbers(this.ast);
    this.regexps = jast.regexps(this.ast);
    this.contexts = [];
    this.rootContext = this.buildContextTree();
    this.rootContext.postAnalyze();
  }
  CodeInput.prototype.buildContextTree = function(node, parent) {
    var child, ctx, _i, _len, _ref4;
    if (node == null) {
      node = this.ast;
    }
    if (parent == null) {
      parent = null;
    }
    ctx = new Context(node, parent);
    ctx.id = this.contexts.push(ctx) - 1;
    _ref4 = ctx.childNodes;
    for (_i = 0, _len = _ref4.length; _i < _len; _i++) {
      child = _ref4[_i];
      ctx.pushChild(this.buildContextTree(child, node));
    }
    return ctx;
  };
  CodeInput.prototype.getContext = function(node) {
    var ctx, _i, _len, _ref4;
    _ref4 = this.contexts;
    for (_i = 0, _len = _ref4.length; _i < _len; _i++) {
      ctx = _ref4[_i];
      if (ctx.node === node) {
        return ctx;
      }
    }
    throw new Error('Unmatched context.');
  };
  return CodeInput;
})();
compiler.readFile = function(filepath) {
  var File, Scanner;
  Scanner = java.util.Scanner;
  File = java.io.File;
  try {
    return new Scanner(new File(filepath)).useDelimiter("\\Z").next();
  } catch (e) {
    return "";
  }
};
compiler.writeClasses = function(outpath, files) {
  var File, FileOutputStream, bytes, file, path, stream, _i, _len, _ref4, _ref5, _results;
  _ref4 = java.io, File = _ref4.File, FileOutputStream = _ref4.FileOutputStream;
  _results = [];
  for (_i = 0, _len = files.length; _i < _len; _i++) {
    _ref5 = files[_i], path = _ref5[0], bytes = _ref5[1];
    _results.push((function() {
      try {
        file = new File(outpath + path);
        file.getParentFile().mkdirs();
        stream = new FileOutputStream(file);
        return stream.write(bytes);
      } catch (e) {
        return console.log('Error writing out file:', e);
      } finally {
        stream.close();
      }
    })());
  }
  return _results;
};
compiler.writeJar = function(outpath, jarpath, files) {
  var Attributes, File, FileOutputStream, JarEntry, JarOutputStream, Manifest, attributes, bytes, entry, fstream, manifest, path, stream, _i, _len, _ref4, _ref5, _ref6;
  _ref4 = java.io, File = _ref4.File, FileOutputStream = _ref4.FileOutputStream;
  _ref5 = java.util.jar, Manifest = _ref5.Manifest, Attributes = _ref5.Attributes, JarOutputStream = _ref5.JarOutputStream, JarEntry = _ref5.JarEntry;
  manifest = new Manifest();
  attributes = manifest.getMainAttributes();
  attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
  new File(outpath).mkdirs();
  fstream = new FileOutputStream(outpath + jarpath);
  stream = new JarOutputStream(fstream, manifest);
  for (_i = 0, _len = files.length; _i < _len; _i++) {
    _ref6 = files[_i], path = _ref6[0], bytes = _ref6[1];
    entry = new JarEntry(path);
    stream.putNextEntry(entry);
    stream.write(bytes);
  }
  stream.flush();
  return stream.close();
};
input = null;
compiler.cli = function(args) {
  var cmp, code, ctx, curns, end, filepath, files, ns, opts, scriptname, _base, _i, _j, _len, _len2, _ref4, _ref5;
  if (args.length === 0) {
    throw new Error('Please specify a filename.');
  }
  opts = {
    output: "out/",
    jar: null,
    files: {
      "": []
    }
  };
  curns = "";
  while (args.length) {
    switch (args[0]) {
      case '-h':
      case '--help':
        console.log("Usage:\n--output, -o <arg>    Output directory (default \"out\")\n--jar, -j <arg>       Create jar archive in output directory\n--namespace, -n <arg>  Namespace to use for subsequent modules (default \"\")");
        return;
      case '-o':
      case '--output':
        args.shift();
        opts.output = args.shift().replace(/\/?$/, '/');
        break;
      case '-j':
      case '--jar':
        args.shift();
        opts.jar = args.shift();
        break;
      case '-n':
      case '--namespace':
        args.shift();
        curns = args.shift().replace(/\./g, '/') + '/';
        (_base = opts.files)[curns] || (_base[curns] = []);
        break;
      default:
        opts.files[curns].push(args.shift());
    }
  }
  log.notify('Starting compilation.');
  files = [];
  for (ns in opts.files) {
    _ref4 = opts.files[ns];
    for (_i = 0, _len = _ref4.length; _i < _len; _i++) {
      filepath = _ref4[_i];
      scriptname = filepath.replace(/\.js$/, '').replace(/^.*\//, '').replace(/\-/g, '_');
      compiler.qn.script = 'js/' + ns + scriptname;
      log.notify("Compiling file " + filepath + " as '" + compiler.qn.script + "'...");
      code = compiler.readFile(filepath) || "";
      input = new CodeInput(code);
      log.verbose('Finished parsing.');
      files.push((new ConstantsCompiler).compileClass());
      _ref5 = input.contexts;
      for (_j = 0, _len2 = _ref5.length; _j < _len2; _j++) {
        ctx = _ref5[_j];
        log.verbose('Compiling context #' + ctx.id);
        if (ctx.node.type === 'closure-context') {
          cmp = new ClosureContextCompiler(filepath, ctx);
          files.push(cmp.compileScopeClass());
          files.push(cmp.compileClass());
        } else if (ctx.node.type === 'script-context') {
          cmp = new ScriptContextCompiler(filepath, ctx);
          files.push(cmp.compileScopeClass());
          files.push(cmp.compileClass());
        }
      }
    }
  }
  if (opts.jar !== null) {
    log.notify("Writing out classes to archive '" + opts.output + opts.jar + "'...");
    compiler.writeJar(opts.output, opts.jar, files);
  } else {
    log.notify("Writing out classes to folder '" + opts.output + "'...");
    compiler.writeClasses(opts.output, files);
  }
  end = new Date();
  return log.notify('Compiled in', (end - start) / 1000, 's');
};
compiler.cli([].slice.call(arguments));