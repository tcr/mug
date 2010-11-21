function gen_clojure(ast) {
	var generators = {
		"atom": function(name) {
			return [make_string(name)];
		},
		"num": function (value) {
			return [make_number(value)];
		},
		"string": function(str) {
			return [make_string(str)];
		},
		"name": function (value) {
			return [make_string(value)];
		},
		"array": function(elements) {
			return elements.map(make);
		},
		"object": function(props) {
			var out = ["{"];
			props.map(function (p) {
				out.push("\"" + p[0] + "\"");
				out.push(make(p[1]));
			});
			return out.concat(["}"]);
		},
		"regexp": function(rx, mods) {
			return [make_string(rx), make_string(mods)];
		},
		"assign": function(op, lvalue, rvalue) {
			return [typeof op == "string" ? make_string(op) : op ? "true" : "false", make(lvalue), make(rvalue)];
		},
		"binary": function(operator, lvalue, rvalue) {
			return [make_string(operator), make(lvalue), make(rvalue)];
		},
		"unary-postfix": function(operator, expr) {
			return [make_string(operator), make(expr)];
		},
		"unary-prefix": function(operator, expr) {
			return [make_string(operator), make(expr)];
		},
		"call": function(func, args) {
			return [make(func), "["].concat(args.map(make)).concat(["]"]);
		},
		"dot": function(expr, attr) {
			return [make(expr), make_string(attr)];
		},
		"sub": function(expr, subscript) {
			return [make(expr), make(subscript)];
		},
		"seq": function(pre, expr) {
			return [make(pre), make(expr)];
		},
		"conditional": function(co, th, el) {
			return [make(co), make(th), make(el)];
		},
		"function": function (name, args, body) {
			return [make_string(name), "["].concat(args.map(make_string)).concat(["]"]).concat(body.map(make));
		},
		"new": function(ctor, args) {
			return [make(ctor), "["].concat(args.map(make)).concat(["]"]);
		},



		"toplevel": function(statements) {
			return statements.map(make);
		},
		"block": function(statements) {
			return statements ? statements.map(make) : [];
		},
		"stat": function(stmt) {
			return [make(stmt)];
		},
		"label": function(name, block) {
			return [make_string(name), make(block)];
		},
		"if": function(co, th, el) {
			return [make(co), make(th), el ? make(el) : "nil"];
		},
		"with": function(expr, block) {
			return [make(expr), make(block)];
		},
		"var": function(defs) {
			var out = ["{"];
			defs.map(function (p) {
				out.push(":" + p[0]);
				out.push(p[1] ? make(p[1]) : "nil");
			});
			return out.concat(["}"]);
		},
		"defun": function (name, args, body) {
			return [make_string(name), "["].concat(args.map(make_string)).concat(["]"]).concat(body.map(make));
		},
		"return": function(expr) {
			return [make(expr)];
		},
		"debugger": function() {
			return [];
		},


		"try": function(tr, ca, fi) {
			return [make(tr), make(ca), make(fi)];
		},
		"throw": function(expr) {
			return [make(expr)];
		},



		"break": function(label) {
			return [make_string(name)];
		},
		"continue": function(label) {
			return [make_string(name)];
		},
		"while": function(condition, block) {
			return [make(condition), make(block)];
		},
		"do": function(condition, block) {
			return [make(condition), make(block)];
		},
		"for": function(init, cond, step, block) {
			return [init ? make(init) : "nil",
			        cond ? make(cond) : "nil",
			        step ? make(step) : "nil",
			        block ? make(block) : "nil"];
		},
		"for-in": function(has_var, key, hash, block) {
			return [has_var ? "true" : "false", make_string(key), make(hash), make(block)];
		},
		"switch": function(expr, body) {
			return [make(expr), make(body)];
		},
		"case": function(expr) {
			return [make(expr)];
		},
		"default": function() {
			return [];
		},

		"comment1": function(text) {
			return [make_string(text)];
		},
		"comment2": function(text) {
			return [make_string(text)];
		}
	};

	function make(node) {
		var type = node[0];
		var gen = generators[type];
		if (!gen)
			throw new Error("Can't find generator for \"" + type.toSource() + "\"");
		return add_spaces(["(:" + type].concat(gen.apply(type, node.slice(1))).concat([")"]));
	};

	return make(ast);
};

////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////

function best_of(a) {
	if (a.length == 1) {
		return a[0];
	}
	if (a.length == 2) {
		var b = a[1];
		a = a[0];
		return a.length <= b.length ? a : b;
	}
	return best_of([ a[0], best_of(a.slice(1)) ]);
};

function make_number(value) {
	return isFinite(value) ? String(value) : 'null';
}

/*
function make_number(num) {
	var str = num.toString(10), a = [ str ], m;
	if (Math.floor(num) === num) {
		a.push("0x" + num.toString(16).toLowerCase(), // probably pointless
		       "0" + num.toString(8)); // same.
		if ((m = /^(.*?)(0+)$/.exec(num))) {
			a.push(m[1] + "e" + m[2].length);
		}
	} else if ((m = /^0?\.(0+)(.*)$/.exec(num))) {
		a.push(m[2] + "e-" + (m[1].length + 1),
		       str.substr(str.indexOf(".")));
	}
	return best_of(a);
};
*/

function make_string(str) {
	if (str == null) return "nil";
	 return '"' +
		 str.replace(/\x5c/g, "\\\\")
		 .replace(/\r?\n/g, "\\n")
		 .replace(/\t/g, "\\t")
		 .replace(/\r/g, "\\r")
		 .replace(/\f/g, "\\f")
		 .replace(/[\b]/g, "\\b")
		 .replace(/\x22/g, "\\\"")
		 .replace(/[\x00-\x1f]|[\x80-\xff]/g, function(c){
			 var hex = c.charCodeAt(0).toString(16);
			 if (hex.length < 2)
				 hex = "0" + hex;
			 return "\\x" + hex;
		 })
		 + '"';
	//return JSON.stringify(str); // STILL cheating.
};

function add_spaces(a) {
	return a.join(" ");
};