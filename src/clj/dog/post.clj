(ns dog.post
  (:refer-clojure :exclude [read replace])
  (:require [dog.dynamite :as d]
            [dog.misc :as m]
            [dog.jaguar :as j]
            [clojure.string :refer [join replace]]
            [clojure.core.async :refer [go <!!]]
            [clojure.java.jdbc :as jdbc]
            [clojure.edn :as e]
            [clojure.set :refer [rename-keys]]
            [clojure.walk :refer [keywordize-keys]]
            [clj-time.core :as time]
            [clj-time.coerce :as coerce]))

(defn create-post-table!
  [config]
  (let [command (str "create table post"
                     "(id int not null auto_increment primary key, "
                     "user varchar(64) not null, "
                     "title varchar(256) not null, "
                     "subtitle varchar(256) not null, "
                     "content longtext null,"
                     "created timestamp default 0, "
                     "modified timestamp not null default current_timestamp on update current_timestamp)"
                     "default character set utf8 collate utf8_unicode_ci")]
    (jdbc/execute! config [command])
    {:status :ok}))

(defn read
  [config id]
  (first (jdbc/query config ["select id, user, title, subtitle, content, created from post where id = ?" id])))

(defn create!
  [config post]
  (jdbc/with-db-transaction [tx config]
    (let [id (d/insert! tx "post" (assoc post :created (j/now)))]
      (read tx id))))

(defn delete!
  [config id]
  (jdbc/delete! config "post" ["id = ?" id]))

(defn edit!
  [config id changes]
  (jdbc/with-db-transaction [tx config]
    (let [id (jdbc/update! tx "post" changes ["id = ?" id])]
      (read tx id))))

;; TODO: genericize
(defn get-range
  [config lo hi]
  (jdbc/query config ["select id, user, title, subtitle, created from post order by created desc limit ?, ?" lo hi]))

(defn get-count [config] (d/count-rows config "post"))

(def config {:subprotocol "mysql"
             :subname "//localhost:3306/entity_dev"
             :user "root"
             :password "goose"})

