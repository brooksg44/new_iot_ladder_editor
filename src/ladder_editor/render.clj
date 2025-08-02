(ns ladder-editor.render
  "ASCII rendering for ladder diagrams that produces correct output"
  (:require [clojure.string :as str]))

(defn justify-text
  "Justify text within a given width"
  [text width align]
  (let [padding (max 0 (- width (count text)))]
    (case align
      :left (str text (str/join (repeat padding " ")))
      :right (str (str/join (repeat padding " ")) text)
      :center (let [left-pad (int (/ padding 2))
                    right-pad (- padding left-pad)]
                (str (str/join (repeat left-pad " "))
                     text
                     (str/join (repeat right-pad " ")))))))

(defn render-element
  "Render a single element with proper spacing"
  [element width]
  (let [name (:name element "")
        ascii (:ascii-art element "")
        name-line (if (empty? name)
                    (str/join (repeat width " "))
                    (justify-text name width :center))
        ascii-line (justify-text ascii width :center)]
    [name-line ascii-line]))

(defn render-rung
  "Render a rung as ASCII art"
  [rung]
  (let [elements (:elements rung)]
    (if (empty? elements)
      ["" ""]
      (let [element-renders (map #(render-element % (max 8 (count (:name % "")))) elements)
            name-line (str/join "  " (map first element-renders))
            ascii-line (str/join "──" (map second element-renders))]
        [name-line ascii-line]))))

(defn render-branch
  "Render a branch as ASCII art"
  [branch]
  (let [rungs (:rungs branch)
        rung-count (count rungs)]
    (if (empty? rungs)
      []
      (loop [result []
             rung-idx 0]
        (if (>= rung-idx rung-count)
          result
          (let [current-rung (nth rungs rung-idx)
                is-first? (zero? rung-idx)
                is-last? (= rung-idx (dec rung-count))
                [name-line ascii-line] (render-rung current-rung)
                
                ; Add branch symbols
                branch-name (if is-first?
                              (str "   " name-line)
                              (str "   " name-line))
                branch-ascii (cond
                               is-first? (str "─┬─" ascii-line)
                               is-last? (str " └─" ascii-line)
                               :else (str " ├─" ascii-line))]
            
            (recur (if is-first?
                     (conj result branch-name branch-ascii)
                     (conj result " │" branch-name branch-ascii))
                   (inc rung-idx))))))))

(defn render-ladder
  "Render a complete ladder as ASCII art"
  [ladder-data]
  (let [rungs (:rungs ladder-data)]
    (if (empty? rungs)
      "█\n█\n█"
      (let [rendered-rungs (map #(cond
                                   (= (:type %) :rung) (render-rung %)
                                   (= (:type %) :branch) (render-branch %)
                                   :else ["" ""])
                                rungs)
            all-lines (mapcat identity rendered-rungs)]
        (str "█\n"
             (str/join "\n" 
                       (map #(str "█─" %) all-lines))
             "\n█")))))

(defn ladder-statistics
  "Calculate ladder statistics"
  [ladder-data]
  (let [rungs (:rungs ladder-data)
        total-rungs (count rungs)
        total-elements (reduce + (map #(cond
                                         (= (:type %) :rung) (count (:elements %))
                                         (= (:type %) :branch) (reduce + (map (fn [r] (count (:elements r))) (:rungs %)))
                                         :else 0) rungs))
        total-branches (count (filter #(= (:type %) :branch) rungs))]
    {:total-rungs total-rungs
     :total-elements total-elements
     :total-branches total-branches}))

(defn render-ladder-detailed
  "Render ladder with detailed analysis"
  [ladder-data]
  (let [base-render (render-ladder ladder-data)
        stats (ladder-statistics ladder-data)]
    (str base-render
         "\n\n=== LADDER ANALYSIS ==="
         "\nTotal Rungs: " (:total-rungs stats)
         "\nTotal Elements: " (:total-elements stats)
         "\nTotal Branches: " (:total-branches stats))))
