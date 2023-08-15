(ns cohere.dataset
  (:require [clojure.string :as str]
            [clojure.java.io :as io]))

(def default-file-config {:separator ""
                          :switchColumns false
                          :hasHeader false
                          :delimiter ""})

(defprotocol Dataset
  (file-config [this])
  (train-file-name [this])
  (eval-file-name [this])
  (has-eval-file? [this])
  (get-train-data [this])
  (get-eval-data [this]))

(defrecord LocalFileCustomModelDataset [train-file eval-file file-config]
  Dataset
  (file-config [this]
    file-config)
  (train-file-name [this]
    (let [file (io/file train-file)]
      (when (.exists file)
        (.getName file))))
  (eval-file-name [this]
    (when (some? eval-file)
      (let [file (io/file eval-file)]
        (when (.exists file)
          (.getName file)))))
  (has-eval-file? [this]
    (boolean eval-file))
  (get-train-data [this]
    (slurp train-file))
  (get-eval-data [this]
    (when (some? eval-file)
      (slurp eval-file))))


(defn new-local-file-custom-model-dataset [& {:keys [train-file eval-file file-config] :or {file-config default-file-config}}]
  (map->LocalFileCustomModelDataset {:train-file train-file :eval-file eval-file :file-config file-config}))

(defn csv-dataset [& {:keys [train-file eval-file delimiter]}]
  (new-local-file-custom-model-dataset :train-file train-file :eval-file eval-file :file-config (assoc default-file-config :delimiter delimiter)))
(defn jsonl-dataset [& {:keys [train-file eval-file file-config] :as options}]
  (new-local-file-custom-model-dataset options))
(defn text-dataset [& {:keys [train-file eval-file separator]}]
  (new-local-file-custom-model-dataset :train-file train-file :eval-file eval-file :file-config (assoc default-file-config :separator separator)))
(defn in-memory-dataset [& {:keys [train-file eval-file] :as options}])
