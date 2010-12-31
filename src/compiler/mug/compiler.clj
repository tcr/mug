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
    [mug.asm code contexts constants scopes util analyze config]
    [clojure.contrib.io :only (delete-file-recursively)]
    [clojure.contrib.string :only (replace-str)]
    clojure.pprint
    clojure.contrib.command-line)
  (:import
    [java.io FileOutputStream File]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; compiler
;

(defn compile-js [ast qn writer]  
  ; update atom
  (swap! pkg-compiled #(identity %2)
    (str "mug/modules/" (replace-str "." "/" (replace-str "-" "_" qn)) "$"))

  ; contexts
  (println "  Contexts...")
	(doseq [[qn bytes] (compile-context-classes ast qn)]
		(writer (str qn ".class") bytes)
    (println (str "    Wrote out " qn)))
	
  ; constants
  (println "  Constants...")
	(writer (str (qn-js-constants) ".class") (asm-compile-constants-class ast))

  ; scopes
  (println "  Scopes...")
	(doseq [[qn bytes] (asm-compile-scope-classes ast)]
		(writer (str qn ".class") bytes)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; command line
;

(defn -main [& args]
  (with-command-line args
    "Mug JavaScript Compiler for JVM"
    [[output o "Output directory" "bin/"]
     [print? p? "Print AST directory to stdout"]
     [jar j "Output contents as jar file" nil]
     paths]
   
    ; filter files list for nonexisting files, directories
    (let [paths (apply concat (map (fn [path]
            (let [file (new File path)]
				      ; check file exists
					    (when (not (.exists file))
					      (throw (new Exception (str "File not found \"" path "\"."))))
              ; folders or files
              (if (.isDirectory file)
                (filter #(not (nil? (re-find #"^(.*)\.js$" %))) (map #(.getPath %) (file-seq file)))
                [path]))) paths))
          output (.getPath (doto (File. output) .mkdirs))]
    
	    ; iterate files
	    (doseq [path paths]
		    (let [file (new File path)
		          qn (second (re-find #"^(.*)\.js$" (.getName file)))]		     
		      ; parse
		      (println (str "Parsing \"" path "\"...\n"))
		      (let [ast (parse-js-ast (slurp path))]
				    (if print?
	            ; print
				      (pprint ast)
	            ; compile
	            (if (nil? jar)
	              (do
	                ;[TODO] delete all files in this qualified namespace
	                (compile-js ast qn
                    (fn [path bytes] (write-file-mkdirs (str output "/" path) bytes))))
	              (do
	                (let [stream (open-jar (str output "/" jar))
	                      writer (fn [path bytes] (write-file-jar stream path bytes))]
	                  (compile-js ast qn writer)
	                  (.close stream))))))))
     
      (println (str "Done. Output is in \"" output "/\" directory.\n")))))