(ns ladder-editor.ladder
  "Ladder diagram data structures using nested maps approach")

;; Nested Maps Approach for Ladder Logic Data Structure
;; This follows approach #1 from your original question

(defn contact
  "Create a contact element"
  [name & {:keys [normally-closed?] :or {normally-closed? false}}]
  {:type :contact
   :name name
   :normally-closed? normally-closed?
   :ascii-art (if normally-closed? "─┤/├─" "─┤ ├─")
   :width 5})

(defn coil
  "Create a coil element"
  [name & {:keys [normally-closed?] :or {normally-closed? false}}]
  {:type :coil
   :name name
   :normally-closed? normally-closed?
   :ascii-art (if normally-closed? "─(/)" "─( )")
   :width 4})

(defn rung
  "Create a rung with elements"
  [& elements]
  {:type :rung
   :elements (vec elements)})

(defn branch
  "Create a branch with multiple rungs"
  [& rungs]
  {:type :branch
   :rungs (vec rungs)})

(defn ladder
  "Create a ladder with rungs"
  [& rungs]
  {:type :ladder
   :rungs (vec rungs)})

;; Enhanced element creation functions
(defn normally-open-contact [name]
  (contact name :normally-closed? false))

(defn normally-closed-contact [name]
  (contact name :normally-closed? true))

(defn normal-coil [name]
  (coil name :normally-closed? false))

(defn negated-coil [name]
  (coil name :normally-closed? true))

;; Utility functions
(defn element-width
  "Calculate the display width of an element"
  [element]
  (max (:width element 0)
       (count (:name element ""))))

(defn rung-width
  "Calculate the total width of a rung"
  [rung]
  (if (empty? (:elements rung))
    0
    (reduce + (map element-width (:elements rung)))))

(defn ladder-max-width
  "Calculate the maximum width needed for the ladder"
  [ladder-data]
  (if (empty? (:rungs ladder-data))
    0
    (apply max (map #(cond
                       (= (:type %) :rung) (rung-width %)
                       (= (:type %) :branch) (apply max (map rung-width (:rungs %)))
                       :else 0)
                    (:rungs ladder-data)))))
