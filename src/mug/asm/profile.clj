(ns mug.asm.profile)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; profile analysis
;

;
; profile manipulation	
;

(defn new-asm-profile [] {
	:closures #{} :scopes {} :structs #{} :numbers #{} :strings #{} :accessors #{}
})

(defn asm-profile-add [n v profile]
	(if (contains? v (profile n)) profile (merge profile {n (merge (profile n) v)})))

(defn asm-profile-assign [n k v profile]
	(if (contains? v (profile n)) profile (merge profile {n (merge (profile n) {k v})})))

(defn asm-profile-add-coll [k coll profile]
	(loop [profile profile coll coll]
		(if (empty? coll)
      profile
			(recur (asm-profile-add k (first coll) profile) (next coll)))))

;
; analysis
;

(defmulti asm-analyze-ast (fn [& args] (:type (first args))))
	
(defn asm-analyze-ast-coll [coll closure profile]
	(loop [profile profile coll coll]
		(if (empty? coll)
      profile
			(recur (asm-analyze-ast (first coll) closure profile) (next coll)))))
		
;
; literals
;

(defmethod asm-analyze-ast :mug/num-literal [node closure profile]
	(asm-profile-add :numbers (node :value) profile))

(defmethod asm-analyze-ast :mug/str-literal [node closure profile]
	(asm-profile-add :strings (node :value) profile))

(defmethod asm-analyze-ast :mug/null-literal [node closure profile]
	profile)

(defmethod asm-analyze-ast :mug/func-literal [node closure profile]
	(asm-analyze-ast (node :closure) closure profile))

;
; expressions
;

(defmethod asm-analyze-ast :mug/unary-op-expr [node closure profile]
	(asm-analyze-ast (node :expr) closure profile))

(defmethod asm-analyze-ast :mug/binary-op-expr [node closure profile]
	(->> profile
		(asm-analyze-ast (node :left) closure ,,,)
		(asm-analyze-ast (node :right) closure ,,,)))

(defmethod asm-analyze-ast :mug/scope-ref-expr [node closure profile]
	profile)

(defmethod asm-analyze-ast :mug/static-ref-expr [node closure profile]
	(->> (asm-profile-add :accessors (node :value) profile)
		(asm-analyze-ast (node :base) closure ,,,)))

(defmethod asm-analyze-ast :mug/dyn-ref-expr [node closure profile]
	(->> profile
    (asm-analyze-ast (node :index) closure ,,,)
		(asm-analyze-ast (node :base) closure ,,,)))

(defmethod asm-analyze-ast :mug/static-method-call-expr [node closure profile]
	(->> (asm-profile-add :accessors (node :value) profile)
		(asm-analyze-ast (node :base) closure ,,,)
    (asm-analyze-ast-coll (node :args) closure ,,,)))

(defmethod asm-analyze-ast :mug/scope-assign-expr [node closure profile]
	(->> profile
		(asm-analyze-ast (node :expr) closure ,,,)))

(defmethod asm-analyze-ast :mug/static-assign-expr [node closure profile]
	(->> profile
    (asm-profile-add :accessors (node :value) ,,,)
		(asm-analyze-ast (node :base) closure ,,,)
    (asm-analyze-ast (node :expr) closure ,,,)))

(defmethod asm-analyze-ast :mug/dyn-assign-expr [node closure profile]
	(->> profile
		(asm-analyze-ast (node :base) closure ,,,)
    (asm-analyze-ast (node :index) closure ,,,)
    (asm-analyze-ast (node :expr) closure ,,,)))

(defmethod asm-analyze-ast :mug/new-expr [node closure profile]
	(->> profile
		(asm-analyze-ast (node :constructor) closure ,,,)
    (asm-analyze-ast-coll (node :args) closure ,,,)))

(defmethod asm-analyze-ast :mug/this-expr [node closure profile]
	profile)
	
(defmethod asm-analyze-ast :mug/call-expr [node closure profile]
	(->> profile
		(asm-analyze-ast (node :ref) closure ,,,)
		(asm-analyze-ast-coll (node :args) closure ,,,)))

;
; statements
;

(defmethod asm-analyze-ast :mug/class-stat [node closure profile]
  (->> profile
    (asm-profile-add :structs (set (keys (node :prototype))) ,,,)
    (asm-profile-add-coll :accessors (keys (or (node :prototype) {})) ,,,)
    (asm-analyze-ast ((node :constructor) :closure) closure ,,,)
    (asm-analyze-ast-coll (vals (or (node :prototype) {})) closure ,,,)
    (asm-analyze-ast-coll (vals (or (node :static) {})) closure ,,,)))

(defmethod asm-analyze-ast :mug/expr-stat [node closure profile]
	(asm-analyze-ast (node :expr) closure profile))

(defmethod asm-analyze-ast :mug/block-stat [node closure profile]
	(asm-analyze-ast-coll (node :stats) closure profile))

(defmethod asm-analyze-ast :mug/ret-stat [node closure profile]
	(if (nil? (node :expr))
    profile
    (asm-analyze-ast (node :expr) closure profile)))

(defmethod asm-analyze-ast :mug/if-stat [node closure profile]
	(let [profile (->> profile
		(asm-analyze-ast (node :expr) closure ,,,)
		(asm-analyze-ast (node :then-stat) closure ,,,))]
    (if (nil? (node :else-stat))
      profile
      (asm-analyze-ast (node :else-stat) closure profile))))

(defmethod asm-analyze-ast :mug/while-stat [node closure profile]
	(->> profile
		(asm-analyze-ast (node :expr) closure ,,,)
		(asm-analyze-ast (node :stat) closure ,,,)))

(defmethod asm-analyze-ast :mug/for-in-stat [node closure profile]
	(->> profile
		(asm-analyze-ast (node :from) closure ,,,)
		(asm-analyze-ast (node :to) closure ,,,)
    (asm-analyze-ast (node :by) closure ,,,)
    (asm-analyze-ast (node :stat) closure ,,,)))

;
; closure
;

(defmethod asm-analyze-ast :mug/closure [node closure profile]
	(->> profile
    (asm-profile-assign :scopes node
      (if (nil? closure) [] (into ((profile :scopes) closure) [closure])) ,,,)
;    (asm-profile-assign :scopes node 
;      (into (if (nil? closure) #{} ((profile :scopes) closure))
;        (into (into (node :args) (node :vars)) (if (nil? (node :name)) [] [(node :name)]))) ,,,)
		(asm-profile-add :closures node ,,,)
		(asm-analyze-ast-coll (node :stats) node ,,,)))