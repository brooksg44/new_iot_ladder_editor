;; Test file to verify the fixes work in your new_iot_ladder_editor
;; Run this with: clj -M -e "(load-file \"test_fixes.clj\")"

(require '[ladder-editor.core :as core]
         '[ladder-editor.converter :as converter]
         '[ladder-editor.render :as render]
         '[ladder-editor.ladder :as ladder])

(println "=== Testing FIXED IOT Ladder Editor ===\n")

;; Test 1: Manual ladder creation (should have clean output now)
(println "Test 1: Manual Ladder Creation (Fixed Rendering)")
(def test-ladder 
  (ladder/ladder
   (ladder/rung
    (ladder/normally-open-contact "Start")
    (ladder/normally-closed-contact "Stop")
    (ladder/normal-coil "Motor"))))

(println "Expected: Clean, well-spaced output with no extra dashes")
(println (render/render-ladder test-ladder))
(println)

;; Test 2: IL conversion (should work now)
(println "Test 2: FIXED IL Conversion")
(let [il-code "LD %I0.0\nAND %I0.1\nST %Q0.0"
      result (converter/convert-il-text il-code)]
  (println "IL Code:")
  (println il-code)
  (println "\nExpected: Successfully converted ladder (not empty)")
  (if (:success result)
    (do
      (println "✅ SUCCESS! Conversion working!")
      (println "Result:")
      (println (render/render-ladder (:result result))))
    (println "❌ FAILED:" (:message result))))

(println)

;; Test 3: Complex IL (multiple rungs)
(println "Test 3: Complex IL with Multiple Rungs")
(let [complex-il "LD %I0.0\nANDN %I0.1\nST %Q0.0\n\nLD %I0.2\nSTN %Q0.1"
      result (converter/convert-il-text complex-il)]
  (println "Complex IL:")
  (println complex-il)
  (println "\nExpected: Multiple rungs rendered properly")
  (if (:success result)
    (do
      (println "✅ SUCCESS! Complex conversion working!")
      (println "Result:")
      (println (render/render-ladder (:result result))))
    (println "❌ FAILED:" (:message result))))

(println)

;; Test 4: Use the original test functions from core.clj
(println "Test 4: Original core.clj test functions")
(println "\n=== quick-test ===")
(core/quick-test)

(println "\n=== test-il-conversion ===")
(core/test-il-conversion)

(println "\n=== Summary ===")
(println "✅ If you see properly formatted ladders above (no extra dashes)")
(println "✅ And IL conversion produces actual ladder content (not empty)")
(println "✅ Then the fixes are working correctly!")
(println "\n🎉 Your nested maps IOT Ladder Editor is now fixed!")
