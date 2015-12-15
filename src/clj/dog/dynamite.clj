(ns dog.dynamite
  (:refer-clojure :exclude [replace])
  (:require [dog.jaguar :refer [runtime]]
            [clojure.java.jdbc :as jdbc]
            [clojure.set :refer [union]]
            [clojure.string :refer [join replace]]))

(defn count-rows
  [config table]
  (let [query (str "select count(*) as count from " table)
        result (clojure.java.jdbc/query config [query])]
    (:count (first result))))

(defn id-taken?
  [config table id]
  (let [query (str "select id from " table " where id = ?")
        results (jdbc/query config [query id])]
    (not (empty? results))))

(defn gen-rand-id
  [row-count]
  (inc (rand-int row-count)))

(defn gen-rand-ids
  [n row-count]
  (take n (repeatedly (partial gen-rand-id row-count))))

(defn gen-id-set
  [n row-count]
  (into #{} (gen-rand-ids n row-count)))

(defn take-random
  [n available]
  (into #{} (take n (repeatedly #(rand-nth available)))))

(defn take-unique-random
  [n available]
  (if (> n (count available))
    (runtime "Requested " n " unique items, but only " (count available) " available")
    (loop [taken #{}]
      (let [left (- n (count taken))]
        (if (= 0 left)
          taken
          (recur (union taken (take-random left available))))))))

(defn gen-unique-rand-ids
  [n row-count]
  (if (> n row-count)
    (runtime "Requested " n " unique ids, but only " row-count " available")
    (loop [ids #{}]
      (let [left (- n (count ids))]      
        (if (= 0 left)
          ids
          (recur (union ids (gen-id-set left row-count))))))))

(defn select-by-ids
  [config table ids]
  (if (empty? ids) []
      (let [query (str "select * from " table " where id in (" (join "," ids) ")")
            result (jdbc/query config [query])]
        (vec result))))

(defn select-by-id-range
  [config table start-id end-id]
  (let [query (str "select * from " table " where id between ? and ?")]
    (jdbc/query config [query start-id end-id])))

(defn select-by-column
  [config table col v]
  (let [query (str "select * from " table " where " (name col) " = ?")]
    (first (jdbc/query config [query v]))))

(defn select-by-id [config table id] (select-by-column config table :id id))
(defn select-by-user [config table id] (select-by-column config table :user id))

(defn select-all
  [config table & {:keys [row-fn] :or {row-fn identity}}]
   (vec (jdbc/query config [(str "select * from " table)] :row-fn row-fn)))

(defn select-equal
  [config table column value & {:keys [row-fn] :or {row-fn identity}}]
  (let [query (str "select * from " table " where " (name column) " = ?")]
    (vec (jdbc/query config [query value] :row-fn row-fn))))

;; return maps
(defn get-row-by-id
  [config table column id]
  (let [row-count (count-rows config table)]
    (if (= row-count 0)
      {:status :empty :message (str "Table " (name table) " is empty.")}
      (let [query (str "select * from " (name table) " where " (name column) " = ?")
            row (first (jdbc/query config [query id]))]
        (if (nil? row)
          {:status :missing :message (str "No row found with ID " id " in table " (name table))}
          {:status :ok :body row})))))

(defn get-random-row
  [config table column]
  (try
    (let [row-count (count-rows config table)]
      (if (= row-count 0)
        {:status :empty :message (str "Table " (name table) " is empty.")}
        (let [id (gen-rand-id row-count)
              result (get-row-by-id config table column id)]
          (if (= (:status result) :missing)
            {:status :error :message (str "Failed to generate a random ID for column " column ".")}
            result))))
    (catch Exception e {:status :error :message (.getMessage e) :exception e})))

(defn get-rows-by-ids
  [config table ids]
  (let [query (str "select * from " (name table) " where id in (" (join "," ids) ")")
        result (jdbc/query config [query])]
    (into [] result)))

(defn get-random-rows
  [config table column n]
  (let [row-count (count-rows config table)]
    (if (= row-count 0)
      {:status :empty
       :message (str "Table " (name table) " is empty.")}
      (if (> n row-count)
        {:status :not-enough
         :row-count row-count
         :message (str "Requested " n " unique rows from table " (name table) ", but only " row-count " exist.")}
        (let [ids (gen-unique-rand-ids n row-count)
              rows (get-rows-by-ids config table ids)]
          {:status :ok :body rows})))))

(defn get-rows-in-range
  [config table column start-id end-id]
  (let [row-count (count-rows config table)]
    (if (= row-count 0)
      {:status :empty :message (str "Table " (name table) " is empty.")}
      (if (< start-id 1)
        {:status :error :message (str end-id " is not a valid ID.")}
        (if (> end-id row-count)
          {:status :error :message (str "Requested rows from ID " start-id " to " end-id ", but only " row-count " exist.")}
          (let [query (str "select * from " (name table) " where id between ? and ?")
                rows (jdbc/query config [query start-id end-id])]
            {:status :ok :body rows}))))))

(defn insert!
  [config table row]
  (:generated_key (first (jdbc/insert! config table row))))

(defn no-row?
  [config table id]
  (nil? (select-by-id config table id)))

;; maintenance
(defmacro defquiet
  [fn-symbol args & body]
  `(def ~fn-symbol
     (fn ~args
       (try
         ~(conj body `do)
         (catch Exception e#)))))

(defn drop-table!
  [config table]
  (jdbc/db-do-commands config (jdbc/drop-table-ddl table)))

(defquiet drop-table-quietly!
  [config table]
  (jdbc/db-do-commands config (jdbc/drop-table-ddl table)))

(defn drop-database!
  [config name]
  (let [command (str "drop database " name)]
    (jdbc/execute! config [command])))

(defn create-database!
  [config name]
  (let [command (str "create database " name " CHARACTER SET utf8 COLLATE utf8_general_ci")]
    (jdbc/execute! config [command])))

(defn drop-tables!
  [config tables]
  (doseq [table tables] (drop-table! config table)))

(defn drop-tables-quietly!
  [config tables]
  (doseq [table tables] (drop-table-quietly! config table)))

(defn get-table-names
  [db]
  (map :table_name (jdbc/with-db-metadata [md db]
       (jdbc/metadata-result (.getTables md nil nil nil (into-array ["TABLE" "VIEW"]))))))

(defn drop-all-tables!
  [db]
  (doseq [table-name (get-table-names db)]
    (jdbc/execute! db [(str "drop table " table-name)]))
  {:status :ok})

(defn drop-database!
  [config database-name]
  (jdbc/execute! config [(str "drop database " database-name)])
  {:status :ok})

(defn create-database!
  [config database-name]
  (let [command (str "create database "
                     database-name
                     " character set utf8 collate utf8_general_ci;")]
    (jdbc/execute! config [command])
    {:status :ok}))
