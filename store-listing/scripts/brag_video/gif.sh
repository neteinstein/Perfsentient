#!/usr/bin/env bash
# Makes the autoplaying README preview (looping GIF, 800px wide, 12 fps) from brag.mp4.
# Usage: store-listing/scripts/brag_video/gif.sh brag.mp4 brag-preview.gif
set -euo pipefail
in="$1"; out="$2"
pal="$(mktemp --suffix=.png)"
vf="fps=12,scale=800:-1:flags=lanczos"
ffmpeg -y -loglevel error -i "$in" -vf "$vf,palettegen=max_colors=128:stats_mode=diff" "$pal"
ffmpeg -y -loglevel error -i "$in" -i "$pal" -lavfi "$vf [x]; [x][1:v] paletteuse=dither=bayer:bayer_scale=4" -loop 0 "$out"
rm -f "$pal"
