(ns cohere.rerank
  (:require [clj-http.client :as client]))

(defn rerank [& {:keys [query documents model top_n return_documents max_chunks_per_doc]
                    :or {model "rerank-english-v2.0"
                         return_documents false
                         max_chunks_per_doc 10
                         top_n (count documents)}}]
  {:pre [(some? query) (string? query)
         (some? documents)
         (some #{model} ["rerank-english-v2.0" "rerank-multilingual-v2.0"])
         (pos? top_n)]}
  (let [options {:as :auto
                 :content-type :json
                 :headers {"Authorization" (str "Bearer " (System/getProperty "cohere.api.key"))}
                 :form-params {:query query
                               :documents documents
                               :return_documents return_documents
                               :max_chunks_per_doc max_chunks_per_doc
                               :top_n top_n
                               :model model}}]
    (-> (client/post (str (System/getProperty "cohere.api.url") "/rerank") options )
       :body)))
