![edit-owners](data/title.png)
![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/vinsg/edit-owners)
-----
Edit-owners is a command line tool to help managed the CODEOWNERS files
of many GitHub repositories.

This tool is a proof of concept for Kotlin Multiplatform tech and
its source code is heavily annotated. Please leave feedback or report any bugs
by opening an issue on GitHub.

## Table of Content

- [Installation](#installation)
- [Usage](#usage)
    * [Personal Access Token](#personal-access-token)
    * [Basic usage](#basic-usage)
    * [Note on Windows usage](#note-on-windows-usage)
    * [Add a specific user](#add-a-specific-user)
    * [Add a user using a list of repositories in a file](#add-a-user-using-a-list-of-repositories-in-a-file)
- [Tech Used](#tech-used)
    * [Local dev](#local-dev)
- [License](#license)

## Installation

Get the proper binary for your system on the [release page](https://github.com/vinsg/edit-owners/releases)

## Usage

### Personal Access Token

Authentication with GitHub is done through the use of a Personal Access Token (PAT).
[Token Guide](./token-guide.md)

### Basic usage

```bash
./edit-owners \
-t <your GitHub token> \
add -r "org/repo"
```

### Note on Windows usage

Edit-owners currently depends on libcurl which is not bundled with the binary yet(see issue).
The use of [Git-bash](https://gitforwindows.org/) is heavily recommended for now.

### Add a specific user

Use the --user (-u) flag.

```bash
./edit-owners \
-t <your GitHub token> \
add -u "vinsg" \
-r "org/repo"
```

### Add a user using a list of repositories in a file

Use the --file (-f) flag to pass a csv file of repositories with format "owner/firstRepo, owner/secondRepo"

```bash
./edit-owners \
-t <your GitHub token> \
add -u "vinsg" \
-f <path to csv file>
```

## Tech used

- Kotlin
- Kotlin Multiplatform
- Ktor
- Clikt
- Mordant
- Okio
- Serialization-kotlinx
- Koin

### Local dev

You can generate the table of content for this page
using [markdown-toc](https://www.npmjs.com/package/markdown-toc) `markdown-toc README.md`

## License

    Copyright 2022 Vincent S-G.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
