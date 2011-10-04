(function() {
  var __slice = Array.prototype.slice, __bind = function(fn, me){ return function(){ return fn.apply(me, arguments); }; }, __indexOf = Array.prototype.indexOf || function(item) {
    for (var i = 0, l = this.length; i < l; i++) {
      if (this[i] === item) return i;
    }
    return -1;
  };
  exports.populate = function(jast) {
    var set;
    set = jast.set = function() {
      var a, args, r, v, _i, _len, _ref;
      args = 1 <= arguments.length ? __slice.call(arguments, 0) : [];
      a = (_ref = []).concat.apply(_ref, args);
      r = [];
      for (_i = 0, _len = a.length; _i < _len; _i++) {
        v = a[_i];
        if (r.indexOf(v) === -1) {
          r.push(v);
        }
      }
      return r;
    };
    jast.walk = function(n, f) {
      var check;
      if (f == null) {
        f = jast.walk;
      }
      check = __bind(function(type, value) {
        var prop, ptype, pvalue, _ref, _ref2;
        if (!value) {
          return [];
        } else if (type === jast.Node) {
          return f(value);
        } else if ((type != null ? type.constructor : void 0) === Array) {
          return (_ref = []).concat.apply(_ref, (function() {
            var _i, _len, _results;
            _results = [];
            for (_i = 0, _len = value.length; _i < _len; _i++) {
              pvalue = value[_i];
              _results.push(check(type[0], pvalue));
            }
            return _results;
          })());
        } else if (typeof type === 'object') {
          return (_ref2 = []).concat.apply(_ref2, (function() {
            var _results;
            _results = [];
            for (prop in type) {
              ptype = type[prop];
              if (value[prop]) {
                _results.push(check(ptype, value[prop]));
              }
            }
            return _results;
          })());
        } else {
          return [];
        }
      }, this);
      return check(jast.types[n.type], n);
    };
    jast.strings = function(n) {
      var check;
      check = function(n) {
        if (n.type === "str-literal") {
          return [n.value];
        }
        return jast.walk(n, check);
      };
      return set(check(n));
    };
    jast.numbers = function(n) {
      var check;
      check = function(n) {
        if (n.type === "num-literal") {
          return [n.value];
        }
        return jast.walk(n, check);
      };
      return set(check(n));
    };
    jast.regexps = function(n) {
      var check;
      check = function(n) {
        if (n.type === "regexp-literal") {
          return [[n.expr, n.flags]];
        }
        return jast.walk(n, check);
      };
      return set(check(n));
    };
    jast.contexts = function(ctx, f) {
      var x, _ref;
      if (f == null) {
        f = jast.contexts;
      }
      if (jast.isContext(ctx)) {
        return (_ref = [ctx]).concat.apply(_ref, (function() {
          var _i, _len, _ref, _results;
          _ref = ctx.stats;
          _results = [];
          for (_i = 0, _len = _ref.length; _i < _len; _i++) {
            x = _ref[_i];
            _results.push(f(x));
          }
          return _results;
        })());
      }
      return jast.walk(ctx, f);
    };
    jast.childContexts = function(ctx) {
      var check;
      check = function(n) {
        var _ref;
        if ((_ref = n.type) === "script-context" || _ref === "closure-context") {
          return [n];
        }
        return jast.walk(n, check);
      };
      return jast.walk(ctx, check);
    };
    jast.vars = function(n, f) {
      var v, x, _ref, _ref2;
      if (f == null) {
        f = jast.vars;
      }
      switch (n.type) {
        case "closure-context":
          return (_ref = (n.name != null ? [n.name] : [])).concat.apply(_ref, [n.args].concat(__slice.call((function() {
            var _i, _len, _ref, _results;
            _ref = n.stats;
            _results = [];
            for (_i = 0, _len = _ref.length; _i < _len; _i++) {
              x = _ref[_i];
              _results.push(f(x));
            }
            return _results;
          })())));
        case "var-stat":
          return (_ref2 = (function() {
            var _i, _len, _ref3, _results;
            _ref3 = n.vars;
            _results = [];
            for (_i = 0, _len = _ref3.length; _i < _len; _i++) {
              v = _ref3[_i];
              _results.push(v.value);
            }
            return _results;
          })()).concat.apply(_ref2, (function() {
            var _i, _len, _ref2, _results;
            _ref2 = n.vars;
            _results = [];
            for (_i = 0, _len = _ref2.length; _i < _len; _i++) {
              v = _ref2[_i];
              if (v.expr) {
                _results.push(f(v.expr));
              }
            }
            return _results;
          })());
        case "for-in-stat":
          return (n.isvar ? [n.value] : []).concat(f(n.stat));
        case "try-stat":
          return f(n.tryStat).concat((n.catchBlock ? [n.catchBlock.value].concat(f(n.catchBlock.stat)) : []), (n.finallyStat ? f(n.finallyStat) : []));
        case "scope-ref-expr":
          if (n.value === "arguments") {
            return ["arguments"];
          } else {
            return [];
          }
        case "defn-stat":
          if (n.closure.name != null) {
            return [n.closure.name];
          } else {
            return [];
          }
        default:
          return jast.walk(n, f);
      }
    };
    jast.localVars = function(ctx) {
      var check;
      check = function(n) {
        var _ref;
        if ((_ref = n.type) === "script-context" || _ref === "closure-context") {
          return [];
        }
        return jast.vars(n, check);
      };
      return set(jast.vars(ctx, check));
    };
    jast.usesArguments = function(closure) {
      return __indexOf.call(jast.localVars(closure), "arguments") >= 0;
    };
    jast.refs = function(n, f) {
      if (f == null) {
        f = jast.refs;
      }
      switch (n.type) {
        case "scope-ref-expr":
        case "scope-delete-expr":
          return [n.value];
        case "scope-assign-expr":
          return [n.value].concat(f(n.expr));
        default:
          return jast.walk(n, f);
      }
    };
    jast.localRefs = function(ctx) {
      var check;
      check = function(n) {
        var _ref;
        if ((_ref = n.type) === "script-context" || _ref === "closure-context") {
          return [];
        }
        return jast.refs(n, check);
      };
      return set(jast.refs(ctx, check));
    };
    jast.localUndefinedRefs = function(ctx) {
      var k, refs, vars;
      refs = jast.localRefs(ctx);
      vars = jast.localVars(ctx);
      return set((function() {
        var _i, _len, _results;
        _results = [];
        for (_i = 0, _len = refs.length; _i < _len; _i++) {
          k = refs[_i];
          if (__indexOf.call(vars, k) < 0) {
            _results.push(k);
          }
        }
        return _results;
      })());
    };
    return jast.undefinedRefs = function(ctx) {
      var k, refs, vars;
      refs = jast.refs(ctx);
      vars = jast.vars(ctx);
      return set((function() {
        var _i, _len, _results;
        _results = [];
        for (_i = 0, _len = refs.length; _i < _len; _i++) {
          k = refs[_i];
          if (__indexOf.call(vars, k) < 0) {
            _results.push(k);
          }
        }
        return _results;
      })());
    };
  };
}).call(this);
