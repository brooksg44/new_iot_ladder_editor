(ns ladder-editor.ui
  "cljfx-based user interface for the new ladder editor"
  (:require [cljfx.api :as fx]
            [ladder-editor.converter :as converter]
            [ladder-editor.render :as render]
            [ladder-editor.ladder :as ladder]
            [clojure.string :as str])
  (:import [javafx.stage FileChooser FileChooser$ExtensionFilter]))

;; Application state
(defonce *state
  (atom {:il-code ""
         :ladder-output ""
         :status "Ready"
         :status-type :info
         :output-format :ascii
         :last-conversion-success false}))

;; Sample IL code for examples
(def sample-il-code
  "LD %I0.0    ; Load input bit 0 (start button)
ANDN %I0.1  ; AND with inverted input bit 1 (stop button)
OR %Q0.0    ; OR with previous output (seal-in)
ST %Q0.0    ; Store result to output bit 0 (motor)

LD %I0.2    ; Load input bit 2 (emergency stop)
STN %Q0.1   ; Store inverted to alarm output")

;; Event handlers
(defn on-il-code-change [event]
  (swap! *state assoc :il-code event))

(defn on-convert-click [_event]
  (let [il-code (:il-code @*state)]
    (if (str/blank? il-code)
      (swap! *state assoc
             :status "Please enter some IL code"
             :status-type :warning
             :last-conversion-success false)
      (let [result (converter/convert-il-text il-code)]
        (if (:success result)
          (let [ladder-data (:result result)
                output (render/render-ladder ladder-data)]
            (swap! *state assoc
                   :ladder-output output
                   :status "Conversion successful"
                   :status-type :success
                   :last-conversion-success true))
          (swap! *state assoc
                 :ladder-output ""
                 :status (:message result)
                 :status-type :error
                 :last-conversion-success false))))))

(defn on-clear-click [_event]
  (swap! *state assoc
         :il-code ""
         :ladder-output ""
         :status "Cleared"
         :status-type :info
         :last-conversion-success false))

(defn on-load-example-click [_event]
  (swap! *state assoc
         :il-code sample-il-code
         :status "Example loaded - Click Convert to see result"
         :status-type :info
         :last-conversion-success false))

(defn on-three-wire-example-click [_event]
  (let [three-wire-ladder (converter/create-three-wire-example)
        output (render/render-ladder three-wire-ladder)]
    (swap! *state assoc
           :il-code "LD %I0.0\nORN %I0.1\nAND %Q0.0\nST %Q0.0"
           :ladder-output output
           :status "Three-wire example loaded"
           :status-type :success
           :last-conversion-success true)))

(defn on-output-format-change [event]
  (swap! *state assoc :output-format event)
  (when (:last-conversion-success @*state)
    (on-convert-click nil)))

(defn on-exit-click [_event]
  (println "New IOT Ladder Editor stopped")
  (System/exit 0))

;; UI Components
(defn status-style [status-type]
  (case status-type
    :success {:-fx-text-fill "green" :-fx-font-weight "bold"}
    :error {:-fx-text-fill "red" :-fx-font-weight "bold"}
    :warning {:-fx-text-fill "orange" :-fx-font-weight "bold"}
    :info {:-fx-text-fill "blue"}))

(defn create-toolbar []
  {:fx/type :tool-bar
   :items [{:fx/type :button
            :text "Convert"
            :on-action on-convert-click
            :style {:-fx-font-size "12px"}}
           {:fx/type :separator
            :orientation :vertical}
           {:fx/type :button
            :text "Clear All"
            :on-action on-clear-click}
           {:fx/type :separator
            :orientation :vertical}
           {:fx/type :button
            :text "Load Example"
            :on-action on-load-example-click}
           {:fx/type :separator
            :orientation :vertical}
           {:fx/type :button
            :text "Three-Wire Example"
            :on-action on-three-wire-example-click}]})

(defn root-view [{:keys [il-code ladder-output status status-type]}]
  {:fx/type :stage
   :showing true
   :title "New IOT Ladder Editor - Nested Maps Approach"
   :width 1200
   :height 800
   :on-close-request on-exit-click
   :scene {:fx/type :scene
           :root {:fx/type :border-pane
                  :top {:fx/type :h-box
                        :children [{:fx/type :button
                                    :text "Convert"
                                    :on-action on-convert-click}
                                   {:fx/type :button
                                    :text "Clear"
                                    :on-action on-clear-click}
                                   {:fx/type :button
                                    :text "Load Example"
                                    :on-action on-load-example-click}
                                   {:fx/type :button
                                    :text "Exit"
                                    :on-action on-exit-click}]}
                  :center {:fx/type :split-pane
                           :divider-positions [0.5]
                           :items [{:fx/type :v-box
                                    :children [{:fx/type :label
                                                :text "IL Code Input:"}
                                               {:fx/type :text-area
                                                :v-box/vgrow :always
                                                :text il-code
                                                :on-text-changed on-il-code-change
                                                :prompt-text "Enter IL code here..."}]}
                                   {:fx/type :v-box
                                    :children [{:fx/type :label
                                                :text "Ladder Diagram Output:"}
                                               {:fx/type :text-area
                                                :v-box/vgrow :always
                                                :text ladder-output
                                                :editable false
                                                :style {:-fx-font-family "monospace"}}]}]}
                  :bottom {:fx/type :h-box
                           :children [{:fx/type :label
                                       :text (str "Status: " status)}]}}}})

;; App definition
(def app
  (fx/create-app *state
                 :desc-fn (fn [state]
                            (root-view state))))

(defn start-app []
  (fx/mount-renderer *state #'app)
  (println "New IOT Ladder Editor started successfully"))

(defn stop-app []
  (fx/unmount-renderer *state #'app)
  (println "New IOT Ladder Editor stopped"))
