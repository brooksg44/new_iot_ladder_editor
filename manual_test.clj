;; Manual test of the fixed IL conversion
;; This tests each step independently

(require '[ladder-editor.converter :as converter]
         '[ladder-editor.render :as render]
         '[ladder-editor.ladder :as ladder])

(println "=== MANUAL IL CONVERSION TEST ===\n")

;; Step 1: Test parsing
(println "Step 1: Parse IL code")
(def il-code "LD %I0.0\nAND %I0.1\nST %Q0.0")
(println "IL Code:" il-code)

(def parsed (converter/parse-il-program il-code))
(println "Parsed instructions:")
(doseq [instr (:instructions parsed)]
  (println "  " (:operation instr) (:operand instr)))
(println)

;; Step 2: Test grouping manually with correct logic
(println "Step 2: Group instructions (manual test)")
(defn test-grouping [instructions]
  (reduce
   (fn [acc instruction]
     (let [op (:operation instruction)]
       (cond
         ;; Start new rung on LD/LDN
         (some #{op} ["LD" "LDN"])
         (conj acc [instruction])
         
         ;; Add to current rung if we have one
         (seq acc)
         (let [current-group (last acc)
               other-groups (butlast acc)]
           (conj (vec other-groups)
                 (conj (vec current-group) instruction)))
         
         ;; No existing group, create new one
         :else
         (conj acc [instruction]))))
   []
   instructions))

(def manual-groups (test-grouping (:instructions parsed)))
(println "Manual grouping result:")
(doseq [[idx group] (map-indexed vector manual-groups)]
  (println (str "  Group " idx ":"))
  (doseq [instr group]
    (println "    " (:operation instr) (:operand instr))))
(println)

;; Step 3: Convert to elements
(println "Step 3: Convert to elements")
(defn convert-group [group]
  (map converter/il-instruction-to-ladder-element group))

(doseq [[idx group] (map-indexed vector manual-groups)]
  (println (str "Group " idx " elements:"))
  (doseq [element (convert-group group)]
    (println "  " (:type element) (:name element) (:ascii-art element))))
(println)

;; Step 4: Create ladder
(println "Step 4: Create ladder")
(def manual-rungs 
  (map (fn [group]
         (ladder/rung (map converter/il-instruction-to-ladder-element group)))
       manual-groups))

(def manual-ladder {:type :ladder :rungs (vec manual-rungs)})
(println "Created ladder with" (count (:rungs manual-ladder)) "rungs")
(println)

;; Step 5: Render
(println "Step 5: Render result")
(println (render/render-ladder manual-ladder))
(println)

;; Step 6: Compare with automatic conversion
(println "Step 6: Compare with automatic conversion")
(def auto-result (converter/convert-il-text il-code))
(if (:success auto-result)
  (do
    (println "Automatic conversion succeeded")
    (println "Automatic result:")
    (println (render/render-ladder (:result auto-result))))
  (println "Automatic conversion failed:" (:message auto-result)))

(println "\n=== TEST COMPLETE ===")
