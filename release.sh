#!/bin/bash
#
# Build and release a BEAST 3 package.
#
set -euo pipefail

VERSION_XML="beast-classic/version.xml"
if [[ ! -f "$VERSION_XML" ]]; then
    echo "ERROR: $VERSION_XML not found in $(pwd)" >&2
    exit 1
fi

PKG_LINE=$(grep '<package ' "$VERSION_XML" | head -1)
PKG_NAME=$(echo "$PKG_LINE" | sed "s/.*name=['\"]\\([^'\"]*\\)['\"].*/\\1/")
VERSION=$(echo "$PKG_LINE" | sed "s/.*version=['\"]\\([^'\"]*\\)['\"].*/\\1/")

if [[ -z "$PKG_NAME" || -z "$VERSION" ]]; then
    echo "ERROR: could not parse name/version from $VERSION_XML" >&2
    exit 1
fi

ZIP_NAME="${PKG_NAME}.v${VERSION}.zip"
GITHUB_REPO=$(git remote get-url origin 2>/dev/null \
    | sed 's|.*github.com[:/]\(.*\)\.git$|\1|; s|.*github.com[:/]\(.*\)$|\1|')

echo "=== ${PKG_NAME} v${VERSION} ==="
echo ""

echo "--- Building with Maven ---"
mvn clean package -Dmaven.test.skip=true
echo ""

ZIP_PATH="beast-classic/target/${ZIP_NAME}"
if [[ ! -f "$ZIP_PATH" ]]; then
    echo "ERROR: expected ZIP not found at ${ZIP_PATH}" >&2
    exit 1
fi

cp "$ZIP_PATH" "$ZIP_NAME"

echo "=== Package: ${ZIP_NAME} ==="
unzip -l "$ZIP_NAME"

if [[ "${1:-}" == "--release" ]]; then
    if [[ -z "$GITHUB_REPO" ]]; then
        echo "ERROR: could not determine GitHub repo from git remote" >&2
        exit 1
    fi

    echo ""
    echo "--- Creating GitHub release v${VERSION} on ${GITHUB_REPO} ---"
    gh release create "v${VERSION}" "$ZIP_NAME" \
        --repo "$GITHUB_REPO" \
        --title "${PKG_NAME} v${VERSION}" \
        --generate-notes

    DOWNLOAD_URL="https://github.com/${GITHUB_REPO}/releases/download/v${VERSION}/${ZIP_NAME}"
    echo ""
    echo "=== Release created ==="
    echo "URL: https://github.com/${GITHUB_REPO}/releases/tag/v${VERSION}"
    echo ""
    echo "--- Next step: submit to CBAN ---"
    cat <<XMLEOF
    <package name="${PKG_NAME}" version="${VERSION}"
        url="${DOWNLOAD_URL}"
        projectURL="https://github.com/${GITHUB_REPO}"
        description="Classic BEAST models: phylogeography, GMRF skyride, GLM">
        <depends on="BEAST.base" atleast="2.8.0"/>
    </package>
XMLEOF
fi
