(ns reaction.sidecar
  (:require [clojure.string :refer [join split replace]]
            [clojure.pprint :refer [pprint]]
            [rum.core :refer [defc]]))

;; Helper - Scroll into view if needed
(defn scroll-into-view-if-needed
    [node]
    (let [parent (aget (.getElementsByClassName js/document "pit-plugin--actions-list") 0)

          parentComputedStyle (.getComputedStyle js/window parent nil)
          parentBorderTopWidth (js/parseInt (.getPropertyValue parentComputedStyle "border-top-width"))
          parentBorderLeftWidth (js/parseInt (.getPropertyValue parentComputedStyle "border-left-width"))
          overTop? (< (- (.-offsetTop node) parent.offsetTop) parent.scrollTop)
          overBottom? (> (- (+ (.-offsetTop node) (.-clientHeight node)) parent.offsetTop parentBorderTopWidth) (+ parent.scrollTop parent.clientHeight))]
        (cond
          overTop? (aset parent "scrollTop" (- (.-offsetTop node) (.-offsetTop parent) parentBorderTopWidth))
          overBottom? (aset parent "scrollTop" (- (+ (.-offsetTop node) (.-clientHeight node) ) (.-offsetTop parent) parentBorderTopWidth (.-clientHeight parent))))))

;; Search functions
(def fuzzy-search
  (memoize
    (fn ([word pattern]
         (fuzzy-search 0 word pattern))
      ([i word pattern]
       (cond
         (and (zero? (count pattern))
              (zero? (count word))) []
         (pos? (count word)) (let [first-equal? (= (first word)
                                                   (first pattern))]
                               (if first-equal?
                                 (when-let [out (fuzzy-search (inc i) (rest word) (rest pattern))]
                                   (concat [i] out))
                                 (when-let [out (fuzzy-search (inc i) (rest word) pattern)]
                                   out))))))))
(defn input-fn [state input-value]
  (swap! state (fn [m]
                 (-> (reduce (fn [buf k]
                               (assoc-in buf [:actions k :matches?] (fuzzy-search (name k) input-value)))
                             m
                             (keys (:actions m)))
                     (assoc :input-value input-value)
                     (dissoc :selected-id)
                     (dissoc :selected-action)))))

;; Selection functions
(defn select-action-fn [state id flat-visible-actions]
  (when (< 0 (count flat-visible-actions))
    (let [valid-id (max 0 (min (dec (count flat-visible-actions)) id))
          new-action (:name (get flat-visible-actions valid-id))]
      (scroll-into-view-if-needed (aget (.getElementsByClassName js/document "pit-plugin--action") valid-id))
      (swap! state #(-> %
                        (assoc :selected-id valid-id)
                        (assoc :selected-action new-action))))))
(defn select-relative-action-fn [state delta flat-visible-actions]
  (let [old-id (get @state :selected-id -1)
        new-id (+ old-id delta)]
    (select-action-fn state new-id flat-visible-actions)))

;; Close functions
(defn close-actions-fn [state]
  (let [input-node (.getElementById js/document "pit-plugin--actions-card--input")]
    (when input-node (.blur input-node))
    (swap! state dissoc :actions-visibles)))

