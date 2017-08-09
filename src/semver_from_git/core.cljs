(ns semver-from-git.core
  (:require [cljs.core.async :as async]
            [clojure.string :as str]
            [clojure.pprint :as pp])
  (:require-macros [cljs.core.async.macros :as async-macros]))

(enable-console-print!)

;; version-file set to default (VERSION) or to argument if set
;;
;; latest-tag = Get the latest semver 
;;   If no latest-tag
;;     latest-tag = 0.0.0
;;
;; If HEAD is > latest-tag <(git rev-list ${latest-tag}..HEAD --count>
;;   new-tag = increment the build (most right) digit of latest-tag
;;
;; major-minor = Get the largest value of a tag that starts with RELEASE-x.y
;; If there is a major-minor and its greater than the major-minor of new-tag
;;  Update the major and/or minor values of new-tag
;;  Reset the build number to 0
;;
;; If current-branch != master
;;  new-tag = (str current-branch "-" new-tag)
;;
;; Print the value of new-tag
;; Write new-tag to version-file

;; The id of the "empty tree" in Git and it's always available in every repository.
;; This could change if git ever stopped using sha1
;; See https://stackoverflow.com/a/40884093/38841
(def git-empty-tree "4b825dc642cb6eb9a060e54bf8d69288fbee4904")

(def git-latest (js/require "git-latest-semver-tag"))
(def git-latest-res-chan (async/chan))

(def child_process (js/require "child_process"))

(defn sh
  "Function to to run programs thru the shell"
  [args]
  (let [result (.spawnSync child_process
                           (first args)
                           (clj->js (rest args))
                           #js {"shell" true})]

    (when-not (= 0 (.-status result))
      (do (println (.toString (.-stderr result) "utf8"))
          (throw (js/Error. (str "Error whilst performing shell command.")))))

    (str/trim (str (.-stdout result)))))

(defn latest-semver-tag
  "Use the git-latest-semver-tag npm library to get the latest semver style tag
  of the current repo Its asynchronous so a callback is used to get the resutls.
  This uses core.async to get the results back into the main program"
  []
  (git-latest (fn [err,tag] (async-macros/go (async/>! git-latest-res-chan tag)))))

(defn latest-major-minor []
  (sh "git describe --tags --match RELEASE-\\*"))

(defn latest-semver-tag []
  (async-macros/go
    (async/<! git-latest-res-chan)))

(defn git-distance
  ([earlier] (git-distance earlier "HEAD"))
  ([earlier later]
   (sh (str "git rev-list " earlier ".." later " --count"))))

;; (defn new-semver []
;;   (async/go
;;     (let [latest-tag (if (empty? (latest-semver-tag)) git-empty-tree)
;;           distance  (git-distance latest-tag)]

;;       )))

(defn -main []
  (println "YAY: " (sh ["git describe --tags --match RELEASE-\\*"])))


;;  (.exec shell "ls"))
  ;;  (println "latest-major-minor: " (latest-major-minor)))
;;  (async-macros/go
;;  (println "FOO: " (latest-semver-tag)))
;;   (println "Git-Latest RESULTS: " (async/<! git-latest-res-chan))))
