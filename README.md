# semver-from-git

Generates a new SEMVER based on existing SEMVER tags in the git repo its run in.


## Overview

Calculates and outputs a new SEMVER value based on existing SEMVER values.

* SEMVER must be in the form of `<MAJOR>.<MINOR>.<PATCH>` I.E.: `0.1.0` 
  * or optionally based on the --prefix argument (in this example `--prefix v`)
    `<PREFIX><MAJOR>.<MINOR>.<PATCH>` I.E.: `v0.1.0`
* SEMVER `PATCH` is incremented by one if the current commit is one or more
  commits later than the last SEMVER git tag in the repo

By default the program outputs the new SEMVER to stdout and to the file `VERSION`
You can override the output filename/path by adding name to the command:

```
semver-from-git my-version-file
semver-from-git /somepath/my-version-file

```

If you don't want it to write to a file use the `-n` / `--no-file` flags

```
semver-from-git -n
```

### SEMVER Scenarios

#### No Previous SEMVER Tag

If there are no git tag in the form of a SEMVER, it will consider `0.0.0` as the
earlier SEMVER and will create a new SEMVER 0.0.1 as long as the current git
commit is later than the first commit.

#### Bump the Major or Minor numbers

Create a tag in the form of `RELEASE-<MAJOR>.<MINOR>`. For instance to make SEMVERs start from 0.1.0:

```
git tag RELEASE-0.1
git push origin refs/tags/RELEASE-0.1
```

The next SEMVER the program will produce will be `0.1.0`. Each one after that
will bump the `PATCH` (last number) by one until a new `RELEASE-m.n` is created.

#### Your SEMVER tags have a prefix

You may have a prefix in front of the SEMVER such as a `v`: `v0.1.0` If so run
the program with the `-p` or `--prefix` flag

```
semver-from-git -p v
```

Note that the results do NOT have the prefix. It is up to you to add a prefix when you make the tag such as:

```
NEWTAG=v`semver-from-git -p v`
```

## General Usage

```shell
$> semver-from-git -h
Generate a new semver based incremented from the last git tag that is a semver

Writes the result to stdout and to a file
The default output filename is VERSION

Usage: semver-from-git [filename]

Options:
  -p, --prefix PREFIX  Require a prefix before SEMVER
  -s, --sync           Do a git fetch --prune --tags first
  -n, --no-file        Only write to stdout. No output file
  -h, --help
```

## Hacking

The program is written in [Clojurescript](https://clojurescript.org) and uses
[Lumo](https://github.com/anmonteiro/lumo), a standalone ClojureScript
environment that runs on Node.js and the V8 JavaScript engine.

The main source is in `src/semver_from_git/core.cljs`. A Bash script,
`bin/semver-from-git`, is used to start it up as a lumo script. The Bash script
has logic to run within the development environment:
```
git clone git@github.com:omnypay/semver-from-git.git
cd semver-from-git
npm install
bin/semver-from-git
```

Or after it is installed from npmjs.com or other source
```
npm install semver-from-git -g
semver-from-git
```

### Adding New Clojure/Clojurescript Libraries
Note that this program expects to have any Clojure/Clojurescript libraries
installed in `lib/`. They are explicitly specified in the lumo command line in
`bin/semver-from-git`. Lumo requires that you must use libraries that are
compatible with bootstrapped clojurescript.

For instance if you want to use `core.async` you must use the
[andar](https://github.com/mfikes/andare) version.

It is possible to get these dependencies from your local maven repo by adding
the `-D` to the lumo command line but that will not allow you to package the
program up into your own npm package.

So you must put all the jars of any Clojure/Clojurescript libraries you want to
use into `lib/` in the repo. Also recursively any dependencies those libraries
have. There isn't tooling yet to automate this.
[Calvin-cljs](https://github.com/eginez/calvin) looks promising to help with
this. Lumo project may also have something eventually.

### Adding new NPM Libraries

Incorporating npm libraries is basically easy. You follow the normal npm process
of adding dependencies by doing the following in the top of the repo:

```
npm install <package> --save
```

This should update your `package.json` with the new dependency

## Author

Robert J. Berger @rberger Omnyway Inc.

## Copyright & License

The MIT License (MIT)

Copyright © 2017 Omnyway Inc.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
