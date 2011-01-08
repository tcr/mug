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

(defn compile-js [ast qn path writer]  
  ; update atom
  (swap! pkg-compiled #(identity %2) (str qn "$"))

  ; contexts
  (println "  Contexts...")
	(doseq [[qn bytes] (compile-context-classes ast qn path)]
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
     [print? "Print AST directory to stdout"]
     [jar j "Output contents as jar file" nil]
     [package "Java package to compile modules into" ""]
     paths]
    
    (if (empty? paths)
      (println "No arguments specified. Run mug with --help or -h to see options.")
    
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
	          output (.getPath (doto (File. output) .mkdirs))
            stream (when jar (open-jar (str output "/" jar)))]
	    
		    ; iterate files
		    (doseq [path paths]
			    (let [file (new File path)
                path (.substring (.getCanonicalPath file) (+ (count (.getCanonicalPath (File. "."))) 1))
	              modulename (replace-str "-" "_" (second (re-find #"^(.*)\.js$" path)))
			          qn (str (if (empty? package) "" (str (replace-str "." "/" package) "/")) modulename)]		     
			      ; parse
			      (println (str "Parsing \"" path "\"...\n"))
			      (let [script (slurp path)
                  ast (parse-js-ast script)]
					    (if print?
		            ; print
					      (pprint ast)
		            ; compile
		            (if (nil? jar)
		              (do
	                  ; delete all files in this qualified namespace
	                  (let [parent (.getParentFile (File. (str output "/" qn)))]
	                    (when (.exists parent)
	                      (doseq [f (map #(new File (str (.getPath parent) "/" %)) (filter #(re-matches
	                                  (re-pattern (str "\\Q" modulename "\\E" "(\\$.*)?\\.class")) %)
	                                  (.list parent)))]
	                        (try (.delete f) (catch Exception e)))))
	                  
		                (compile-js ast qn path
	                    (fn [path bytes] (write-file-mkdirs (str output "/" path) bytes))))
		              (do
		                (compile-js ast qn path
                      (fn [path bytes] (write-file-jar stream path bytes)))
                    (write-file-jar stream (str qn ".js") (.getBytes script))))))))
      
        ; close jar streams
        (when jar
          (.close stream))
	     
	      (println (str "Done. Output is in \"" output "/\" directory.\n"))))))