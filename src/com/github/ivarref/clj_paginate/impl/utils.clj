(ns com.github.ivarref.clj-paginate.impl.utils
  (:require [clojure.edn :as edn]
            [com.github.ivarref.clj-paginate.impl.bst :as bst])
  (:import (java.util UUID)
           (java.io StringWriter Writer)))


(defonce instance-id (delay (str (UUID/randomUUID))))


(defn balanced-tree
  [v opts]
  (let [srt-by (get opts :sort-by)
        v (vec (sort-by (apply juxt srt-by) v))]
    {:root         (bst/balanced-tree v)
     :input-vector v
     :id           (str (UUID/randomUUID))
     :instance-id  @instance-id
     :opts         {:sort-by srt-by}}))


(defn maybe-decode-cursor [cursor]
  (when cursor
    (when (string? cursor)
      (edn/read-string cursor))))


(defn get-cursor [opts]
  (or
    (let [cursor (or (get opts :after) (get opts :before))]
      (when (and (string? cursor) (not-empty cursor))
        (edn/read-string cursor)))
    {}))


(defn get-total-count [root keep? id decoded-cursor old-count]
  (if (and old-count (= id (:id decoded-cursor)))
    old-count
    (let [cnt (atom 0)]
      (bst/visit-all-depth-first
        root
        keep?
        (fn [_] (swap! cnt inc)))
      @cnt)))


(defn cursor-pre [cursor]
  (let [s (pr-str (dissoc cursor :cursor))
        s (subs s 0 (dec (count s)))]
    (str s " :cursor [")))


(defn node-cursor [cursor-pre node sort-attrs]
  (with-open [^Writer sw (StringWriter.)]
    (.append sw ^String cursor-pre)
    (doseq [attr sort-attrs]
      (print-method (get node attr) sw)
      (.append sw " "))
    (.append sw "]}")
    (.toString sw)))