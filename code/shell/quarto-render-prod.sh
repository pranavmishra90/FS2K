#!/bin/bash

## Initialization
function dataset_root() {
	if [ -d ".git" ] || git rev-parse --git-dir >/dev/null 2>&1; then
		cd "$(git rev-parse --show-toplevel)"
	else
		echo "Not inside a Git repository"
		exit 1
	fi
}

DATE=$(date +%Y_%m_%d)

## Begin script

# Go to the datalad root directory
dataset_root

# Create a new branch for the new website render
git checkout -b web/$DATE

# Render using the production profile
quarto render --profile production
