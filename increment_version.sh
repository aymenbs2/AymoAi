#!/bin/bash
# increment_version.sh

# Get the latest tag from git
latest_tag=$(git describe --tags --abbrev=0)

# Extract the major, minor, and patch numbers
IFS='.' read -r -a version_parts <<< "$latest_tag"
major=${version_parts[0]}
minor=${version_parts[1]}
patch=${version_parts[2]}

# Increment the patch number
new_patch=$((patch + 1))

# Create the new version tag
new_version="$major.$minor.$new_patch"

# Output the new version
echo $new_version
