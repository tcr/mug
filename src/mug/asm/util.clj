(ns mug.asm.util
  (:use clojure.contrib.str-utils))
		
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

(def qn-js-undefined "mug/JSUndefined")
(def qn-js-boolean "mug/JSBoolean")
(def qn-js-string "mug/JSString")
(def qn-js-number "mug/JSNumber")
(def qn-js-primitive "mug/JSPrimitive")
(def qn-js-utils "mug/JSUtils")
(def qn-js-function "mug/JSFunction")

(def qn-js-constants "mug/compiled/JSConstants")
(def qn-js-object "mug/compiled/JSObject")
(def qn-js-objectbase "mug/JSObjectBase")
(def qn-js-script "mug/compiled/JSScript")
(def qn-js-scriptscope "mug/compiled/JSScriptScope")
(defn qn-js-context [x] 
  (if (= x 0)
    qn-js-script
    (str "mug/compiled/JSContext$" x)))
(defn qn-js-scope [x]
  (if (= x 0)
    qn-js-scriptscope
    (str "mug/compiled/JSScope$" x)))

(def qn-object "java/lang/Object")
(def qn-string "java/lang/String")
(def qn-void "V")
(def qn-double "D")
(def qn-integer "I")
(def qn-boolean "Z")

(defn sig-call [& args] (str "(" (apply str (butlast args)) ")" (or (last args) "V")))
(defn sig-obj [x] (str "L" x ";"))

(def sig-execute (sig-call (sig-obj (qn-js-scope 0)) (sig-obj qn-js-primitive)))
(def sig-instantiate (str "(" (apply str (repeat arg-limit (sig-obj qn-js-primitive))) ")Lmug/JSPrimitive;"))
(def sig-invoke (str "(Lmug/compiled/JSObject;" (apply str (repeat arg-limit "Lmug/JSPrimitive;")) ")Lmug/JSPrimitive;"))

(defmulti sig-context-init (fn [& args] (:type (first args))))
(defmethod sig-context-init :mug/script-context [context ast]
  (sig-call qn-void))
(defmethod sig-context-init :mug/closure-context [context ast] 
  (apply sig-call (conj (vec (map (fn [x] (sig-obj (qn-js-scope x))) (context :parents))) qn-void)))  

(defn ident-num [x] (str "NUM_" x))
(defn ident-str [x] (str "STR_" x))
(defn ident-scope [x] (str "SCOPE_" x))

(def script-default-vars #{"Math" "print" "Array"})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; analysis
;

(defmulti context-scope-vars (fn [& args] (:type (first args))))
(defmethod context-scope-vars :mug/script-context [context]
  (into #{} (into (context :globals) (into (context :vars) script-default-vars))))
(defmethod context-scope-vars :mug/closure-context [context]
  (into #{} (into (context :args) (into (context :vars) (if (context :name) [(context :name)] [])))))

(defn context-index [context ast]
  (index-of (ast :contexts) context))