(ns mug.asm.util
  (:use
    clojure.contrib.str-utils
    [clojure.set :only (difference)]))
		
(defn index [coll]
	(map vector (iterate inc 0) coll))

(defn index-of [s x]
	((zipmap (vec s) (iterate inc 0)) x))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; configuration
;

(def arg-limit 8)
(def script-default-vars #{"exports" "require" "print" "Math" "Array"})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; naming
;

; packages
(def pkg-mug "mug/js/")
(def pkg-compiled (atom "mug/modules/script$")) ; atom

; types
(def qn-js-undefined (str pkg-mug "JSUndefined"))
(def qn-js-boolean (str pkg-mug "JSBoolean"))
(def qn-js-string (str pkg-mug "JSString"))
(def qn-js-number (str pkg-mug "JSNumber"))
(def qn-js-primitive (str pkg-mug "JSPrimitive"))
(def qn-js-utils (str pkg-mug "JSUtils"))
(def qn-js-function (str pkg-mug "JSFunction"))
(def qn-js-object (str pkg-mug "JSObject"))
(def qn-js-regex (str pkg-mug "JSRegExp"))
(def qn-js-module (str pkg-mug "JSModule"))

(def qn-js-atoms (str pkg-mug "JSAtoms"))
(defn qn-js-constants [] (str @pkg-compiled "JSConstants"))

;(def qn-js-compiled-object (str pkg-mug "JSCompiledObject"))
;(def qn-js-compiled-function (str pkg-mug "JSCompiledFunction"))
(def qn-js-globalscope (str pkg-mug "JSGlobalScope"))

(defn qn-js-script [] (chop @pkg-compiled))
(defn qn-js-scriptscope [] (str @pkg-compiled "JSScriptScope"))
(defn qn-js-context [x] 
  (if (= x 0)
    (qn-js-script)
    (str @pkg-compiled "JSContext$" x)))
(defn qn-js-scope [x]
  (if (= x 0)
    (qn-js-scriptscope)
    (str @pkg-compiled "JSScope$" x)))

;;;[TODO] these should be "sig-void", "sig-double", etc.
(def qn-object "java/lang/Object")
(def qn-string "java/lang/String")
(def sig-void "V")
(def sig-double "D")
(def sig-integer "I")
(def sig-boolean "Z")

(defn sig-call [& args] (str "(" (apply str (butlast args)) ")" (or (last args) "V")))
(defn sig-obj [x] (str "L" x ";"))
(defn sig-array [x] (str "[" x))

;(defn sig-execute [] (sig-call (sig-obj (qn-js-scope 0)) (sig-obj qn-js-primitive))) deprecated
(defn sig-load [] (sig-call (sig-obj qn-js-object)))
(def sig-instantiate (apply sig-call
  (conj (conj (into [sig-integer]
    (vec (repeat arg-limit (sig-obj qn-js-primitive))))
    (sig-array (sig-obj qn-js-primitive))) (sig-obj qn-js-primitive))))
(def sig-invoke (apply sig-call
  (conj (conj (into [(sig-obj qn-js-object) sig-integer]
    (vec (repeat arg-limit (sig-obj qn-js-primitive))))
    (sig-array (sig-obj qn-js-primitive))) (sig-obj qn-js-primitive))))

(defmulti sig-context-init (fn [& args] (:type (first args))))
(defmethod sig-context-init :mug.ast/script-context [context ast]
  (sig-call sig-void))
(defmethod sig-context-init :mug.ast/closure-context [context ast] 
  (apply sig-call (conj (vec (map (fn [x] (sig-obj (qn-js-scope x))) (context :parents))) sig-void)))  

(defn ident-num [x] (str "NUM_" x))
(defn ident-str [x] (str "STR_" x))
(defn ident-regex [x] (str "REGEX_" x))
(defn ident-scope [x] (str "SCOPE_" x))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; analysis
;

(defmulti context-scope-vars (fn [& args] (:type (first args))))
(defmethod context-scope-vars :mug.ast/script-context [context]
  (difference (into #{} (into (context :globals) (context :vars))) script-default-vars))
(defmethod context-scope-vars :mug.ast/closure-context [context]
  (into #{} (into (context :args) (into (context :vars) (if (context :name) [(context :name)] [])))))

(defn context-index [context ast]
  (index-of (ast :contexts) context))