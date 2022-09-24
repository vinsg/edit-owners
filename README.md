# Edit-Owners

![Status: Alpha](https://img.shields.io/badge/Status-Alpha-red)
![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/vinsg/edit-owners)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
![Supported platforms: Windows, MacosArm, MacosX86, Linux](https://img.shields.io/badge/Platforms-Windows%20%7C%20MacosArm%20%7C%20MarcosX86%20%7C%20Linux-blue)

Edit-owners is a command line tool to help managed the CODEOWNERS files
of many GitHub repositories.

This tool is a proof of concept for Kotlin Multiplatform tech and
its source code is heavily annotated. Please leave feedback or report any bugs
by opening an issue on GitHub.

## Table of Content

- [Installation](#installation)
- [Basic Usage](#basic-usage)
    * [Personal Access Token](#personal-access-token)
    * [Basic usage](#basic-usage)
    * [Note on Windows usage](#note-on-windows-usage)
- [Commands](#commands)
    * [Add a specific user](#add-a-specific-user)
    * [Add a user using a list of repositories in a file](#add-a-user-using-a-list-of-repositories-in-a-file)
    * [Create a file based on user ownership](#create-a-file-based-on-user-ownership)
    * [Remove a user using a list of repositories in a file](#add-a-user-using-a-list-of-repositories-in-a-file)
- [Tech used](#tech-used)
- [Local dev](#local-dev)
- [License](#license)

## Installation

Get the proper binary for your system on the [release page](https://github.com/vinsg/edit-owners/releases)

## Basic Usage

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

## Commands

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

### Create a file based on user ownership

Use the create-file command to create a csv file of repositories based on all the GitHub repositories containing
@username in their CODEOWNERS file.

```bash
./edit-owners \
-t <your GitHub token> \
create-file -u "username"
```

you can use the -o (--org) flag to narrow the list to only a specific org.

```bash
./edit-owners \
-t <your GitHub token> \
create-file -u "username" -g "myOrg"
```

### Remove a user using a list of repositories in a file

Use the --file (-f) flag to pass a csv file of repositories with format "owner/firstRepo, owner/secondRepo"

```bash
./edit-owners \
-t <your GitHub token> \
remove -u "vinsg" \
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

## Local dev

You can generate the table of content for this page
using [markdown-toc](https://www.npmjs.com/package/markdown-toc) `markdown-toc README.md`

## License

This project is licensed under [MIT License](LICENSE)