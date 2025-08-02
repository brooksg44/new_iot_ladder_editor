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
  "Group IL instructions into logical rungs based on LD/ST patterns - REALLY FIXED"
  [instructions]
  (if (empty? instructions)
    []
    (let [groups (reduce
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
                  instructions)]
      ;; Filter out any empty groups and ensure we have actual groups
      (vec (filter #(and (seq %) (not-empty %)) groups)))))

(defn convert-instruction-group-to-rung
  "Convert a group of instructions to a ladder rung"
  [instruction-group]
  (let [elements (map il-instruction-to-ladder-element instruction-group)]
    (apply ladder/rung elements)))

(defn partition-by-ld-st
  "Partition instructions into groups based on LD...ST boundaries"
  [instructions]
  (loop [remaining instructions
         current-group []
         all-groups []]
    (if (empty? remaining)
      (if (seq current-group)
        (conj all-groups current-group)
        all-groups)
      (let [instr (first remaining)
            op (:operation instr)]
        (cond
          ;; LD/LDN starts new group (unless it's the very first)
          (and (some #{op} ["LD" "LDN"]) (seq current-group))
          (recur (rest remaining)
                 [instr]
                 (conj all-groups current-group))
          
          ;; Add to current group
          :else
          (recur (rest remaining)
                 (conj current-group instr)
                 all-groups))))))

(defn split-by-or
  "Split instruction group into before-OR, OR operations, and after-OR"
  [instructions]
  (let [or-index (first (keep-indexed #(when (some #{(:operation %2)} ["OR" "ORN"]) %1) instructions))]
    (if or-index
      (let [before (take or-index instructions)
            or-and-after (drop or-index instructions)
            or-ops (take-while #(some #{(:operation %)} ["OR" "ORN"]) or-and-after)
            after (drop (count or-ops) or-and-after)]
        {:before before :or or-ops :after after})
      {:before instructions :or [] :after []})))

(defn convert-group-to-rung
  "Convert a group of instructions to a rung, handling OR operations"
  [instruction-group]
  (let [;; Split into parts: everything before OR, OR operations, everything after
        {before-or :before or-ops :or after-or :after} (split-by-or instruction-group)]
    
    (if (empty? or-ops)
      ;; No OR operations, simple series
      (apply ladder/rung (map il-instruction-to-ladder-element instruction-group))
      
      ;; Has OR operations, create branch
      (let [main-path-elements (map il-instruction-to-ladder-element before-or)
            or-path-elements (map il-instruction-to-ladder-element or-ops)  
            coil-elements (map il-instruction-to-ladder-element after-or)
            
            ;; Create main path and OR path as separate rungs
            main-path (if (seq main-path-elements)
                        (apply ladder/rung main-path-elements)
                        (ladder/rung))
            or-path (apply ladder/rung or-path-elements)
            
            ;; Create branch
            branch-section (ladder/branch main-path or-path)]
        
        ;; Combine branch with coil
        (if (seq coil-elements)
          (apply ladder/rung (concat [branch-section] coil-elements))
          (ladder/rung branch-section))))))

(defn convert-instructions-to-rungs
  "Convert instruction list to rungs, handling OR as branches"
  [instructions]
  (let [groups (partition-by-ld-st instructions)]
    (map convert-group-to-rung groups)))

(defn convert-il-to-ladder-improved
  "Improved IL to ladder conversion with basic OR handling"
  [parsed-program]
  (if-not (= :program (:type parsed-program))
    (throw (ex-info "Invalid program structure"
                    {:received-type (:type parsed-program)})))
  
  (let [instructions (:instructions parsed-program)]
    (if (empty? instructions)
      {:type :ladder :rungs []}
      
      ;; Simple approach: detect OR patterns and create branches
      (let [rungs (convert-instructions-to-rungs instructions)]
        {:type :ladder :rungs (vec rungs)}))))

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
