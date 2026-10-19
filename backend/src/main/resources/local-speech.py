"""Render narration with local Piper models. No network or account is used."""
import argparse
from pathlib import Path
import wave
from piper import PiperVoice, SynthesisConfig

MODELS = {"fr": "fr_FR-siwis-medium", "en": "en_US-lessac-high", "ar": "ar_JO-kareem-medium"}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--models", type=Path, required=True)
    parser.add_argument("--language", choices=MODELS, required=True)
    parser.add_argument("--text-file", type=Path, required=True)
    parser.add_argument("--output-file", type=Path, required=True)
    args = parser.parse_args()
    text = args.text_file.read_text(encoding="utf-8").strip()
    if not text or len(text) > 650:
        parser.error("Narration must contain 1 to 650 characters")
    voice = PiperVoice.load(str(args.models / (MODELS[args.language] + ".onnx")))
    with wave.open(str(args.output_file), "wb") as output:
        voice.synthesize_wav(text, output, syn_config=SynthesisConfig(length_scale=1.05))


if __name__ == "__main__":
    main()
