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
  (let [args (str/split cmdline #" ")
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

(defn last-release
  ""
  []
  (let [result (:stdout (sh "git describe --tags --match RELEASE-\\*"))
        match (re-matches #"RELEASE-(\d+)\.(\d+).*" result)]
    (if match
      {:release-major (js/parseInt (nth match 1)) :release-minor (js/parseInt (nth match 2))}
      {:release-major 0 :release-minor 0})))


(defn git-distance
  ([earlier] (git-distance earlier "HEAD"))
  ([earlier later]
   (let [result (sh (str "git rev-list " earlier ".." later " --count"))
         stdout (:stdout result)]
     stdout)))
;;   (:stdout (sh (str "git rev-list " earlier ".." later " --count")))))

(defn initial-new-tag
  "Take the latest-semver-tag and calculate if patch should increment
  based on if the current commit is newer than the inputed latest-tag commit.
  Return a map of major, minor and patch with patch potentially updated"
  [latest-tag]
  (let [earlier-commit (if (empty? latest-tag) git-empty-tree latest-tag)
        distance (git-distance earlier-commit)
        increment  (if (> distance 0) 1 0)]
    (if-let [match (re-matches #"(\d+)\.(\d+)\.(\d+)" earlier-commit)]
      (let [major (js/parseInt (nth  match 1))
            minor (js/parseInt (nth match 2))
            patch (+ increment (js/parseInt (nth match 3)))
            new-tag {:major major :minor minor :patch patch}]
        new-tag)
      {:major 0 :minor 0 :patch 0})))

(defn final-semver-tag [{:keys [major minor patch]}
                        {:keys [release-major release-minor]}]
  (let [final-major (max major release-major)
        final-minor (max minor release-minor)]
    (if (or (> final-major major) (> final-minor minor))
      (str final-major "." final-minor "." "0")
      (str major "." minor "." patch))))

(defn new-semver []
  (let [initial-tag (initial-new-tag (latest-semver-tag))
        final-result (final-semver-tag initial-tag (last-release))]
    final-result))

(defn -main []
  (println (new-semver)))
