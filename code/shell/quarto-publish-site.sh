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

echo "Starting the quarto render using the production profile"
echo "-----------------------------------------------------------"
bash ./code/shell/quarto-render-prod.sh

# Push the current branch to the origin remote
echo "Publish to origin"
echo "----------------------------------------------"
BRANCH_NAME=$(git branch --show-current)

echo "Publishing $BRANCH_NAME to 'origin'"

git add notebook/_site
git commit --no-gpg-sign -m "ci(quarto): Site updated on $DATE"

git push origin $BRANCH_NAME

# Cleanup
echo "Cleanup"
echo "----------------------------------------------"
echo "Deleting $BRANCH_NAME and switching to main. Deleting notebook/_site"

git checkout main
git branch -D $BRANCH_NAME
rm -rf notebook/_site

echo "Done."
