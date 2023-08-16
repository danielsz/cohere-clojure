(ns cohere.embed
  (:require [clj-http.client :as client]))

(defn embed [& {:keys [texts model truncate] :or {model "embed-english-v2.0" truncate "END"}}]
  {:pre [(some #{truncate} ["NONE" "START" "END"]) (some? texts)]}
  (let [options {:as :auto
                 :content-type :json
                 :headers {"Authorization" (str "Bearer " (System/getProperty "cohere.api.key"))}
                 :form-params {:truncate truncate
                               :texts texts
                               :model model}}]
    (-> (client/post (str (System/getProperty "cohere.api.url") "/embed") options )
       :body)))
