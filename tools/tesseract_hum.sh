#!/usr/bin/env bash
# Synthesises the Tesseract's hum: a deep, throbbing organ drone that beats against a
# slightly detuned twin, a glassy FM whine riding on it, high crystalline partials that
# swell and fade, and a faint fizz of energy, through a slow phaser and a small room.
# Usage: tools/tesseract_hum.sh [path to ffmpeg]
#
# The result loops seamlessly: every partial and every LFO completes a whole number of
# cycles in LOOP seconds, the noise is itself a LOOP-second loop, and the clip is cut
# from the middle of three repeats so the phaser and echoes are already in steady state.
# Mono, because it is played positionally on whoever holds the Tesseract.
set -euo pipefail

FF="${1:-ffmpeg}"
OUT="$(cd "$(dirname "$0")/.." && pwd)/src/main/resources/assets/eternity/sounds/tesseract"
mkdir -p "$OUT"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

LOOP=12
T="(2*PI*t)"

# The drone: harmonics of 55 Hz, and a twin at 55.5 Hz that beats against it, the
# n-th harmonic at n/2 Hz, so the upper ones shimmer faster than the lower ones.
drone=""
for h in 1 2 3 4 5 6 7 8; do
  a=$(awk -v h="$h" 'BEGIN{printf "%.4f", 1/(h^1.15) * (h%2 ? 1 : 0.7)}')
  drone="$drone+$a*(sin($T*55*$h)+0.8*sin($T*55.5*$h+$h))"
done
# Its middle swells and recedes at 1.5 Hz: the throb.
throb="(1+0.35*sin($T*1.5))"
sub="0.55*sin($T*27.5)"
# A glassy whine: 220 Hz frequency-modulated by 330 Hz, its brightness breathing over six seconds.
whine="0.16*sin($T*220+(1.6+1.2*sin($T/6))*sin($T*330))*(0.6+0.4*sin($T/4))"
# Crystal partials high above, each swelling on its own slow cycle, with a little vibrato.
crystal="0.05*sin($T*880+0.8*sin($T*5))*(0.5+0.5*sin($T/4))"
crystal="$crystal+0.035*sin($T*1320+0.6*sin($T*4.5))*(0.5+0.5*sin($T/3+1))"
crystal="$crystal+0.025*sin($T*1760.25+0.5*sin($T*6))*(0.5+0.5*sin($T/2+2))"

expr="0.16*(($drone)*$throb+$sub)+$whine+$crystal"

"$FF" -v error -y \
  -f lavfi -i "aevalsrc=exprs='$expr':s=44100:d=$((LOOP * 3))" \
  -f lavfi -i "anoisesrc=d=$LOOP:c=white:seed=2012:r=44100:a=0.5" \
  -filter_complex "\
[1:a]aloop=loop=2:size=$((LOOP * 44100)),highpass=f=2500,lowpass=f=7000,\
volume='0.05*(0.55+0.45*sin(2*PI*t*0.75))':eval=frame[fizz];\
[0:a][fizz]amix=inputs=2:normalize=0,\
aphaser=in_gain=0.6:out_gain=0.9:delay=2.5:decay=0.45:speed=0.25:type=t,\
aecho=0.85:0.5:37|61|97:0.25|0.18|0.12,\
atrim=start=$LOOP:duration=$LOOP,asetpts=PTS-STARTPTS[out]" \
  -map "[out]" -ac 1 -c:a pcm_f32le "$TMP/hum.wav"

# A fixed gain to -3 dBFS peak; anything time-varying would break the loop.
peak=$("$FF" -v info -i "$TMP/hum.wav" -af volumedetect -f null - 2>&1 | sed -n 's/.*max_volume: \(-\?[0-9.]*\) dB.*/\1/p')
gain=$(awk -v p="$peak" 'BEGIN{print -3 - p}')
"$FF" -v error -y -i "$TMP/hum.wav" -af "volume=${gain}dB" -ar 44100 -ac 1 -c:a libvorbis -q:a 5 "$OUT/hum.ogg"
echo "  tesseract/hum.ogg (peak was ${peak} dB, gain ${gain} dB)"
