![edit-owners](data/title.png)
![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/vinsg/edit-owners)
-----
Edit-owners is a command line tool to help managed the CODEOWNERS files
of many GitHub repositories.

This tool is a proof of concept for Kotlin Multiplatform tech and
its source code is heavily annotated. Please leave feedback or report any bugs
by opening an issue on GitHub.

Made by Vincent S.-G., you can contact me at **contact@vinsg.ca**

## Table of Contents

* [Installation](#installation)
    * [Usage](#usage)
        + [Personal Access Token](#personal-access-token)
        + [Linux / MacOS](#linux--macos)
        + [Windows](#windows)
    * [Tech Used](#tech-used)
    * [License](#license)

## Installation

Get the proper binary for your system on the [release page](https://github.com/vinsg/edit-owners/releases)

## Usage

### Personal Access Token

Authentication with GitHub is done through the use of a Personal Access Token (PAT).
[Token Guide](./token-guide.md)

### Linux / MacOS

```bash
# add the 
./edit-owners \
-t <your GitHub token> \
add -f <repository file>.csv
```

### Windows

Edit-owners currently depends on libcurl which is not bundled with the binary yet(see issue).
The use of [Git-bash](https://gitforwindows.org/) is heavily recommended for now.

```bash
# Using git-bash
./edit-owners \
-t <your GitHub token> \
add -f <repository file>.csv
```

## Tech Used

- Kotlin
- Kotlin Multiplatform
- Ktor
- Clikt
- Mordant
- Okio
- Serialization-kotlinx
- Koin

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