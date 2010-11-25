;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; mug.parser
;
; A post-processor for parse-js.js to convert our JSON parse
; tree into our custom AST structure (defined in mug.ast)
;

(ns mug.parser
  (:use clojure.set mug.ast)
  (:require [clojure.contrib.json :as json])
  (:import [org.mozilla.javascript Context Scriptable ScriptableObject]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; ast generation
;

(defmulti gen-ast-code (fn [node & args] (first node)))
(defmethod gen-ast-code :default [node & args]
  (println (str "###Error: No AST generator found for type " (first node))))

(defmethod gen-ast-code "atom" [[_ value] input]
  (case value
    "true" (boolean-literal true)
    "false" (boolean-literal false)
    "this" (this-expr)
    "null" (null-literal)))
(defmethod gen-ast-code "num" [[_ value] input]
  (num-literal value))
(defmethod gen-ast-code "string" [[_ value] input]
  (str-literal value))
(defmethod gen-ast-code "name" [[_ value] input]
  (case value
    "true" (boolean-literal true)
    "false" (boolean-literal false)
    "this" (this-expr)
    "null" (null-literal)
    (scope-ref-expr value)))
(defmethod gen-ast-code "array" [[_ elems] input]
  (array-literal (map #(gen-ast-code % input) elems)))
(defmethod gen-ast-code "object" [[_ props] input]
  (obj-literal (zipmap (map first props) (map #(gen-ast-code (second %) input) props))))
(defmethod gen-ast-code "regexp" [[_ expr flags] input]
  (regexp-literal expr flags))

(defmethod gen-ast-code "assign" [[_ op place val] input]
  (let [val (if (not= op true) (list "binary" op place val) val)]
    (case (first place)
      "name" (scope-assign-expr ((vec place) 1) (gen-ast-code val input))
      "dot" (static-assign-expr (gen-ast-code ((vec place) 1) input) ((vec place) 2) (gen-ast-code val input))
      "sub" (dyn-assign-expr (gen-ast-code ((vec place) 1) input) (gen-ast-code ((vec place) 2) input) (gen-ast-code val input))
      (println (str "###ERROR: Unrecognized assignment: " (first place))))))
(defmethod gen-ast-code "binary" [[_ op lhs rhs] input]
  (({"+" add-op-expr
	  "-" sub-op-expr
	  "*" mul-op-expr
	  "/" div-op-expr
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
	  "<<" lsh-op-expr} op) (gen-ast-code lhs input) (gen-ast-code rhs input)))
(defmethod gen-ast-code "unary-postfix" [[_ op place] input]
  (case op
    "++" (gen-ast-code (list "binary" "-" (list "assign" "+" place (list "num" 1)) (list "num" 1)) input)
    "--" (gen-ast-code (list "binary" "+" (list "assign" "-" place (list "num" 1)) (list "num" 1)) input)
    (println (str "###ERROR: Bad unary postfix: " op))))
(defmethod gen-ast-code "unary-prefix" [[_ op place] input]
  (case op
    "+" (num-op-expr (gen-ast-code place input))
    "-" (neg-op-expr (gen-ast-code place input))
    "++" (gen-ast-code (list "assign" "+" place (list "num" 1)) input)
    "--" (gen-ast-code (list "assign" "-" place (list "num" 1)) input)
    "!" (not-op-expr (gen-ast-code place input))
    "typeof" (typeof-expr (gen-ast-code place input))
    (println (str "###ERROR: Bad unary prefix: " op))))
(defmethod gen-ast-code "call" [[_ func args] input]
  (case (first func)
    "name" (call-expr (gen-ast-code func input) (map #(gen-ast-code %1 input) args))
    "dot"
      (let [[_ base value] func]
        (static-method-call-expr (gen-ast-code base input) value (map #(gen-ast-code %1 input) args)))
    (println (str "###ERROR: Unrecognized call format: " (first func)))))
(defmethod gen-ast-code "dot" [[_ obj attr] input]
  (static-ref-expr (gen-ast-code obj input) attr))
(defmethod gen-ast-code "sub" [[_ obj attr] input]
  (dyn-ref-expr (gen-ast-code obj input) (gen-ast-code attr input)))
(defmethod gen-ast-code "seq" [[_ form1 result] input]
  (seq-expr (gen-ast-code form1 input) (gen-ast-code result input)))
(defmethod gen-ast-code "conditional" [[_ test then else] input]
  (if-expr (gen-ast-code test input)
    (gen-ast-code then input)
    (gen-ast-code else input)))
(defmethod gen-ast-code "function" [node input]
  (let [[_ name args stats] node]
    (func-literal (closure-context name args (map #(gen-ast-code % input) stats)))))
(defmethod gen-ast-code "new" [[_ func args] input]
  (new-expr (gen-ast-code func input) (map #(gen-ast-code %1 input) args)))

(defmethod gen-ast-code "toplevel" [node input]
  (let [[_ stats] node]
    (script-context (map #(gen-ast-code % input) stats))))
(defmethod gen-ast-code "block" [[_ stats] input]
  (block-stat (map #(gen-ast-code %1 input) stats)))
(defmethod gen-ast-code "stat" [[_ form] input]
  (case _
    ;"stat" (gen-ast-code (form) input)
    "if" (gen-ast-code (form) input)
    ;"new" (gen-ast-code (form) input)
    (expr-stat (gen-ast-code form input))))
;(defmethod gen-ast-code "label" [[_ name form]])
(defmethod gen-ast-code "if" [[_ test then else] input]
  (if-stat (gen-ast-code test input)
    (gen-ast-code then input)
    (if else (gen-ast-code else input) else)))
;(defmethod gen-ast-code "with" [[_ obj body]])
(defmethod gen-ast-code "var" [[_ bindings] input]
  (var-stat (vec (map (fn [[k v]] [k (if v (gen-ast-code v input) nil)]) bindings))))
(defmethod gen-ast-code "defun" [node input]
  (let [[_ name args stats] node]
    (defn-stat (closure-context name args (map #(gen-ast-code % input) stats)))))
(defmethod gen-ast-code "return" [[_ value] input]
  (ret-stat (if value (gen-ast-code value input) nil)))
;(defmethod gen-ast-code "debugger" [[_] input])

;(defmethod gen-ast-code "try" [[_ body catch finally] input] )
;(defmethod gen-ast-code "throw" [[_ expr] input] )

(defmethod gen-ast-code "break" [[_ label] input]
  (break-stat label))
(defmethod gen-ast-code "continue" [[_ label] input]
  (continue-stat label))
(defmethod gen-ast-code "while" [[_ cond body] input]
  (while-stat (gen-ast-code cond input) (gen-ast-code body input)))
(defmethod gen-ast-code "do" [[_ cond body] input]
  (do-while-stat (gen-ast-code cond input) (gen-ast-code body input)))
(defmethod gen-ast-code "for" [[_ init cond step body] input]
  (for-stat
    (when init (gen-ast-code init input))
    (when cond (gen-ast-code cond input))
    (when step (gen-ast-code step input))
    (if body (gen-ast-code body input) [])))
(defmethod gen-ast-code "for-in" [[_ var name obj body] input]
  (for-in-stat var name (gen-ast-code obj input) (gen-ast-code body input)))
;(defmethod gen-ast-code "switch" [[_ val body] input] )
;(defmethod gen-ast-code "case" [[_ expr] input] )
;(defmethod gen-ast-code "default" [[_] input] )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; ast generation
;

(defn parse-js-json [input]
  (try
    (let [cx (Context/enter)
          scope (. cx initStandardObjects)
          wrappedString (Context/javaToJS input scope)]
      (. cx evaluateString scope (slurp (ClassLoader/getSystemResource "parse-js.js")) "parse-js.js" 1 nil)
      (. cx evaluateString scope (slurp (ClassLoader/getSystemResource "json2.js")) "parse-js.js" 1 nil)
      (ScriptableObject/putProperty scope "inputSource" wrappedString)
      (json/read-json (. cx evaluateString scope "JSON.stringify(parse(inputSource))" "<parse-script>" 1 nil)))
    (catch Exception e
      (. e printStackTrace))
    (finally
      (Context/exit))))

;[TODO] not sure why gen-ast-code even takes a second argument
(defn parse-js-ast [input]
  (let [json (parse-js-json input)]
    (gen-ast-code json json)))