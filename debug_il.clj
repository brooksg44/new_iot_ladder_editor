;; Debug the IL conversion issue
;; Run this to see what's happening step by step

(require '[ladder-editor.converter :as converter]
         '[ladder-editor.render :as render]
         '[ladder-editor.ladder :as ladder]
         '[clojure.pprint :as pprint])

(println "=== DEBUGGING IL CONVERSION ===\n")

;; Test the parsing first
(def test-il "LD %I0.0\nAND %I0.1\nST %Q0.0")
(println "1. Input IL:")
(println test-il)
(println)

;; Check parsing
(def parsed (converter/parse-il-program test-il))
(println "2. Parsed program:")
(pprint/pprint parsed)
(println)

;; Check instruction grouping
(def instructions (:instructions parsed))
(println "3. Individual instructions:")
(doseq [instr instructions]
  (println "  " instr))
(println)

;; Test grouping function directly
(def groups (converter/group-instructions-into-rungs instructions))
(println "4. Instruction groups:")
(pprint/pprint groups)
(println)

;; Test element conversion
(println "5. Converting groups to elements:")
(doseq [[idx group] (map-indexed vector groups)]
  (println (str "  Group " idx ":"))
  (doseq [instr group]
    (let [element (converter/il-instruction-to-ladder-element instr)]
      (println "    " instr " -> " element))))
(println)

;; Test full conversion
(def result (converter/convert-il-text test-il))
(println "6. Full conversion result:")
(pprint/pprint result)
(println)

;; Test rendering
(if (:success result)
  (do
    (println "7. Rendered ladder:")
    (println (render/render-ladder (:result result))))
  (println "7. Conversion failed:" (:message result)))

(println "\n=== DEBUG COMPLETE ===")
