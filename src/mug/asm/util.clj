(ns mug.asm.util)
		
(defn index [coll]
	(map vector (iterate inc 0) coll))

;(defn map-indexed [callback coll]
;	(map (fn [[k v]] (callback k v)) (index coll)))

(defn index-of [s x]
	((zipmap (vec s) (iterate inc 0)) x))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; naming
;

(def qn-classes-undefined "mug/JSUndefined")
(def qn-classes-boolean "mug/JSBoolean")
(def qn-classes-string "mug/JSString")
(def qn-classes-number "mug/JSNumber")

(def arg-limit 10)
(def sig-instantiate (str "(" (apply str (repeat arg-limit "Lmug/JSPrimitive;")) ")Lmug/JSPrimitive;"))
(def sig-invoke (str "(Lmug/compiled/JSObject;" (apply str (repeat arg-limit "Lmug/JSPrimitive;")) ")Lmug/JSPrimitive;"))