;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; mug.parser
;
; A post-processor for parse-js.js to convert our JSON parse
; tree into our custom AST structure (defined in mug.ast)
;

(ns mug.parser
  (:gen-class)
  (:use clojure.set mug.ast)
  (:require [clojure.contrib.json :as json])
  (:import [org.mozilla.javascript Context Scriptable ScriptableObject]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; ast generation
;

(defmulti gen-ast-code (fn [node & args] (first node)))
(defmethod gen-ast-code :default [[type ln & _] & args]
  (println (str "###Error: No AST generator found for type " type "(line " ln ")")))

(defmethod gen-ast-code "atom" [[_ ln value] input]
  (case value
    "true" (boolean-literal ln true)
    "false" (boolean-literal ln false)
    "this" (this-expr ln)
    "null" (null-literal ln)))
(defmethod gen-ast-code "num" [[_ ln value] input]
  (num-literal ln value))
(defmethod gen-ast-code "string" [[_ ln value] input]
  (str-literal ln value))
(defmethod gen-ast-code "name" [[_ ln value] input]
  (case value
    "true" (boolean-literal ln true)
    "false" (boolean-literal ln false)
    "this" (this-expr ln)
    "null" (null-literal ln)
    "undefined" (undef-literal ln)
    (scope-ref-expr ln value)))
(defmethod gen-ast-code "array" [[_ ln elems] input]
  (array-literal ln (map #(gen-ast-code % input) elems)))
(defmethod gen-ast-code "object" [[_ ln props] input]
  (obj-literal ln (zipmap (map first props) (map #(gen-ast-code (second %) input) props))))
(defmethod gen-ast-code "regexp" [[_ ln expr flags] input]
  (regexp-literal ln expr flags))

(defmethod gen-ast-code "assign" [[_ ln op place val] input]
  (let [val (if (not= op true) (list "binary" ln op place val) val)]
    (case (first place)
      "name" (scope-assign-expr ln ((vec place) 2) (gen-ast-code val input))
      "dot" (static-assign-expr ln (gen-ast-code ((vec place) 2) input) ((vec place) 3) (gen-ast-code val input))
      "sub" (dyn-assign-expr ln (gen-ast-code ((vec place) 2) input) (gen-ast-code ((vec place) 3) input) (gen-ast-code val input))
      (println (str "###ERROR: Unrecognized assignment: " (first place) "(line " ln ")")))))
(defmethod gen-ast-code "binary" [[_ ln op lhs rhs] input]
  (({"+" add-op-expr
	  "-" sub-op-expr
	  "*" mul-op-expr
	  "/" div-op-expr
    "%" mod-op-expr
	  "<" lt-op-expr
	  ">" gt-op-expr
	  "<=" lte-op-expr
	  ">=" gte-op-expr
	  "==" eq-op-expr
	  "===" eqs-op-expr
	  "!=" neq-op-expr
	  "!==" neqs-op-expr
    "||" or-op-expr
    "&&" and-op-expr
	  "<<" lsh-op-expr
    ">>" rsh-op-expr
    "&" bit-or-op-expr
    "|" bit-or-op-expr
    "^" bit-xor-op-expr
    "instanceof" instanceof-op-expr
    "in" in-op-expr} op) ln (gen-ast-code lhs input) (gen-ast-code rhs input)))
(defmethod gen-ast-code "unary-postfix" [[_ ln op place] input]
  (case op
    "++" (gen-ast-code (list "binary" ln "-" (list "assign" ln "+" place (list "num" ln 1)) (list "num" ln 1)) input)
    "--" (gen-ast-code (list "binary" ln "+" (list "assign" ln "-" place (list "num" ln 1)) (list "num" ln 1)) input)
    (println (str "###ERROR: Bad unary postfix: " op) "(line " ln ")")))
(defmethod gen-ast-code "unary-prefix" [[_ ln op place] input]
  (case op
    "+" (num-op-expr ln (gen-ast-code place input))
    "-" (neg-op-expr ln (gen-ast-code place input))
    "~" (bit-not-op-expr ln (gen-ast-code place input))
    "++" (gen-ast-code (list "assign" ln "+" place (list "num" ln 1)) input)
    "--" (gen-ast-code (list "assign" ln "-" place (list "num" ln 1)) input)
    "!" (not-op-expr ln (gen-ast-code place input))
    "typeof" (typeof-expr ln (gen-ast-code place input))
    (println (str "###ERROR: Bad unary prefix: " op "(line " ln ")"))))
(defmethod gen-ast-code "call" [[_ ln func args] input]
  (case (first func)
    "dot"
      (let [[_ ln base value] func]
        (static-method-call-expr ln (gen-ast-code base input) value (map #(gen-ast-code %1 input) args)))
    (call-expr ln (gen-ast-code func input) (map #(gen-ast-code %1 input) args))))
(defmethod gen-ast-code "dot" [[_ ln obj attr] input]
  (static-ref-expr ln (gen-ast-code obj input) attr))
(defmethod gen-ast-code "sub" [[_ ln obj attr] input]
  (dyn-ref-expr ln (gen-ast-code obj input) (gen-ast-code attr input)))
(defmethod gen-ast-code "seq" [[_ ln form1 result] input]
  (seq-expr ln (gen-ast-code form1 input) (gen-ast-code result input)))
(defmethod gen-ast-code "conditional" [[_ ln test then else] input]
  (if-expr ln (gen-ast-code test input)
    (gen-ast-code then input)
    (gen-ast-code else input)))
(defmethod gen-ast-code "function" [node input]
  (let [[_ ln name args stats] node]
    (func-literal ln (closure-context ln name args (map #(gen-ast-code % input) stats)))))
(defmethod gen-ast-code "new" [[_ ln func args] input]
  (new-expr ln (gen-ast-code func input) (map #(gen-ast-code %1 input) args)))

(defmethod gen-ast-code "toplevel" [node input]
  (let [[_ ln stats] node]
    (script-context ln (map #(gen-ast-code % input) stats))))
(defmethod gen-ast-code "block" [[_ ln stats] input]
  (block-stat ln (map #(gen-ast-code %1 input) stats)))
(defmethod gen-ast-code "stat" [[_ ln form] input]
  (case _
    ;"stat" (gen-ast-code (form) input)
    "if" (gen-ast-code (form) input)
    ;"new" (gen-ast-code (form) input)
    (expr-stat ln (gen-ast-code form input))))
;(defmethod gen-ast-code "label" [[_ ln name form]])
(defmethod gen-ast-code "if" [[_ ln test then else] input]
  (if-stat ln (gen-ast-code test input)
    (gen-ast-code then input)
    (if else (gen-ast-code else input) else)))
;(defmethod gen-ast-code "with" [[_ ln obj body]])
(defmethod gen-ast-code "var" [[_ ln bindings] input]
  (var-stat ln (vec (map (fn [[k v]] [k (if v (gen-ast-code v input) nil)]) bindings))))
(defmethod gen-ast-code "defun" [node input]
  (let [[_ ln name args stats] node]
    (defn-stat ln (closure-context ln name args (map #(gen-ast-code % input) stats)))))
(defmethod gen-ast-code "return" [[_ ln value] input]
  (ret-stat ln (if value (gen-ast-code value input) nil)))
;(defmethod gen-ast-code "debugger" [[_ ln] input])
(defmethod gen-ast-code "try" [[_ ln body catch finally] input]
  (try-stat ln
    (map #(gen-ast-code % input) body)
    (when-let [[label stats] catch]
      [label (map #(gen-ast-code % input) stats)])
    (when-let [stats finally]
      (map #(gen-ast-code % input) stats))))
(defmethod gen-ast-code "throw" [[_ ln expr] input]
  (throw-stat ln (gen-ast-code expr input)))
(defmethod gen-ast-code "break" [[_ ln label] input]
  (break-stat ln label))
(defmethod gen-ast-code "continue" [[_ ln label] input]
  (continue-stat ln label))
(defmethod gen-ast-code "while" [[_ ln cond body] input]
  (while-stat ln (gen-ast-code cond input) (gen-ast-code body input)))
(defmethod gen-ast-code "do" [[_ ln cond body] input]
  (do-while-stat ln (gen-ast-code cond input) (gen-ast-code body input)))
(defmethod gen-ast-code "for" [[_ ln init cond step body] input]
  (for-stat ln
    (when init (gen-ast-code init input))
    (when cond (gen-ast-code cond input))
    (when step (gen-ast-code step input))
    (if body (gen-ast-code body input) [])))
(defmethod gen-ast-code "for-in" [[_ ln var name obj body] input]
  (for-in-stat ln var name (gen-ast-code obj input) (gen-ast-code body input)))
(defmethod gen-ast-code "switch" [[_ ln val body] input]
  (switch-stat ln 
    (gen-ast-code val input)
	  (map (fn [[case stats]]
	    [(if (nil? case) nil (gen-ast-code case input))
	     (vec (map #(gen-ast-code % input) stats))]) body)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; ast generation
;

(defn parse-js-json [input]
  (try
    (let [cx (Context/enter)
          scope (. cx initStandardObjects)
          wrappedString (Context/javaToJS input scope)]
      (. cx evaluateString scope "var exports = {}" "<parse-script>" 1 nil)
      (. cx evaluateString scope (slurp (ClassLoader/getSystemResource "parse-js.js")) "parse-js.js" 1 nil)
      (. cx evaluateString scope (slurp (ClassLoader/getSystemResource "json2.js")) "parse-js.js" 1 nil)
      (ScriptableObject/putProperty scope "inputSource" wrappedString)
      (json/read-json (. cx evaluateString scope "JSON.stringify(exports.parse(inputSource))" "<parse-script>" 1 nil)))
    (catch Exception e
      (. e printStackTrace))
    (finally
      (Context/exit))))

;[TODO] not sure why gen-ast-code even takes a second argument
(defn parse-js-ast [input]
  (let [json (parse-js-json input)]
    (gen-ast-code json json)))