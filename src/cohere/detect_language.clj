(ns cohere.detect-language
  (:require [clj-http.client :as client]))

(defn detect-language [& {:keys [texts]}]
  {:pre [(some? texts) (seq texts) (every? string? texts)]}
  (let [options {:as :auto
                 :content-type :json
                 :headers {"Authorization" (str "Bearer " (System/getProperty "cohere.api.key"))}
                 :form-params {:texts texts}}]
    (-> (client/post (str (System/getProperty "cohere.api.url") "/detect-language") options )
       :body)))
