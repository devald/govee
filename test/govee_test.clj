(ns govee-test
  (:require [clojure.test :refer :all])
  (:require [govee :refer [expired?]]
            [tick.core :as t]))

(deftest expired?-test
  (testing "Token should be valid"
    (let [now (t/now)
          token {:expiry (t/>> now (t/new-duration 57600 :seconds))}]
      (is (not (expired? token)))
      (is (not (expired? token (t/>> now (t/new-duration 1 :seconds)))))
      (is (not (expired? token (t/>> now (t/new-duration 6 :hours)))))
      (is (not (expired? token (t/>> now (t/new-duration 12 :hours)))))
      (is (not (expired? token (t/>> now (t/new-duration 57600 :seconds)))))
      ))
  (testing "Token should be invalid"
    (let [now (t/now)
          token {:expiry (t/>> now (t/new-duration 57600 :seconds))}]
      (is (expired? token (t/>> now (t/new-duration 57601 :seconds))))
      (is (expired? token (t/>> now (t/new-duration 1 :days))))
      )))
