#!/bin/sh

set -e

DESKTOP_DIR="/usr/share/applications"

remove_entry() {
    ENTRY_PATH="$DESKTOP_DIR/$1"
    if [ -f "$ENTRY_PATH" ]; then
        rm -f "$ENTRY_PATH"
    fi
}

# jpackageが生成する別名の.desktopを削除して一つに統一
remove_entry "JJDicomViewerLite.desktop"
remove_entry "jjdicomviewerlite.desktop"

# GNOME等のメニューキャッシュを更新（失敗しても無視）
if command -v update-desktop-database >/dev/null 2>&1; then
    update-desktop-database "$DESKTOP_DIR" >/dev/null 2>&1 || true
fi

