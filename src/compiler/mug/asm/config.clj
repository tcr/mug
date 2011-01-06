(ns mug.asm.config
  (:gen-class)
  (:use
    clojure.contrib.str-utils))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; optimizations
;

(def *local-variable-opt* false)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; env
;

(def arg-limit 8)

(def script-default-vars
  #{"exports" "require" "print" "console"
    "parseInt" "parseFloat" "isNaN" "isFinite"
    "Math" "JSON"
    "Object" "Array" "Number" "String" "Boolean" "Function" "Date"
    "Error" "SyntaxError"
    "setTimeout" "setInterval" "clearTimeout" "clearInterval"})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; naming
;

; packages
(def pkg-mug "mug/js/")
(def pkg-compiled (atom "mug/modules/script$")) ; atom

; types
(def qn-js-null (str pkg-mug "JSNull"))
(def qn-js-boolean (str pkg-mug "JSBoolean"))
(def qn-js-string (str pkg-mug "JSString"))
(def qn-js-number (str pkg-mug "JSNumber"))
(def qn-js-utils (str pkg-mug "JSUtils"))
(def qn-js-function (str pkg-mug "JSFunction"))
(def qn-js-object (str pkg-mug "JSObject"))
(def qn-js-array (str pkg-mug "JSArray"))
(def qn-js-regexp (str pkg-mug "JSRegExp"))
(def qn-js-module (str pkg-mug "JSModule"))
(def qn-js-exception (str pkg-mug "JSException"))
(def qn-js-value-exception (str pkg-mug "JSValueException"))

(def qn-js-atoms (str pkg-mug "JSAtoms"))
(defn qn-js-constants [] (str @pkg-compiled "constants"))

(def qn-js-toplevel (str pkg-mug "JSEnvironment"))

(defn qn-js-script [] (chop @pkg-compiled))
(defn qn-js-scriptscope [] (str @pkg-compiled "scope$script"))
(defn qn-js-context [x] 
  (if (= x 0)
    (qn-js-script)
    (str @pkg-compiled "context$" x)))
(defn qn-js-scope [x]
  (if (= x 0)
    (qn-js-scriptscope)
    (str @pkg-compiled "scope$" x)))

;;;[TODO] these should be "sig-void", "sig-double", etc.
(def qn-object "java/lang/Object")
(def qn-string "java/lang/String")
(def qn-exception "java/lang/Exception")
(def qn-pattern "java/util/regex/Pattern")
(def sig-void "V")
(def sig-double "D")
(def sig-integer "I")
(def sig-boolean "Z")

(defn sig-call [& args] (str "(" (apply str (butlast args)) ")" (or (last args) "V")))
(defn sig-obj [x] (str "L" x ";"))
(defn sig-array [x] (str "[" x))

(defn sig-load [] (sig-call (sig-obj qn-js-object)))
(def sig-instantiate (apply sig-call
  (concat
    [sig-integer]
    (vec (repeat arg-limit (sig-obj qn-object)))
    [(sig-array (sig-obj qn-object))
     (sig-obj qn-object)])))
(def sig-invoke (apply sig-call
  (concat
    [(sig-obj qn-object)
     sig-integer]
    (vec (repeat arg-limit (sig-obj qn-object)))
    [(sig-array (sig-obj qn-object))
     (sig-obj qn-object)])))

(defn ident-num [x] (str "NUM_" x))
(defn ident-str [x] (str "STR_" x))
(defn ident-regex [x] (str "REGEX_" x))
(defn ident-scope [x] (str "SCOPE_" x))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; jvm registers
;

(def offset-reg 3) ; [this, "ths", count]
(def scope-reg (+ 1 offset-reg arg-limit))
(def exports-reg (+ 2 offset-reg arg-limit)) ;[TODO] this shouldn't be a register?
(def ref-offset-reg (+ exports-reg 1)) 