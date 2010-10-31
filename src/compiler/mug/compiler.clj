(ns mug.compiler (:gen-class)
  (:use mug.ast mug.parse.parser [mug.asm code contexts constants object scopes util]
    [clojure.contrib.io :only (delete-file-recursively)]))

;
; file i/o
;

(import (java.io FileOutputStream File))

; writes bytes to path
; creating directories if they don't exist
(defn write-file [path bytes]
  (.mkdirs (.getParentFile (new File path)))
	(let [fos (new FileOutputStream path)]
		(.write fos bytes)
		(.close fos)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; compiler
;

(defn compile-js [ast out-dir]
	; clean output directory
	(doseq [f (.listFiles (new File out-dir))]
    (delete-file-recursively f))

  ; closures
  (println " Closures...")
	(doseq [[qn bytes] (asm-compile-closure-classes ast)]
		(write-file (str out-dir qn ".class") bytes))
	
  ; constants
  (println " Constants...")
	(write-file (str out-dir qn-js-constants ".class") (asm-compile-constants-class ast))
	
  ; object shim
  (println " Objects...")
;	(write-file (str out-dir qn-js-object ".class") (asm-compile-object-class ast))
	(write-file (str out-dir pkg-compiled "JSCompiledObject.class")
   (asm-compile-object-class ast))
	
  ; scopes
  (println " Scopes...")
	(doseq [[qn bytes] (asm-compile-scope-classes ast)]
		(write-file (str out-dir qn ".class") bytes))
 
  (println "Done. Output is in \"out/\" directory."))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; main
;

(defn -main [& args]
  (doseq [path args]
    (let [file (new File path)]
      ; check file exists
	    (when (not (.exists file))
	      (throw (new Exception (str "File not found \"" path "\"."))))
      (println (str "Compiling \"" path "\""))
     
      ; parse
      (let [ast (gen-ast (parse-js (slurp path)))
            cwd (.getParent file)]
        
        ; compile
        (compile-js ast (str cwd "/out/"))))))