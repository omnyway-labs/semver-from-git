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

### SEMVER Scenaries

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

## Author

Robert J. Berger @rberger Omnyway Inc.

## License

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
