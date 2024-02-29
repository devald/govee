(ns govee
  (:require
    [clj-http.client :as client]
    [cheshire.core :as json]
    [tick.core :as t]
    [clojure.pprint :as pp]))

(def loginURL "https://app2.govee.com/account/rest/account/v1/login")
(def deviceURL "https://app2.govee.com/device/rest/devices/v1/list")
(def email "")
(def password "")
(defonce uuid (random-uuid))
(defn ? [x] (doto x pp/pprint))

(defonce svc {:now       t/now
              :http-post client/post
              :session   (atom nil)})

(defn get-token
  "It gets a new OAuth token and returns it with its expiry time.

  Example:

  {:token \"xyz\", :expiry #time/instant\"xyz\"}"
  ([] (get-token svc email password uuid))
  ([{:keys [now http-post]} email password uuid]
   (let [{:keys [token tokenExpireCycle]}
         (-> {:as          :json
              :form-params {:email    email
                            :password password
                            :client   uuid}}
           (->> (http-post loginURL))
           :body
           :client)]
     {:token  token
      :expiry (t/>> (now) (t/new-duration tokenExpireCycle :seconds))})))

(comment
  (get-token))

(defn expired?
  "Returns true if token is expired."
  ([token] (expired? token (t/now)))
  ([token now]
   (if-let [expiry (:expiry token)]
     (t/< expiry now)
     true)))

(comment
  (-> svc :session deref expired?))

(defn session
  "Create an active session."
  []
  (-> svc
    :session
    (swap! (fn [token]
             (if (expired? token)
               (get-token)
               token)))))

(comment
  (session))

(defn get-info
  "It gets device information."
  [device]
  (-> {:device/name        (:deviceName device)
       :device/temperature (-> device
                             :deviceExt
                             :lastDeviceData
                             (json/parse-string true)
                             (get :tem)
                             (/ 100)
                             (double))
       :device/humidity    (-> device
                             :deviceExt
                             :lastDeviceData
                             (json/parse-string true)
                             (get :hum)
                             (/ 100)
                             (double))}
    (with-meta device)))

(defn get-devices
  "It gets a list of devices."
  ([] (get-devices svc))
  ([{:keys [http-post]}]
   (-> {:as          :json
        :oauth-token (:token (session))}
     (->> (http-post deviceURL))
     :body
     :devices
     (->> (map get-info)))))

(comment
  (set! *print-meta* true)
  (get-devices)
  (-> get-devices meta))
