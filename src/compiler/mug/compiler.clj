(ns mug.compiler
  (:gen-class
    :name mug.Compiler)
  (:use
    mug.ast
    mug.parse.parser
    [mug.asm code contexts constants scopes util]
    [clojure.contrib.io :only (delete-file-recursively)]
    [clojure.contrib.string :only (replace-str)]))

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

(defn compile-js [ast qn out-dir]
  (println (str "AST: " ast))
  
  ; update atom
  (swap! pkg-compiled #(identity %2) (str "mug/modules/" (replace-str "." "/" qn) "$"))

  ; closures
  (println " Closures...")
	(doseq [[qn bytes] (asm-compile-closure-classes ast)]
		(write-file (str out-dir qn ".class") bytes))
	
  ; constants
  (println " Constants...")
	(write-file (str out-dir (qn-js-constants) ".class") (asm-compile-constants-class ast))
	
  ; object shim
;  (println " Objects...")
;	(write-file (str out-dir qn-js-object ".class") (asm-compile-object-class ast))
;	(write-file (str out-dir pkg-compiled "JSCompiledObject.class")
;   (asm-compile-object-class ast))
	
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
  (def out-dir "./out/")
  
 	; clean output directory
	(doseq [f (.listFiles (new File out-dir))]
    (delete-file-recursively f))
 
  (doseq [[qn path] (apply hash-map args)]
    (let [file (new File path)]
      ; check file exists
	    (when (not (.exists file))
	      (throw (new Exception (str "File not found \"" path "\"."))))
      (println (str "Compiling \"" path "\""))
     
      ; parse
      (let [ast (gen-ast (parse-js (slurp path)))]
        ; compile
        (compile-js ast qn out-dir)))))