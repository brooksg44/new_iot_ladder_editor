;; New IL to Ladder conversion that properly handles OR operations as branches

(ns ladder-editor.converter-v2
  "Enhanced IL converter that properly handles OR operations as branches"
  (:require [ladder-editor.ladder :as ladder]
            [ladder-editor.render :as render]
            [clojure.string :as str]))

;; First, copy over the basic functions we need
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

;; Enhanced IL to Ladder conversion that handles OR properly
(defn convert-il-to-ladder-smart
  "Smart IL to ladder conversion that handles OR as branches"
  [parsed-program]
  (if-not (= :program (:type parsed-program))
    (throw (ex-info "Invalid program structure"
                    {:received-type (:type parsed-program)})))
  
  (let [instructions (:instructions parsed-program)]
    (if (empty? instructions)
      {:type :ladder :rungs []}
      
      ;; Process instructions to build ladder structure
      (let [rungs (loop [remaining instructions
                         current-rung []
                         or-stack []  ; Stack for OR operations
                         all-rungs []]
                    
                    (if (empty? remaining)
                      ;; End of instructions - finish current rung if any
                      (if (seq current-rung)
                        (conj all-rungs (build-rung-with-branches current-rung or-stack))
                        all-rungs)
                      
                      (let [instr (first remaining)
                            op (:operation instr)
                            operand (:operand instr)]
                        
                        (case op
                          ;; LD/LDN - Start new rung or continue current
                          ("LD" "LDN")
                          (if (seq current-rung)
                            ;; We have a current rung, finish it and start new
                            (recur (rest remaining)
                                   [instr]
                                   []
                                   (conj all-rungs (build-rung-with-branches current-rung or-stack)))
                            ;; Start new rung
                            (recur (rest remaining)
                                   [instr]
                                   or-stack
                                   all-rungs))
                          
                          ;; AND/ANDN - Add to current series
                          ("AND" "ANDN")
                          (recur (rest remaining)
                                 (conj current-rung instr)
                                 or-stack
                                 all-rungs)
                          
                          ;; OR/ORN - Add to OR stack for branch creation
                          ("OR" "ORN")
                          (recur (rest remaining)
                                 current-rung
                                 (conj or-stack instr)
                                 all-rungs)
                          
                          ;; ST/STN - End current rung
                          ("ST" "STN")
                          (recur (rest remaining)
                                 []
                                 []
                                 (conj all-rungs (build-rung-with-branches (conj current-rung instr) or-stack)))
                          
                          ;; Default - add to current rung
                          (recur (rest remaining)
                                 (conj current-rung instr)
                                 or-stack
                                 all-rungs)))))]
        
        {:type :ladder
         :rungs (vec rungs)}))))

(defn build-rung-with-branches
  "Build a rung that may include branches for OR operations"
  [main-instructions or-instructions]
  (if (empty? or-instructions)
    ;; No OR operations, simple series rung
    (apply ladder/rung (map instruction-to-element main-instructions))
    
    ;; We have OR operations, need to create branches
    (let [main-elements (map instruction-to-element main-instructions)
          or-elements (map instruction-to-element or-instructions)
          
          ;; Find the insertion point (before the coil)
          coil-instructions (filter #(some #{(:operation %)} ["ST" "STN"]) main-instructions)
          non-coil-main (remove #(some #{(:operation %)} ["ST" "STN"]) main-instructions)
          
          ;; Create the main path and OR path  
          main-path (if (seq non-coil-main)
                      (apply ladder/rung (map instruction-to-element non-coil-main))
                      (ladder/rung))
          or-path (apply ladder/rung or-elements)
          
          ;; Create branch
          branch-section (ladder/branch main-path or-path)
          
          ;; Add coil if present
          coil-elements (map instruction-to-element coil-instructions)]
      
      (if (seq coil-elements)
        (apply ladder/rung (concat [branch-section] coil-elements))
        (ladder/rung branch-section)))))

(defn instruction-to-element
  "Convert an IL instruction to a ladder element"
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
      ;; Default
      {:type :unknown :name operand :operation op :ascii-art "─???─" :width 5})))

;; Test function
(defn test-or-conversion []
  (let [il-code "LD %I0.0\nANDN %I0.1\nOR %Q0.0\nST %Q0.0\n\nLD %I0.2\nSTN %Q0.1"
        parsed (parse-il-program il-code)
        result (convert-il-to-ladder-smart parsed)]
    (println "IL Code:")
    (println il-code)
    (println "\nResult:")
    (clojure.pprint/pprint result)
    (println "\nRendered:")
    (println (render/render-ladder result))))
