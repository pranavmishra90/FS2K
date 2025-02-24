#!/bin/bash

function dataset_root() {
	if [ -d ".git" ] || git rev-parse --git-dir >/dev/null 2>&1; then
		cd "$(git rev-parse --show-toplevel)"
	else
		echo "Not inside a Git repository"
		exit 1
	fi
}

dataset_root

quarto render
