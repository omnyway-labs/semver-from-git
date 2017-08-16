;; This file is not used by lumo or the program
;; Just shows the dependencies
;;
(defproject semver-from-git "0.1.0-SNAPSHOT"
  :description "Generate next semver based on git tags"
  :url "https://github.com/omnypay/semver-from-git"
  :license {:name "The MIT License"
            :url "https://opensource.org/licenses/MIT"}

  :min-lein-version "2.7.1"

  :dependencies [[org.clojure/clojurescript "1.9.854"]
                 [macchiato/fs "0.1.1"]
                 [org.clojure/tools.cli "0.3.5"]]

  :plugins [[lein-cljsbuild "1.1.7"]]

  :source-paths ["src"]

  :cljsbuild {:builds
              [{:main
                :source-paths ["src"]
                :compiler {:target :nodejs}}}})
