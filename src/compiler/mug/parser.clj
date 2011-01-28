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

(defmethod gen-ast-code "atom" [[_ ln value]]
  (case value
    "true" (boolean-literal ln true)
    "false" (boolean-literal ln false)
    "this" (this-expr ln)
    "null" (null-literal ln)))
(defmethod gen-ast-code "num" [[_ ln value]]
  (num-literal ln value))
(defmethod gen-ast-code "string" [[_ ln value]]
  (str-literal ln value))
(defmethod gen-ast-code "name" [[_ ln value]]
  (case value
    "true" (boolean-literal ln true)
    "false" (boolean-literal ln false)
    "this" (this-expr ln)
    "null" (null-literal ln)
    "undefined" (undef-literal ln)
    (scope-ref-expr ln value)))
(defmethod gen-ast-code "array" [[_ ln elems]]
  (array-literal ln (map #(gen-ast-code %) elems)))
(defmethod gen-ast-code "object" [[_ ln props]]
  (obj-literal ln
    (map (fn [[k v & [type]]]
           [(if type (keyword type) :value) ; :get :set :value
            k
            (if type
						  (let [[_ ln name args stats] v]
						    (closure-context ln name args (map #(gen-ast-code %) stats)))
              (gen-ast-code v))]) props)))
(defmethod gen-ast-code "regexp" [[_ ln expr flags]]
  (regexp-literal ln expr flags))

(defmethod gen-ast-code "assign" [[_ ln op place val]]
  (let [val (if (not= op true) (list "binary" ln op place val) val)]
    (case (first place)
      "name" (scope-assign-expr ln ((vec place) 2) (gen-ast-code val))
      "dot" (static-assign-expr ln (gen-ast-code ((vec place) 2)) ((vec place) 3) (gen-ast-code val))
      "sub" (dyn-assign-expr ln (gen-ast-code ((vec place) 2)) (gen-ast-code ((vec place) 3)) (gen-ast-code val))
      (println (str "###ERROR: Unrecognized assignment: " (first place) "(line " ln ")")))))
(defmethod gen-ast-code "binary" [[_ ln op lhs rhs]]
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
    "in" in-op-expr} op) ln (gen-ast-code lhs) (gen-ast-code rhs)))
(defmethod gen-ast-code "unary-postfix" [[_ ln op place]]
  (case op
    "++" (gen-ast-code (list "binary" ln "-" (list "assign" ln "+" place (list "num" ln 1)) (list "num" ln 1)))
    "--" (gen-ast-code (list "binary" ln "+" (list "assign" ln "-" place (list "num" ln 1)) (list "num" ln 1)))
    (println (str "###ERROR: Bad unary postfix: " op) "(line " ln ")")))
(defmethod gen-ast-code "unary-prefix" [[_ ln op place]]
  (case op
    "+" (num-op-expr ln (gen-ast-code place))
    "-" (neg-op-expr ln (gen-ast-code place))
    "~" (bit-not-op-expr ln (gen-ast-code place))
    "++" (gen-ast-code (list "assign" ln "+" place (list "num" ln 1)))
    "--" (gen-ast-code (list "assign" ln "-" place (list "num" ln 1)))
    "!" (not-op-expr ln (gen-ast-code place))
    "void" (void-expr ln (gen-ast-code place))
    "typeof" (typeof-expr ln (gen-ast-code place))
    "delete" (case (first place)
      "name" (scope-delete-expr ln ((vec place) 2))
      "dot" (static-delete-expr ln (gen-ast-code ((vec place) 2)) ((vec place) 3))
      "sub" (dyn-delete-expr ln (gen-ast-code ((vec place) 2)) (gen-ast-code ((vec place) 3)))
      (println (str "###ERROR: Bad delete operation (line " ln ")")))
    (println (str "###ERROR: Bad unary prefix: " op "(line " ln ")"))))
(defmethod gen-ast-code "call" [[_ ln func args]]
  (case (first func)
    "dot"
      (let [[_ ln base value] func]
        (static-method-call-expr ln (gen-ast-code base) value (map #(gen-ast-code %1) args)))
    (call-expr ln (gen-ast-code func) (map #(gen-ast-code %1) args))))
(defmethod gen-ast-code "dot" [[_ ln obj attr]]
  (static-ref-expr ln (gen-ast-code obj) attr))
(defmethod gen-ast-code "sub" [[_ ln obj attr]]
  (dyn-ref-expr ln (gen-ast-code obj) (gen-ast-code attr)))
(defmethod gen-ast-code "seq" [[_ ln form1 result]]
  (seq-expr ln (gen-ast-code form1) (gen-ast-code result)))
(defmethod gen-ast-code "conditional" [[_ ln test then else]]
  (if-expr ln (gen-ast-code test)
    (gen-ast-code then)
    (gen-ast-code else)))
(defmethod gen-ast-code "function" [node]
  (let [[_ ln name args stats] node]
    (func-literal ln (closure-context ln name args (map #(gen-ast-code %) stats)))))
(defmethod gen-ast-code "new" [[_ ln func args]]
  (new-expr ln (gen-ast-code func) (map #(gen-ast-code %1) args)))

(defmethod gen-ast-code "toplevel" [node]
  (let [[_ ln stats] node]
    (script-context ln (map #(gen-ast-code %) stats))))
(defmethod gen-ast-code "block" [[_ ln stats]]
  (block-stat ln (map #(gen-ast-code %1) stats)))
(defmethod gen-ast-code "stat" [[_ ln form]]
  (case _
    ;"stat" (gen-ast-code (form))
    "if" (gen-ast-code (form))
    ;"new" (gen-ast-code (form))
    (expr-stat ln (gen-ast-code form))))
;(defmethod gen-ast-code "label" [[_ ln name form]])
(defmethod gen-ast-code "if" [[_ ln test then else]]
  (if-stat ln (gen-ast-code test)
    (gen-ast-code then)
    (if else (gen-ast-code else) else)))
;(defmethod gen-ast-code "with" [[_ ln obj body]])
(defmethod gen-ast-code "var" [[_ ln bindings]]
  (var-stat ln (vec (map (fn [[k v]] [k (if v (gen-ast-code v) nil)]) bindings))))
(defmethod gen-ast-code "defun" [node]
  (let [[_ ln name args stats] node]
    (defn-stat ln (closure-context ln name args (map #(gen-ast-code %) stats)))))
(defmethod gen-ast-code "return" [[_ ln value]]
  (ret-stat ln (if value (gen-ast-code value) nil)))
;(defmethod gen-ast-code "debugger" [[_ ln]])
(defmethod gen-ast-code "try" [[_ ln body catch finally]]
  (try-stat ln
    (map #(gen-ast-code %) body)
    (when-let [[label stats] catch]
      [label (map #(gen-ast-code %) stats)])
    (when-let [stats finally]
      (map #(gen-ast-code %) stats))))
(defmethod gen-ast-code "throw" [[_ ln expr]]
  (throw-stat ln (gen-ast-code expr)))
(defmethod gen-ast-code "break" [[_ ln label]]
  (break-stat ln label))
(defmethod gen-ast-code "continue" [[_ ln label]]
  (continue-stat ln label))
(defmethod gen-ast-code "while" [[_ ln cond body]]
  (while-stat ln (gen-ast-code cond) (gen-ast-code body)))
(defmethod gen-ast-code "do" [[_ ln cond body]]
  (do-while-stat ln (gen-ast-code cond) (gen-ast-code body)))
(defmethod gen-ast-code "for" [[_ ln init cond step body]]
  (for-stat ln
    (when init (gen-ast-code init))
    (when cond (gen-ast-code cond))
    (when step (gen-ast-code step))
    (if body (gen-ast-code body) [])))
(defmethod gen-ast-code "for-in" [[_ ln var name obj body]]
  (for-in-stat ln var name (gen-ast-code obj) (gen-ast-code body)))
(defmethod gen-ast-code "switch" [[_ ln val body]]
  (switch-stat ln 
    (gen-ast-code val)
	  (map (fn [[case stats]]
	    [(if (nil? case) nil (gen-ast-code case))
	     (vec (map #(gen-ast-code %) stats))]) body)))

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

(defn parse-js-ast [input]
  (let [json (parse-js-json input)]
    (gen-ast-code json)))