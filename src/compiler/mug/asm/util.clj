(ns mug.asm.util
  (:use
    mug.ast
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