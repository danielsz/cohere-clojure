(ns cohere.finetune
  (:require [clj-http.client :as client]
            [cheshire.core :as json]
            [cohere.dataset :as dataset]
            [cohere.client :refer [api-endpoint]]))

(def model-type-map {:generative "GENERATIVE"
                     :classify "CLASSIFICATION"
                     :rerank "RERANK"})

(def custom-model-status #{"UNKNOWN",
                           "CREATED",
                           "DATA_PROCESSING",
                           "FINETUNING",
                           "EXPORTING_MODEL",
                           "DEPLOYING_API",
                           "READY",
                           "FAILED",
                           "DELETED",
                           "DELETE_FAILED",
                           "CANCELLED",
                           "TEMPORARILY_OFFLINE",
                           "PAUSED",
                           "QUEUED"})

(defn create-signed-url [model-type name filename]
  {:pre [(some #{model-type} [:generative :classify :rerank])]}
  (let [data {:finetuneName name
              :fileName filename
              :finetuneType (model-type model-type-map)}
        options {:as :auto
                 :content-type :json                        
                 :headers {"Authorization" (str "Bearer " (System/getProperty "cohere.api.key"))}
                 :body (json/generate-string data)}]
    (-> (client/post (str api-endpoint "/finetune/GetFinetuneUploadSignedURL") options)
       :body)))

(defn upload-dataset [model-type name filename data]
  (let [{url :url gcspath :gcspath} (create-signed-url model-type name filename)]
    (client/put url {:content-type "text/plain"
                     :body data})
    gcspath))

(defn create-custom-model [name model-type dataset hyperparameters]
  {:pre [(some #{model-type} [:generative :classify :rerank])]}
  (let [train-files-remote-path (upload-dataset model-type name (dataset/train-file-name dataset) (dataset/get-train-data dataset))
        data (cond-> {:name name
                      :settings {:trainFiles [(assoc (.file-config dataset) :path train-files-remote-path)]                         
                                 :baseModel "medium"
                                 :finetuneType (model-type model-type-map)
                                 :hyperparameters hyperparameters}}
               (dataset/has-eval-file? dataset) (assoc-in [:settings :evalFiles] [(assoc (.file-config dataset) :path (upload-dataset model-type name (dataset/eval-file-name dataset) (dataset/get-eval-data dataset)))]))
        options {:as :auto
                 :content-type :json                        
                 :headers {"Authorization" (str "Bearer " (System/getProperty "cohere.api.key"))}
                 :body (json/generate-string data)}]
    (-> (client/post (str api-endpoint "/finetune/CreateFinetune") options)
       :body)))

(defn get-custom-model [id]
  (let [data {:finetuneID id}
        options {:as :auto
                 :content-type :json                        
                 :headers {"Authorization" (str "Bearer " (System/getProperty "cohere.api.key"))}
                 :body (json/generate-string data)}
        resp (client/post (str api-endpoint "/finetune/GetFinetune") options)]
    (when-let [warning (get-in resp [:headers "x-api-warning"])]
      (println warning))
    (:body resp)))

(defn get-custom-model-by-name [name]
  (let [data {:name name}
        options {:as :auto
                 :content-type :json                        
                 :headers {"Authorization" (str "Bearer " (System/getProperty "cohere.api.key"))}
                 :body (json/generate-string data)}
        resp (client/post (str api-endpoint "/finetune/GetFinetuneByName") options)]
    (when-let [warning (get-in resp [:headers "x-api-warning"])]
      (println warning))
    (:body resp)))

(defn get-custom-model-metrics [id]
  (let [data {:finetuneID id}
        options {:as :auto
                 :content-type :json                        
                 :headers {"Authorization" (str "Bearer " (System/getProperty "cohere.api.key"))}
                 :body (json/generate-string data)}]
    (client/post (str api-endpoint "/finetune/GetFinetuneMetrics") options)))

(defn list-custom-models [& {:keys [statuses before after orderBy]}]
  {:pre [(every? custom-model-status statuses)]}
  (let [data {:query {:statuses statuses
                      :before before
                      :after after
                      :orderBy orderBy}}
        options {:as :auto
                 :content-type :json                        
                 :headers {"Authorization" (str "Bearer " (System/getProperty "cohere.api.key"))}
                 :body (json/generate-string data)}]
    (-> (client/post (str api-endpoint "/finetune/ListFinetunes") options)
       :body)))

