(ns dog.core
  (:require [reagent.core :as reagent]))

(enable-console-print!)

(defn set-property!
  [state k v e]
  (swap! state assoc k v)
  (.preventDefault e))


(defn editor [state]
  [:textarea.form-control
   {:value (:content @state)
    :name "content"
    :on-change #(swap! state assoc :content (-> % .-target .-value))}])

(defn highlight-code [html-node]
  (let [nodes (.querySelectorAll html-node "pre code")]
    (loop [i (.-length nodes)]
      (when-not (neg? i)
        (when-let [item (.item nodes i)]
          (.highlightBlock js/hljs item))
        (recur (dec i))))))

(defn markdown-component [state]
  [(with-meta
     (fn []
       [:div {:dangerouslySetInnerHTML
              {:__html (-> (:content @state) str js/marked)}}])
     {:component-did-mount
      (fn [this]
        (let [node (reagent/dom-node this)]
          (highlight-code node)))})])

(defn preview [state]
  (markdown-component state))

(defn page [initial-state]
  (let [state (reagent/atom (assoc initial-state :mode :content))]
    (fn []
      (let [{:keys [method action mode title subtitle content]} @state]
        [:div.container-fluid
         [:form {:role "form" :method method :action action}
          [:div.row
           [:div.col-sm-12
            [:h3 "Title"]
            [:div.form-group
             [:input.form-control {:type "text"
                                   :name "title"
                                   :value title
                                   :on-change #(swap! state assoc :title (-> % .-target .-value))}]]]
           [:div.col-sm-12
            [:h3 "Subtitle"]
            [:div.form-group
             [:input.form-control {:type "text"
                                   :name "subtitle"
                                   :value subtitle
                                   :on-change #(swap! state assoc :subtitle (-> % .-target .-value))}]]]]
          [:ul.nav.nav-tabs
           [:li {:role "presentation"}
            (map (fn [[k v]]
                   (let [class (if (= k mode)
                                 "active"
                                 "")]
                     [:li {:role "presentation"
                           :key k
                           :class class}
                      [:a {:href "#"
                           :on-click #(set-property! state :mode k %)}
                       v]]))
                 {:content "Content" :preview "Preview" :side-by-side "Side by Side"})]]
          (case mode
            :content [:div.row
                      [:div.col-sm-12
                       [:h3 "Content"]
                       [editor state]]]
            :preview [:div.row
                      [:div.col-sm-12
                       [:h3 "Preview"]
                       [:div.panel.panel-default.preview
                        [:div.panel-body
                         [preview state]]]]]
            :side-by-side [:div.row
                           [:div.col-sm-6
                            [:h3 "Content"]
                            [editor state]]
                           [:div.col-sm-6
                            [:h3 "Preview"]
                            [:div.panel.panel-default.preview
                             [:div.panel-body
                              [preview state]]]]]) 
          [:div.row {:style {:marginTop "10px"}}
           [:div.col-sm-6
            [:input.btn.btn-primary {:type "submit" :value "Submit"}]]]]]))))

(defn ^:export create []
  (reagent/render [#(page {:title nil
                           :subtitle nil
                           :content nil
                           :method "post"
                           :action "post"})] (.getElementById js/document "app")))

(defn ^:export edit [post]
  (println post)
  (let [post (js->clj post :keywordize-keys true)
        id (:id post)
        _ (println "WHAT?" id)
        initial-state (merge post {:method "post" :action (str "/admin/post/" id)})]
    (reagent/render [#(page initial-state)] (.getElementById js/document "app"))))
