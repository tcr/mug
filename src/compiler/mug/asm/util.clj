(ns mug.asm.util
  (:use
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; configuration
;

(def arg-limit 8)
(def script-default-vars #{"exports" "require" "print" "Math" "Array" "parseInt" "parseFloat" "Number" "Object" "String" "Boolean"})

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

(defmulti sig-context-init (fn [context ast] (first context)))
(defmethod sig-context-init :mug.ast/script-context [[_ globals vars stats] ast]
  (sig-call sig-void))
(defmethod sig-context-init :mug.ast/closure-context [[_ parents name args vars stats] ast] 
  (apply sig-call (conj (into [(sig-obj qn-js-object)] (vec (map (fn [x] (sig-obj (qn-js-scope x))) parents))) sig-void)))  

(defn ident-num [x] (str "NUM_" x))
(defn ident-str [x] (str "STR_" x))
(defn ident-regex [x] (str "REGEX_" x))
(defn ident-scope [x] (str "SCOPE_" x))

; registers

(def offset-reg 3) ; [this, "ths", count]
(def scope-reg (+ 1 offset-reg arg-limit))
(def exports-reg (+ 2 offset-reg arg-limit)) ;[TODO] this shouldn't be a register?
(def ref-offset-reg (+ exports-reg 1)) 

;[TODO] Optimization: local register writes
; this should only return a register if the following cases are true:
;   the register is not used in a child closure
; it should assign a register if not used as argument (+ exports-reg 2)
(defmulti ref-reg (fn [context value] (first context)))
(defmethod ref-reg :mug.ast/script-context [[_ globals vars stats] value]
  nil)
(defmethod ref-reg :mug.ast/closure-context [[_ parents name args vars stats] value]
  nil)
;  (if-let [pos (index-of args value)]
;    (+ offset-reg pos)
;;    nil))
;    (when-let [pos (index-of (vec vars) value)]
;;      (println (+ ref-offset-reg pos))
;      (+ ref-offset-reg pos))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; analysis
;

(defmulti context-scope-vars (fn [context] (first context)))
(defmethod context-scope-vars :mug.ast/script-context [[_ globals vars stats]]
  (difference (into #{} (into globals vars)) script-default-vars))
(defmethod context-scope-vars :mug.ast/closure-context [[_ parents name args vars stats]]
  (into #{} (into args (into vars (if name [name] [])))))

(defn context-index [context ast]
  (index-of (ast :contexts) context))

(defmulti context-parents (fn [context] (first context)))
(defmethod context-parents :mug.ast/script-context [[_ globals vars stats]]
  [])
(defmethod context-parents :mug.ast/closure-context [[_ parents name args vars stats]]
  parents)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; mutable compiler state
;

(def state (atom {}))

(defn get-state [key]
  (@state key))

(defn update-state [key val]
  (swap! state assoc key val))