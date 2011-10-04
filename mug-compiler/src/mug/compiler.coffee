start = new Date()

log =
	verbose: (args...) -> #console.log(args...)
	notify: (args...) -> console.log(args...)
	error: (args...) -> console.error(args...)

# modules
jast = require '../jast/jast'

#######################################################################
# ringo vs mug
#######################################################################

if this.addToClasspath
	# rhino
	this.addToClasspath this.module.resolve("../asm-3.3.jar")
	arguments = this.system.args.slice(1)
	{Opcodes, ClassWriter, FieldVisitor, MethodVisitor, AnnotationVisitor, Label} = this.Packages.org.objectweb.asm

else
	# Mug
	java = require('java')
	{Opcodes, ClassWriter, FieldVisitor, MethodVisitor, AnnotationVisitor, Label} = java.import('org.objectweb.asm')

#######################################################################
# names and stuff
#######################################################################

jvm = {}
jvm.qn =
	object: "java/lang/Object"
	boolean: "java/lang/Boolean"
	number: "java/lang/Number"
	double: "java/lang/Double"
	string: "java/lang/String"
	exception: "java/lang/Exception"
	pattern: "java/util/regex/Pattern"
jvm.sig =
	void: "V"
	double: "D"
	integer: "I"
	boolean: "Z"
	obj: (x) -> "L" + x + ";"
	call: (args..., ret) -> "(" + args.join('') + ")" + (ret or "V")
	array: (x) -> "[" + x

mug = {}
mug.qn = {}
mug.qn.pkg = "mug/runtime/"
mug.qn.null = mug.qn.pkg + "JSNull"
mug.qn.boolean = mug.qn.pkg + "JSBoolean"
mug.qn.string = mug.qn.pkg + "JSString"
mug.qn.number = mug.qn.pkg + "JSNumber"
mug.qn.utils = mug.qn.pkg + "JSUtils"
mug.qn.function = mug.qn.pkg + "JSFunction"
mug.qn.object = mug.qn.pkg + "JSObject"
mug.qn.array = mug.qn.pkg + "JSArray"
mug.qn.regexp = mug.qn.pkg + "JSRegExp"
mug.qn.module = mug.qn.pkg + "JSModule"
mug.qn.exception = mug.qn.pkg + "JSException"
mug.qn.valueException = mug.qn.pkg + "JSValueException"
mug.qn.timers = mug.qn.pkg + "JSConcurrency"
mug.qn.toplevel = mug.qn.pkg + "JSEnvironment"

compiler = {}

compiler.globals = [
  "exports", "require", "print", "console", "arguments"
    "parseInt", "parseFloat", "isNaN", "isFinite"
    "Math", "JSON"
    "Object", "Array", "Number", "String", "Boolean", "Function", "Date"
    "Error", "SyntaxError", "RegExp"
    "setTimeout", "setInterval", "clearTimeout", "clearInterval"
   ]

compiler.closureArgCount = 8

compiler.qn = {}
compiler.qn.script = -> "js/script"
compiler.qn.pkg = -> compiler.qn.script + "$"
compiler.qn.constants = -> compiler.qn.pkg() + "constants"
compiler.qn.scriptscope = -> compiler.qn.pkg() + "scope$script"
compiler.qn.scope = (i) ->
	if i == 0
		compiler.qn.scriptscope()
	else
		compiler.qn.pkg() + "scope$" + i
compiler.qn.context = (i) ->
	if i == 0
		compiler.qn.script
	else
		compiler.qn.pkg() + "closure$" + i

compiler.ident =
	num: (n) -> "NUM_" + n
	str: (n) -> "STR_" + n
	regex: (n) -> "REGEX_" + n
	scope: (n) -> "SCOPE_" + n
	env: 'ENV'

compiler.sig = {}
compiler.sig.instantiate = jvm.sig.call(
	jvm.sig.integer # Argument count.
	(jvm.sig.obj(jvm.qn.object) for i in [0...compiler.closureArgCount])... # Args
	jvm.sig.array(jvm.sig.obj(jvm.qn.object)) # Arg overflow array
	jvm.sig.obj(jvm.qn.object) # Return object.
	)
compiler.sig.invoke = (c = compiler.closureArgCount) ->
	jvm.sig.call(
		jvm.sig.obj(jvm.qn.object) # "this" object.
		jvm.sig.integer # Argument Count
		(jvm.sig.obj(jvm.qn.object) for i in [0...c])... # Args
		jvm.sig.array(jvm.sig.obj(jvm.qn.object)) # Arg overflow array
		jvm.sig.obj(jvm.qn.object) # Return object.
	)

compiler.regs = {}
compiler.regs.thisObj = 1
compiler.regs.argc = 2
compiler.regs.offset = 3
compiler.regs.scope = compiler.regs.offset + compiler.closureArgCount + 1
compiler.regs.ref = compiler.regs.scope + 1 

#######################################################################
# utility
#######################################################################

compiler.stringArray = (strs...) ->
	arr = java.lang.reflect.Array.newInstance(java.lang.String, strs.length)
	(arr[i] = strs[i]) for i in [0...strs.length]
	return arr

#######################################################################
# constants
#######################################################################

class ConstantsCompiler

	compileClass: ->
		nums = input.numbers
		regexps = input.regexps

		# Initialize class.
		cw = new ClassWriter(ClassWriter.COMPUTE_MAXS)
		cw.visit Opcodes.V1_6,
			Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER,
			compiler.qn.constants(),
			null,
			jvm.qn.object,
			null

		# Write field definitions.
		for num, i in nums
			cw.visitField(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, compiler.ident.num(i), jvm.sig.obj(jvm.qn.double), null, null).visitEnd()
		for regexp, i in regexps
			cw.visitField(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, compiler.ident.regex(i), jvm.sig.obj(jvm.qn.pattern), null, null).visitEnd()

		# Static initializer.
		mv = cw.visitMethod Opcodes.ACC_STATIC, "<clinit>",
			jvm.sig.call(jvm.sig.void), null, null
		mv.visitCode()

		# Write numbers.
		for num, i in nums
			mv.visitTypeInsn Opcodes.NEW, jvm.qn.double
			mv.visitInsn Opcodes.DUP
			# Take advantage of AST representation of ints to see if we should decode
			# long numbers like 0xFF00FF00 into (negative) ints or raw doubles
			
			# translate to negative int
			num = new java.lang.Long(num).intValue() if num > 0x7fffffff
			mv.visitLdcInsn new java.lang.Double(num)
			mv.visitMethodInsn Opcodes.INVOKESPECIAL, jvm.qn.double, "<init>",
				jvm.sig.call(jvm.sig.double, jvm.sig.void)
			mv.visitFieldInsn Opcodes.PUTSTATIC, compiler.qn.constants(),
				compiler.ident.num(i), jvm.sig.obj(jvm.qn.double)
		
		# Write regexps.
		for [expr, flags], i in regexps
			mv.visitLdcInsn expr
			mv.visitLdcInsn flags
			mv.visitMethodInsn Opcodes.INVOKESTATIC, mug.qn.utils, "compilePattern", jvm.sig.call(jvm.sig.obj(jvm.qn.string), jvm.sig.obj(jvm.qn.string), jvm.sig.obj(jvm.qn.pattern))
			mv.visitFieldInsn Opcodes.PUTSTATIC, compiler.qn.constants(), compiler.ident.regex(i), jvm.sig.obj(jvm.qn.pattern)
			
		mv.visitInsn Opcodes.RETURN
		mv.visitMaxs 0, 0
		mv.visitEnd

		# End writing class.
		cw.visitEnd()
		log.verbose '...Compiled constants struct ' + compiler.qn.constants()
		return [compiler.qn.constants() + '.class', cw.toByteArray()]

#######################################################################
# code compilation
#######################################################################

