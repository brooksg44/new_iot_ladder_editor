(ns ladder-editor.core
  "Main entry point for the New IOT Ladder Editor"
  (:require [ladder-editor.ladder :as ladder]
            [ladder-editor.render :as render]
            [ladder-editor.converter :as converter]
            [clojure.pprint]))

(defn -main
  "Main entry point"
  [& args]
  (if (some #{"--cli"} args)
    (do
      (println "=== New IOT Ladder Editor (CLI Mode) ===")
      (println "Use (quick-test) or (test-il-conversion) in REPL"))
    (do
      (println "Starting New IOT Ladder Editor GUI...")
      (try
        ;; Dynamically require the UI namespace only when needed
        (require '[ladder-editor.ui :as ui])
        (let [start-app (resolve 'ladder-editor.ui/start-app)]
          (start-app))
        (catch Exception e
          (println "Failed to start GUI:" (.getMessage e))
          (println "This might be due to missing JavaFX. Try running in CLI mode with --cli")
          (System/exit 1))))))

;; Utility functions for REPL usage
(defn quick-test
  "Quick test of the nested maps approach"
  []
  (let [ladder-data (ladder/ladder
                     (ladder/rung
                      (ladder/normally-open-contact "Input1")
                      (ladder/normally-closed-contact "Input2")
                      (ladder/normal-coil "Output1")))]
    (println "Nested Maps Structure:")
    (clojure.pprint/pprint ladder-data)
    (println "\nRendered ASCII:")
    (println (render/render-ladder ladder-data))))

(defn test-il-conversion
  "Test IL to ladder conversion"
  []
  (let [il-code "LD %I0.0\nAND %I0.1\nST %Q0.0"
        result (converter/convert-il-text il-code)]
    (println "IL Code:")
    (println il-code)
    (println "\nConversion Result:")
    (if (:success result)
      (do
        (println "Success!")
        (println (render/render-ladder (:result result))))
      (println "Error:" (:message result)))))

(comment
  ;; REPL usage examples
  
  ;; Test the nested maps approach
  (quick-test)
  
  ;; Test IL conversion
  (test-il-conversion)
  
  ;; Start GUI
  (ui/start-app)
  
  ;; Stop GUI
  (ui/stop-app)
  
  ;; Demo ASCII rendering
  (demo-ascii-rendering))
