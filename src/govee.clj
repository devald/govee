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

(def memo-post (memoize client/post))

(defonce svc
         {:now       t/now
          :http-post client/post
          :session   (atom nil)})

(defn new-session
  "Govee OAuth token"
  ([svc] (new-session svc email password uuid))
  ([{:as   _svc
     :keys [now http-post]}
    email password uuid]
   (let [{:as   _resp
          :keys [token
                 tokenExpireCycle]}
         (-> {:as          :json
              :form-params {:email    email
                            :password password
                            :client   uuid}}
             (->> (http-post loginURL))
             :body
             :client)]
     {:token  token
      :expiry (? (t/>> (now) (t/new-duration tokenExpireCycle :seconds)))})))

(defn expired?
  ([current-token] (expired? current-token (t/now)))
  ([current-token now]
   (if-let [expiry (:expiry current-token)]
     (t/< expiry now)
     true)))

(defn active-session
  "Govee OAuth token"
  [svc]
  (-> svc
      :session
      (swap! (fn [current-token]
               (if (expired? current-token)
                 (new-session svc email password uuid)
                 current-token)))))

(defn device-info "Get device information"
  [raw-device]
  (-> {:device/name        (:deviceName raw-device)
       :device/temperature (-> raw-device
                               :deviceExt
                               :lastDeviceData
                               json/parse-string
                               (get :tem)
                               (/ 100)
                               (double))}
      (with-meta raw-device)))

(defn device-list
  "List of devices"
  [{:as svc :keys [http-post]}]
  (-> {:as          :json
       :oauth-token (:token (active-session svc))}
      (->> (http-post deviceURL))
      :body
      :devices
      (->> (map device-info))))

(comment
  (set! *print-meta* true)
  (reset! session nil)
  (-> svc new-session)
  (t/>> (t/now)
        (t/new-duration 57600 :seconds))

  (-> @session ? expired?)
  (-> @session ? (expired? (-> (t/tomorrow) (t/at (t/time "12:38")) (t/offset-by 8))))
  (-> svc active-session)
  (-> svc (assoc :http-post memo-post) device-list ? first meta)

  )
