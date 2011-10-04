(function() {
  exports.populate = function(jast) {
    var genAst, node, preparser;
    preparser = require('./parser-base.js');
    node = function(type, ln, props) {
      var k, ret, v;
      if (props == null) {
        props = {};
      }
      ret = {
        type: type,
        ln: ln
      };
      for (k in props) {
        v = props[k];
        ret[k] = v;
      }
      return ret;
    };
    genAst = function(o) {
      var args, attr, base, bindings, body, cond, cse, ctch, elems, els, expr, flags, fnlly, form, form1, func, inc, init, k, label, lhs, ln, map, name, obj, op, place, result, rhs, stats, step, test, thn, v, val, value, vari, x, _;
      switch (o[0]) {
        case "atom":
        case "name":
          _ = o[0], ln = o[1], value = o[2];
          switch (value) {
            case "true":
              return node("boolean-literal", ln, {
                value: true
              });
            case "false":
              return node("boolean-literal", ln, {
                value: false
              });
            case "this":
              return node("this-expr", ln);
            case "null":
              return node("null-literal", ln);
            case "undefined":
              return node("undef-literal", ln);
            default:
              return node("scope-ref-expr", ln, {
                value: value
              });
          }
        case "num":
          _ = o[0], ln = o[1], value = o[2];
          return node("num-literal", ln, {
            value: value
          });
        case "string":
          _ = o[0], ln = o[1], value = o[2];
          return node("str-literal", ln, {
            value: value
          });
        case "array":
          _ = o[0], ln = o[1], elems = o[2];
          return node("array-literal", ln, {
            exprs: (function() {
              var _i, _len, _results;
              _results = [];
              for (_i = 0, _len = elems.length; _i < _len; _i++) {
                x = elems[_i];
                _results.push(genAst(x));
              }
              return _results;
            })()
          });
        case "object":
          _ = o[0], ln = o[1], elems = o[2];
          return node("obj-literal", ln, {
            props: (function() {
              var _i, _len, _ref, _results;
              _results = [];
              for (_i = 0, _len = elems.length; _i < _len; _i++) {
                _ref = elems[_i], k = _ref[0], v = _ref[1];
                _results.push({
                  value: k,
                  expr: genAst(v)
                });
              }
              return _results;
            })()
          });
        case "regexp":
          _ = o[0], ln = o[1], expr = o[2], flags = o[3];
          return node("regexp-literal", ln, {
            expr: expr,
            flags: flags
          });
        case "assign":
          _ = o[0], ln = o[1], op = o[2], place = o[3], val = o[4];
          if (op !== true) {
            val = ["binary", ln, op, place, val];
          }
          switch (place[0]) {
            case "name":
              return node("scope-assign-expr", ln, {
                value: place[2],
                expr: genAst(val)
              });
            case "dot":
              return node("static-assign-expr", ln, {
                base: genAst(place[2]),
                value: place[3],
                expr: genAst(val)
              });
            case "sub":
              return node("dyn-assign-expr", ln, {
                base: genAst(place[2]),
                index: genAst(place[3]),
                expr: genAst(val)
              });
          }
        case "binary":
          _ = o[0], ln = o[1], op = o[2], lhs = o[3], rhs = o[4];
          map = {
            "+": "add-op-expr",
            "-": "sub-op-expr",
            "*": "mul-op-expr",
            "/": "div-op-expr",
            "%": "mod-op-expr",
            "<": "lt-op-expr",
            ">": "gt-op-expr",
            "<=": "lte-op-expr",
            ">=": "gte-op-expr",
            "==": "eq-op-expr",
            "===": "eqs-op-expr",
            "!=": "neq-op-expr",
            "!==": "neqs-op-expr",
            "||": "or-op-expr",
            "&&": "and-op-expr",
            "<<": "lsh-op-expr",
            ">>": "rsh-op-expr",
            "&": "bit-or-op-expr",
            "|": "bit-or-op-expr",
            "^": "bit-xor-op-expr",
            "instanceof": "instanceof-op-expr",
            "in": "in-op-expr"
          };
          return node(map[op], ln, {
            left: genAst(lhs),
            right: genAst(rhs)
          });
        case "unary-postfix":
          _ = o[0], ln = o[1], op = o[2], place = o[3];
          inc = op === "++" ? 1 : -1;
          switch (place[0]) {
            case "name":
              return node("scope-inc-expr", ln, {
                pre: false,
                inc: inc,
                value: place[2]
              });
            case "dot":
              return node("static-inc-expr", ln, {
                pre: false,
                inc: inc,
                base: genAst(place[2]),
                value: place[3]
              });
            case "sub":
              return node("dyn-inc-expr", ln, {
                pre: false,
                inc: inc,
                base: genAst(place[2]),
                index: genAst(place[3])
              });
          }
          break;
        case "unary-prefix":
          _ = o[0], ln = o[1], op = o[2], place = o[3];
          switch (op) {
            case "+":
              return node("num-op-expr", ln, {
                expr: genAst(place)
              });
            case "-":
              return node("neg-op-expr", ln, {
                expr: genAst(place)
              });
            case "~":
              return node("bit-op-expr", ln, {
                expr: genAst(place)
              });
            case "++":
            case "--":
              inc = op === "++" ? 1 : -1;
              switch (place[0]) {
                case "name":
                  return node("scope-inc-expr", ln, {
                    pre: true,
                    inc: inc,
                    value: place[2]
                  });
                case "dot":
                  return node("static-inc-expr", ln, {
                    pre: true,
                    inc: inc,
                    base: genAst(place[2]),
                    value: place[3]
                  });
                case "sub":
                  return node("dyn-inc-expr", ln, {
                    pre: true,
                    inc: inc,
                    base: genAst(place[2]),
                    index: genAst(place[3])
                  });
              }
              break;
            case "!":
              return node("not-op-expr", ln, {
                expr: genAst(place)
              });
            case "void":
              return node("void-op-expr", ln, {
                expr: genAst(place)
              });
            case "typeof":
              return node("typeof-op-expr", ln, {
                expr: genAst(place)
              });
            case "delete":
              switch (place[0]) {
                case "name":
                  return node("scope-delete-expr", ln, {
                    value: place[2]
                  });
                case "dot":
                  return node("static-delete-expr", ln, {
                    base: genAst(place[2]),
                    value: place[3]
                  });
                case "sub":
                  return node("dyn-delete-expr", ln, {
                    base: genAst(place[2]),
                    index: genAst(place[3])
                  });
              }
          }
        case "call":
          _ = o[0], ln = o[1], func = o[2], args = o[3];
          switch (func[0]) {
            case "dot":
              _ = func[0], ln = func[1], base = func[2], value = func[3];
              return node("static-method-call-expr", ln, {
                base: genAst(base),
                value: value,
                args: (function() {
                  var _i, _len, _results;
                  _results = [];
                  for (_i = 0, _len = args.length; _i < _len; _i++) {
                    x = args[_i];
                    _results.push(genAst(x));
                  }
                  return _results;
                })()
              });
            default:
              return node("call-expr", ln, {
                expr: genAst(func),
                args: (function() {
                  var _i, _len, _results;
                  _results = [];
                  for (_i = 0, _len = args.length; _i < _len; _i++) {
                    x = args[_i];
                    _results.push(genAst(x));
                  }
                  return _results;
                })()
              });
          }
          break;
        case "dot":
          _ = o[0], ln = o[1], obj = o[2], attr = o[3];
          return node("static-ref-expr", ln, {
            base: genAst(obj),
            value: attr
          });
        case "sub":
          _ = o[0], ln = o[1], obj = o[2], attr = o[3];
          return node("dyn-ref-expr", ln, {
            base: genAst(obj),
            index: genAst(attr)
          });
        case "seq":
          _ = o[0], ln = o[1], form1 = o[2], result = o[3];
          return node("seq-op-expr", ln, {
            left: genAst(form1),
            right: genAst(result)
          });
        case "conditional":
          _ = o[0], ln = o[1], test = o[2], thn = o[3], els = o[4];
          return node("if-expr", ln, {
            expr: genAst(test),
            thenExpr: genAst(thn),
            elseExpr: genAst(els)
          });
        case "function":
          _ = o[0], ln = o[1], name = o[2], args = o[3], stats = o[4];
          return node("func-literal", ln, {
            closure: node("closure-context", ln, {
              name: name,
              args: args,
              stats: (function() {
                var _i, _len, _results;
                _results = [];
                for (_i = 0, _len = stats.length; _i < _len; _i++) {
                  x = stats[_i];
                  _results.push(genAst(x));
                }
                return _results;
              })()
            })
          });
        case "new":
          _ = o[0], ln = o[1], func = o[2], args = o[3];
          return node("new-expr", ln, {
            constructor: genAst(func),
            args: (function() {
              var _i, _len, _results;
              _results = [];
              for (_i = 0, _len = args.length; _i < _len; _i++) {
                x = args[_i];
                _results.push(genAst(x));
              }
              return _results;
            })()
          });
        case "toplevel":
          _ = o[0], ln = o[1], stats = o[2];
          return node("script-context", ln, {
            stats: (function() {
              var _i, _len, _results;
              _results = [];
              for (_i = 0, _len = stats.length; _i < _len; _i++) {
                x = stats[_i];
                _results.push(genAst(x));
              }
              return _results;
            })()
          });
        case "block":
          _ = o[0], ln = o[1], stats = o[2];
          stats = stats || [];
          return node("block-stat", ln, {
            stats: (function() {
              var _i, _len, _results;
              _results = [];
              for (_i = 0, _len = stats.length; _i < _len; _i++) {
                x = stats[_i];
                _results.push(genAst(x));
              }
              return _results;
            })()
          });
        case "stat":
          _ = o[0], ln = o[1], form = o[2];
          return node("expr-stat", ln, {
            expr: genAst(form)
          });
        case "label":
          _ = o[0], ln = o[1], name = o[2], form = o[3];
          return node("label-stat", ln, {
            name: name,
            stat: genAst(form)
          });
        case "if":
          _ = o[0], ln = o[1], test = o[2], thn = o[3], els = o[4];
          return node("if-stat", ln, {
            expr: genAst(test),
            thenStat: genAst(thn),
            elseStat: (els ? genAst(els) : null)
          });
        case "var":
          _ = o[0], ln = o[1], bindings = o[2];
          return node("var-stat", ln, {
            vars: (function() {
              var _i, _len, _ref, _results;
              _results = [];
              for (_i = 0, _len = bindings.length; _i < _len; _i++) {
                _ref = bindings[_i], k = _ref[0], v = _ref[1];
                _results.push({
                  value: k,
                  expr: (v ? genAst(v) : null)
                });
              }
              return _results;
            })()
          });
        case "defun":
          _ = o[0], ln = o[1], name = o[2], args = o[3], stats = o[4];
          return node("defn-stat", ln, {
            closure: node("closure-context", ln, {
              name: name,
              args: args,
              stats: (function() {
                var _i, _len, _results;
                _results = [];
                for (_i = 0, _len = stats.length; _i < _len; _i++) {
                  x = stats[_i];
                  _results.push(genAst(x));
                }
                return _results;
              })()
            })
          });
        case "return":
          _ = o[0], ln = o[1], value = o[2];
          return node("ret-stat", ln, {
            expr: (value ? genAst(value) : null)
          });
        case "try":
          _ = o[0], ln = o[1], body = o[2], ctch = o[3], fnlly = o[4];
          return node("try-stat", ln, {
            tryStat: node("block-stat", ln, {
              stats: (function() {
                var _i, _len, _results;
                _results = [];
                for (_i = 0, _len = body.length; _i < _len; _i++) {
                  x = body[_i];
                  _results.push(genAst(x));
                }
                return _results;
              })()
            }),
            catchBlock: (ctch ? ((label = ctch[0], stats = ctch[1], ctch), {
              value: label,
              stat: node("block-stat", ln, {
                stats: (function() {
                  var _i, _len, _results;
                  _results = [];
                  for (_i = 0, _len = stats.length; _i < _len; _i++) {
                    x = stats[_i];
                    _results.push(genAst(x));
                  }
                  return _results;
                })()
              })
            }) : void 0),
            finallyStat: (fnlly ? node("block-stat", ln, {
              stats: (function() {
                var _i, _len, _results;
                _results = [];
                for (_i = 0, _len = fnlly.length; _i < _len; _i++) {
                  x = fnlly[_i];
                  _results.push(genAst(x));
                }
                return _results;
              })()
            }) : void 0)
          });
        case "throw":
          _ = o[0], ln = o[1], expr = o[2];
          return node("throw-stat", ln, {
            expr: genAst(expr)
          });
        case "break":
          _ = o[0], ln = o[1], label = o[2];
          return node("break-stat", ln, {
            label: label
          });
        case "continue":
          _ = o[0], ln = o[1], label = o[2];
          return node("continue-stat", ln, {
            label: label
          });
        case "while":
          _ = o[0], ln = o[1], cond = o[2], body = o[3];
          return node("while-stat", ln, {
            expr: genAst(cond),
            stat: genAst(body)
          });
        case "do":
          _ = o[0], ln = o[1], cond = o[2], body = o[3];
          return node("do-while-stat", ln, {
            expr: genAst(cond),
            stat: genAst(body)
          });
        case "for":
          _ = o[0], ln = o[1], init = o[2], cond = o[3], step = o[4], body = o[5];
          return node("for-stat", ln, {
            init: init ? genAst(init) : null,
            expr: cond ? genAst(cond) : null,
            step: step ? genAst(step) : null,
            stat: body ? genAst(body) : []
          });
        case "for-in":
          _ = o[0], ln = o[1], vari = o[2], name = o[3], obj = o[4], body = o[5];
          return node("for-in-stat", ln, {
            isvar: vari,
            value: name,
            expr: genAst(obj),
            stat: genAst(body)
          });
        case "switch":
          _ = o[0], ln = o[1], val = o[2], body = o[3];
          return node("switch-stat", ln, {
            expr: genAst(val),
            cases: (function() {
              var _i, _len, _ref, _results;
              _results = [];
              for (_i = 0, _len = body.length; _i < _len; _i++) {
                _ref = body[_i], cse = _ref[0], stats = _ref[1];
                _results.push({
                  match: (cse ? genAst(cse) : null),
                  stat: node("block-stat", ln, {
                    stats: (function() {
                      var _j, _len2, _results2;
                      _results2 = [];
                      for (_j = 0, _len2 = stats.length; _j < _len2; _j++) {
                        x = stats[_j];
                        _results2.push(genAst(x));
                      }
                      return _results2;
                    })()
                  })
                });
              }
              return _results;
            })()
          });
        default:
          return console.log("[ERROR] Can't generate AST for node \"" + o[0] + "\"");
      }
    };
    return jast.parse = function(str) {
      var ast;
      try {
        ast = preparser.parse(str);
      } catch (e) {
        throw new Error("Parsing error: " + e);
      }
      return genAst(ast);
    };
  };
}).call(this);
