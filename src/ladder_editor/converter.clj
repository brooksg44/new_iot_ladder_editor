(ns ladder-editor.converter
  "Convert IL (Instruction List) to Ladder Diagram using nested maps"
  (:require [ladder-editor.ladder :as ladder]
            [ladder-editor.render :as render]
            [clojure.string :as str]))

(defn parse-il-instruction
  "Parse a single IL instruction line"
  [line]
  (let [line (str/trim line)]
    (when-not (or (empty? line) (str/starts-with? line ";"))
      (let [parts (str/split line #"\s+")
            operation (first parts)
            operand (second parts)]
        {:operation operation
         :operand operand}))))

(defn parse-il-program
  "Parse an IL program into instructions"
  [il-code]
  (when-not (str/blank? il-code)
    (let [lines (str/split-lines il-code)
          instructions (keep parse-il-instruction lines)]
      {:type :program
       :instructions (vec instructions)})))

(defn il-instruction-to-ladder-element
  "Convert an IL instruction to a ladder element using nested maps"
  [instruction]
  (let [op (:operation instruction)
        operand (:operand instruction)]
    (case op
      "LD"   (ladder/normally-open-contact operand)
      "LDN"  (ladder/normally-closed-contact operand)
      "ST"   (ladder/normal-coil operand)
      "STN"  (ladder/negated-coil operand)
      "AND"  (ladder/normally-open-contact operand)
      "ANDN" (ladder/normally-closed-contact operand)
      "OR"   (ladder/normally-open-contact operand)
      "ORN"  (ladder/normally-closed-contact operand)
      ;; Default fallback
      {:type :unknown
       :operation op
       :operand operand
       :ascii-art "─???─"
       :width 5})))

(defn convert-il-to-ladder
  "Convert IL program to ladder diagram using nested maps approach"
  [parsed-program]
  (if-not (= :program (:type parsed-program))
    (throw (ex-info "Invalid program structure"
                    {:received-type (:type parsed-program)})))
  
  (let [instructions (:instructions parsed-program)]
    (if (empty? instructions)
      {:type :ladder
       :rungs []}
      ;; Convert instructions to ladder elements and group into rungs
      (let [elements (map il-instruction-to-ladder-element instructions)
            ;; Simple approach: each instruction becomes an element in a single rung
            ;; More sophisticated logic would group elements based on LD/ST boundaries
            rung-elements (vec elements)]
        {:type :ladder
         :rungs [(ladder/rung rung-elements)]}))))

(defn group-instructions-into-rungs
  "Group IL instructions into logical rungs based on LD/ST patterns"
  [instructions]
  (let [groups (reduce
                (fn [acc instruction]
                  (let [op (:operation instruction)]
                    (cond
                      ;; Start new rung on LD/LDN
                      (some #{op} ["LD" "LDN"])
                      (conj acc [instruction])
                      
                      ;; Add to current rung
                      (seq acc)
                      (conj (vec (butlast acc))
                            (conj (last acc) instruction))
                      
                      ;; First instruction is not LD/LDN
                      :else
                      [[instruction]])))
                []
                instructions)]
    (vec groups)))

(defn convert-instruction-group-to-rung
  "Convert a group of instructions to a ladder rung"
  [instruction-group]
  (let [elements (map il-instruction-to-ladder-element instruction-group)]
    (ladder/rung elements)))

(defn convert-il-to-ladder-improved
  "Improved IL to ladder conversion with proper rung grouping"
  [parsed-program]
  (if-not (= :program (:type parsed-program))
    (throw (ex-info "Invalid program structure"
                    {:received-type (:type parsed-program)})))
  
  (let [instructions (:instructions parsed-program)]
    (if (empty? instructions)
      {:type :ladder
       :rungs []}
      (let [instruction-groups (group-instructions-into-rungs instructions)
            rungs (map convert-instruction-group-to-rung instruction-groups)]
        {:type :ladder
         :rungs (vec rungs)}))))

(defn create-three-wire-example
  "Create the three-wire control example using nested maps"
  []
  (ladder/ladder
   (ladder/rung
    (ladder/normally-closed-contact "Stop")
    (ladder/branch
     (ladder/rung (ladder/normally-open-contact "Start"))
     (ladder/rung (ladder/normally-open-contact "Motor")))
    (ladder/normal-coil "Motor"))))

;; Public API functions
(defn convert-il-text
  "Convert IL text to ladder diagram"
  [il-text]
  (try
    (let [parsed (parse-il-program il-text)
          ladder-data (convert-il-to-ladder-improved parsed)]
      {:success true
       :result ladder-data
       :message "Conversion successful"})
    (catch Exception e
      {:success false
       :error :conversion-error
       :message (.getMessage e)
       :details (ex-data e)})))

(defn render-il-as-ladder
  "Convert IL text and render as ASCII ladder"
  [il-text]
  (let [result (convert-il-text il-text)]
    (if (:success result)
      (render/render-ladder (:result result))
      (str "Error: " (:message result)))))
