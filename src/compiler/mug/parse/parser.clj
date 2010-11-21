(ns mug.parse.parser
  (:use clojure.set mug.ast))

(import mug.parse.Parser)

(defn parse-js [x]
  (load-string (str "'" (Parser/parse x))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; input walking
;

; NOTE: not sure what's actually a rest parameter (& stat) and what isn't
; probably actually analyze this part some time

(defmulti walk-input (fn [node & args] (first node)) :default :no-match)
(defmethod walk-input :no-match [node & args]
  (println (str "No AST walker found for type " (first node) " (" node ")")))

(defmethod walk-input :atom [[_ atom] walker]
  (walker atom walker))
(defmethod walk-input :num [[_ value] walker]
  [])
(defmethod walk-input :string [[_ value] walker]
  [])
(defmethod walk-input :name [[_ name] walker]
  [])
(defmethod walk-input :array [[_ & elems] walker]
  (apply concat (map #(walker %1 walker) elems)))
(defmethod walk-input :object [[_ props] walker]
  
  (apply concat (map (fn [[k v]] (walker v walker)) props)))
(defmethod walk-input :regexp [[_ expr flags] walker]
  [])

(defmethod walk-input :assign [[_ op place val] walker]
  (concat (walker place walker) (walker val walker)))
(defmethod walk-input :binary [[_ op lhs rhs] walker]
  (concat (walker lhs walker) (walker rhs walker)))
(defmethod walk-input :unary-postfix [[_ op place] walker]
  (walker place walker))
(defmethod walk-input :unary-prefix [[_ op place] walker]
  (walker place walker))
(defmethod walk-input :call [[_ func args] walker]
  (apply concat (conj (map #(walker %1 walker) args) (walker func walker))))
(defmethod walk-input :dot [[_ obj attr] walker]
  (walker obj walker))
(defmethod walk-input :sub [[_ obj attr] walker]
  (concat (walker obj walker) (walker attr walker)))
(defmethod walk-input :seq [[_ form1 result] walker]
  (concat (walker form1 walker) (walker result walker)))
(defmethod walk-input :conditional [[_ test then else] walker]
  (concat (walker test walker) (walker then walker) (walker else walker)))
(defmethod walk-input :function [[_ name args & stat] walker]
  (apply concat (map #(walker %1 walker) stat)))
(defmethod walk-input :new [[_ func args] walker]
  (apply concat (conj (map #(walker %1 walker) args) (walker func walker))))

(defmethod walk-input :toplevel [[_ & stat] walker]
  (apply concat (map #(walker %1 walker) stat)))
(defmethod walk-input :block [[_ & stat] walker]
  (apply concat (map #(walker %1 walker) stat)))
(defmethod walk-input :stat [[_ form] walker]
  (walker form walker))
(defmethod walk-input :label [[_ name form] walker]
  (walker form walker))
(defmethod walk-input :if [[_ test then else] walker]
  (concat (walker test walker) (walker then walker) (if else (walker else walker) [])))
(defmethod walk-input :with [[_ obj body] walker]
  (apply concat (conj (map #(walker %1 walker) body) (walker obj walker))))
(defmethod walk-input :var [[_ bindings] walker]
  (apply concat (map (fn [[k v]] (walker v walker)) bindings)))
(defmethod walk-input :defun [[_ name args & stat] walker]
  (apply concat (map #(walker %1 walker) stat)))
(defmethod walk-input :return [[_ value] walker]
  (if (nil? value) #{} (walker value walker)))
(defmethod walk-input :debugger [[_] walker]
  [])

(defmethod walk-input :try [[_ body catch finally] walker] )
(defmethod walk-input :throw [[_ expr] walker] )

(defmethod walk-input :break [[_ label] walker] )
(defmethod walk-input :continue [[_ label] walker] )
(defmethod walk-input :while [[_ cond body] walker]
  (concat (walker body walker) (walker cond walker)))
(defmethod walk-input :do [[_ cond body] walker]
  (concat (walker body walker) (walker cond walker)))
(defmethod walk-input :for [[_ init cond step body] walker]
  (concat (walker init walker) (walker cond walker) (walker step walker) (walker body walker)))
(defmethod walk-input :for-in [[_ var name obj body] walker]
  (concat (walker obj walker) (walker body walker)))
(defmethod walk-input :switch [[_ val body] walker] )
(defmethod walk-input :case [[_ expr] walker] )
(defmethod walk-input :default [[_] walker] )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; input analysis
;

; strings

(defmulti find-strings (fn [node & args] (first node)) :default :no-match)
(defmethod find-strings :no-match [node & args]
  (walk-input node find-strings))
(defmethod find-strings :string [[_ value] walker]
  [value])

; numbers

(defmulti find-numbers (fn [node & args] (first node)) :default :no-match)
(defmethod find-numbers :no-match [node & args]
  (walk-input node find-numbers))
(defmethod find-numbers :num [[_ value] walker]
  [value])
;[TODO] if ++, --, etc.
(defmethod find-numbers :unary-postfix [[_ op place] walker]
  (case op
    "++" [1]
    "--" [1]
    (walk-input (list _ op place) find-numbers)))
(defmethod find-numbers :unary-prefix [[_ op place] walker]
  (case op
    "++" [1]
    "--" [1]
    (walk-input (list _ op place) find-numbers)))

; regexes

(defmulti find-regexes (fn [node & args] (first node)) :default :no-match)
(defmethod find-regexes :no-match [node & args]
  (walk-input node find-regexes))
(defmethod find-regexes :regexp [[_ expr flags] walker]
  [[expr flags]])

; accessors

(defmulti find-accessors (fn [node & args] (first node)) :default :no-match)
(defmethod find-accessors :no-match [node & args]
  (walk-input node find-accessors))
(defmethod find-accessors :dot [[_ obj attr] walker]
  (concat [attr] (find-accessors obj find-accessors)))

; variables

(defn find-vars-in-scope [context]
  ; mock a toplevel context so we don't miss in-function definitions
  (defmulti mock-toplevel first)
  (defmethod mock-toplevel :toplevel [node] node)
  ;NOTE: this sucks, also, doesn't include defun function names
  (defmethod mock-toplevel :function [[_ name args & stat]]
    (conj (apply list stat) :toplevel))
  (defmethod mock-toplevel :defun [[_ name args & stat]]
    (conj (apply list stat) :toplevel))
  
  (defmulti vars-in-scope-walker (fn [node & args] (first node)) :default :no-match)
	(defmethod vars-in-scope-walker :no-match [node & args]
	  (walk-input node vars-in-scope-walker))
	(defmethod vars-in-scope-walker :function [[_ name args & stat] walker]
	  [])
	(defmethod vars-in-scope-walker :defun [[_ name args & stat] walker]
	  [name])
	(defmethod vars-in-scope-walker :var [[_ bindings] walker]
	  (vec (map (fn [[k v]] (name k)) bindings)))
  (defmethod vars-in-scope-walker :for-in [[_ var name obj body] walker]
    (if var [name] []))
 
  (set (vars-in-scope-walker (mock-toplevel context) vars-in-scope-walker)))

(def *reserved-words* #{"typeof" "null" "this" "true" "false"})

(defn find-globals [context & [parent-vars]]
  (def vars (union (or parent-vars #{}) (find-vars-in-scope context)))

	(defmulti globals-walker (fn [node & args] (first node)) :default :no-match)
	(defmethod globals-walker :no-match [node & args]
	  (walk-input node globals-walker))
	(defmethod globals-walker :function [[_ name args & stats] walker]
    (find-globals (concat (list :toplevel) stats) (union vars args)))
	(defmethod globals-walker :defun [[_ name args & stats] walker]
	  (find-globals (concat (list :toplevel) stats) (union vars args)))
	(defmethod globals-walker :name [[_ ident] walker]
	  (if (contains? (union vars *reserved-words*) ident) [] [ident]))

  (set (globals-walker context globals-walker)))

; find context parents

(defn find-context-info [toplevel & [parents]]
	(defmulti contexts-walker (fn [node & args] (first node)) :default :no-match)
	(defmethod contexts-walker :no-match [node parents walker]
	  (walk-input node walker))
	(defmethod contexts-walker :toplevel [node parents walker]
	  (let [[_ & stats] node]
	    (concat
	      [[node parents]]
	      (apply concat (map #(find-context-info %1 (conj (or parents []) node)) stats)))))
	(defmethod contexts-walker :function [node parents walker]
	  (let [[_ name args & stats] node]
	    (concat
	      [[node parents]]
	      (apply concat (map #(find-context-info %1 (conj (or parents []) node)) stats)))))
	(defmethod contexts-walker :defun [node parents walker]
	  (let [[_ name args & stats] node]
	    (concat
	      [[node parents]]
	    (apply concat (map #(find-context-info %1 (conj (or parents []) node)) stats)))))
 
  (contexts-walker toplevel parents #(contexts-walker %1 parents %2)))

; contexts

(defn find-contexts [toplevel]
  (let [info (find-context-info toplevel)]
    (map #(%1 0) info)))

(defn find-context-index [context toplevel]
  (let [info (find-context-info toplevel)]
    ((into {} (map vector (map #(%1 0) info) (iterate inc 0))) context)))

(defn find-context-parents [context toplevel]
  (let [info (find-context-info toplevel)]
    (vec (map #(find-context-index %1 toplevel) (((vec info) (find-context-index context toplevel)) 1)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; code generation
;

; code

(defmulti gen-ast-code (fn [node & args] (first node)) :default :no-match)
(defmethod gen-ast-code :no-match [node & args]
  (println (str "###Error: No AST generator found for type " (first node))))

(defmethod gen-ast-code :atom [[_ value] input]
  (case value
    "true" (boolean-literal true)
    "false" (boolean-literal false)
    "this" (this-expr)
    "null" (null-literal)))
(defmethod gen-ast-code :num [[_ value] input]
  (num-literal value))
(defmethod gen-ast-code :string [[_ value] input]
  (str-literal value))
(defmethod gen-ast-code :name [[_ value] input]
  (case value
    "true" (boolean-literal true)
    "false" (boolean-literal false)
    "this" (this-expr)
    "null" (null-literal)
    (scope-ref-expr value)))
(defmethod gen-ast-code :array [[_ & elems] input]
  (array-literal (map #(gen-ast-code % input) elems)))
(defmethod gen-ast-code :object [[_ props] input]
  (obj-literal (zipmap (keys props) (map #(gen-ast-code % input) (vals props)))))
(defmethod gen-ast-code :regexp [[_ expr flags] input]
  (regex-literal expr flags))

(defmethod gen-ast-code :assign [[_ op place val] input]
  (let [val (if (not= op true) (list :binary op place val) val)]
    (case (first place)
      :name (scope-assign-expr ((vec place) 1) (gen-ast-code val input))
      :dot (static-assign-expr (gen-ast-code ((vec place) 1) input) ((vec place) 2) (gen-ast-code val input))
      :sub (dyn-assign-expr (gen-ast-code ((vec place) 1) input) (gen-ast-code ((vec place) 2) input) (gen-ast-code val input))
      :else (println (str "###ERROR: Unrecognized assignment: " (first place))))))
(defmethod gen-ast-code :binary [[_ op lhs rhs] input]
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
(defmethod gen-ast-code :unary-postfix [[_ op place] input]
  (case op
    "++" (gen-ast-code (list :binary "-" (list :assign "+" place (list :num 1)) (list :num 1)) input)
    "--" (gen-ast-code (list :binary "+" (list :assign "-" place (list :num 1)) (list :num 1)) input)
    (println (str "###ERROR: Bad unary postfix: " op))))
(defmethod gen-ast-code :unary-prefix [[_ op place] input]
  (case op
    "+" (num-op-expr (gen-ast-code place input))
    "-" (neg-op-expr (gen-ast-code place input))
    "++" (gen-ast-code (list :assign "+" place (list :num 1)) input)
    "--" (gen-ast-code (list :assign "-" place (list :num 1)) input)
    "!" (not-op-expr (gen-ast-code place input))
    "typeof" (typeof-expr (gen-ast-code place input))
    (println (str "###ERROR: Bad unary prefix: " op))))
(defmethod gen-ast-code :call [[_ func args] input]
  (case (first func)
    :name (apply call-expr (into [(gen-ast-code func input)] (map #(gen-ast-code %1 input) args)))
    :dot
      (let [[_ base value] func]
        (apply static-method-call-expr (into [(gen-ast-code base input) value] (map #(gen-ast-code %1 input) args))))
    (println (str "###ERROR: Unrecognized call format: " (first func)))))
(defmethod gen-ast-code :dot [[_ obj attr] input]
  (static-ref-expr (gen-ast-code obj input) attr))
(defmethod gen-ast-code :sub [[_ obj attr] input]
  (dyn-ref-expr (gen-ast-code obj input) (gen-ast-code attr input)))
(defmethod gen-ast-code :seq [[_ form1 result] input]
  (seq-expr (gen-ast-code form1 input) (gen-ast-code result input)))
(defmethod gen-ast-code :conditional [[_ test then else] input]
  (if-expr (gen-ast-code test input)
    (gen-ast-code then input)
    (gen-ast-code else input)))
(defmethod gen-ast-code :function [node input]
  (func-literal (find-context-index node input)))
(defmethod gen-ast-code :new [[_ func args] input]
  (apply new-expr (concat [(gen-ast-code func input)] (map #(gen-ast-code %1 input) args))))

;(defmethod gen-ast-code :toplevel [context])
(defmethod gen-ast-code :block [[_ & stats] input]
  (apply block-stat (map #(gen-ast-code %1 input) stats)))
(defmethod gen-ast-code :stat [[_ form] input]
  (case _
    ;:stat (gen-ast-code (form) input)
    :if (gen-ast-code (form) input)
    ;:new (gen-ast-code (form) input)
    (expr-stat (gen-ast-code form input))))
;(defmethod gen-ast-code :label [[_ name form]])
(defmethod gen-ast-code :if [[_ test then else] input]
  (if-stat (gen-ast-code test input)
    (gen-ast-code then input)
    (if else (gen-ast-code else input) else)))
;(defmethod gen-ast-code :with [[_ obj body]])
(defmethod gen-ast-code :var [[_ bindings] input]
  (apply block-stat
    (map (fn [[k v]] (expr-stat (scope-assign-expr (name k) (gen-ast-code v input))))
      (filter (fn [[k v]] (not (nil? v))) bindings))))
(defmethod gen-ast-code :defun [node input]
  (expr-stat (scope-assign-expr ((vec node) 1) (func-literal (find-context-index node input)))))
(defmethod gen-ast-code :return [[_ value] input]
  (ret-stat (if value (gen-ast-code value input) nil)))
;(defmethod gen-ast-code :debugger [[_] input])

;(defmethod gen-ast-code :try [[_ body catch finally] input] )
;(defmethod gen-ast-code :throw [[_ expr] input] )

(defmethod gen-ast-code :break [[_ label] input]
  (break-stat label))
(defmethod gen-ast-code :continue [[_ label] input]
  (continue-stat label))
(defmethod gen-ast-code :while [[_ cond body] input]
  (while-stat (gen-ast-code cond input) (gen-ast-code body input)))
(defmethod gen-ast-code :do [[_ cond body] input]
  (do-while-stat (gen-ast-code cond input) (gen-ast-code body input)))
(defmethod gen-ast-code :for [[_ init cond step body] input]
  (for-stat
    (gen-ast-code init input)
    (gen-ast-code cond input)
    (gen-ast-code step input)
    (gen-ast-code body input)))
(defmethod gen-ast-code :for-in [[_ var name obj body] input]
  (for-in-stat name (gen-ast-code obj input) (gen-ast-code body input)))
;(defmethod gen-ast-code :switch [[_ val body] input] )
;(defmethod gen-ast-code :case [[_ expr] input] )
;(defmethod gen-ast-code :default [[_] input] )

;
; contexts
;

(defmulti gen-ast-context (fn [node & args] (first node)) :default :no-match)
(defmethod gen-ast-context :no-match [node & args]
  (println (str "No context generator found for type " (first node))))

(defmethod gen-ast-context :toplevel [context input]
  (let [[_ & stats] context]
    (apply script-context
      (into [(find-globals context) (find-vars-in-scope context)]
        (vec (map #(gen-ast-code %1 input) stats))))))
(defmethod gen-ast-context :function [context input]
  (let [[_ name args & stats] context]
    (apply closure-context
      (into [(find-context-parents context input) name (vec args) (find-vars-in-scope context)]
        (vec (map #(gen-ast-code %1 input) stats))))))
(defmethod gen-ast-context :defun [context input]
  (let [[_ name args & stats] context]
    (apply closure-context
      (into [(find-context-parents context input) name (vec args) (find-vars-in-scope context)]
        (vec (map #(gen-ast-code %1 input) stats))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; ast generation
;

(defn gen-ast [input]
  (js-ast
    (vec (map #(do (println %1) (println (gen-ast-context %1 input)) (gen-ast-context %1 input)) (find-contexts input)))
    []
    (set (find-accessors input))
    (set (find-numbers input))
    (set (find-strings input))
    (set (find-regexes input))))