class MethodCompiler
	constructor: (@ctx) ->
		@lineNumber = -1
		@labels = {}

		@vars = ctx.localVars
		@enclosedVars = ctx.enclosedVars
		@unenclosedVars = ctx.unenclosedVars

		log.verbose 'Context unenclosed vars:', @unenclosedVars

	getNodeCompileType: (n) ->
		return switch n.type
			# Literals.
			when "null-literal" then "null"
			when "boolean-literal" then "boolean"
			when "num-literal" then "number"
			when "undef-literal" then "Object"
			when "str-literal" then "String"
			when "regexp-literal" then "JSRegExp"
			when "array-literal" then "JSArray"
			when "obj-literal" then "JSObject"
			when "func-literal" then "JSFunction"

			# Operations.
			when "add-op-expr" #TODO we should know this by output types
				"Object"
			when "sub-op-expr", "div-op-expr", "mul-op-expr", "mod-op-expr", "lsh-op-expr", "neg-op-expr", "bit-and-op-expr", "bit-or-op-expr", "bit-xor-op-expr", "bit-not-op-expr"
				"number"
			when "eq-op-expr", "neq-op-expr", "not-op-expr", "eqs-op-expr", "neqs-op-expr", "lt-op-expr", "lte-op-expr", "gt-op-expr", "gte-op-expr", "instanceof-op-expr", "in-op-expr"
				"boolean"
			when "or-op-expr", "and-op-expr", "if-expr"
				"Object"
			when "typeof-op-expr" then "String"
			when "void-op-expr" then "Object"
			when "seq-op-expr" then @getNodeCompileType(n.right)

			# Expressions.
			when "this-expr", "scope-ref-expr", "static-ref-expr", "dyn-ref-expr", "static-method-call-expr", "call-expr", "new-expr"
				"Object"
			when "scope-assign-expr", "static-assign-expr", "dyn-assign-expr", "if-expr"
				"Object" # @getNodeCompileType(n.expr)
			
			when "scope-inc-expr", "static-inc-expr", "dyn-inc-expr"
				"number"
			
			when "scope-delete-expr", "static-delete-expr", "dyn-delete-expr"
				"boolean"

			else throw new Error "Couldn't match compile type " + n.type

	# Assembly utilities.

	pushLabel: (label, cont, brk) ->
		if label then @pushLabel("", cont, brk)
		else label = ""

		@labels[label] = @labels[label] or []
		@labels[label].push {cont: cont, brk: brk}
		return

	popLabel: (label) ->
		if label then @popLabel("")
		else label = ""
		
		return @labels[label]?.pop()

	getLabel: (label) ->
		label = label or ""
		return @labels[label]?[@labels[label].length-1]

	compilePop: (mv, node) ->
		if @getNodeCompileType(node) == 'number' then mv.visitInsn Opcodes.POP2
		else mv.visitInsn Opcodes.POP
		return

	compileLoadInt: (mv, i) ->
		if i >= -128 and i <= 127
			mv.visitIntInsn Opcodes.BIPUSH, i
		else
			mv.visitLdcInsn new java.lang.Integer(i)
		return

	# Get reference to the environment scope.

	compileLoadEnvironment: (mv) ->
		mv.visitVarInsn Opcodes.ALOAD, 0
		mv.visitFieldInsn Opcodes.GETFIELD, compiler.qn.context(@ctx.id), compiler.ident.env, jvm.sig.obj(mug.qn.toplevel)
		return

	# Search scopes for reference. Returns qn of scope object.

	compileScopeRef: (mv, name, ln) ->
		log.verbose "Seeking var #{name} in current scope (#{@vars})"

		if name in @vars or (@ctx.id == 0 and name in compiler.globals)
			# Variable defined in current scope.
			log.verbose ' . Found in current scope.'
			mv.visitVarInsn Opcodes.ALOAD, compiler.regs.scope
			return compiler.qn.scope(@ctx.id)
		
		# Search parents
		for parent in @ctx.getParents().reverse()
			if name in parent.localVars or (parent.id == 0 and name in compiler.globals)
				# Variable found in ancestor scope.
				log.verbose " . Found in parent scope \##{parent.id}."
				mv.visitVarInsn Opcodes.ALOAD, 0
				mv.visitFieldInsn Opcodes.GETFIELD, compiler.qn.context(@ctx.id), "SCOPE_" + parent.id, jvm.sig.obj(compiler.qn.scope(parent.id))
				return compiler.qn.scope(parent.id)
			
		# Not found in parents
		#if name in compiler.globals
		#TODO global object or something
		
		# Not found at all.
		throw new Error "Identifier not found in any scope: #{name} (line #{ln})"

	# ASM boxing/unboxing.

	compileAsObject: (mv, expr, dupPrimitive = false) ->
		# Number literal optimization
		if expr.type == 'num-literal'
			nums = input.numbers
			if nums.indexOf(expr.value) != -1 #TODO remove all code-gen'd numbers
				log.verbose ' . Num literal optimization.'
				mv.visitFieldInsn Opcodes.GETSTATIC, compiler.qn.constants(), compiler.ident.num(nums.indexOf expr.value), jvm.sig.obj(jvm.qn.double)
				return

		switch @getNodeCompileType(expr)
			when "boolean"
				mv.visitTypeInsn Opcodes.NEW, jvm.qn.boolean
				mv.visitInsn Opcodes.DUP
			when "number"
				mv.visitTypeInsn Opcodes.NEW, jvm.qn.double
				mv.visitInsn Opcodes.DUP
		
		@compileNode mv, expr

		switch @getNodeCompileType(expr)
			when 'boolean'
				if dupPrimitive then mv.visitInsn Opcodes.DUP_X2
				mv.visitMethodInsn Opcodes.INVOKESPECIAL, jvm.qn.boolean, "<init>", jvm.sig.call(jvm.sig.boolean, jvm.sig.void)
			when 'number'
				if dupPrimitive then mv.visitInsn Opcodes.DUP_X2
				mv.visitMethodInsn Opcodes.INVOKESPECIAL, jvm.qn.double, "<init>", jvm.sig.call(jvm.sig.double, jvm.sig.void)
			else
				if dupPrimitive then mv.visitInsn Opcodes.DUP_X2
		
		return

	# as-(primitive) conversion

	compileJSObject: (mv, expr) ->
		switch @getNodeCompileType(expr)
			when 'boolean'
				mv.visitTypeInsn Opcodes.NEW, mug.qn.boolean
				mv.visitInsn Opcodes.DUP

				@compileLoadEnvironment mv
				@compileNode mv, expr

				mv.visitMethodInsn Opcodes.INVOKESPECIAL, mug.qn.boolean, "<init>", jvm.sig.call(jvm.sig.obj(mug.qn.toplevel), jvm.sig.boolean, jvm.sig.void)
			when 'number'
				mv.visitTypeInsn Opcodes.NEW, mug.qn.number
				mv.visitInsn Opcodes.DUP

				@compileLoadEnvironment mv
				@compileNode mv, expr

				mv.visitMethodInsn Opcodes.INVOKESPECIAL, mug.qn.number, "<init>", jvm.sig.call(jvm.sig.obj(mug.qn.toplevel), jvm.sig.double, jvm.sig.void)
			when 'String'
				mv.visitTypeInsn Opcodes.NEW, mug.qn.string
				mv.visitInsn Opcodes.DUP

				@compileLoadEnvironment mv
				@compileNode mv, expr

				mv.visitMethodInsn Opcodes.INVOKESPECIAL, mug.qn.string, "<init>", jvm.sig.call(jvm.sig.obj(mug.qn.toplevel), jvm.sig.obj(jvm.qn.string), jvm.sig.void)
			else
				@compileLoadEnvironment mv
				@compileNode mv, expr

				mv.visitMethodInsn Opcodes.INVOKESTATIC, mug.qn.utils, "asJSObject", jvm.sig.call(jvm.sig.obj(mug.qn.toplevel), jvm.sig.obj(jvm.qn.object), jvm.sig.obj(mug.qn.object))
		return

	compileAsNumber: (mv, type) ->
		switch type
			#TODO when "boolean" 
			when "number"
			else
				mv.visitMethodInsn Opcodes.INVOKESTATIC, mug.qn.utils, "asNumber", jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig.double)
		return

	compileAsBoolean: (mv, type) ->
		switch type
			#TODO when "number"
			#TODO when "String" 
			when "boolean"
			else
				mv.visitMethodInsn Opcodes.INVOKESTATIC, mug.qn.utils, "asBoolean", jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig.boolean)
		return

	# Populate stack with arguments for invoke.

	compileInvokeArgs: (mv, args) ->
		# Argument count.
		mv.visitLdcInsn new java.lang.Integer(args.length)
		
		# Automatic arguments.
		for i in [0...compiler.closureArgCount]
			if args[i]
				@compileAsObject mv, args[i]
			else
				mv.visitInsn Opcodes.ACONST_NULL
				#mv.visitInsn Opcodes.DUP
		# Overflow arguments.
		if args.length > compiler.closureArgCount
			@compileLoadInt mv, (args.length - compiler.closureArgCount)
			mv.visitTypeInsn Opcodes.ANEWARRAY, jvm.qn.object
			for i in [compiler.closureArgCount...args.length]
				mv.visitInsn Opcodes.DUP
				@compileLoadInt mv, i - compiler.closureArgCount
				@compileAsObject mv, args[i]
				mv.visitInsn Opcodes.AASTORE
		else
			mv.visitInsn Opcodes.ACONST_NULL
		return

	# Comparison operations.

	compileComparison: (mv, op, left, right) ->
		falseCase = new Label(); trueCase = new Label()

		@compileNode mv, left
		@compileAsNumber mv, @getNodeCompileType(left)
		@compileNode mv, right
		@compileAsNumber mv, @getNodeCompileType(right)

		mv.visitInsn Opcodes.DCMPG
		mv.visitJumpInsn op, trueCase
		mv.visitInsn Opcodes.ICONST_0
		mv.visitJumpInsn Opcodes.GOTO, falseCase
		mv.visitLabel trueCase
		mv.visitInsn Opcodes.ICONST_1
		mv.visitLabel falseCase
		return
	
	# Assign variables to registers when possible.
	# Leave arguments in registers when possible.

	getRegister: (name) ->
		return -1 if name not in @unenclosedVars

		# closure arguments
		if @ctx.id != 0
			if name in @ctx.node.args
				return compiler.regs.offset + @ctx.node.args.indexOf(name)
			if name == @ctx.node.name
				return 0
		# builtin globals
		#TODO these can be scoped too, actually do this work
		if @ctx.id == 0 and name in compiler.globals
			return -1
		# scope variable
		return compiler.regs.ref + @unenclosedVars.indexOf(name)

	# The big compile matrix.

	compileNode: (mv, n) ->
		# Compile line number.
		if @lineNumber != n.ln
			label = new Label()
			mv.visitLabel label
			mv.visitLineNumber n.ln, label
			@lineNumber = n.ln

		# compile something baby
		#console.log 'Compiling node ' + JSON.stringify(n.type)

		switch n.type
			
			# Literals.

			when 'null-literal'
				@compileLoadEnvironment mv
				mv.visitMethodInsn Opcodes.INVOKEVIRTUAL, mug.qn.toplevel, "getNullObject", jvm.sig.call(jvm.sig.obj(mug.qn.null))
			
			when 'boolean-literal'
				if n.value then mv.visitInsn Opcodes.ICONST_1
				else mv.visitInsn Opcodes.ICONST_0
			
			when 'undef-literal'
				mv.visitInsn Opcodes.ACONST_NULL
			
			when 'num-literal'
				# Take advantage of AST representation of ints to see if we should decode
				# long numbers like 0xFF00FF00 into (negative) ints or raw doubles
				# translate to negative int
				num = n.value
				num = new java.lang.Long(num).intValue() if num > 0x7fffffff
				mv.visitLdcInsn new java.lang.Double(num)
				# TODO that
			
			when 'str-literal'
				mv.visitLdcInsn n.value
			
			when 'regexp-literal'
				# Get regexp index.
				for regex, i in input.regexps
					break if regex[0] == n.expr and regex[1] == n.flags

				mv.visitTypeInsn Opcodes.NEW, mug.qn.regexp
				mv.visitInsn Opcodes.DUP
				@compileLoadEnvironment mv
				mv.visitFieldInsn Opcodes.GETSTATIC, compiler.qn.constants(), compiler.ident.regex(i), jvm.sig.obj(jvm.qn.pattern)
				mv.visitLdcInsn n.flags
				mv.visitMethodInsn Opcodes.INVOKESTATIC, mug.qn.utils, "isPatternGlobal", jvm.sig.call(jvm.sig.obj(jvm.qn.string), jvm.sig.boolean)
				mv.visitMethodInsn Opcodes.INVOKESPECIAL, mug.qn.regexp, "<init>", jvm.sig.call(jvm.sig.obj(mug.qn.toplevel), jvm.sig.obj(jvm.qn.pattern), jvm.sig.boolean, jvm.sig.void)
			
			when 'array-literal'
				mv.visitTypeInsn Opcodes.NEW, mug.qn.array
				mv.visitInsn Opcodes.DUP
				@compileLoadEnvironment mv
				@compileLoadInt mv, n.exprs.length
				mv.visitMethodInsn Opcodes.INVOKESPECIAL, mug.qn.array, "<init>", jvm.sig.call(jvm.sig.obj(mug.qn.toplevel), jvm.sig.integer, jvm.sig.void)
				mv.visitInsn Opcodes.DUP
				@compileLoadInt mv, n.exprs.length
				mv.visitTypeInsn Opcodes.ANEWARRAY, jvm.qn.object
				for i in [0...n.exprs.length]
					mv.visitInsn Opcodes.DUP
					@compileLoadInt mv, i
					@compileAsObject mv, n.exprs[i]
					mv.visitInsn Opcodes.AASTORE
				mv.visitMethodInsn Opcodes.INVOKEVIRTUAL, mug.qn.array, "load", jvm.sig.call(jvm.sig.array(jvm.sig.obj(jvm.qn.object)), jvm.sig.void)
			
			when 'obj-literal'
				mv.visitTypeInsn Opcodes.NEW, mug.qn.object
				mv.visitInsn Opcodes.DUP
				# Object prototype.
				@compileLoadEnvironment mv
				mv.visitMethodInsn Opcodes.INVOKESPECIAL, mug.qn.object, "<init>", jvm.sig.call(jvm.sig.obj(mug.qn.toplevel), jvm.sig.void)
				for {value, expr} in n.props
					mv.visitInsn Opcodes.DUP
					mv.visitLdcInsn value
					@compileAsObject mv, expr
					mv.visitMethodInsn Opcodes.INVOKEVIRTUAL, mug.qn.object, "set", jvm.sig.call(jvm.sig.obj(jvm.qn.string), jvm.sig.obj(jvm.qn.object), jvm.sig.void)
			
			when 'func-literal'
				# Find child index.
				child = input.getContext(n.closure)
				qn = compiler.qn.context(child.id)
				# Create context instance.
				mv.visitTypeInsn Opcodes.NEW, qn
				mv.visitInsn Opcodes.DUP
				# Function prototype
				@compileLoadEnvironment mv
				# Load scopes.
				for parent in @ctx.getParents()
					mv.visitVarInsn Opcodes.ALOAD, 0
					mv.visitFieldInsn Opcodes.GETFIELD, compiler.qn.context(@ctx.id), "SCOPE_" + parent.id, jvm.sig.obj(compiler.qn.scope(parent.id))
				mv.visitVarInsn Opcodes.ALOAD, compiler.regs.scope
				# Invoke.
				mv.visitMethodInsn Opcodes.INVOKESPECIAL, qn, "<init>", 
					jvm.sig.call jvm.sig.obj(mug.qn.toplevel),
					(jvm.sig.obj(compiler.qn.scope(ctx.id)) for ctx in @ctx.getLineage())...,
					jvm.sig.void

			# Operations.

			when 'add-op-expr'
				#TODO optimize based on type
				@compileAsObject mv, n.left
				@compileAsObject mv, n.right
				mv.visitMethodInsn Opcodes.INVOKESTATIC, mug.qn.utils, "add", jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig.obj(jvm.qn.object), jvm.sig.obj(jvm.qn.object))
			
			when 'sub-op-expr'
				@compileNode mv, n.left
				@compileAsNumber mv, @getNodeCompileType(n.left)
				@compileNode mv, n.right
				@compileAsNumber mv, @getNodeCompileType(n.right)
				mv.visitInsn Opcodes.DSUB

			when 'div-op-expr'
				@compileNode mv, n.left
				@compileAsNumber mv, @getNodeCompileType(n.left)
				@compileNode mv, n.right
				@compileAsNumber mv, @getNodeCompileType(n.right)
				mv.visitInsn Opcodes.DDIV

			when 'mul-op-expr'
				@compileNode mv, n.left
				@compileAsNumber mv, @getNodeCompileType(n.left)
				@compileNode mv, n.right
				@compileAsNumber mv, @getNodeCompileType(n.right)
				mv.visitInsn Opcodes.DMUL

			when 'mod-op-expr'
				@compileNode mv, n.left
				@compileAsNumber mv, @getNodeCompileType(n.left)
				@compileNode mv, n.right
				@compileAsNumber mv, @getNodeCompileType(n.right)
				mv.visitInsn Opcodes.DREM

			when 'lsh-op-expr'
				@compileNode mv, n.left
				@compileAsNumber mv, @getNodeCompileType(n.left)
				mv.visitInsn Opcodes.D2I
				@compileNode mv, n.right
				@compileAsNumber mv, @getNodeCompileType(n.right)
				mv.visitInsn Opcodes.D2I
				mv.visitInsn Opcodes.ISHL
				mv.visitInsn Opcodes.I2D
			
			when 'eq-op-expr'
				@compileAsObject mv, n.left
				@compileAsObject mv, n.right
				mv.visitMethodInsn Opcodes.INVOKESTATIC, mug.qn.utils, "testEquality", jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig.obj(jvm.qn.object), jvm.sig.boolean)
			
			when 'neq-op-expr'
				@compileNode mv, {
					type: 'not-op-expr',
					ln: n.ln, expr: {
						type: 'eq-op-expr',
						ln: n.ln, left: n.left, right: n.right
					} }
			
			when 'not-op-expr'
				falseCase = new Label(); trueCase = new Label()
				@compileNode mv, n.expr
				@compileAsBoolean mv, @getNodeCompileType(n.expr)
				mv.visitJumpInsn Opcodes.IFNE, falseCase
				mv.visitInsn Opcodes.ICONST_1
				mv.visitJumpInsn Opcodes.GOTO, trueCase
				mv.visitLabel falseCase
				mv.visitInsn Opcodes.ICONST_0
				mv.visitLabel trueCase

			when 'eqs-op-expr'
				@compileAsObject mv, n.left
				@compileAsObject mv, n.right
				mv.visitMethodInsn Opcodes.INVOKESTATIC, mug.qn.utils, "testStrictEquality", jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig.obj(jvm.qn.object), jvm.sig.boolean)
			
			when 'neqs-op-expr'
				@compileNode mv, {
					type: 'not-op-expr',
					ln: n.ln, expr: {
						type: 'eqs-op-expr',
						ln: n.ln, left: n.left, right: n.right
					} }
				
			when 'lt-op-expr'
				@compileComparison mv, Opcodes.IFLT, n.left, n.right

			when 'lte-op-expr'
				@compileComparison mv, Opcodes.IFLE, n.left, n.right

			when 'gt-op-expr'
				@compileComparison mv, Opcodes.IFGT, n.left, n.right

			when 'gte-op-expr'
				@compileComparison mv, Opcodes.IFGE, n.left, n.right

			when 'lt-op-expr'
				@compileComparison mv, Opcodes.IFLT, n.left, n.right

			when 'neg-op-expr'
				@compileNode mv, n.expr
				@compileAsNumber mv, @getNodeCompileType(n.expr)
				mv.visitInsn Opcodes.DNEG
			
			when 'or-op-expr'
				@compileAsObject mv, n.left
				mv.visitInsn Opcodes.DUP #TODO without this DUP, we could post-facto autobox
				@compileAsBoolean mv, 'Object'
				trueCase = new Label()
				mv.visitJumpInsn Opcodes.IFNE, trueCase
				mv.visitInsn Opcodes.POP
				@compileAsObject mv, n.right
				mv.visitLabel trueCase
			
			when 'and-op-expr'
				@compileAsObject mv, n.left
				mv.visitInsn Opcodes.DUP #TODO without this DUP, we could post-facto autobox
				@compileAsBoolean mv, 'Object'
				falseCase = new Label()
				mv.visitJumpInsn Opcodes.IFEQ, falseCase
				mv.visitInsn Opcodes.POP
				@compileAsObject mv, n.right
				mv.visitLabel falseCase
			
			when 'bit-and-op-expr'
				@compileNode mv, n.left
				@compileAsNumber mv, @getNodeCompileType(n.left)
				mv.visitInsn Opcodes.D2L
				@compileNode mv, n.right
				@compileAsNumber mv, @getNodeCompileType(n.right)
				mv.visitInsn Opcodes.D2L
				mv.visitInsn Opcodes.LAND
				mv.visitInsn Opcodes.L2D

			when 'bit-or-op-expr'
				@compileNode mv, n.left
				@compileAsNumber mv, @getNodeCompileType(n.left)
				mv.visitInsn Opcodes.D2L
				@compileNode mv, n.right
				@compileAsNumber mv, @getNodeCompileType(n.right)
				mv.visitInsn Opcodes.D2L
				mv.visitInsn Opcodes.LOR
				mv.visitInsn Opcodes.L2D

			when 'bit-xor-op-expr'
				@compileNode mv, n.left
				@compileAsNumber mv, @getNodeCompileType(n.left)
				mv.visitInsn Opcodes.D2L
				@compileNode mv, n.right
				@compileAsNumber mv, @getNodeCompileType(n.right)
				mv.visitInsn Opcodes.D2L
				mv.visitInsn Opcodes.LXOR
				mv.visitInsn Opcodes.L2D
			
			when 'instanceof-op-expr'
				switch @getNodeCompileType(n.left)
					when 'number', 'boolean'
						mv.visitInsn Opcodes.ICONST_0
					else
						switch @getNodeCompileType(n.right)
							when 'number', 'boolean', 'String'
								throw new Error "Objects cannot be instances of primitives (line #{@ln})"
							else
								@compileAsObject mv, n.right
								mv.visitTypeInsn Opcodes.CHECKCAST, mug.qn.object
								@compileAsObject mv, n.left
								mv.visitMethodInsn Opcodes.INVOKEVIRTUAL, mug.qn.object, "hasInstance", jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig.boolean)

			when 'in-op-expr'
				throw new Error("TODO: support 'in' operator (line #{@ln}")

			when 'typeof-op-expr'
				@compileNode mv, n.expr
				mv.visitMethodInsn Opcodes.INVOKESTATIC, mug.qn.utils, "typeof", jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig.obj(jvm.qn.string))
			
			when 'void-op-expr'
				@compileNode mv, n.expr
				@compilePop mv, n.expr
				mv.visitInsn Opcodes.ACONST_NULL # undefined
			
			when 'seq-op-expr'
				@compileNode mv, n.left
				@compilePop mv, n.left
				@compileNode mv, n.right

			# Expressions.

			when 'this-expr'
				mv.visitVarInsn Opcodes.ALOAD, 1
			
			when 'if-expr'
				@compileNode mv, n.expr
				@compileAsBoolean mv, @getNodeCompileType(n.expr)
				
				falseCase = new Label(); trueCase = new Label()
				mv.visitJumpInsn Opcodes.IFEQ, falseCase
				@compileAsObject mv, n.thenExpr
				mv.visitJumpInsn Opcodes.GOTO, trueCase
				mv.visitLabel falseCase
				@compileAsObject mv, n.elseExpr
				mv.visitLabel trueCase

			when 'scope-ref-expr'
				if (reg = @getRegister(n.value)) != -1
					mv.visitVarInsn Opcodes.ALOAD, reg
				else 
					qn_parentC = @compileScopeRef mv, n.value, n.ln
					mv.visitMethodInsn Opcodes.INVOKEVIRTUAL, qn_parentC, "get_" + n.value, jvm.sig.call(jvm.sig.obj(jvm.qn.object))

			when 'static-ref-expr'
				@compileJSObject mv, n.base
				mv.visitLdcInsn n.value
				mv.visitMethodInsn Opcodes.INVOKEVIRTUAL, mug.qn.object, "get", jvm.sig.call(jvm.sig.obj(jvm.qn.string), jvm.sig.obj(jvm.qn.object))
			
			when 'dyn-ref-expr'
				@compileJSObject mv, n.base
				switch @getNodeCompileType(n.index)
					when 'number'
						#TODO this isn't valid for non-integers!!!
						@compileNode mv, n.index
						mv.visitInsn Opcodes.D2I
						mv.visitMethodInsn Opcodes.INVOKEVIRTUAL, mug.qn.object, "get", jvm.sig.call(jvm.sig.integer, jvm.sig.obj(jvm.qn.object))
					else
						@compileAsObject mv, n.index
						mv.visitMethodInsn Opcodes.INVOKEVIRTUAL, mug.qn.object, "get", jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig.obj(jvm.qn.object))
			
			when 'static-method-call-expr'
				# get argument and method
				@compileJSObject mv, n.base
				mv.visitInsn Opcodes.DUP
				mv.visitLdcInsn n.value
				mv.visitMethodInsn Opcodes.INVOKEVIRTUAL, mug.qn.object, "get", jvm.sig.call(jvm.sig.obj(jvm.qn.string), jvm.sig.obj(jvm.qn.object))
				mv.visitTypeInsn Opcodes.CHECKCAST, mug.qn.object
				mv.visitInsn Opcodes.SWAP
				@compileInvokeArgs mv, n.args
				mv.visitMethodInsn Opcodes.INVOKEVIRTUAL, mug.qn.object, "invoke", compiler.sig.invoke()

			when 'call-expr'
				@compileJSObject mv, n.expr
				mv.visitInsn Opcodes.ACONST_NULL # "this"
				@compileInvokeArgs mv, n.args
				mv.visitMethodInsn Opcodes.INVOKEVIRTUAL, mug.qn.object, "invoke", compiler.sig.invoke()
			
			when 'new-expr'
				@compileNode mv, n.constructor
				mv.visitTypeInsn Opcodes.CHECKCAST, mug.qn.object
				@compileInvokeArgs mv, n.args
				mv.visitMethodInsn Opcodes.INVOKEVIRTUAL, mug.qn.object, "instantiate", compiler.sig.instantiate

			#TODO
			# we should be able to compile assignments without autoboxing the resulting
			# value, so that we can gain from type analysis
			when 'scope-assign-expr'
				@compileAsObject mv, n.expr
				mv.visitInsn Opcodes.DUP
				if (reg = @getRegister(n.value)) != -1
					mv.visitVarInsn Opcodes.ASTORE, reg
				else 
					qn_parentD = @compileScopeRef mv, n.value, n.ln
					mv.visitInsn Opcodes.SWAP
					mv.visitMethodInsn Opcodes.INVOKEVIRTUAL, qn_parentD, "set_" + n.value, jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig.void)
			
			when 'static-assign-expr'
				@compileJSObject mv, n.base
				mv.visitLdcInsn n.value
				@compileAsObject mv, n.expr
				mv.visitInsn Opcodes.DUP_X2
				mv.visitMethodInsn Opcodes.INVOKEVIRTUAL, mug.qn.object, "set", jvm.sig.call(jvm.sig.obj(jvm.qn.string), jvm.sig.obj(jvm.qn.object), jvm.sig.void)
			
			when 'dyn-assign-expr'
				# mv.visitInsn Opcodes.DUP
				@compileJSObject mv, n.base
				# mv.visitInsn Opcodes.SWAP
				switch @getNodeCompileType(n.index)
					#TODO this isn't valid for non-integers!!!
					when 'number'
						@compileNode mv, n.index
						mv.visitInsn Opcodes.D2I
						@compileAsObject mv, n.expr
						mv.visitInsn Opcodes.DUP_X2
						mv.visitMethodInsn Opcodes.INVOKEVIRTUAL, mug.qn.object, "set", jvm.sig.call(jvm.sig.integer, jvm.sig.obj(jvm.qn.object), jvm.sig.void)

					else
						@compileAsObject mv, n.index
						@compileAsObject mv, n.expr
						mv.visitInsn Opcodes.DUP_X2
						mv.visitMethodInsn Opcodes.INVOKEVIRTUAL, mug.qn.object, "set", jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig.obj(jvm.qn.object), jvm.sig.void)
			
			when 'scope-inc-expr'
				if n.pre
					n2 =
						type: 'scope-assign-expr'
						ln: n.ln
						value: n.value
						expr:
							type: 'add-op-expr'
							ln: n.ln
							left:
								type: 'scope-ref-expr'
								ln: n.ln
								value: n.value
							right:
								type: 'num-literal'
								ln: n.ln
								value: n.inc
					@compileNode mv, n2
					@compileAsNumber mv, @getNodeCompileType(n2)

				else
					if (reg = @getRegister(n.value)) != -1
						mv.visitVarInsn Opcodes.ALOAD, reg
						# {var}
						@compileAsNumber mv, 'Object'
						# {double|value}
						mv.visitInsn Opcodes.DUP2
						# {double|value} {double|value}
					else
						qn_parentE = @compileScopeRef mv, n.value, n.ln
						# {scope}
						mv.visitInsn Opcodes.DUP
						# {scope} {scope}
						mv.visitMethodInsn Opcodes.INVOKEVIRTUAL, qn_parentE, "get_" + n.value, jvm.sig.call(jvm.sig.obj(jvm.qn.object))
						# {scope} {var}
						@compileAsNumber mv, 'Object'
						# {scope} {double|value}
						mv.visitInsn Opcodes.DUP2_X1
						# {double|value} {scope} {double|value}
					mv.visitTypeInsn Opcodes.NEW, jvm.qn.double
					# ... {double|value} {obj}
					mv.visitInsn Opcodes.DUP_X2
					# ... {obj} {double|value} {obj}
					mv.visitInsn Opcodes.DUP_X2
					# ... {obj} {obj} {double|value} {obj}
					mv.visitInsn Opcodes.POP
					# ... {obj} {obj} {double|value}
					mv.visitLdcInsn new java.lang.Double(n.inc)
					# ... {obj} {obj} {double|value} {double|1.0}
					mv.visitInsn Opcodes.DADD
					# ... {obj} {obj} {double|value+1}
					mv.visitMethodInsn Opcodes.INVOKESPECIAL, jvm.qn.double, "<init>", jvm.sig.call(jvm.sig.double, jvm.sig.void)
					# ... {obj}
					if reg != -1
						mv.visitVarInsn Opcodes.ASTORE, reg
					else
						mv.visitMethodInsn Opcodes.INVOKEVIRTUAL, qn_parentE, "set_" + n.value, jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig.void)
					# {double|value}

			when 'static-inc-expr'
				if n.pre
					#TODO this doesn't really work, need to save refs!
					n2 =
						type: 'static-assign-expr'
						ln: n.ln
						base: n.base
						value: n.value
						expr:
							type: 'add-op-expr'
							ln: n.ln
							left:
								type: 'static-ref-expr'
								ln: n.ln
								base: n.base
								value: n.value
							right:
								type: 'num-literal'
								ln: n.ln
								value: n.inc
					@compileNode mv, n2
					@compileAsNumber mv, @getNodeCompileType(n2)

				else
					@compileJSObject mv, n.base
					# {base}
					mv.visitInsn Opcodes.DUP
					# {base} {base}
					mv.visitLdcInsn n.value
					mv.visitMethodInsn Opcodes.INVOKEVIRTUAL, mug.qn.object, "get", jvm.sig.call(jvm.sig.obj(jvm.qn.string), jvm.sig.obj(jvm.qn.object))
					# {base} {var}
					@compileAsNumber mv, 'Object'
					# {base} {double|value}
					mv.visitInsn Opcodes.DUP2_X1
					# {double|value} {base} {double|value}
					mv.visitTypeInsn Opcodes.NEW, jvm.qn.double
					# {double|value} {scope} {double|value} {obj}
					mv.visitInsn Opcodes.DUP_X2
					# {double|value} {scope} {obj} {double|value} {obj}
					mv.visitInsn Opcodes.DUP_X2
					# {double|value} {scope} {obj} {obj} {double|value} {obj}
					mv.visitInsn Opcodes.POP
					# {double|value} {scope} {obj} {obj} {double|value}
					mv.visitLdcInsn new java.lang.Double(n.inc)
					# {double|value} {scope} {obj} {obj} {double|value} {double|1.0}
					mv.visitInsn Opcodes.DADD
					# {double|value} {scope} {obj} {obj} {double|value+1}
					mv.visitMethodInsn Opcodes.INVOKESPECIAL, jvm.qn.double, "<init>", jvm.sig.call(jvm.sig.double, jvm.sig.void)
					# {double|value} {scope} {obj}
					mv.visitLdcInsn n.value
					mv.visitInsn Opcodes.SWAP
					mv.visitMethodInsn Opcodes.INVOKEVIRTUAL, mug.qn.object, "set", jvm.sig.call(jvm.sig.obj(jvm.qn.string), jvm.sig.obj(jvm.qn.object), jvm.sig.void)
					# {double|value}

			when 'dyn-inc-expr'
				if true
					#TODO this doesn't really work, need to save refs!
					n2 =
						type: 'dyn-assign-expr'
						ln: n.ln
						base: n.base
						index: n.index
						expr:
							type: 'add-op-expr'
							ln: n.ln
							left:
								type: 'dyn-ref-expr'
								ln: n.ln
								base: n.base
								index: n.index
							right:
								type: 'num-literal'
								ln: n.ln
								value: n.inc
					@compileNode mv, n2
					@compileAsNumber mv, @getNodeCompileType(n2)
				else
					throw new Error 'No support for dyn-inc-expr(pre)'
				
			when 'scope-delete-expr'
				#TODO could allow deletion of global (scope) variables
				mv.visitInsn Opcodes.ICONST_0
			
			when 'static-delete-expr'
				@compileJSObject mv, n.base
				mv.visitLdcInsn n.value
				mv.visitMethodInsn Opcodes.INVOKEVIRTUAL, mug.qn.object, "delete", jvm.sig.call(jvm.sig.obj(jvm.qn.string), jvm.sig.boolean)
			
			when 'dyn-delete-expr'
				@compileJSObject mv, n.base
				@compileAsObject mv, n.index
				mv.visitMethodInsn Opcodes.INVOKEVIRTUAL, mug.qn.object, "delete", jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig.boolean)

			# Statements.

			when 'block-stat'
				(@compileNode mv, stat) for stat in n.stats
			
			when 'expr-stat'
				@compileNode mv, n.expr
				@compilePop mv, n.expr

			when 'ret-stat'
				unless n.expr
					mv.visitInsn Opcodes.ACONST_NULL
				else
					@compileAsObject mv, n.expr
				mv.visitInsn Opcodes.ARETURN
			
			when 'while-stat'
				trueCase = new Label(); falseCase = new Label()
				@pushLabel null, trueCase, falseCase

				mv.visitLabel trueCase
				@compileNode mv, n.expr
				@compileAsBoolean mv, @getNodeCompileType(n.expr)
				mv.visitJumpInsn Opcodes.IFEQ, falseCase
				if n.stat
					@compileNode mv, n.stat
				mv.visitJumpInsn Opcodes.GOTO, trueCase
				mv.visitLabel falseCase

				@popLabel null
			
			when 'do-while-stat'
				trueCase = new Label(); falseCase = new Label()
				@pushLabel null, trueCase, falseCase

				mv.visitLabel trueCase
				if n.stat
					@compileNode mv, n.stat
				@compileNode mv, n.expr
				@compileAsBoolean mv, @getNodeCompileType(n.expr)
				mv.visitJumpInsn Opcodes.IFNE, trueCase
				mv.visitLabel falseCase

				@popLabel null
			
			when 'for-stat'
				if n.init
					@compileNode mv, n.init
					if jast.isExpr(n.init)
						@compilePop mv, n.init
				
				startLabel = new Label(); continueLabel = new Label(); breakLabel = new Label()
				@pushLabel null, continueLabel, breakLabel

				mv.visitLabel startLabel
				if n.expr
					@compileNode mv, n.expr
					@compileAsBoolean mv, @getNodeCompileType(n.expr)
					mv.visitJumpInsn Opcodes.IFEQ, breakLabel
				if n.stat
					@compileNode mv, n.stat
				mv.visitLabel continueLabel
				if n.step
					@compileNode mv, n.step
					@compilePop mv, n.step
				mv.visitJumpInsn Opcodes.GOTO, startLabel
				mv.visitLabel breakLabel

				@popLabel null

			when 'if-stat'
				@compileNode mv, n.expr
				@compileAsBoolean mv, @getNodeCompileType(n.expr)
				falseCase = new Label(); trueCase = new Label()
				mv.visitJumpInsn Opcodes.IFEQ, falseCase
				@compileNode mv, n.thenStat
				mv.visitJumpInsn Opcodes.GOTO, trueCase
				mv.visitLabel falseCase
				if n.elseStat
					@compileNode mv, n.elseStat
				mv.visitLabel trueCase
			
			when 'switch-stat'
				endCase = new Label(); labels = (new Label for _ in n.cases)
				@pushLabel null, null, endCase

				@compileAsObject mv, n.expr
				# Test for cases.
				for {match, stat}, i in n.cases when match 
					mv.visitInsn Opcodes.DUP
					@compileAsObject mv, match
					mv.visitMethodInsn Opcodes.INVOKESTATIC, mug.qn.utils, "testEquality", jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig.obj(jvm.qn.object), jvm.sig.boolean)
					mv.visitJumpInsn Opcodes.IFNE, labels[i]
				# Default cases.
				for {match, stat}, i in n.cases when not match 
					mv.visitJumpInsn Opcodes.GOTO, labels[i]
				mv.visitJumpInsn Opcodes.GOTO, endCase

				# Write out clauses.
				for {match, stat}, i in n.cases
					mv.visitLabel labels[i]
					@compileNode mv, stat
				
				mv.visitLabel endCase
				mv.visitInsn Opcodes.POP # original switch expression

				@popLabel null
			
			when 'throw-stat'
				mv.visitTypeInsn Opcodes.NEW, mug.qn.valueException
				mv.visitInsn Opcodes.DUP
				@compileAsObject mv, n.expr
				mv.visitMethodInsn Opcodes.INVOKESPECIAL, mug.qn.valueException, "<init>", jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig.void)
				mv.visitInsn Opcodes.ATHROW
			
			when 'try-stat'
				# Holy mother is this one long
				tryLabel = new Label; catchLabel = new Label; finallyLabel = new Label
				doubleThrowLabel = new Label; endLabel = new Label

				# Try.
				mv.visitLabel tryLabel
				@compileNode mv, n.tryStat
				mv.visitJumpInsn Opcodes.GOTO, finallyLabel

				# Catch (body may be empty)
				mv.visitLabel catchLabel
				if n.catchBlock
					exceptionLabel = new Label; catchEndLabel = new Label

					# Unwrap JSValueExceptions
					mv.visitInsn Opcodes.DUP
					mv.visitTypeInsn Opcodes.INSTANCEOF, mug.qn.valueException
					mv.visitJumpInsn Opcodes.IFEQ, exceptionLabel
					mv.visitTypeInsn Opcodes.CHECKCAST, mug.qn.valueException
					mv.visitFieldInsn Opcodes.GETFIELD, mug.qn.valueException, "value", jvm.sig.obj(jvm.qn.object)
					mv.visitJumpInsn Opcodes.GOTO, catchEndLabel
					# ...and wrap non-JS exceptions.
					mv.visitLabel exceptionLabel
					mv.visitInsn Opcodes.DUP
					mv.visitTypeInsn Opcodes.INSTANCEOF, mug.qn.exception
					mv.visitJumpInsn Opcodes.IFNE, catchEndLabel
					mv.visitTypeInsn Opcodes.NEW, mug.qn.exception
					mv.visitInsn Opcodes.DUP_X1
					mv.visitInsn Opcodes.SWAP
					@compileLoadEnvironment mv
					mv.visitInsn Opcodes.SWAP
					mv.visitMethodInsn Opcodes.INVOKESPECIAL, mug.qn.exception, "<init>", jvm.sig.call(jvm.sig.obj(mug.qn.toplevel), jvm.sig.obj(jvm.qn.exception), jvm.sig.void)
					# Done with check.
					mv.visitLabel catchEndLabel

					# Assign variable.
					if (reg = @getRegister(n.catchBlock.value)) != -1
						mv.visitVarInsn Opcodes.ASTORE, reg
					else
						qn_parentA = @compileScopeRef mv, n.catchBlock.value, n.catchBlock.ln
						mv.visitInsn Opcodes.SWAP
						mv.visitMethodInsn Opcodes.INVOKEVIRTUAL, qn_parentA, "set_" + n.catchBlock.value, jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig.void)

					# Body
					@compileNode mv, n.catchBlock.stat if n.catchBlock.stat
				else
					mv.visitInsn Opcodes.POP
				
				# Finally (optional)
				mv.visitLabel finallyLabel
				if n.finallyStat
					@compileNode mv, n.finallyStat
				mv.visitJumpInsn Opcodes.GOTO, endLabel

				# Throws inside catch (duplicates finally body)
				mv.visitLabel doubleThrowLabel
				if n.finallyStat
					@compileNode mv, n.finallyStat
				mv.visitInsn Opcodes.ATHROW

				mv.visitLabel endLabel

				# Catch block
				mv.visitTryCatchBlock tryLabel, catchLabel, catchLabel, jvm.qn.exception
				# Finally block.
				mv.visitTryCatchBlock tryLabel, finallyLabel, doubleThrowLabel, null

			when 'break-stat'
				unless (label = @getLabel null)
					throw new Error "Cannot break outside of loop"
				mv.visitJumpInsn Opcodes.GOTO, label.brk

			when 'continue-stat'
				unless (label = @getLabel null)
					throw new Error "Cannot continue outside of loop"
				mv.visitJumpInsn Opcodes.GOTO, label.cont
			
			when 'var-stat'
				for {value, expr} in n.vars when expr
					@compileNode mv, 
						type: 'expr-stat'
						ln: expr.ln
						expr: 
							type: 'scope-assign-expr'
							ln: expr.ln
							value: value
							expr: expr

			when 'defn-stat'
				@compileNode mv, {
					type: 'expr-stat'
					ln: n.ln
					expr: {
						type: 'scope-assign-expr'
						ln: n.ln
						value: n.closure.name
						expr: {type: 'func-literal', ln: n.ln, closure: n.closure}
					}}
			
			when 'for-in-stat'
				checkLabel = new Label(); statLabel = new Label()
				@pushLabel null, statLabel, checkLabel

				@compileJSObject mv, n.expr
				mv.visitMethodInsn Opcodes.INVOKEVIRTUAL, mug.qn.object, "getKeys", jvm.sig.call(jvm.sig.array(jvm.sig.obj(jvm.qn.string)))
				mv.visitInsn Opcodes.ICONST_0
				mv.visitJumpInsn Opcodes.GOTO, checkLabel
				mv.visitLabel statLabel
				# load from array
				mv.visitInsn Opcodes.DUP2
				mv.visitInsn Opcodes.AALOAD
				# store in scope
				if (reg = @getRegister(n.value)) != -1
					mv.visitVarInsn Opcodes.ASTORE, reg
				else
					qn_parentB = @compileScopeRef mv, n.value, n.ln
					mv.visitInsn Opcodes.SWAP
					mv.visitMethodInsn Opcodes.INVOKEVIRTUAL, qn_parentB, "set_" + n.value, jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig.void)

				@compileNode mv, n.stat

				mv.visitInsn Opcodes.ICONST_1
				mv.visitInsn Opcodes.IADD
				mv.visitLabel checkLabel
				mv.visitInsn Opcodes.DUP2
				mv.visitInsn Opcodes.SWAP
				mv.visitInsn Opcodes.ARRAYLENGTH
				mv.visitJumpInsn Opcodes.IF_ICMPLT, statLabel
				mv.visitInsn Opcodes.POP2

				@popLabel null

			# Fallback.

			else
				throw new Error 'Unrecognized type ' + JSON.stringify(n.type)
		
		return

