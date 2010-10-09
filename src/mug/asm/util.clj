(ns mug.asm.util
  (:use clojure.contrib.str-utils))
		
(defn index [coll]
	(map vector (iterate inc 0) coll))

;(defn map-indexed [callback coll]
;	(map (fn [[k v]] (callback k v)) (index coll)))

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
(defn qn-js-closure [x] (str "mug/compiled/JSClosure$" x))
(defn qn-js-scope [x] (str "mug/compiled/JSScope$" x))
(def qn-js-script "mug/compiled/JSScript")

(def qn-object "java/lang/Object")
(def qn-string "java/lang/String")
(def qn-void "V")
(def qn-double "D")
(def qn-integer "I")
(def qn-boolean "Z")

(defn sig-call [& args] (str "(" (apply str (butlast args)) ")" (or (last args) "V")))
(defn sig-obj [x] (str "L" x ";"))
(def sig-instantiate (str "(" (apply str (repeat arg-limit (sig-obj qn-js-primitive))) ")Lmug/JSPrimitive;"))
(def sig-invoke (str "(Lmug/compiled/JSObject;" (apply str (repeat arg-limit "Lmug/JSPrimitive;")) ")Lmug/JSPrimitive;"))

(defn ident-str [x] (str "STR_" x))
(defn ident-scope [x] (str "SCOPE_" x))
(defn ident-num [x] (str "NUM_" x))