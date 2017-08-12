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

(defn last-release []
  (let [result (:stdout (sh "git describe --tags --match RELEASE-\\*"))
        _ (println "result: " result)
        match (re-matches #"RELEASE-(\d+)\.(\d+).*" result)]
    (println "match: " match)
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
        _ (println "earlier-commit: " earlier-commit)
        distance (git-distance earlier-commit)
        _ (println "distance: " distance)
        increment  (if (> distance 0) 1 0)
        _ (println "increment: " increment)]
    (if-let [match (re-matches #"(\d+)\.(\d+)\.(\d+)" earlier-commit)]
      (let [major (js/parseInt (nth  match 1))
            _ (println "major: " major)
            minor (js/parseInt (nth match 2))
            _ (println "minor: " minor)
            patch (+ increment (js/parseInt (nth match 3)))
            _ (println "patch: " patch)
            new-tag {:major major :minor minor :patch patch}
            _ (println "new-tag: " new-tag)]
        new-tag)
      {:major 0 :minor 0 :patch 0})))

(defn final-semver-tag [{:keys [major minor patch]}
                        {:keys [release-major release-minor]}]
  (let [_ (println "major: " major " minor: " minor " patch: " patch " release-major: " release-major " release-minor: " release-minor)
        final-major (max major release-major)
        _ (println "final-major: " final-major)
        final-minor (max minor release-minor)
        _ (println "final-minor: " final-minor)]
    (if (or (> final-major major) (> final-minor minor))
      (str final-major "." final-minor "." "0")
      (str major "." minor "." patch))))

(defn new-semver []
  (let [initial-tag (initial-new-tag (latest-semver-tag))
        _ (println "initial-tag: " initial-tag)
        final-result (final-semver-tag initial-tag (last-release))
        _ (println "final-result: " final-result)]
    final-result))


;; (defn new-semver []
;;   (let [latest-tag (latest-semver-tag)
;;         _ (println "latest-tag: " latest-tag)
;;         initial-latest-tag (if (empty? latest-tag) git-empty-tree latest-tag)
;;         _ (println "initial-latest-tag: " initial-latest-tag)
;;         distance (git-distance initial-latest-tag)
;;         _ (println "distance: " distance)
;;         increment  (if (> distance 0) 1 0)
;;         _ (println "increment: " increment)]

;;     ;; TODO: need to reset patch if RELEASE-x.y triggers a change in major or minor
;;     (if-let [match (re-matches #"(\d+)\.(\d+)\.(\d+)" initial-latest-tag)]
;;       (let [{:keys [release-major release-minor]} (last-release)
;;             _ (println "last-release: " (last-release))
;;             _ (println "release-major: " release-major " release-minor: " release-minor)
;;             major (max release-major (js/parseInt (nth  match 1)))
;;             _ (println "major: " major)
;;             minor (max release-minor (js/parseInt (nth match 2)))
;;             _ (println "minor: " minor)
;;             initial-patch (js/parseInt (nth match 3))
;;             _ (println "initial-patch: " initial-patch)
;;             final-patch (+ increment initial-patch)
;;             _ (println "final-patch: " final-patch)]
;;         (str major "." minor "." final-patch))
;;       "0.0.0")))

(defn -main []
  (println "new-semver result: " (new-semver)))