;; Components
(defc action-list [state]
  (let [card-visible? (@state :actions-visibles)
        input-value (or (@state :input-value) "")
        input-value (or (@state :input-value) "")
        actions (->> (:actions @state)
                     (map (fn [[action-name m]] (assoc m :name action-name)))
                     (sort-by :line)
                     (group-by :namespace)
                     (map (fn [[a b]] [a {:actions b}])))
        flat-actions (vec (mapcat (fn [[_ {:keys [actions]}]] actions) actions))
        flat-visible-actions (filterv #(get % :matches? true) flat-actions)
        selected-action (@state :selected-action)]
    [:div.pit-plugin--column
     ;; Title
     [:div.pit-plugin--actions-card--title
      "ACTION LIST"]

     ;; Search Input
     [:div.pit-plugin--input--container
      [:input#pit-plugin--actions-card--input
       {:placeholder "Search"
        :value input-value
        :disabled (not card-visible?)
        :on-key-down (fn [e] (let [keycode (.-which e)]
                               (case keycode
                                 27 (if (not= "" input-value)
                                      (input-fn state "")
                                      (close-actions-fn state))
                                 38 (select-relative-action-fn state -1 flat-visible-actions)
                                 40 (select-relative-action-fn state +1 flat-visible-actions)
                                 "")))
        :on-change #(let [input-value (.. % -target -value)]
                      (input-fn state input-value))}]
      (when (and input-value (not= "" input-value))
        [:div.pit-plugin--input--cross {:on-click #(input-fn state "")}
         [:svg {:height "1em" :width "1em" :viewBox "0 0 100 100"}
          [:line {:x1 20 :y1 20 :x2 80 :y2 80 :stroke-width 20 :stroke "grey"}]
          [:line {:x1 20 :y1 80 :x2 80 :y2 20 :stroke-width 20 :stroke "grey"}]]])]

     ;; List/Placeholder
     (if (some (fn [[actions-namespace {:keys [actions]}]] (some (fn [action] (action :matches? true)) actions)) actions)
       [:div.pit-plugin--actions-list
        (mapv (fn [[actions-namespace {:keys [actions]}]]
                (when (some (fn [action] (action :matches? true)) actions)
                  [:div.pit-plugin--namespace
                   [:div.pit-plugin--namespace-name actions-namespace]
                   [:div.pit-plugin--actions
                    (map (fn [action]
                           (let [action-name (:name action)
                                 action-matches (action :matches? [])
                                 set-matches (into #{} action-matches)
                                 action-bang (re-matches #".* !$" (name (:name action)))
                                 action-string (let [n (name (:name action))]
                                                 (if action-bang
                                                   (replace n #" !$" "")
                                                   n))
                                 action-arity (:params action)
                                 action-doc (action :documentation)
                                 action-line (str ":"
                                                  (if-let [line (action :line)]
                                                    line
                                                    "???"))
                                 action-visible? (or (= 1 (count flat-visible-actions))
                                                     (= action-name selected-action))]
                             (when action-matches
                               [:div.pit-plugin--action {:on-click #(let [my-id (->> flat-visible-actions
                                                                                     (map-indexed vector)
                                                                                     (keep (fn [[id action]] (when (= (:name action) action-name) id)))
                                                                                     (first))]
                                                                      (select-action-fn state my-id flat-visible-actions))}
                                [:div.pit-plugin--action-name--container {:class (if action-visible? "visible" "hidden")}
                                 [:div.pit-plugin--action-bang {:class (when action-bang "enabled")}
                                  (when action-bang "!")]
                                 [:div.pit-plugin--action-name
                                  (map-indexed
                                    (fn [i letter]
                                      (if (set-matches i)
                                        [:b letter]
                                        letter))
                                    action-string) ]
                                 [:u.pit-plugin--line-number
                                  action-line]
                                 (when-not action-doc
                                   [:div.pit-plugin--no-documentation
                                    "No documentation!"])]
                                [:div.pit-plugin--drawer
                                 [:div.pit-plugin--params
                                  (join " " (map #(str "[" (join " "  %) "]") action-arity))]
                                 [:div.pit-plugin--documentation
                                  (or action-doc "No documentation!")]]])))
                         actions)]]))
              actions)]
       [:div.pit-plugin--no-result--placeholder "No action found"])]))
(defc action-history [state]
  [:div.pit-plugin--column
   [:div.pit-plugin--actions-card--title
    "ACTION HISTORY"]
   [:div.pit-plugin--actions-card--sub-title
    "50 latest actions"]
   [:div.pit-plugin--action-history--container
    (map-indexed (fn [index action]
                   (let [action-name (:name action)
                         action-time (:time action)
                         action-params (:params action)
                         expanded? (get action :expanded? false)]
                     [:div.pit-plugin--pointer{:on-click #(swap! state update-in [:actions-history index :expanded?] not)}
                      [:div.pit-plugin--action-history-element
                       [:div.pit-plugin--action-history-time action-time]
                       [:div action-name]
                       [:div.pit-plugin--action-history-params
                        "[" (count action-params) "]"]]
                      (when expanded?
                        [:div (if (empty? action-params)
                                [:div "No parameters to this action"]
                                [:ul
                                 (map (fn [param]
                                        [:li (str param)])
                                      action-params)])])]))
                 (get @state :actions-history []))]])
(defc actions-component
  [state]
  (let [card-visible? (@state :actions-visibles)]
    [:div.pit-plugin--actions-card {:class (when card-visible? "visible")}
     [:div.pit-plugin--actions-card--left {:class (when card-visible? "visible")}
      [:div.pit-plugin--close-button {:on-click #(close-actions-fn state)}
       [:svg {:height "2em" :width "2em" :viewBox "0 0 100 100"}
        [:line {:x1 20 :y1 20 :x2 80 :y2 80 :stroke-width 20 :stroke "grey"}]
        [:line {:x1 20 :y1 80 :x2 80 :y2 20 :stroke-width 20 :stroke "grey"}]]]
      (action-list state)]
     [:div.pit-plugin--actions-card--right {:class (when card-visible? "visible")}
      [:div.pit-plugin--close-button {:on-click #(close-actions-fn state)}
       [:svg {:height "2em" :width "2em" :viewBox "0 0 100 100"}
        [:line {:x1 20 :y1 20 :x2 80 :y2 80 :stroke-width 20 :stroke "grey"}]
        [:line {:x1 20 :y1 80 :x2 80 :y2 20 :stroke-width 20 :stroke "grey"}]]]
      (when card-visible?
        (action-history state))]]))


