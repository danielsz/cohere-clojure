(ns cohere.client
  (:require [clj-http.client :as client]))

(def api-endpoint "https://api.cohere.ai")

(defn check-api-key []
  (let [options {:as :auto
                 :content-type :json
                 :headers {"Authorization" (str "Bearer " (System/getProperty "cohere.api.key"))
                           "Cohere-Version" (System/getProperty "cohere.api.version")}}]
    (-> (client/post (str api-endpoint "/check-api-key") options )
       :body)))

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
    (-> (client/post (str api-endpoint "/detokenize") options)
       :body)))

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
                 :headers {"Authorization" (str "Bearer " (System/getProperty "cohere.api.key"))
                           "Cohere-Version" (System/getProperty "cohere.api.version")}
                 :form-params {:query query
                               :documents documents
                               :return_documents return_documents
                               :max_chunks_per_doc max_chunks_per_doc
                               :top_n top_n
                               :model model}}]
    (-> (client/post (str api-endpoint "/rerank") options)
       :body)))

(defn embed [& {:keys [texts model truncate] :or {model "embed-english-v2.0" truncate "END"}}]
  {:pre [(some #{truncate} ["NONE" "START" "END"]) (some? texts)]}
  (let [options {:as :auto
                 :content-type :json
                 :headers {"Authorization" (str "Bearer " (System/getProperty "cohere.api.key"))
                           "Cohere-Version" (System/getProperty "cohere.api.version")}
                 :form-params {:truncate truncate
                               :texts texts
                               :model model}}]
    (-> (client/post (str api-endpoint "/embed") options)
       :body)))

(defn classify [& {:keys [examples inputs model preset truncate] :or {model "embed-english-v2.0" truncate "END"}}]
  {:pre [(some #{truncate} ["NONE" "START" "END"]) (some? examples) (some? inputs)]}
  (let [options {:as :auto
                 :content-type :json
                 :headers {"Authorization" (str "Bearer " (System/getProperty "cohere.api.key"))
                           "Cohere-Version" (System/getProperty "cohere.api.version")}
                 :form-params {:truncate truncate
                               :examples examples
                               :inputs inputs
                               :model model
                               :preset preset}}]
    (-> (client/post (str api-endpoint "/classify") options)
       :body)))

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
                 :headers {"Authorization" (str "Bearer " (System/getProperty "cohere.api.key"))
                           "Cohere-Version" (System/getProperty "cohere.api.version")}
                 :form-params {:text text
                               :length length
                               :format format
                               :extractiveness extractiveness
                               :model model
                               :temperature temperature
                               :additional_command additional_command}}]
    (-> (client/post (str api-endpoint "/summarize") options)
       :body)))

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
                 :content-type :json                        
                 :headers {"Authorization" (str "Bearer " (System/getProperty "cohere.api.key"))
                           "Cohere-Version" (System/getProperty "cohere.api.version")}
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
    (-> (client/post (str api-endpoint "/generate") options)
       :body)))


(defn generate-feedback [& {:keys [request_id good_response model desired_response flagged_response flagged_reason prompt annotator_id] :as args}]
  (let [options {:as :auto
                 :content-type :json
                 :headers {"Authorization" (str "Bearer " (System/getProperty "cohere.api.key"))
                           "Cohere-Version" (System/getProperty "cohere.api.version")}
                 :form-params {:request_id request_id
                               :good_response good_response
                               :desired_response desired_response
                               :flagged_response flagged_response
                               :flagged_reason flagged_reason
                               :prompt prompt
                               :model model
                               :annotator_id annotator_id}}]
    (-> (client/post (str api-endpoint "/generate/feedback") options)
       :body)))

(defn generate-feedback-preference [& {:keys [ratings model prompt annotator_id] :as args}]
  (let [options {:as :auto
                 :content-type :json
                 :headers {"Authorization" (str "Bearer " (System/getProperty "cohere.api.key"))
                           "Cohere-Version" (System/getProperty "cohere.api.version")}
                 :form-params args}]
    (-> (client/post (str api-endpoint "/generate/feedback/preference") options)
       :body)))

(defn detect-language [& {:keys [texts]}]
  {:pre [(some? texts) (seq texts) (every? string? texts)]}
  (let [options {:as :auto
                 :content-type :json
                 :headers {"Authorization" (str "Bearer " (System/getProperty "cohere.api.key"))
                           "Cohere-Version" (System/getProperty "cohere.api.version")}
                 :form-params {:texts texts}}]
    (-> (client/post (str api-endpoint "/detect-language") options)
       :body)))

(defn chat [& {:keys [message conversation_id model return_chatlog return_prompt return_preamble chat_history preamble_override temperature max_tokens stream user_name p k logit_bias]
               :or {stream false
                    return_chatlog false
                    return_prompt false
                    temperature 0.8}}]
  {:pre [(some? message) (<= 0.0 temperature 5.0) (boolean? stream)]}
  (let [options {:as (if stream :stream :auto)
                 :content-type :json
                 :headers {"Authorization" (str "Bearer " (System/getProperty "cohere.api.key"))
                           "Cohere-Version" (System/getProperty "cohere.api.version")}
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
    (-> (client/post (str api-endpoint "/chat") options)
       :body)))


