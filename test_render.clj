;; Test the render fix
(require '[ladder-editor.render :as render]
         '[ladder-editor.ladder :as ladder]
         '[clojure.pprint :as pprint])

(println "=== TESTING RENDER FIX ===\n")

;; Create a simple ladder manually
(def test-ladder 
  (ladder/ladder
   (ladder/rung
    (ladder/normally-open-contact "Start")
    (ladder/normally-closed-contact "Stop") 
    (ladder/normal-coil "Motor"))))

(println "Test ladder structure:")
(pprint/pprint test-ladder)
(println)

(println "Rendered output:")
(println (render/render-ladder test-ladder))
(println)

(println "Expected: Clean output with proper spacing, no extra dashes")
(println "Should look like:")
(println "█")
(println "█─  Start     Stop      Motor  ")
(println "█───┤ ├─────── ┤/├────── ( ) ───")
(println "█")
