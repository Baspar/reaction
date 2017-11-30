(ns reaction.core
  (:require [clojure.string :refer [join split replace]]
            [reaction.sidecar :refer [actions-component]]
            [cljs.analyzer.api :refer [resolve]]
            [reaction.style :refer [style]]
            [rum.core :refer [defc mount]]))

;; Atom
(defonce actions-list (atom {:actions-history []}))
(defonce actions-id (atom 0))

;; Helper - Set Ctrl-Shift-A
(defn set-key-down-document! [state]
  (let [old-fn (.-onkeydown js/document)]
    (set! (.-onkeydown js/document)
          #(do
             (when old-fn old-fn %)
             (when (and (= (.-which %) 65)
                        (.-ctrlKey %)
                        (.-shiftKey %))
               (let [visible-keyword :actions-visibles
                     visible? (not (get @state visible-keyword false))
                     input-dom-node (.getElementById js/document "pit-plugin--actions-card--input")]
                 (swap! state update visible-keyword not)
                 (.setTimeout js/window
                              (fn [] (if visible?
                                       (.focus input-dom-node)
                                       (.blur input-dom-node)))
                              100)))))))

;; Binder
(defn render-actions-list [node]
  (mount (actions-component actions-list) node))
(defn bind-actions-list []
  (let [node (.getElementById js/document "pit--plugin--action--bar")]
    (when-not node
      (let [js-node (.createElement js/document "div")
            css-node (.createElement js/document "style")]

        (set-key-down-document! actions-list)
        (aset css-node "innerHTML" style)
        (aset js-node "id" "pit--plugin--action--bar")
        (.appendChild (.-body js/document) js-node)
        (.appendChild (.-head js/document) css-node)

        (add-watch actions-list
                   :actions-list-watch
                   (fn [_ _ old-state new-state]
                     (render-actions-list js-node)))
        (render-actions-list js-node)))))

;; Helpers
(defn atom? [x]
  (= cljs.core/Atom (type x)))
(defn- pop-actions-list [action-name params parent-id action-id]
  (let [date (js/Date.)
        h (.getHours date)
        h (str (when (< h 10) 0) h)
        m (.getMinutes date)
        m (str (when (< m 10) 0) m)
        s (.getSeconds date)
        s (str (when (< s 10) 0) s)
        infos {:time (str h ":" m ":" s)
               :name (name action-name)
               :action-id action-id
               :parent-id parent-id
               :params params}]
    (swap! actions-list update :actions-history
           #(vec (take 50 (concat [infos] %))))))

;; Multimethods
(defmulti apply-action
  (fn [parent-id action-id _ action-name & params]
    (when-not (get-in @actions-list [:actions action-name :silent])
      (pop-actions-list action-name params parent-id action-id))
    action-name))
(defmethod apply-action nil
  [_ _ m & _]
  m)
(defmethod apply-action :default
  [_ _ m action-name & body]
  (println "/!\\ Cannot find action \"" action-name "\"\nYou can define it with the function (defaction" action-name "[m] (...))")
  m)
(defmulti apply-action!
  (fn [parent-id action-id _ action-name & params]
    (when-not (get-in @actions-list [:actions (keyword (str (name action-name) " !")) :silent])
      (pop-actions-list (keyword (str (name action-name) " !")) params parent-id action-id))
    action-name))
(defmethod apply-action! nil
  [& _])
(defmethod apply-action! :default
  [_ _ _ action-name & _]
  (println "/!\\ Cannot find side-effect action \"" action-name "\"\nYou can define it with the function (defaction!" action-name "[m] (...))"))

;; Dispatch!
(defn -dispatch! [parent-id state & params]
  (if (atom? parent-id)
    (apply -dispatch! nil parent-id state params)
    (let [indexed-params (map-indexed vector params)
          partitioning (fn [[index item]] (if (or (keyword? item)
                                                  (and (coll? item)
                                                       (not= (keyword "   !") (first item))))
                                            -1
                                            index))
          partitioned-params (partition-by partitioning indexed-params)
          mapped-params (map (fn [x]
                               (let [remove-index (map second x)
                                     remove-bang (map (fn [y] (if (or (keyword? y)
                                                                      (not= (keyword "   !") (first y)))
                                                                y
                                                                (vec (rest y))))
                                                      remove-index)]
                                 (vec (concat
                                        [(if (= -1 (partitioning (first x))) :normal :side-effect)]
                                        (map second x)))))
                             partitioned-params)
          instructions (map (fn [x]
                              (if (= :side-effect (first x))
                                [:side-effect (vec (rest (second x)))]
                                x))
                            mapped-params)
          reducer (fn [m actions]
                    (reduce (fn [local-state action]
                              (let [action-id (swap! actions-id inc)]
                                (if (coll? action)
                                  (apply apply-action parent-id action-id local-state action)
                                  (apply-action parent-id action-id local-state action))))
                            m
                            actions))]
      (doseq [instruction instructions]
        (let [type-action (first instruction)
              actions (rest instruction)]

          (if (= :normal type-action)
            (swap! state #(reducer % actions))
            (doseq [action actions]
              (let [action-id (swap! actions-id inc)]
                (if (coll? action)
                  (apply apply-action! parent-id action-id state action)
                  (apply-action! parent-id action-id state action))))))))))

;; Functions - Remove-action
(defn remove-action [action-name]
  (assert (keyword? action-name) "The action name has to be a keyword")
  (swap! actions-list dissoc action-name)
  (remove-method apply-action action-name))
(defn remove-action! [action-name]
  (assert (keyword? action-name) "The action name has to be a keyword")
  (swap! actions-list dissoc action-name)
  (remove-method apply-action! action-name))
