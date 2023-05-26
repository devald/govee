(ns govee
  (:require [clj-http.client :as client]
            [cheshire.core :as json]))

(def loginURL "https://app2.govee.com/account/rest/account/v1/login")
(def deviceURL "https://app2.govee.com/device/rest/devices/v1/list")
(def email "")
(def password "")
(defonce uuid (random-uuid))

(def memo-post (memoize client/post))

(def token "Govee OAuth token"
  (->> (memo-post loginURL {:as          :json
                            :form-params {:email    email
                                          :password password
                                          :client   uuid}})
       :body
       :client
       :token))

(def device-list "List of devices"
  (->> (memo-post deviceURL {:as          :json
                             :oauth-token token})
       :body
       :devices))

device-list

(defn device-info "Get device information"
  [device-list]
  {:name        (:deviceName device-list)
   :temperature (double (/ (get (->> device-list
                                     :deviceExt
                                     :lastDeviceData
                                     json/parse-string) "tem") 100))})

(map device-info device-list)
