(ns cohere.client
  (:require [clj-http.client :as client]))

(def api-endpoint "https://api.cohere.ai/v1")

(defn do-request [endpoint options]
  (let [resp (->> options
                (merge {:as :auto
                        :content-type :json
                        :oauth-token (System/getProperty "cohere.api.key")
                        :headers {"Request-Source" "clojure-sdk"}})
                (client/post (str api-endpoint endpoint)))]
    (when-let [warning (get-in resp [:headers "x-api-warning"])]
      (println warning))
    (:body resp)))

(defn check-api-key []
  (let [options {:as :auto
                 :oauth-token (System/getProperty "cohere.api.key")
                 :headers {"Request-Source" "clojure-sdk"}}]
    (:body (client/post "https://api.cohere.ai/check-api-key" options))))

(defn tokenize [& {:keys [text model] :or {model "command"}}]
  {:pre [(some? text) (<= 1 (count text) 65536)]}
  (let [options {:form-params {:text text
                               :model model}}]
    (do-request "/tokenize" options)))

(defn detokenize [& {:keys [tokens model] :or {model "command"}}]
  {:pre [(some? tokens) (seq tokens) (every? integer? tokens)]}
  (let [options {:form-params {:tokens tokens
                               :model model}}]
    (do-request "/detokenize" options)))

(defn rerank [& {:keys [query documents model top_n return_documents max_chunks_per_doc]
                    :or {model "rerank-english-v2.0"
                         return_documents false
                         max_chunks_per_doc 10
                         top_n (count documents)}}]
  {:pre [(some? query) (string? query)
         (some? documents)
         (some #{model} ["rerank-english-v2.0" "rerank-multilingual-v2.0"])
         (pos? top_n)]}
  (let [options {:form-params {:query query
                               :documents documents
                               :return_documents return_documents
                               :max_chunks_per_doc max_chunks_per_doc
                               :top_n top_n
                               :model model}}]
    (do-request "/rerank" options)))

(defn embed [& {:keys [texts model truncate] :or {model "embed-english-v2.0" truncate "END"}}]
  {:pre [(some #{truncate} ["NONE" "START" "END"]) (some? texts)]}
  (let [options {:form-params {:truncate truncate
                               :texts texts
                               :model model}}]
    (do-request "/embed" options)))

(defn classify [& {:keys [examples inputs model preset truncate] :or {model "embed-english-v2.0" truncate "END"}}]
  {:pre [(some #{truncate} ["NONE" "START" "END"]) (some? examples) (some? inputs)]}
  (let [options {:form-params {:truncate truncate
                               :examples examples
                               :inputs inputs
                               :model model
                               :preset preset}}]
    (do-request "/classify" options)))

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
  (let [options {:form-params {:text text
                               :length length
                               :format format
                               :extractiveness extractiveness
                               :model model
                               :temperature temperature
                               :additional_command additional_command}}]
    (do-request "/summarize" options)))

(defn generate [& {:keys [max_tokens num_generations truncate stream model p k presence_penalty frequency_penalty temperature prompt preset end_sequences stop_sequences return_likelihoods logit_bias]
                   :or {max_tokens 300
                        num_generations 1
                        truncate "END"
                        model "command"
                        p 0
                        k 0
                        presence_penalty 0
                        frequency_penalty 0
                        temperature 0.75
                        stream false
                        return_likelihoods "NONE"}}]
  {:pre [(some? prompt)
         (<= 1 num_generations 5)
         (boolean? stream)
         (some #{truncate} ["NONE" "START" "END"])
         (some #{return_likelihoods} ["GENERATION" "ALL" "NONE"])
         (float? temperature)
         (<= 0.0 temperature 5.0)
         (<= 0 k 500)
         (<= 0 p 0.99)
         (<= 0.0 presence_penalty 1.0)
         (< 1 max_tokens 4096)]}
  (let [options {:as (if stream :stream :auto)                 
                 :form-params {:max_tokens max_tokens
                               :num_generations num_generations
                               :truncate truncate
                               :prompt prompt
                               :model model
                               :p p
                               :k k
                               :stream stream
                               :presence_penalty presence_penalty
                               :frequency_penalty frequency_penalty
                               :temperature temperature
                               :preset preset
                               :end_sequences end_sequences
                               :stop_sequences stop_sequences
                               :return_likelihoods return_likelihoods
                               :logit_bias logit_bias}}]
    (do-request "/generate" options)))


(defn generate-feedback [& {:keys [request_id good_response model desired_response flagged_response flagged_reason prompt annotator_id]}]
  (let [options {:form-params {:request_id request_id
                               :good_response good_response
                               :desired_response desired_response
                               :flagged_response flagged_response
                               :flagged_reason flagged_reason
                               :prompt prompt
                               :model model
                               :annotator_id annotator_id}}]
    (do-request "/feedback/generate" options)))

(defn generate-feedback-preference [& {:keys [ratings model prompt annotator_id] :as args}]
  (let [options {:form-params args}]
    (do-request "/feedback/generate/preference" options)))

(defn detect-language [& {:keys [texts]}]
  {:pre [(some? texts) (seq texts) (every? string? texts)]}
  (let [options {:form-params {:texts texts}}]
    (do-request "/detect-language" options)))

(defn chat [& {:keys [message conversation_id model return_chatlog return_prompt return_preamble chat_history preamble_override temperature max_tokens stream user_name p k logit_bias]
               :or {stream false
                    return_chatlog false
                    return_prompt false
                    temperature 0.8}}]
  {:pre [(some? message) (<= 0.0 temperature 5.0) (boolean? stream)]}
  (let [options {:as (if stream :stream :auto)
                 :form-params {:message message
                               :conversation_id conversation_id
                               :model model
                               :return_chatlog return_chatlog
                               :return_prompt return_prompt
                               :return_preamble return_preamble
                               :chat_history chat_history
                               :preamble_override preamble_override
                               :temperature temperature
                               :max_tokens max_tokens
                               :stream stream
                               :user_name user_name
                               :p p
                               :k k
                               :logit_bias logit_bias}}]
    (do-request "/chat" options)))


