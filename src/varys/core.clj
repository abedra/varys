(ns varys.core
  (:use clojure.tools.cli
        varys.spider)
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:gen-class))

(defn write-results []
  (with-open [out (io/writer "invalid-links.txt")]
    (let [invalid (keys (filter #(not= 200 (val %)) @visited))]
      (doseq [u invalid]
        (.write out (str u "\n"))))))

(defn asset? [url]
  (let [extension (last (str/split url #"\."))]
    (or (= "pdf" extension)
        (= "zip" extension)
        (= "jpg" extension))))

(defn crawl [options]
  (binding [*base-url* (:base-url options)]
    (push *base-url*)
    (while (not (.isEmpty queue))
      (let [link (.poll queue)
            response (response-code link)]
        (if (and (= 200 response) (not (contains? @visited link)))
          (do
            (swap! visited assoc link response)
            (when-not (asset? link)
              (try
                (doseq [url (extract (fetch link))]
                  (push url))
                (catch Exception e
                  (println (str "Exception extracting from: " link))))))
          (swap! visited assoc link response))))))

(defn -main [& args]
  (let [[options args banner] (cli args
                                   ["-u" "--base-url" "Base Url"]
                                   ["-h" "--help" "Print Help" :default false :flag true])]
    (when (or (:help options) (not (:base-url options)))
      (println banner)
      (System/exit 0))
    (crawl options)
    (write-results)))