(ns mug.asm.util
  (:use clojure.contrib.str-utils [clojure.set :only (difference)]))
		
(defn index [coll]
	(map vector (iterate inc 0) coll))

(defn index-of [s x]
	((zipmap (vec s) (iterate inc 0)) x))

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
(def pkg-compiled "binarytrees/")

; types
(def qn-js-undefined (str pkg-mug "JSUndefined"))
(def qn-js-boolean (str pkg-mug "JSBoolean"))
(def qn-js-string (str pkg-mug "JSString"))
(def qn-js-number (str pkg-mug "JSNumber"))
(def qn-js-primitive (str pkg-mug "JSPrimitive"))
(def qn-js-utils (str pkg-mug "JSUtils"))
(def qn-js-function (str pkg-mug "JSFunction"))
(def qn-js-object (str pkg-mug "JSObject"))

(def qn-js-atoms (str pkg-mug "JSAtoms"))
(def qn-js-constants (str pkg-compiled "JSConstants"))

;(def qn-js-compiled-object (str pkg-mug "JSCompiledObject"))
;(def qn-js-compiled-function (str pkg-mug "JSCompiledFunction"))
(def qn-js-globalscope (str pkg-mug "JSGlobalScope"))

(def qn-js-script (str pkg-compiled "JSScript"))
(def qn-js-scriptscope (str pkg-compiled "JSScriptScope"))
(defn qn-js-context [x] 
  (if (= x 0)
    qn-js-script
    (str pkg-compiled "JSContext$" x)))
(defn qn-js-scope [x]
  (if (= x 0)
    qn-js-scriptscope
    (str pkg-compiled "JSScope$" x)))

;;;[TODO] these should be "sig-void", "sig-double", etc.
(def qn-object "java/lang/Object")
(def qn-string "java/lang/String")
(def qn-void "V")
(def qn-double "D")
(def qn-integer "I")
(def qn-boolean "Z")

(defn sig-call [& args] (str "(" (apply str (butlast args)) ")" (or (last args) "V")))
(defn sig-obj [x] (str "L" x ";"))

(def sig-execute (sig-call (sig-obj (qn-js-scope 0)) (sig-obj qn-js-primitive)))
(def sig-instantiate (apply sig-call (conj (vec (repeat arg-limit (sig-obj qn-js-primitive))) (sig-obj qn-js-primitive))))
(def sig-invoke (apply sig-call (conj (vec (conj (repeat arg-limit (sig-obj qn-js-primitive)) (sig-obj qn-js-object))) (sig-obj qn-js-primitive))))

(defmulti sig-context-init (fn [& args] (:type (first args))))
(defmethod sig-context-init :mug.ast/script-context [context ast]
  (sig-call qn-void))
(defmethod sig-context-init :mug.ast/closure-context [context ast] 
  (apply sig-call (conj (vec (map (fn [x] (sig-obj (qn-js-scope x))) (context :parents))) qn-void)))  

(defn ident-num [x] (str "NUM_" x))
(defn ident-str [x] (str "STR_" x))
(defn ident-scope [x] (str "SCOPE_" x))

(def script-default-vars #{"exports" "Math" "print" "Array" "nanoTime"})

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