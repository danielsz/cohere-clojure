(ns cohere.summarize
  (:require [clj-http.client :as client]))


(defn summarize [& {:keys [text length format model extractiveness temperature additional_command]
                    :or {model "command"
                         length "medium"
                         format "paragraph"
                         extractiveness "low"
                         temperature 0}}]
  {:pre [(some? text) (string? text) (<= 1 (count text) 100000)
         (some #{length} ["short" "medium" "long" "auto"])
         (some #{format} ["paragraph" "bullets" "auto"])
         (some #{extractiveness} ["low" "medium" "high" "auto"])
         (<= 0 temperature 5)]}
  (let [options {:as :auto
                 :content-type :json
                 :headers {"Authorization" (str "Bearer " (System/getProperty "cohere.api.key"))}
                 :form-params {:text text
                               :length length
                               :format format
                               :extractiveness extractiveness
                               :model model
                               :temperature temperature
                               :additional_command additional_command}}]
    (-> (client/post (str (System/getProperty "cohere.api.url") "/summarize") options )
       :body)))
