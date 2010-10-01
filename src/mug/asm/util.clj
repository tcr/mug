(ns mug.asm.util)
		
(defn index [coll]
	(map vector (iterate inc 0) coll))

;(defn map-indexed [callback coll]
;	(map (fn [[k v]] (callback k v)) (index coll)))

(defn index-of [s x]
	((zipmap (vec s) (iterate inc 0)) x))