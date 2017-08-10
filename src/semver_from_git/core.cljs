(ns semver-from-git.core
  (:require [clojure.string :as str]
            [clojure.pprint :as pp]))

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

(defn sh
  "Function to to run programs thru the shell
  Returns map with error status, stdout and stderr"
  [cmdline]
;;  (println "sh cmdline: " cmdline "\n")
  (let [args (str/split cmdline #" ")
        ;; _ (println "args: " args)
        result (.spawnSync child_process
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
  (let [result (:stdout (sh "git describe --tags --match RELEASE-\\*"))
        _ (println "result: " result)
        match (re-matches #"RELEASE-(\d+)\.(\d+).*" result)]
    (println "match: " match)
    ;; (println "match: " match " second match: " (nth match 2) " 3rd match: " ((nth match 3)))
    {:release-major (js/parseInt (nth match 1)) :release-minor (js/parseInt (nth match 2))}))


(defn git-distance
  ([earlier] (git-distance earlier "HEAD"))
  ([earlier later]
   (let [result (sh (str "git rev-list " earlier ".." later " --count"))
         stdout (:stdout result)]
     stdout)))
;;   (:stdout (sh (str "git rev-list " earlier ".." later " --count")))))

(defn new-semver []
  (let [latest-tag (latest-semver-tag)
        _ (println "latest-tag: " latest-tag)
        initial-latest-tag (if (empty? latest-tag) git-empty-tree latest-tag)
        _ (println "initial-latest-tag: " initial-latest-tag)
        distance (git-distance initial-latest-tag)
        _ (println "distance: " distance)
        increment  (if (> distance 0) 1 0)
        _ (println "increment: " increment)]

    ;; TODO: need to reset patch if RELEASE-x.y triggers a change in major or minor
    (if-let [match (re-matches #"(\d+)\.(\d+)\.(\d+)" initial-latest-tag)]
      (let [{:keys [release-major release-minor]} (latest-major-minor)
            _ (println "latest-major-minor: " (latest-major-minor))
            _ (println "release-major: " release-major " release-minor: " release-minor)
            major (max release-major (js/parseInt (nth  match 1)))
            _ (println "major: " major)
            minor (max release-minor (js/parseInt (nth match 2)))
            _ (println "minor: " minor)
            initial-patch (js/parseInt (nth match 3))
            _ (println "initial-patch: " initial-patch)
            final-patch (+ increment initial-patch)
            _ (println "final-patch: " final-patch)]
        (str major "." minor "." final-patch))
      "0.0.0")))

(defn -main []
  (println "new-semver result: " (new-semver)))