#######################################################################
# contexts
#######################################################################

class ContextCompiler
	
	constructor: (@path, @ctx) ->

	compileScopeClass: ->
		# Initialize class.
		qn = compiler.qn.scope(@ctx.id)
		scope = @ctx.localVars.slice() # todo filter out variables stored locally in registers (analyze.clj)
		if @ctx.id == 0
			scope.push(x) for x in compiler.globals when x not in scope
		cw = new ClassWriter(ClassWriter.COMPUTE_MAXS)
		cw.visit Opcodes.V1_6, Opcodes.ACC_SUPER + Opcodes.ACC_PUBLIC, qn, null, jvm.qn.object, null

		# Compile fields.hahha
		for name in scope
			cw.visitField(0, "_" + name, jvm.sig.obj(jvm.qn.object), null, null).visitEnd()
		
		# Compile methods.
		for name in scope
			mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "get_" + name, jvm.sig.call(jvm.sig.obj(jvm.qn.object)), null, null)
			mv.visitCode()
			mv.visitVarInsn Opcodes.ALOAD, 0
			mv.visitFieldInsn Opcodes.GETFIELD, qn, "_" + name, jvm.sig.obj(jvm.qn.object)
			mv.visitInsn Opcodes.ARETURN
			mv.visitMaxs 1, 1
			mv.visitEnd()

			mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "set_" + name, jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig.void), null, null)
			mv.visitCode()
			mv.visitVarInsn Opcodes.ALOAD, 0
			mv.visitVarInsn Opcodes.ALOAD, 1
			mv.visitFieldInsn Opcodes.PUTFIELD, qn, "_" + name, jvm.sig.obj(jvm.qn.object)
			mv.visitInsn Opcodes.RETURN
			mv.visitMaxs 2, 2
			mv.visitEnd()

		# Write initializer.
		if @ctx.id == 0
			mv = cw.visitMethod Opcodes.ACC_PUBLIC, "<init>", jvm.sig.call(jvm.sig.obj(mug.qn.toplevel), jvm.sig.void), null, null
			mv.visitCode()
			mv.visitVarInsn Opcodes.ALOAD, 0
			mv.visitMethodInsn Opcodes.INVOKESPECIAL, jvm.qn.object, "<init>", jvm.sig.call(jvm.sig.void)

			# load some props
			for name in compiler.globals when name != 'exports'
				mv.visitVarInsn Opcodes.ALOAD, 0
				mv.visitVarInsn Opcodes.ALOAD, 1
				mv.visitMethodInsn Opcodes.INVOKEVIRTUAL, mug.qn.toplevel, "get_" + name, jvm.sig.call(jvm.sig.obj(jvm.qn.object))
				mv.visitFieldInsn Opcodes.PUTFIELD, qn, "_" + name, jvm.sig.obj(jvm.qn.object)

			mv.visitInsn Opcodes.RETURN
			mv.visitMaxs 1, 1
			mv.visitEnd()
		else
			mv = cw.visitMethod Opcodes.ACC_PUBLIC, "<init>", jvm.sig.call(jvm.sig.void), null, null
			mv.visitCode()
			mv.visitVarInsn Opcodes.ALOAD, 0
			mv.visitMethodInsn Opcodes.INVOKESPECIAL, jvm.qn.object, "<init>", jvm.sig.call(jvm.sig.void)
			mv.visitInsn Opcodes.RETURN
			mv.visitMaxs 1, 1
			mv.visitEnd()
		
		# Finalize class.
		cw.visitEnd()
		log.verbose '...Compiled scope ' + qn
		return [qn + '.class', cw.toByteArray()]

