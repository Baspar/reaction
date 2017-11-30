(ns reaction.core
  (:require [clojure.walk :refer [postwalk]]))

;; Macro - Dispatch!
(defn -dispatch! [state & args]
  (let [params args
        final-params (postwalk #(if (= '! %) (keyword "   !") %)
                               args)]
    `(reaction.core/-dispatch! ~state ~@final-params)))
(defmacro dispatch! [& args]
  (apply -dispatch! args))

;; Defaction
(defn -defaction [action-infos bang? action-name & body]
  (assert (symbol? action-name) "The action name has to be a symbol")
  (let [fn-name (gensym)

        doc-string (when (string? (first body)) (first body))
        body (if doc-string (rest body) body)

        options (if (map? (first body)) (first body) {})
        body (if (map? (first body)) (rest body) body)

        in-coll? (list? (first body))
        let-params (vec (concat ['reaction fn-name]
                                (if (not bang?)
                                    ['dispatch! `(fn [& ~'_] (throw "Cannot use a dispatch! on an action, please use an action!"))]
                                    ['dispatch! `(fn [& ~'rest] (apply -dispatch! ~'parent-id ~'rest))
                                     '! (keyword "   !")])))
        body (if in-coll? body (list body))
        body (map (fn [[params & rest-body]]
                    (list (into ['parent-id] params) `(let ~let-params
                                    ~@rest-body)))
                  body)

        action-ns (str *ns*)
        signatures (->> body
                        (mapv first)
                        (mapv #(mapv str %)))

        action-list-key (if bang?
                          (keyword (str action-name " !"))
                          (keyword action-name))
        defmulti-key (if bang?
                       'reaction.core/apply-action!
                       'reaction.core/apply-action)
        action-line (:line action-infos)
        silent? (get options :silent false)]
    `(do
       ;; (declare ~'xxx)
       ;; (when (not ~'xxx)
       ;;   (def ~'xxx true)
       ;;   (swap! actions-list
       ;;          update :actions
       ;;          #(->> %
       ;;                (filter (fn [[~'k ~'v]]
       ;;                          (= (get ~'v :namespace) ~action-ns)))
       ;;                (into {}))))
       (swap! actions-list
              update-in [:actions ~action-list-key]
              #(merge %
                      {:params ~signatures
                       :namespace ~action-ns
                       :line ~action-line
                       :silent ~silent?
                       :documentation ~doc-string}))
       (defn ~fn-name ~@body)
       (defmethod ~defmulti-key ~(keyword action-name)
         [~'parent-id ~'action-id ~'state ~'_ & ~'params]
         (apply ~fn-name ~'action-id ~'state ~'params)))))

;; Macros - Defaction
(defmacro defaction! [& params]
  (let [action-infos (meta &form)]
    (apply -defaction action-infos true params)))
(defmacro defaction [& params]
  (let [action-infos (meta &form)]
    (apply -defaction action-infos false params)))
