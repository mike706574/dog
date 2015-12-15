(ns dog.auth
  (:require [dog.dynamite :as d]
            [dog.config :as c]
            [cemerick.friend.credentials :as creds]
            [clojure.java.jdbc :as jdbc]
            [clj-time.core :as time]
            [clj-time.coerce :as coerce]))

;; dupe
(defn now [] (coerce/to-date (time/now)))

(derive ::admin ::user)

(def user-table "users")
(def role-table "roles")
(def current-type-table "current_type")

(defn create-user-table!
  [config]
  (let [command (str "create table "
                     user-table
                     "(username varchar(32) primary key, "
                     "password char(60) not null, "
                     "created timestamp not null default '0000-00-00 00:00:00')"
                     "default character set utf8 collate utf8_unicode_ci")]
    (jdbc/execute! config [command])
    {:status :ok}))

(defn create-role-table!
  [config]
  (let [command (str "create table "
                     role-table
                     "(username varchar(32) not null, "
                     "role varchar(32) not null, "
                     "created timestamp not null default '0000-00-00 00:00:00', "
                     "primary key (username, role), "
                     "foreign key (username) references "
                     user-table
                     "(username) on delete cascade )"
                     "default character set utf8 collate utf8_unicode_ci")]
    (jdbc/execute! config [command])
    {:status :ok}))

(defn create-current-type-table!
  [config]
  (let [command (str "create table "
                     current-type-table
                     "(username varchar(32) not null, "
                     "type int null, "
                     "primary key (username)," 
                     "foreign key (username) references " user-table "(username) on delete cascade) "
                     "default character set utf8 collate utf8_unicode_ci")]
    (jdbc/execute! config [command])
    {:status :ok}))

(defn- parse-role
  [{role :role}]
  (case role
    "admin" ::admin
    (throw (RuntimeException. (str "Unrecognized role:" role)))))

(defn get-user
  [config username]
  (when-let [user (d/select-by-column config user-table :username username)]
    (let [roles (into #{::user} (d/select-equal config role-table :username username :row-fn parse-role))]
      (assoc user :roles roles))))

(defn delete-user!
  [config username]
  (jdbc/delete! config user-table ["username = ?" username]))

(defn taken?
  [config username]
  (d/select-by-column config user-table :username username))

(defn create-user!
  [config username password admin?]
  (jdbc/with-db-transaction [transaction config]
    (if (taken? transaction username)
      {:status :error :body {:username :taken}}
      (let [user {:username username
                  :password (creds/hash-bcrypt password)
                  :created (now)}]
        (jdbc/insert! transaction user-table user)
        (when admin? (jdbc/insert! transaction role-table {:username username :role "admin" :created (now)}))
        {:status :ok :body (get-user transaction username)}))))

(defn set-current-type!
  [config username type-id]
  (let [command (str "insert into "
                     current-type-table
                     " (username, type) values (?, ?) on duplicate key update type = ?")]
    (jdbc/execute! config [command username type-id type-id])))

(defn get-current-type
  [config username]
  (:type (d/select-by-column config current-type-table "username" username)))

(defn set-up!
  [config]
  (create-user-table! config)
  (create-role-table! config)
  (create-current-type-table! config)
  (create-user! config "admin" "admin" true)
  (create-user! config "mike" "mike" false))
