(ns dog.misc
  (:require [clojure.string :as s]))

(defn third [x] (nth x 2))

(def blank? s/blank?)
(def not-blank? (comp not blank?))

(def not-nil? (comp not nil?)) 
(def not-empty? (comp not empty?))

(defn parse-int
  [s]
  #?(:clj (Integer/parseInt s)
     :cljs (js/parseInt s)))

(defn maps
  [f coll]
  (into #{} (map f coll)))

(defn mapm
  [f coll]
  (into {} (map f coll)))

(defn filterm
  [pred coll]
  (into {} (filter pred coll)))

(defn find-first
  [f coll]
  (first (filter f coll)))

(defn mapback
  [f coll]
  (into (empty coll) (map f coll)))

(defn fmap
  [f m]
  (into (empty m) (for [[k v] m] [k (f v)])))

(defn kmap
  [f m]
  (into (empty m) (for [[k v] m] [(f k) v])))

(defn get-first-index
  [item coll]
  (first (keep-indexed #(when (= item %2) %1) coll)))

(defn remove-by-index [coll pos] (vec (concat (subvec coll 0 pos) (subvec coll (inc pos)))))

(defn get-after
  [v item]
  (let [index (get-first-index item v)]
    (if (nil? index)
      nil
      (let [next-index (inc index)]
        (if (= next-index (count v))
          (first v)
          (get v next-index))))))

(defn cite
  [& args]
  (str "\"" (apply str args) "\""))
