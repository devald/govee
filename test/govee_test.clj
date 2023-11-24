(ns govee-test
  (:require [clojure.test :refer :all])
  (:require [govee :refer [expired?]]
            [tick.core :as t]))

(def now (t/now))
(def token {:expiry (t/>> now (t/new-duration 57600 :seconds))})

(deftest test-expired?
  (testing "The token should not be expired"
    (are [x y] (not (expired? x y))
               token now
               token (t/>> now (t/new-duration 1 :seconds))
               token (t/>> now (t/new-duration 6 :hours))
               token (t/>> now (t/new-duration 12 :hours))
               token (t/>> now (t/new-duration 57600 :seconds))))
  (testing "The token should be expired"
    (are [x y] (expired? x y)
               token (t/>> now (t/new-duration 57601 :seconds))
               token (t/>> now (t/new-duration 1 :days)))))
