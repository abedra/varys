(ns varys.spider
  (:use [clojure.pprint :only (print-table)])
  (:require [clojure.string :as str]
            [net.cgrand.enlive-html :as html])
  (:import (java.net URL HttpURLConnection)
           (java.util.concurrent ConcurrentLinkedQueue)))

(def ^:dynamic *base-url* nil)
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
