(ns mug.asm.util
  (:use
    mug.ast
    clojure.contrib.str-utils
    [clojure.set :only (difference)])
  (:import
    [java.io FileOutputStream File]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; utility functions
;
		
(defn index [coll]
	(map vector (iterate inc 0) coll))

(defn enumerate [coll]
	(map vector coll (iterate inc 0)))

(defn index-of [s x]
	((zipmap (vec s) (iterate inc 0)) x))

; writes bytes to path
; creating directories if they don't exist
(defn write-file-mkdirs [path bytes]
  (.mkdirs (.getParentFile (new File path)))
	(let [fos (new FileOutputStream path)]
		(.write fos bytes)
		(.close fos)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; mutable compiler state
;

(def state (atom {}))

(defn get-state [key]
  (@state key))

(defn update-state [key val]
  (swap! state assoc key val))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; configuration
;

(def arg-limit 8)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; naming
;

; packages
(def pkg-mug "mug/js/")
(def pkg-compiled (atom "mug/modules/script$")) ; atom

; types
(def qn-js-null (str pkg-mug "JSNull"))
(def qn-js-boolean (str pkg-mug "JSBoolean"))
(def qn-js-string (str pkg-mug "JSString"))
(def qn-js-number (str pkg-mug "JSNumber"))
(def qn-js-primitive (str pkg-mug "JSPrimitive"))
(def qn-js-utils (str pkg-mug "JSUtils"))
(def qn-js-function (str pkg-mug "JSFunction"))
(def qn-js-object (str pkg-mug "JSObject"))
(def qn-js-array (str pkg-mug "JSArray"))
(def qn-js-regexp (str pkg-mug "JSRegExp"))
(def qn-js-module (str pkg-mug "JSModule"))

(def qn-js-atoms (str pkg-mug "JSAtoms"))
(defn qn-js-constants [] (str @pkg-compiled "constants"))

(def qn-js-toplevel (str pkg-mug "JSTopLevel"))

(defn qn-js-script [] (chop @pkg-compiled))
(defn qn-js-scriptscope [] (str @pkg-compiled "scope$script"))
(defn qn-js-context [x] 
  (if (= x 0)
    (qn-js-script)
    (str @pkg-compiled "context$" x)))
(defn qn-js-scope [x]
  (if (= x 0)
    (qn-js-scriptscope)
    (str @pkg-compiled "scope$" x)))

;;;[TODO] these should be "sig-void", "sig-double", etc.
(def qn-object "java/lang/Object")
(def qn-string "java/lang/String")
(def qn-pattern "java/util/regex/Pattern")
(def sig-void "V")
(def sig-double "D")
(def sig-integer "I")
(def sig-boolean "Z")

(defn sig-call [& args] (str "(" (apply str (butlast args)) ")" (or (last args) "V")))
(defn sig-obj [x] (str "L" x ";"))
(defn sig-array [x] (str "[" x))

(defn sig-load [] (sig-call (sig-obj qn-js-object)))
(def sig-instantiate (apply sig-call
  (conj (conj (into [sig-integer]
    (vec (repeat arg-limit (sig-obj qn-js-primitive))))
    (sig-array (sig-obj qn-js-primitive))) (sig-obj qn-js-primitive))))
(def sig-invoke (apply sig-call
  (conj (conj (into [(sig-obj qn-js-primitive) sig-integer]
    (vec (repeat arg-limit (sig-obj qn-js-primitive))))
    (sig-array (sig-obj qn-js-primitive))) (sig-obj qn-js-primitive))))

(defmulti sig-context-init (fn [ci ast] (first ((ast-contexts ast) ci))))
(defmethod sig-context-init :mug.ast/script-context [ci ast]
  (sig-call sig-void))
(defmethod sig-context-init :mug.ast/closure-context [ci ast] 
  (apply sig-call (concat
    [(sig-obj qn-js-object)]
    (vec (map #(sig-obj (qn-js-scope %)) ((ast-context-hierarchy ast) ci)))
    [sig-void])))  

(defn ident-num [x] (str "NUM_" x))
(defn ident-str [x] (str "STR_" x))
(defn ident-regex [x] (str "REGEX_" x))
(defn ident-scope [x] (str "SCOPE_" x))

; registers

(def offset-reg 3) ; [this, "ths", count]
(def scope-reg (+ 1 offset-reg arg-limit))
(def exports-reg (+ 2 offset-reg arg-limit)) ;[TODO] this shouldn't be a register?
(def ref-offset-reg (+ exports-reg 1)) 

; local variable registers
; only used when a register is not used in a child context
; otherwise returns nil, and variable is saved as scope property
(def *local-variable-opt* true)

(defmulti ref-reg (fn [context value] (first context)))
(defmethod ref-reg :mug.ast/script-context [context value]
  (when *local-variable-opt*
	  (let [[_ stats] context
	        vars (ast-context-vars context)]
		  (when (not (contains? (ast-enclosed-vars context vars) value))
		    (when-let [pos (index-of (vec vars) value)]
		      (+ ref-offset-reg pos))))))
(defmethod ref-reg :mug.ast/closure-context [context value]
  (when *local-variable-opt*
	  (let [[_ name args stats] context
	        vars (ast-context-vars context)]
		  (when (not (contains? (ast-enclosed-vars context vars) value))
	      (do
				  (if-let [pos (index-of args value)]
				    (+ offset-reg pos)
				    (if-let [pos (index-of (vec vars) value)]
				      (+ ref-offset-reg pos)
	            (when (= name value)
	              0))))))))