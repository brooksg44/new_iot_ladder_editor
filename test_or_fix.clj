;; Test the OR handling fix

(require '[ladder-editor.converter :as converter]
         '[ladder-editor.render :as render]
         '[clojure.pprint :as pprint])

(println "=== Testing OR Handling Fix ===\n")

(def test-il "LD %I0.0\nANDN %I0.1\nOR %Q0.0\nST %Q0.0")

(println "Test IL (Three-wire motor control with seal-in):")
(println test-il)
(println)

(println "This should create:")
(println "- Main path: %I0.0 AND NOT %I0.1")  
(println "- OR branch: %Q0.0 (seal-in contact)")
(println "- Both paths lead to coil %Q0.0")
(println)

(def result (converter/convert-il-text test-il))

(if (:success result)
  (do
    (println "✅ Conversion succeeded!")
    (println "\nLadder structure:")
    (pprint/pprint (:result result))
    (println "\nRendered ladder:")
    (println (render/render-ladder (:result result)))
    (println)
    (println "Expected to see:")
    (println "- A branch structure with ┬, ├, └ symbols")
    (println "- Two parallel paths leading to the coil")
    (println "- NOT just a series of contacts"))
  (do
    (println "❌ Conversion failed:")
    (println (:message result))))

(println "\n=== Test Complete ===")
