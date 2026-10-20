#!/usr/bin/env bash
# Converts the raw Freesound WAV pack into the OGG files the mod ships.
# Usage: tools/convert_sounds.sh <wav source dir> [path to ffmpeg]
#
# Positional sounds MUST be mono (OpenAL only spatialises mono sources);
# ambient beds stay stereo and are played non-positionally.
set -euo pipefail

SRC="${1:?usage: convert_sounds.sh <wav dir> [ffmpeg]}"
FF="${2:-ffmpeg}"
OUT="$(cd "$(dirname "$0")/.." && pwd)/src/main/resources/assets/eternity/sounds"
mkdir -p "$OUT"/{ambient,far,omen,rift,entity,finale}

# "Carried on the wind": strip lows and highs, smear with long echoes.
FAR="highpass=f=90,lowpass=f=2000,aecho=0.8:0.55:190|430|770:0.32|0.22|0.12"
NORM="loudnorm=I=-18:TP=-2:LRA=11"

src() { ls "$SRC"/$1*.wav | head -n1; }

# enc <in> <out> <start> <dur> <channels> <quality> <fade in> <fade out> [extra filters]
enc() {
  local in="$1" out="$2" ss="$3" dur="$4" ch="$5" q="$6" fi="$7" fo="$8" extra="${9:-}"
  local st; st=$(awk -v d="$dur" -v f="$fo" 'BEGIN{print d-f}')
  local chain="atrim=start=${ss}:duration=${dur},asetpts=PTS-STARTPTS"
  [ -n "$extra" ] && chain="$chain,$extra"
  chain="$chain,$NORM,afade=t=in:d=${fi},afade=t=out:st=${st}:d=${fo}"
  "$FF" -v error -y -i "$in" -af "$chain" -ac "$ch" -ar 44100 -c:a libvorbis -q:a "$q" "$OUT/$out.ogg"
  echo "  $out.ogg"
}

echo "ambient beds (stereo, streamed)"
enc "$(src 192671)" ambient/drone     0   180 2 1 4 8
enc "$(src 546871)" ambient/wind      0    58 2 1 3 4
enc "$(src 626087)" ambient/abyss     0   160 2 1 6 8
enc "$(src 148786)" ambient/encounter 0    82 2 1 2 4
enc "$(src 122972)" ambient/rumble    0    14 2 1 1 1 "lowpass=f=500"

echo "finale (stereo, streamed)"
enc "$(src 149794)" finale/drums      0    75 2 2 0.5 6
enc "$(src 260654)" finale/aftermath  0    90 2 1 5 12

echo "wind-carried whispers (mono, distant)"
enc "$(src 149398)" far/chains1   0.4  5.6 1 2 0.6 1.5 "$FAR"
enc "$(src 149398)" far/chains2   9.6  5.4 1 2 0.6 1.5 "$FAR"
enc "$(src 149398)" far/chains3  19.5  5.0 1 2 0.6 1.5 "$FAR"
enc "$(src 149401)" far/groan1    1.0  5.0 1 2 0.8 1.5 "$FAR"
enc "$(src 149401)" far/groan2    6.5  5.0 1 2 0.8 1.5 "$FAR"
enc "$(src 149401)" far/groan3   12.0  6.0 1 2 0.8 2.0 "$FAR"
enc "$(src 360542)" far/horn1    11.3  5.9 1 2 0.5 2.0 "$FAR"
enc "$(src 360542)" far/horn2    24.2 10.6 1 2 0.5 3.0 "$FAR"
enc "$(src 360542)" far/horn3    44.0  9.0 1 2 0.5 3.0 "$FAR"
enc "$(src 539__)"  far/reverse1  8.5 10.0 1 2 1.5 2.0 "$FAR"
enc "$(src 539__)"  far/reverse2 18.5  8.5 1 2 1.5 2.0 "$FAR"
enc "$(src 539__)"  far/reverse3 37.0  9.0 1 2 1.5 2.0 "$FAR"
enc "$(src 194210)" far/noise1    1.4 10.0 1 2 0.3 2.5 "$FAR"
enc "$(src 194210)" far/noise2   13.0 10.0 1 2 0.3 2.5 "$FAR"
enc "$(src 194210)" far/noise3   25.6 10.4 1 2 0.3 2.5 "$FAR"
enc "$(src 194210)" far/noise4   40.0 10.0 1 2 0.3 2.5 "$FAR"
enc "$(src 122972)" far/rift      0   14.0 1 2 2.0 4.0 "lowpass=f=420,$FAR"

echo "omens (mono)"
enc "$(src 260655)" omen/shimmer  0   16.0 1 2 1.0 4.0 "aecho=0.8:0.6:300|650:0.3|0.2"
enc "$(src 148786)" omen/stare   50   12.0 1 2 0.5 3.0

echo "rifts (mono, positional)"
enc "$(src 506710)" rift/open     0    7.0 1 2 0.3 2.0
enc "$(src 168910)" rift/loop     2   20.0 1 2 1.0 1.0
enc "$(src 6478__)" rift/close    0    2.5 1 2 0.0 1.2
enc "$(src 6478__)" rift/impact  24.5  3.0 1 2 0.0 1.5

echo "entity (mono, positional)"
enc "$(src 149401)" entity/voice1  2.0  3.5 1 2 0.3 1.0
enc "$(src 149401)" entity/voice2  7.4  3.5 1 2 0.3 1.0
enc "$(src 672448)" entity/glitch1 0.0  1.4 1 2 0.0 0.3
enc "$(src 672448)" entity/glitch2 1.4  3.6 1 2 0.0 0.5
enc "$(src 672448)" entity/glitch3 5.8  4.6 1 2 0.0 0.5

du -sh "$OUT"
