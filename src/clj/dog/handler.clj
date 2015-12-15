(ns dog.handler
  (:require [compojure.core :refer [routing defroutes routes GET POST DELETE PUT]]
            [ring.middleware.json :refer [wrap-json-body]]
            [cemerick.friend :as friend]
            [cemerick.friend.credentials :as creds]
            [cemerick.friend.workflows :as workflows] 
            [ring.util.response :as r]
            [compojure.handler :as handler]
            [dog.auth :as auth]
            [dog.post :as p]
            [dog.misc :as m]
            [dog.dynamite :as d]
            [dog.jaguar :as j]
            [compojure.route :as cr]
            [selmer.parser :refer [render-file]]
            [selmer.filters :refer [add-filter!]]
            [markdown.core :refer [md-to-html-string]]
            [clojure.data.json :as json]
            [clojure.string :refer [blank?]]
            [clojure.core.async :refer [<!!]]
            [clojure.pprint :refer [pprint]]))

(add-filter! :markdown md-to-html-string)

(def auth-db {:subprotocol "mysql"
              :subname "//localhost:3306/auth_dev"
              :user "auth"
              :password "auth"})

(def about-content
  "Lorem ipsum dolor sit amet, consectetur adipisicing elit. Saepe nostrum ullam eveniet pariatur voluptates odit, fuga atque ea nobis sit soluta odio, adipisci quas excepturi maxime quae totam ducimus consectetur?

  Lorem ipsum dolor sit amet, consectetur adipisicing elit. Eius praesentium recusandae illo eaque architecto error, repellendus iusto reprehenderit, doloribus, minus sunt. Numquam at quae voluptatum in officia voluptas voluptatibus, minus!

  Lorem ipsum dolor sit amet, consectetur adipisicing elit. Nostrum molestiae debitis nobis, quod sapiente qui voluptatum, placeat magni repudiandae accusantium fugit quas labore non rerum possimus, corrupti enim modi! Et.
")

(def config {:subprotocol "mysql"
             :subname "//localhost:3306/dog"
             :user "root"
             :password "goose"})

(def heading "Man must explore, and this is exploration at its greatest")
(def subheading "Problems look mighty small from 150 miles up")
(def user "Mike")
(def date "August 24, 2014")

(def not-found (partial render-file "templates/message.html" {:title "Not found!"
                                                              :message "You tried to go somewhere that doesn't exist..."}))

(defn get-page
  [page-number page-size]
  (when (> page-number 0)
    (let [post-count (p/get-count config)
          lo (* (dec page-number) page-size)]
      (when (<= lo post-count)
        (let [previous-page (when (> page-number 1) (dec page-number))
              next-page (when (< (+ lo page-size) post-count) (inc page-number))
              posts (p/get-range config lo page-size)]
          {:page-number page-number
           :post-count post-count
           :next-page next-page
           :previous-page previous-page
           :posts posts})))))

(defn fix-date [f post] (update post :created f))
(defn fix-dates
  [f posts]
  (map (partial fix-date f) posts))

(defn home
  [{{page-number :page} :params}]
  (if-let [page (get-page (if page-number (m/parse-int page-number) 1) 4)]
    (let [dated (update page :posts #(fix-dates j/readable-date %))]
      (render-file "templates/index.html" dated))
    (not-found)))

(defn browse
  [{{page-number :page} :params}]
  (if-let [page (get-page (if page-number (m/parse-int page-number) 1) 10)]
    (let [dated (update page :posts #(fix-dates j/short-date %))]
      (render-file "templates/browse.html" dated))
    (not-found)))

(def username #(:username (friend/current-authentication)))

(defroutes site-routes
  (GET "/" request (home request))

  (GET "/index" request (home request))
  
  (GET "/about" request
       (render-file "templates/about.html" {:content about-content}))
  
  (GET "/post/:id" {{id :id} :params}
       (if-let [post (p/read config id)]
         (let [dated (update post :created j/readable-date)]
           (render-file "templates/post.html" dated))
           (not-found)))

  (GET "/admin/login" request
       (render-file "templates/login.html" {}))

  (GET "/admin/create" request
       (friend/authenticated
        (render-file "templates/create.html" {})))

  (GET "/admin" request
       (friend/authenticated
        (browse request)))

  (GET "/admin/browse" request
       (friend/authenticated
        (browse request)))

  (GET "/admin/edit/:id" {{id :id} :params}
       (friend/authenticated
        (if-let [post (p/read config id)]
          (render-file "templates/edit.html" {:id id
                                              :post (json/write-str (dissoc post :created :modified))})
          (not-found))))
  
  (DELETE "/admin/post/:id" {{id :id :as params} :params {:strs [referer]} :headers}
        (friend/authenticated
         (p/delete! config id)
         (r/status {} 204)))

  (POST "/admin/post" {params :params :as request}
       (friend/authenticated
        (p/create! config (assoc params :user (username)))
        (browse request)))
    
  (POST "/admin/post/:id" {{id :id :as params} :params :as request}
       (friend/authenticated
        (p/edit! config id (select-keys params [:title :subtitle :content]))
        (browse request)))
  
  (cr/resources "/")
  (cr/not-found (not-found)))

(def site-handler
  (handler/site
   (friend/authenticate
    site-routes
    {:allow-anon? true
     :login-uri "/admin/login"
     :default-landing-uri "/"
     :unauthorized-handler #(-> (render-file "templates/message.html" {:title "Forbidden"
                                                                       :message "You tried to go somewhere you can't go..."}) 
                                r/response
                                (r/status 401))
     :credential-fn #(creds/bcrypt-credential-fn (partial auth/get-user auth-db) %)
     :workflows [(workflows/interactive-form)]})))

(defn app
  [{:keys [headers request-method accept uri method] :as request}]
  ;; todo: real logging
  (site-handler request))

(def init #(println "dog is starting"))
(def destroy #(println "dog is shutting down"))
