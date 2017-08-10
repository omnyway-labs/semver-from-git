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
; This could change if git ever stopped using sha1
;; See https://stackoverflow.com/a/40884093/38841
(def git-empty-tree "4b825dc642cb6eb9a060e54bf8d69288fbee4904")

(def git-latest (js/require "git-latest-semver-tag"))

(def child_process (js/require "child_process"))

(defn sh-simple
  "Function to to run programs thru the shell"
  [args]
  (let [result (.spawnSync child_process
                           (first args)
                           (clj->js (rest args))
                           #js {"shell" true})]

    (when-not (= 0 (.-status result))
      (do (println (.toString (.-stderr result) "utf8"))
          (throw (js/Error. (str "Error whilst performing shell command: " args)))))
    (str/trim (str (.-stdout result)))))

(defn sh
  "Function to to run programs thru the shell
  Returns map with error status, stdout and stderr"
  [args]
  (let [result (.spawnSync child_process
                           (first args)
                           (clj->js (rest args))
                           #js {"shell" true})]

    {:error (if (= 0 (.-status result))false true)
     :stdout (str/trim (str (.-stdout result)))
     :stderr (str/trim (str (.-stderr result)))}))

(defn latest-semver-tag
  "Get the latest (largest) semver of a git repo"
  []
  (:stdout (sh "git tag --sort=v:refname |sed -n 's/^\\([0-9]*\\.[0-9]*\\.[0-9]*\\).*/\\1/p' | tail -1")))

(defn latest-major-minor []
  (if-let [match (re-matches #"Release-(\d+)\.(\d+)" (:stdout (sh "git describe --tags --match RELEASE-\\*")))]
      {:major (second match) :minor (nth match 3)}))

(defn git-distance
  ([earlier] (git-distance earlier "HEAD"))
  ([earlier later]
   (:stdout (sh (str "git rev-list " earlier ".." later " --count")))))

(defn new-semver []
    (let [initial-latest-tag (if (empty? (latest-semver-tag)) git-empty-tree)
          _ (println "initial-latest-tag: " initial-latest-tag)
          increment  (if (> 0 (git-distance initial-latest-tag)) 1 0)
          _ (println "increment: " increment)]

      (if-let [match (re-matches #"(\d+\.)(\d+)\.(\d+)" initial-latest-tag)]
        (println "match: " match " if-let true result: " (str (nth  match 1) "." (nth match 2) "." (+ increment (nth match 3))))
        "0.0.0")))

(defn -main []
  (println "new-semver result: " (new-semver)))


;;  (.exec shell "ls"))
  ;;  (println "latest-major-minor: " (latest-major-minor)))
;;  (async-macros/go
;;  (println "FOO: " (latest-semver-tag)))
;;   (println "Git-Latest RESULTS: " (async/<! git-latest-res-chan))))
