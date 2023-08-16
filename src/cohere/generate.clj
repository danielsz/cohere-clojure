(ns cohere.generate
  (:require [clj-http.client :as client]
            [clojure.string :as str]))

(def openai "Suggest three names for an animal that is a superhero.

Animal: Cat
Names: Captain Sharpclaw, Agent Fluffball, The Incredible Feline
Animal: Dog
Names: Ruff the Protector, Wonder Canine, Sir Barks-a-Lot
Animal: Horse
Names:")

(def french "Translate the following sentence into French: Hi, how are you?")
(def hebrew "Translate the following sentence into Hebrew: Hi, how are you?")
(def spanish "Translate the following sentence into Spanish: Hi, how are you?")
(def clojure "Write a function in Clojure that produces the Fibonacci sequence.")

(def cohere "Please explain to me how LLMs work")
(defn no-framework [product] (str "Generate a social ad copy for the product: " product "."))
(defn aida-framework [product] (str "Generate an ad copy for the product: " product ".

The copy consists of four parts, following the AIDA Framework.
1 - Attention
2 - Interest
3 - Desire
4 - Action

The copy for each part is clear and concise."))


(defn generate [& {:keys [max_tokens num_generations truncate model p k presence_penalty frequency_penalty temperature prompt]
                   :or {max_tokens 1200
                        num_generations 2
                        truncate "NONE"
                        model "command"
                        p 0.95
                        k 0
                        presence_penalty 0
                        frequency_penalty 0
                        temperature 0.3}}]
  {:pre [(some? prompt)]}
  (let [options {:as :auto
                 :content-type :json                        
                 :headers {"Authorization" (str "Bearer " (System/getProperty "cohere.api.key"))}
                 :form-params {:max_tokens max_tokens
                               :num_generations num_generations
                               :truncate truncate
                               :prompt prompt
                               :model model
                               :p p
                               :k k
                               :presence_penalty presence_penalty
                               :frequency_penalty frequency_penalty
                               :temperature temperature}}]    
    (-> (client/post (str (System/getProperty "cohere.api.url") "/generate") options)
       :body)))
