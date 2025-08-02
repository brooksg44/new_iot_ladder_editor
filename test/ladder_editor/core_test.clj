(ns ladder-editor.core-test
  (:require [clojure.test :refer :all]
            [ladder-editor.ladder :as ladder]
            [ladder-editor.render :as render]
            [ladder-editor.converter :as converter]))

(deftest test-nested-maps-structure
  (testing "Nested maps approach creates correct structure"
    (let [contact (ladder/normally-open-contact "Test")
          coil (ladder/normal-coil "Output")]
      (is (= (:type contact) :contact))
      (is (= (:name contact) "Test"))
      (is (= (:normally-closed? contact) false))
      (is (= (:type coil) :coil))
      (is (= (:name coil) "Output")))))

(deftest test-rung-creation
  (testing "Rung creation with nested maps"
    (let [rung (ladder/rung
                (ladder/normally-open-contact "In1")
                (ladder/normal-coil "Out1"))]
      (is (= (:type rung) :rung))
      (is (= (count (:elements rung)) 2)))))

(deftest test-ladder-creation
  (testing "Ladder creation with nested maps"
    (let [ladder-data (ladder/ladder
                       (ladder/rung
                        (ladder/normally-open-contact "In1")
                        (ladder/normal-coil "Out1")))]
      (is (= (:type ladder-data) :ladder))
      (is (= (count (:rungs ladder-data)) 1)))))

(deftest test-ascii-rendering
  (testing "ASCII rendering produces output"
    (let [ladder-data (ladder/ladder
                       (ladder/rung
                        (ladder/normally-open-contact "In1")
                        (ladder/normal-coil "Out1")))
          output (render/render-ladder ladder-data)]
      (is (string? output))
      (is (clojure.string/includes? output "█"))
      (is (clojure.string/includes? output "In1"))
      (is (clojure.string/includes? output "Out1")))))

(deftest test-il-conversion
  (testing "IL to ladder conversion"
    (let [il-code "LD %I0.0\nST %Q0.0"
          result (converter/convert-il-text il-code)]
      (is (:success result))
      (is (= (:type (:result result)) :ladder)))))

(deftest test-three-wire-example
  (testing "Three-wire control example"
    (let [three-wire (converter/create-three-wire-example)
          output (render/render-ladder three-wire)]
      (is (= (:type three-wire) :ladder))
      (is (string? output))
      (is (clojure.string/includes? output "Stop"))
      (is (clojure.string/includes? output "Start"))
      (is (clojure.string/includes? output "Motor")))))