class ScriptContextCompiler extends ContextCompiler

	compileClass: ->
		cw = new ClassWriter(ClassWriter.COMPUTE_MAXS)
		qn = compiler.qn.context(@ctx.id)
		cw.visit Opcodes.V1_6, Opcodes.ACC_SUPER + Opcodes.ACC_PUBLIC, qn, null, mug.qn.module, null
		cw.visitSource @path, null

		@compileInit cw
		@compileMethods cw
		@compileFields cw

		# Main.
		mv = cw.visitMethod Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "main", jvm.sig.call(jvm.sig.array(jvm.sig.obj(jvm.qn.string)), jvm.sig.void), null, compiler.stringArray(jvm.qn.exception)
		mv.visitCode()
		mv.visitTypeInsn Opcodes.NEW, compiler.qn.script
		mv.visitInsn Opcodes.DUP
		mv.visitMethodInsn Opcodes.INVOKESPECIAL, compiler.qn.script, "<init>", jvm.sig.call(jvm.sig.void)
		mv.visitTypeInsn Opcodes.NEW, mug.qn.toplevel
		mv.visitInsn Opcodes.DUP
		mv.visitVarInsn Opcodes.ALOAD, 0
		mv.visitMethodInsn Opcodes.INVOKESPECIAL, mug.qn.toplevel, "<init>", jvm.sig.call(jvm.sig.array(jvm.sig.obj(jvm.qn.string)), jvm.sig.void)
		mv.visitMethodInsn Opcodes.INVOKEVIRTUAL, compiler.qn.script, "load", jvm.sig.call(jvm.sig.obj(mug.qn.toplevel), jvm.sig.obj(mug.qn.object))
		mv.visitInsn Opcodes.POP

		# Wait for timeouts.
		mv.visitMethodInsn Opcodes.INVOKESTATIC, mug.qn.timers, "awaitTaskPool", jvm.sig.call(jvm.sig.void)

		mv.visitInsn Opcodes.RETURN
		mv.visitMaxs 1, 1
		mv.visitEnd

		# finalize
		cw.visitEnd()
		log.verbose '...Compiled script ' + qn
		return [qn + '.class', cw.toByteArray()]

	compileInit: (cw) ->
		sig = jvm.sig.call(jvm.sig.void)
		mv = cw.visitMethod Opcodes.ACC_PUBLIC, "<init>", sig, null, null

		mv.visitCode()
		mv.visitVarInsn Opcodes.ALOAD, 0
		mv.visitMethodInsn Opcodes.INVOKESPECIAL, mug.qn.module, "<init>", sig
		mv.visitInsn Opcodes.RETURN
		mv.visitMaxs 1, 1
		mv.visitEnd()
		return

	compileFields: (cw) ->
		cw.visitField(0, compiler.ident.env, jvm.sig.obj(mug.qn.toplevel), null, null).visitEnd()
		return

	compileMethods: (cw) ->
		mv = cw.visitMethod Opcodes.ACC_PUBLIC, "load", jvm.sig.call(jvm.sig.obj(mug.qn.toplevel), jvm.sig.obj(mug.qn.object)), null, compiler.stringArray(jvm.qn.exception)

		# start method compiler
		asm = new MethodCompiler @ctx

		# Create scope and store in register.
		mv.visitTypeInsn Opcodes.NEW, compiler.qn.scriptscope()
		mv.visitInsn Opcodes.DUP
		mv.visitVarInsn Opcodes.ALOAD, 1 # ENV
		mv.visitMethodInsn Opcodes.INVOKESPECIAL, compiler.qn.scriptscope(), "<init>", jvm.sig.call(jvm.sig.obj(mug.qn.toplevel), jvm.sig.void)
		mv.visitVarInsn Opcodes.ASTORE, compiler.regs.scope
		# Create exports object.
		mv.visitTypeInsn Opcodes.NEW, mug.qn.object
		mv.visitInsn Opcodes.DUP
		mv.visitVarInsn Opcodes.ALOAD, 1 # ENV
		mv.visitMethodInsn Opcodes.INVOKESPECIAL, mug.qn.object, "<init>", jvm.sig.call(jvm.sig.obj(mug.qn.toplevel), jvm.sig.void)
		# Store in scope.
		mv.visitInsn Opcodes.DUP
		mv.visitVarInsn Opcodes.ALOAD, compiler.regs.scope
		mv.visitInsn Opcodes.SWAP
		mv.visitMethodInsn Opcodes.INVOKEVIRTUAL, compiler.qn.scriptscope(), "set_exports", jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig.void)
		# Cache environment as property....
		mv.visitVarInsn Opcodes.ALOAD, 0
		mv.visitVarInsn Opcodes.ALOAD, 1
		mv.visitFieldInsn Opcodes.PUTFIELD, compiler.qn.context(@ctx.id), compiler.ident.env, jvm.sig.obj(mug.qn.toplevel)
		# Store exports object as "this" object (register 1)
		mv.visitVarInsn Opcodes.ASTORE, compiler.regs.thisObj

		# Initialize registers.
		for name in asm.unenclosedVars
			if (reg = asm.getRegister(name)) != -1
				mv.visitInsn Opcodes.ACONST_NULL
				mv.visitVarInsn Opcodes.ASTORE, reg

		# Hoist functions
		for stat in @ctx.node.stats when stat.type == 'defn-stat'
			asm.compileNode mv, stat
		# Compile code
		for stat in @ctx.node.stats when stat.type != 'defn-stat'
			asm.compileNode mv, stat

		# Return "exports" object.
		mv.visitVarInsn Opcodes.ALOAD, compiler.regs.thisObj
		mv.visitInsn Opcodes.ARETURN

		mv.visitMaxs 0, 0
		mv.visitEnd
		return

