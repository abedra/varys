(ns varys.core
  (:use clojure.tools.cli
        varys.spider)
  (:require [clojure.java.io :as io])
  (:gen-class))

(defn -main [& args]
  (let [[options args banner] (cli args
                                   ["-u" "--base-url" "Base Url"]
                                   ["-h" "--help" "Print Help" :default false :flag true])]
    (when (or (:help options) (not (:base-url options)))
      (println banner)
      (System/exit 0))

    (binding [*base-url* (:base-url options)]
      (push *base-url*)
      (while (not (.isEmpty queue))
        (let [link (.poll queue)
              response (response-code link)]
          (if (and (= 200 response) (not (contains? @visited link)))
            (do
              (swap! visited assoc link response)
              (try
                (doseq [url (extract (fetch link))]
                  (push url))
                (catch Exception e)))
            (swap! visited assoc link response)))))

    (with-open [out (io/writer "invalid-links.txt")]
      (let [invalid (keys (filter #(not= 200 (val %)) @visited))]
        (doseq [u invalid]
          (.write out (str u "\n")))))))