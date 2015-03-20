replace_() {
  REPLACER="${1}"
  F0="${2}"
  F1=$(echo "${F0}" | sh -c "${REPLACER}")
  mkdir -p $(dirname "${F1}")
  cat "${F0}" | sh -c "${REPLACER}" > /tmp/replaced
  [ "${REMOVEOLD}" ] && rm -f "${F0}" || true
  mv /tmp/replaced "${F1}"
  echo ${F1}
}

replace() {
  REPLACER="${1}"
  shift
  while [ "${1}" ]; do
    F0="${1}"
    shift
    replace_ "${REPLACER}" "${F0}"
  done
}

replace-files() {
  while read F; do
    replace_ "${1}" "${F}"
  done
}

replace-example() {
  replace "sed 's/board/Board/g'" file.txt
}
