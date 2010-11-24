;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; mug.compiler
;
; External Java class mug.Compiler.
; Parses an AST, then runs it through various compilers.
;

(ns mug.compiler
  (:gen-class
    :name mug.Compiler)
  (:require [clojure.contrib.json :as json])
  (:use
    mug.ast
    mug.parser
    [mug.asm code contexts constants scopes util]
    [clojure.contrib.io :only (delete-file-recursively)]
    [clojure.contrib.string :only (replace-str)]
    clojure.pprint)
  (:import
    [java.io FileOutputStream File]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; compiler
;

(defn compile-js [ast qn out-dir]
  ;(print (str "AST:" ast))
  ;(json/pprint-json ast)
  
  ; update atom
  (swap! pkg-compiled #(identity %2)
    (str "mug/modules/" (replace-str "." "/" (replace-str "-" "_" qn)) "$"))

  ; contexts
  (println "  Contexts...")
	(doseq [[qn bytes] (compile-context-classes ast)]
		(write-file-mkdirs (str out-dir qn ".class") bytes)
    (println (str "    Wrote out " qn)))
	
  ; constants
  (println "  Constants...")
	(write-file-mkdirs (str out-dir (qn-js-constants) ".class") (asm-compile-constants-class ast))

  ; scopes
  (println "  Scopes...")
	(doseq [[qn bytes] (asm-compile-scope-classes ast)]
		(write-file-mkdirs (str out-dir qn ".class") bytes))
 
  (println "Done. Output is in \"out/\" directory.\n"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; main
;

(defn -main [& args]
  (def out-dir "./out/")
  
 	; clean output directory
	(doseq [f (.listFiles (new File out-dir))]
    (delete-file-recursively f))
 
  (doseq [path args]
    (let [qn (second (re-find #"^(.*)\.js$" path))
          file (new File path)]
      ; check file exists
	    (when (not (.exists file))
	      (throw (new Exception (str "File not found \"" path "\"."))))
      (println (str "Compiling \"" path "\""))
     
      ; parse
      (let [ast (parse-js-ast (slurp path))]
        ;(pprint ast)
        ; compile
        (compile-js ast qn out-dir)))))