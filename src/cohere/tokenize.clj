(ns cohere.tokenize
  (:require [clj-http.client :as client]))

(def api-endpoint "https://api.cohere.ai")

(defn tokenize [& {:keys [text model] :or {model "command"}}]
  {:pre [(some? text) (<= 1 (count text) 65536)]}
  (let [options {:as :auto
                 :content-type :json
                 :headers {"Authorization" (str "Bearer " (System/getProperty "cohere.api.key"))
                           "Cohere-Version" (System/getProperty "cohere.api.version")}                 
                 :form-params {:text text
                               :model model}}]
    (-> (client/post (str api-endpoint "/tokenize") options )
       :body)))

(defn detokenize [& {:keys [tokens model] :or {model "command"}}]
  {:pre [(some? tokens) (seq tokens) (every? integer? tokens)]}
  (let [options {:as :auto
                 :content-type :json
                 :headers {"Authorization" (str "Bearer " (System/getProperty "cohere.api.key"))
                           "Cohere-Version" (System/getProperty "cohere.api.version")}
                 :form-params {:tokens tokens
                               :model model}}]
    (-> (client/post (str api-endpoint "/detokenize") options )
       :body)))
