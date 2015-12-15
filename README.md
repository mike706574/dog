# dog

basic blog

stuff used:
  * [clojure](https://github.com/clojure)
  * [ring](https://github.com/ring-clojure)
  * [compojure](https://github.com/weavejester/compojure)
  * [selmer](https://github.com/yogthos/Selmer)
  * [reagent](https://github.com/reagent-project)
  * [startbootstrap-clean-blog](https://github.com/IronSummitMedia/startbootstrap-clean-blog)

## building and running

backend
  * dev: dog.repl/start-server
  * prod: lein install

frontend
  * dev: lein with-profile <profile> figwheel
  * prod: lein with-profile production compile
