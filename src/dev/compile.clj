(ns dev.compile)

(defn compile-libs
  "Improving Development Startup Time - https://clojure.org/guides/dev_startup_time"
  [& args]
  (compile 'clj-http.client)
  (compile 'cheshire.core)
  (compile 'tick.core))
