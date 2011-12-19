(ns varys.core
  (:use [clojure.pprint :only (print-table)])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [net.cgrand.enlive-html :as html])
  (:import (java.net URL HttpURLConnection)
           (java.util.concurrent ConcurrentLinkedQueue))
  (:gen-class))

(def ^:dynamic *base-url* "http://thinkrelevance.com")
(def queue (ConcurrentLinkedQueue.))
(def visited (atom {}))

(defn normalize [url]
  (let [root (first (str/split url #"#"))]
    (cond
     (and (= \/ (first root)) (not= \h (first root))) (str *base-url* root)
     (not= \h (first root)) (str *base-url* "/" root)
     :else root)))

(defn push [link]
  (when-not (.contains queue link)
    (.add queue (normalize link))))

(defn extract [coll]
  (remove #(not (.contains % *base-url*))
          (map normalize
               (remove #(re-find #"mailto" %)
                       coll))))

(defn fetch [url]
  (map #(-> % :attrs :href)
       (html/select
        (html/html-resource (URL. url)) [:body [:a]])))

(defn response-code [address]
  (try
    (let [conn ^HttpURLConnection (.openConnection (URL. address))
          code (.getResponseCode conn)]
      (-> conn .getInputStream .close)
      code)
    (catch Exception e)))

(defn -main []
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
        (swap! visited assoc link response))))
  (with-open [out (io/writer "invalid-links.txt")]
    (let [invalid (keys (filter #(not= 200 (val %)) @visited))]
      (doseq [u invalid]
        (.write out (str u "\n"))))))
