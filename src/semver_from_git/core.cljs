(ns semver-from-git.core
  (:require [cljs.tools.cli :refer [parse-opts]]
            [clojure.string :as str]
            [macchiato.fs :as fs]
            [clojure.pprint :as pp]))

(enable-console-print!)

;; version-file set to default (VERSION) or to argument if set
;;
;; Do a `git fetch --prune --tags` to insure local repo has same remote tags as remote
;; Should not disturb any local tags and will not sync local only tags to remote
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

(def child_process (js/require "child_process"))

;; Atom to store command line options
(def opts (atom {}))

(def cli-options
  [["-p" "--prefix PREFIX" "Require a prefix before SEMVER"]
   ["-h" "--help"]])

;;
;; Set up for CLI stuff
;;
(def default-filename "VERSION")

(defn usage [options-summary]
  (->> ["Generate a new semver based incremented from the last git tag that is a semver"
        ""
        "Writes the result to stdout and to a file"
        (str "The default output filename is " default-filename)
        ""
        "Usage: semver-from-git [filename]"
        ""
        "Options:"
        options-summary
        ""]
       (str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with a error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}
      ;; custom validation on arguments
      (= 1 (count arguments))
      {:filename (first arguments) :options options}
      (= 0 (count arguments))
      {:filename default-filename :options options}
      :else ; failed custom validation => exit with usage summary
      {:exit-message (usage summary)})))

(defn exit [status msg]
  (println msg)
  (js/process.exit status))

;;
;; Various utility functions
;;

;; The id of the "empty tree" in Git and it's always available in every repository.
; This could change if git ever stopped using sha1
;; See https://stackoverflow.com/a/40884093/38841
(def git-empty-tree "4b825dc642cb6eb9a060e54bf8d69288fbee4904")

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

(defn sync-local-tags-to-remote []
  (sh "git fetch --prune --tags"))

(defn latest-semver-tag
  "Get the latest (largest) semver of a git repo"
  []
  (let [cmd (str "git tag --sort=v:refname |sed -n 's/^" (:prefix-regex-snippit @opts) "\\([0-9]*\\.[0-9]*\\.[0-9]*\\).*/\\1/p' | tail -1")]
  (:stdout (sh cmd))))

(defn last-release
  "Get the most resent RELEASE-major.minor specification tag"
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

(defn final-semver-value [{:keys [major minor patch]}
                        {:keys [release-major release-minor]}]
  (let [final-major (max major release-major)
        final-minor (max minor release-minor)]
    (if (or (> final-major major) (> final-minor minor))
      (str final-major "." final-minor "." "0")
      (str major "." minor "." patch))))

(defn current-branch []
  (:stdout (sh "git rev-parse --abbrev-ref HEAD")))

(defn check-branch [final-sevmer-value]
  (let [current-branch (current-branch)]
    (if (not= "master" current-branch)
      (str current-branch "-" final-sevmer-value)
      final-sevmer-value)))

(defn new-semver []
  (let [initial-tag (initial-new-tag (latest-semver-tag))
        final-semver-value (final-semver-value initial-tag (last-release))
        final-result (check-branch final-semver-value)]
    final-result))

(defn -main [& args]
  (sync-local-tags-to-remote)
  (let [{:keys [options filename exit-message ok?]} (validate-args args)
        prefix (:prefix options)
        prefix-regex-snippit (if prefix (str prefix "*" ""))
        _ (reset! opts (assoc options :prefix-regex-snippit prefix-regex-snippit))
        new-semver (new-semver)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (do
        (println new-semver)
        (fs/spit filename (str new-semver "\n"))))))
