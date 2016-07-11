#!/sbin/sh
# 
# /system/addon.d/91-gsamrootcompanion.sh
# During a CM upgrade, this script backs up GSam Battery Root Companion,
# /system is formatted and reinstalled, then the file is restored.
#
# Only edit the lines between the two EOF statements

. /tmp/backuptool.functions

list_files() {
cat << EOF
priv-app/com.gsamlabs.bbm.rootcompanion-1.apk
priv-app/gsamrootcompanion.apk
EOF
}

case "$1" in
  backup)
    list_files | while read FILE DUMMY; do
      backup_file $S/"$FILE"
    done
  ;;
  restore)
    list_files | while read FILE REPLACEMENT; do
      R=""
      [ -n "$REPLACEMENT" ] && R="$S/$REPLACEMENT"
      [ -f "$C/$S/$FILE" ] && restore_file $S/"$FILE" "$R"
    done
  ;;
  pre-backup)
    # Stub
  ;;
  post-backup)
    # Stub
  ;;
  pre-restore)
    # Stub
  ;;
  post-restore)
    # Stub
  ;;
esac