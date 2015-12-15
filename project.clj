(defproject dog "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/data.json "0.2.6"]

                 [markdown-clj "0.9.82"]
                 [selmer "0.9.5"]

                 ;; database
                 [mysql/mysql-connector-java "5.1.35"]
                 [org.clojure/java.jdbc "0.4.2"]

                 [org.apache.commons/commons-lang3 "3.4"]
                 
                 ;; webservice
                 [ring "1.4.0"]
                 [ring-server "0.4.0"]
                 [ring/ring-json "0.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [ring-middleware-accept "2.0.3"]
                 [compojure "1.3.4"]
                 [hiccup "1.0.5"]

                 [clj-time "0.11.0"]

                 [com.cemerick/friend "0.2.2-SNAPSHOT"]
                 
                 [clj-http "1.1.2"]
                 [com.cemerick/url "0.1.1"]

                 [org.clojure/clojurescript "1.7.107"]
                 [reagent "0.5.1"]
                 [cljs-http "0.1.37"]

                 [figwheel "0.3.7"]]
  :plugins [[lein-ring "0.8.12"]
            [lein-beanstalk "0.2.7"]
            [cider/cider-nrepl "0.9.1"]
            [hiccup-bridge "1.0.1"]
            
            [lein-cljsbuild "1.0.6"]
            [lein-figwheel "0.3.7"]]

  :ring {:handler dog.handler/app
         :init dog.handler/init
         :destroy dog.handler/destroy}
  :source-paths ["src/clj"]
  :test-paths ["test/clj"]
 
  :profiles {:uberjar {:aot :all}
             :production {:ring {:open-browser? false
                                 :stacktraces? false
                                 :auto-reload? false}
                          :cljsbuild {:builds
                                      {:client {:compiler {:optimizations :advanced
                                                           :elide-asserts true
                                                           :pretty-print false}}}}}
             
             :core {:cljsbuild
                    {:builds
                     {:client {:figwheel {:on-jsload "dog.core/edit"
                                          :repl false}
                               :compiler {:main "dog.core"
                                          :optimizations :none
                                          :asset-path "/js/client"}}}}}}
  
  :figwheel {:repl false}

  :cljsbuild {:builds {:client
                       {:source-paths ["src/clj" "src/cljs"]
                        :compiler {:output-dir "resources/public/js/client"
                                   :output-to "resources/public/js/client.js"}}}})
