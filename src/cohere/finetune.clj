(ns cohere.finetune
  (:require [clj-http.client :as client]
            [clojure.string :as str]
            [cheshire.core :as json]
            [cohere.dataset :as dataset]))

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
    (-> (client/post (str (System/getProperty "cohere.api.url") "/finetune/GetFinetuneUploadSignedURL") options)
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
    (-> (client/post (str (System/getProperty "cohere.api.url") "/finetune/CreateFinetune") options )
       :body)))

(defn get-custom-model [id]
  (let [data {:finetuneID id}
        options {:as :auto
                 :content-type :json                        
                 :headers {"Authorization" (str "Bearer " (System/getProperty "cohere.api.key"))}
                 :body (json/generate-string data)}]
    (client/post (str (System/getProperty "cohere.api.url") "/finetune/GetFinetune") options )))

(defn get-custom-model-by-name [name]
  (let [data {:name name}
        options {:as :auto
                 :content-type :json                        
                 :headers {"Authorization" (str "Bearer " (System/getProperty "cohere.api.key"))}
                 :body (json/generate-string data)}]
    (client/post (str (System/getProperty "cohere.api.url") "/finetune/GetFinetuneByName") options )))

(defn get-custom-model-metrics [id]
  (let [data {:finetuneID id}
        options {:as :auto
                 :content-type :json                        
                 :headers {"Authorization" (str "Bearer " (System/getProperty "cohere.api.key"))}
                 :body (json/generate-string data)}]
    (client/post (str (System/getProperty "cohere.api.url") "/finetune/GetFinetuneMetrics") options )))

(defn list-custom-models [& {:keys [statuses before after orderBy] :as opts}]
  {:pre [(every? custom-model-status statuses)]}
  (let [data {:query {:statuses statuses
                      :before before
                      :after after
                      :orderBy orderBy}}
        options {:as :auto
                 :content-type :json                        
                 :headers {"Authorization" (str "Bearer " (System/getProperty "cohere.api.key"))}
                 :body (json/generate-string data)}]
    (-> (client/post (str (System/getProperty "cohere.api.url") "/finetune/ListFinetunes") options )
       :body)))

(defn csv->jsonl [file]
  (let [lines (str/split-lines (slurp file))]
    (doseq [line lines
          :let [els (str/split line #"\t")]]
      (spit "/tmp/eval.jsonl" (str (json/generate-string {:prompt (first els) :completion (last els)}) "\n") :append true))))

(defn prepare-dataset []
  (let [train-dataset-url "https://raw.githubusercontent.com/cohere-ai/notebooks/main/notebooks/data/content_rephrasing_train.jsonl"]
    (spit "/tmp/train.jsonl" (:body (client/get train-dataset-url)))
    (dataset/jsonl-dataset :train-file "/tmp/train.jsonl" :eval-file "/tmp/eval.jsonl")))

(defn jsonl->json [url]
  (let [jsonl (str/split-lines (:body (client/get url)))]
    (for [line jsonl]
      (json/parse-string line true))))