class ClosureContextCompiler extends ContextCompiler

	compileClass: ->
		cw = new ClassWriter(ClassWriter.COMPUTE_MAXS)
		qn = compiler.qn.context(@ctx.id)
		cw.visit(Opcodes.V1_6, Opcodes.ACC_SUPER + Opcodes.ACC_PUBLIC, qn, null, mug.qn.function, null)
		cw.visitSource @path, null

		@compileInit cw
		@compileMethods cw
		@compileFields cw

		# finalize
		cw.visitEnd()
		log.verbose '...Compiled closure ' + qn
		return [qn + '.class', cw.toByteArray()]

	compileInit: (cw) ->
		# Signature includes JSEnvironment and parent scopes.
		sig = jvm.sig.call jvm.sig.obj(mug.qn.toplevel),
			(jvm.sig.obj(compiler.qn.scope(parent.id)) for parent in @ctx.getParents())...,
			jvm.sig.void

		mv = cw.visitMethod Opcodes.ACC_PUBLIC, "<init>", sig, null, null
		qn = compiler.qn.context(@ctx.id)
		mv.visitCode()
		mv.visitVarInsn Opcodes.ALOAD, 0
		mv.visitVarInsn Opcodes.ALOAD, 1
		mv.visitMethodInsn Opcodes.INVOKESPECIAL, mug.qn.function, "<init>", jvm.sig.call(jvm.sig.obj(mug.qn.toplevel), jvm.sig.void)

		# Assign ENV property.
		mv.visitVarInsn Opcodes.ALOAD, 0
		mv.visitVarInsn Opcodes.ALOAD, 1
		mv.visitFieldInsn Opcodes.PUTFIELD, qn, compiler.ident.env, jvm.sig.obj(mug.qn.toplevel)
		# Assign SCOPE properties.
		for parent, i in @ctx.getParents()
			mv.visitVarInsn Opcodes.ALOAD, 0
			mv.visitVarInsn Opcodes.ALOAD, i + 2
			mv.visitFieldInsn Opcodes.PUTFIELD, qn, compiler.ident.scope(parent.id), jvm.sig.obj(compiler.qn.scope(parent.id))

		mv.visitInsn Opcodes.RETURN
		mv.visitMaxs 1, 1
		mv.visitEnd
		return

	compileFields: (cw) ->
		# Scope fields.
		cw.visitField(0, compiler.ident.env, jvm.sig.obj(mug.qn.toplevel), null, null).visitEnd()
		for parent in @ctx.getParents()
			cw.visitField(0, compiler.ident.scope(parent.id), jvm.sig.obj(compiler.qn.scope(parent.id)), null, null).visitEnd()
		return

	compileMethods: (cw) ->
		mv = cw.visitMethod Opcodes.ACC_PUBLIC, "invoke", compiler.sig.invoke(), null, compiler.stringArray(jvm.qn.exception)
		mv.visitCode()

		# start method compiler
		asm = new MethodCompiler @ctx

		# Create scope object.
		mv.visitTypeInsn Opcodes.NEW, compiler.qn.scope(@ctx.id)
		mv.visitInsn Opcodes.DUP
		mv.visitMethodInsn Opcodes.INVOKESPECIAL, compiler.qn.scope(@ctx.id), "<init>", jvm.sig.call(jvm.sig.void)
		mv.visitVarInsn Opcodes.ASTORE, compiler.regs.scope

		# Initialize arguments object.
		if @ctx.usesArguments
			asm.compileLoadEnvironment mv
			mv.visitVarInsn Opcodes.ILOAD, compiler.regs.argc
			for i in [compiler.regs.offset...compiler.regs.scope]
				mv.visitVarInsn Opcodes.ALOAD, i
			mv.visitMethodInsn Opcodes.INVOKESTATIC, mug.qn.utils, "arguments", jvm.sig.call(
				jvm.sig.integer,
				jvm.sig.obj(jvm.qn.object), jvm.sig.obj(jvm.qn.object),
				jvm.sig.obj(jvm.qn.object), jvm.sig.obj(jvm.qn.object), 
				jvm.sig.obj(jvm.qn.object), jvm.sig.obj(jvm.qn.object), 
				jvm.sig.obj(jvm.qn.object), jvm.sig.obj(jvm.qn.object), 
				jvm.sig.array(jvm.sig.obj(jvm.qn.object)),
				jvm.sig.array(jvm.sig.obj(jvm.qn.object))
				)
			mv.visitMethodInsn Opcodes.INVOKESTATIC, mug.qn.utils, "createArgumentsObject",
				jvm.sig.call(jvm.sig.obj(mug.qn.toplevel), jvm.sig.array(jvm.sig.obj(jvm.qn.object)), jvm.sig.obj(mug.qn.object))
			if (reg = asm.getRegister("arguments")) != -1
				mv.visitVarInsn Opcodes.ASTORE, reg
			else
				mv.visitVarInsn Opcodes.ALOAD, compiler.regs.scope
				mv.visitInsn Opcodes.SWAP
				mv.visitMethodInsn Opcodes.INVOKEVIRTUAL, compiler.qn.scope(@ctx.id), "set_arguments", jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig.void)

		# Initialize scoped arguments.
		for name, i in @ctx.node.args when asm.getRegister(name) == -1
			mv.visitVarInsn Opcodes.ALOAD, compiler.regs.scope
			mv.visitVarInsn Opcodes.ALOAD, i + 3
			mv.visitMethodInsn Opcodes.INVOKEVIRTUAL, compiler.qn.scope(@ctx.id), "set_" + name, jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig.void)
		# Initialize registers.
		for name in asm.unenclosedVars when name not in @ctx.node.args and name not in [@ctx.node.name, 'arguments']
			mv.visitInsn Opcodes.ACONST_NULL
			mv.visitVarInsn Opcodes.ASTORE, asm.getRegister(name)
		
		# Initialize self.
		if @ctx.node.name and asm.getRegister(@ctx.node.name) == -1
			mv.visitVarInsn Opcodes.ALOAD, compiler.regs.scope
			mv.visitVarInsn Opcodes.ALOAD, 0
			mv.visitMethodInsn Opcodes.INVOKEVIRTUAL, compiler.qn.scope(@ctx.id), "set_" + @ctx.node.name, jvm.sig.call(jvm.sig.obj(jvm.qn.object), jvm.sig.void)
		
		# Hoist functions
		for stat in @ctx.node.stats when stat.type == 'defn-stat'
			asm.compileNode mv, stat
		# Compile code
		for stat in @ctx.node.stats when stat.type != 'defn-stat'
			asm.compileNode mv, stat
			
		# Catch-all return
		mv.visitInsn Opcodes.ACONST_NULL
		mv.visitInsn Opcodes.ARETURN

		# Finish closure.
		mv.visitMaxs 0, 0
		mv.visitEnd()
		return

