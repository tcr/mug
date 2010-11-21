(ns mug.asm.object
  (:use mug.asm.util [clojure.contrib.duck-streams :only (to-byte-array)]))

(import [org.objectweb.asm ClassWriter ClassAdapter ClassReader Opcodes Label] java.io.File)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; object
; NOTE: not currently used
;

(defn asm-compile-object-methods [accessors cw]
  ;[TODO] not filter out "prototype"
  (doseq [k (filter (fn [x] (not= x "prototype")) (vec accessors))]
		(doto (.visitMethod cw, Opcodes/ACC_PUBLIC, (str "get_" k), "()Lmug/js/JSPrimitive;", nil, nil)
			(.visitCode)
			(.visitVarInsn Opcodes/ALOAD, 0)
      (.visitLdcInsn k)
			(.visitMethodInsn Opcodes/INVOKEVIRTUAL, qn-js-compiled-object, "get", "(Ljava/lang/String;)Lmug/js/JSPrimitive;")
			(.visitInsn Opcodes/ARETURN)
      (.visitMaxs 1, 1)
			(.visitEnd))
		(doto (.visitMethod cw, Opcodes/ACC_PUBLIC, (str "set_" k), "(Lmug/js/JSPrimitive;)V", nil, nil)
			(.visitCode)
			(.visitVarInsn Opcodes/ALOAD, 0)
      (.visitLdcInsn k)
      (.visitVarInsn Opcodes/ALOAD, 1)
			(.visitMethodInsn Opcodes/INVOKEVIRTUAL, qn-js-compiled-object, "set", "(Ljava/lang/String;Lmug/js/JSPrimitive;)V")
			(.visitInsn Opcodes/RETURN)
      (.visitMaxs 1, 1)
			(.visitEnd))))

(comment
(defn asm-compile-object-init [cw]
	(doto (.visitMethod cw, Opcodes/ACC_PUBLIC, "<init>", "()V", nil, nil)
		(.visitCode)
		(.visitVarInsn Opcodes/ALOAD, 0)
		(.visitMethodInsn Opcodes/INVOKESPECIAL, qn-js-compiled-object, "<init>", "()V")
		(.visitInsn Opcodes/RETURN)
    (.visitMaxs 1, 1)
		(.visitEnd))
 (doto (.visitMethod cw, Opcodes/ACC_PUBLIC, "<init>", "(Lmug/js/compiled/JSObject;)V", nil, nil)
		(.visitCode)
		(.visitVarInsn Opcodes/ALOAD, 0)
		(.visitMethodInsn Opcodes/INVOKESPECIAL, "mug/js/JSObjectBase", "<init>", "()V")
    (.visitVarInsn Opcodes/ALOAD, 0)
    (.visitVarInsn Opcodes/ALOAD, 1)
    (.visitFieldInsn Opcodes/PUTFIELD, qn-js-compiled-object, "__proto__", (sig-obj qn-js-compiled-object))
		(.visitInsn Opcodes/RETURN)
    (.visitMaxs 1, 1)
		(.visitEnd)))
)

(defn base-class []
  (to-byte-array (new File "bin/mug/js/JSCompiledObject.class")))

(defn obj-adapter [cw ast]
  (proxy [ClassAdapter] [cw]    
    (visit [version access name sig superName interfaces]
      ; import methods/classes
      (.visit cw version access name sig superName interfaces)
      ; write accessors
      (asm-compile-object-methods (ast :accessors) cw))))

(defn asm-compile-object-class [ast]
  (let [cw (new ClassWriter ClassWriter/COMPUTE_MAXS)
        ca (obj-adapter cw ast)
        cr (new ClassReader (base-class))]
    (.accept cr ca 0)
    (.toByteArray cw)))

;		(let [cw (new ClassWriter ClassWriter/COMPUTE_MAXS)]
;			(.visit cw, Opcodes/V1_6, (+ Opcodes/ACC_SUPER Opcodes/ACC_PUBLIC), qn-js-compiled-object, nil, qn-js-compiled-objectbase, nil)
;			(asm-compile-object-methods (ast :accessors) cw)
;			(asm-compile-object-init cw)
;			(.visitEnd cw)
;			(.toByteArray cw)))