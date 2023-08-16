(ns cohere.classify
  (:require [clj-http.client :as client]
            [clojure.string :as str]))

(def foo ["Confirm your email address"
          "hey i need u to send some $"])
(def bar [{:text "Dermatologists don't like her!" :label "spam"}
          {:text "Hello, open to this?" :label "spam"}
          {:text "I need help please wire me $1000 right now" :label "spam"}
          {:text  "Nice to know you ;)" :label "spam"}
          {:text "Please help me?" :label "spam"}
          {:text "Your parcel will be delivered today" :label "not spam"} 
          {:text "Review changes to our Terms and Conditions" :label "not spam"}
          {:text "Weekly sync notes" :label "not spam"}
          {:text  "Re: Follow up from today’s meeting" :label "not spam"}
          {:text  "Pre-read for tomorrow" :label "not spam"}])


(def trec-thousand #(let [data (:body (client/get "https://cogcomp.seas.upenn.edu/Data/QA/QC/train_2000.label"))
                          lines (str/split data #"\n")]
                      (for [line lines
                            :let [s (str/split line #" ")
                                  label (first s)]
                            :when (not (some #{label} ["ENTY:religion" "NUM:temp" "NUM:weight"]))]
                        {:text (str/join " " (rest s))
                         :label  label})))

(defn classify [& {:keys [examples inputs model preset truncate] :or {model "embed-english-v2.0" truncate "END"}}]
  {:pre [(some #{truncate} ["NONE" "START" "END"]) (some? examples) (some? inputs)]}
  (let [options {:as :auto
                 :content-type :json
                 :headers {"Authorization" (str "Bearer " (System/getProperty "cohere.api.key"))}
                 :form-params {:truncate truncate
                               :examples examples
                               :inputs inputs
                               :model model
                               :preset preset}}]
    (-> (client/post (str (System/getProperty "cohere.api.url") "/classify") options )
       :body)))