#######################################################################
# AST analysis
#######################################################################

class Context
	constructor: (@node) ->
		@usesArguments = if @node.type == 'script-context' then false
		else jast.usesArguments(@node)
		@childNodes = jast.childContexts(@node)
		@localUndefinedRefs = jast.localUndefinedRefs(@node)
		@localVars = jast.localVars(@node)
		@children = []
	
	pushChild: (ctx) ->
		@children.push ctx
		ctx.parent = this
		return
	
	getLineage: ->
		line = []; cur = this
		while cur
			line.push(cur)
			cur = cur.parent
		line.reverse()
		return line

	getParents: -> @getLineage().slice(0, -1)

	postAnalyze: ->
		# get enclosed vars of child contexts
		getEnclosedVars = (ctx, vars) ->
			ret = []
			for child in ctx.children
				ret.push(ref for ref in child.localUndefinedRefs when ref in vars)
				ret.push getEnclosedVars(child, v for v in vars when v not in child.localVars)
			return [].concat(ret...)
		# set arrays
		@enclosedVars = getEnclosedVars(this, @localVars)
		@unenclosedVars = (v for v in @localVars when v not in @enclosedVars)

		# postanalyze children
		child.postAnalyze() for child in @children
		return

class CodeInput
	constructor: (@code) ->
		# generate AST
		@ast = jast.parse code

		# code analysis
		@numbers = jast.numbers(@ast)
		@regexps = jast.regexps(@ast)

		@contexts = []
		@rootContext = @buildContextTree()
		@rootContext.postAnalyze()
	
	buildContextTree: (node = @ast, parent = null) ->
		ctx = new Context(node, parent)
		ctx.id = @contexts.push(ctx)-1
		for child in ctx.childNodes
			ctx.pushChild @buildContextTree(child, node)
		return ctx
	
	getContext: (node) ->
		for ctx in @contexts
			return ctx if ctx.node == node
		throw new Error 'Unmatched context.'

#######################################################################
# Entry point
#######################################################################

compiler.readFile = (filepath) ->
	{Scanner} = java.util; {File} = java.io

	try
		return new Scanner(new File(filepath)).useDelimiter("\\Z").next()
	catch e
		return ""

compiler.writeClasses = (outpath, files) ->
	{File, FileOutputStream} = java.io

	for [path, bytes] in files
		try
			file = new File(outpath + path)
			file.getParentFile().mkdirs()
			stream = new FileOutputStream(file)
			stream.write(bytes)
		catch e
			console.log('Error writing out file:', e)
		finally
			stream.close()

compiler.writeJar = (outpath, jarpath, files) ->
	{File, FileOutputStream} = java.io
	{Manifest, Attributes, JarOutputStream, JarEntry} = java.util.jar

	manifest = new Manifest()
	attributes = manifest.getMainAttributes()
	#[TODO] write out manifest attributes?
	attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0")

	new File(outpath).mkdirs()
	fstream = new FileOutputStream(outpath + jarpath)
	stream = new JarOutputStream(fstream, manifest)

	for [path, bytes] in files
		entry = new JarEntry(path)
		stream.putNextEntry(entry)
		stream.write(bytes)

	stream.flush()
	stream.close()

# start parsing

input = null

compiler.cli = (args) ->
	if args.length == 0
		throw new Error('Please specify a filename.')

	opts =
		output: "out/"
		jar: null
		files: {"": []}

	# arguments parsing loop
	curns = ""
	while args.length
		switch args[0]
			when '-h', '--help'
				console.log """Usage:
  --output, -o <arg>    Output directory (default "out")
  --jar, -j <arg>       Create jar archive in output directory
  --namespace, -n <arg>  Namespace to use for subsequent modules (default "")"""
				return
			when '-o', '--output'
				args.shift()
				opts.output = args.shift().replace(/\/?$/, '/')
			when '-j', '--jar'
				args.shift()
				opts.jar = args.shift()
			when '-n', '--namespace'
				args.shift()
				curns = args.shift().replace(/\./g, '/') + '/'
				opts.files[curns] or= []
			else
				opts.files[curns].push args.shift()

	log.notify 'Starting compilation.'

	# output files
	files = []

	for ns of opts.files
		for filepath in opts.files[ns]
			# Script name and qualified name.
			scriptname = filepath.replace(/\.js$/,'').replace(/^.*\//, '').replace(/\-/g, '_')
			compiler.qn.script = 'js/' + ns + scriptname

			log.notify "Compiling file #{filepath} as '#{compiler.qn.script}'..."


			# Parse cde.
			code = compiler.readFile(filepath) or ""
			input = new CodeInput(code)
			log.verbose 'Finished parsing.'

			# Constants class.
			files.push (new ConstantsCompiler).compileClass()
			# Context classes.
			for ctx in input.contexts
				log.verbose 'Compiling context #' + ctx.id
				if ctx.node.type == 'closure-context'
					cmp = new ClosureContextCompiler filepath, ctx
					files.push cmp.compileScopeClass()
					files.push cmp.compileClass()
				else if ctx.node.type == 'script-context'
					cmp = new ScriptContextCompiler filepath, ctx
					files.push cmp.compileScopeClass()
					files.push cmp.compileClass()

	# write out classes
	if opts.jar != null
		log.notify "Writing out classes to archive '#{opts.output}#{opts.jar}'..."
		compiler.writeJar(opts.output, opts.jar, files)
	else
		log.notify "Writing out classes to folder '#{opts.output}'..."
		compiler.writeClasses(opts.output, files)

	# Done.
	end = new Date()
	log.notify 'Compiled in', (end-start)/1000, 's'

# launch
compiler.cli([].slice.call(arguments))