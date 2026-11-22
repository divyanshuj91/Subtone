#include "Metronome.h"
#include <cmath>

namespace subtone {

constexpr double TWO_PI = 6.28318530717958647692;
constexpr int32_t CLICK_DURATION_FRAMES = 240; // 5ms @ 48kHz

Metronome::Metronome() {
    setSampleRate(48000);
    setBpm(120);
}

void Metronome::setSampleRate(int32_t sampleRate) {
    if (sampleRate > 0) {
        sample_rate_ = sampleRate;
        frames_per_beat_ = (static_cast<int64_t>(sample_rate_) * 60) / bpm_.load(std::memory_order_relaxed);
    }
}

void Metronome::setBpm(int32_t bpm) {
    if (bpm >= 30 && bpm <= 300) {
        bpm_.store(bpm, std::memory_order_relaxed);
        frames_per_beat_ = (static_cast<int64_t>(sample_rate_) * 60) / bpm;
    }
}

void Metronome::setEnabled(bool enabled) {
    enabled_.store(enabled, std::memory_order_relaxed);
    if (!enabled) {
        is_clicking_ = false;
        click_frame_index_ = 0;
    }
}

void Metronome::setReferenceTone(double frequencyHz, float amplitude) {
    tone_frequency_.store(static_cast<float>(frequencyHz), std::memory_order_relaxed);
    tone_amplitude_.store(amplitude, std::memory_order_relaxed);
    tone_enabled_.store(amplitude > 0.001f, std::memory_order_relaxed);
}

void Metronome::stopReferenceTone() {
    tone_enabled_.store(false, std::memory_order_relaxed);
    tone_amplitude_.store(0.0f, std::memory_order_relaxed);
}

void Metronome::renderAudio(float* outputBuffer, int32_t numFrames) {
    const bool metroOn = enabled_.load(std::memory_order_relaxed);
    const bool toneOn = tone_enabled_.load(std::memory_order_relaxed);
    const float toneFreq = tone_frequency_.load(std::memory_order_relaxed);
    const float toneAmp = tone_amplitude_.load(std::memory_order_relaxed);
    const double tonePhaseIncrement = (TWO_PI * toneFreq) / sample_rate_;

    for (int32_t i = 0; i < numFrames; ++i) {
        float sample = 0.0f;

        // Reference sine generator
        if (toneOn) {
            sample += static_cast<float>(sin(tone_phase_)) * toneAmp;
            tone_phase_ += tonePhaseIncrement;
            if (tone_phase_ >= TWO_PI) {
                tone_phase_ -= TWO_PI;
            }
        }

        // Metronome click generator
        if (metroOn) {
            if (current_frame_ % frames_per_beat_ == 0) {
                is_clicking_ = true;
                click_frame_index_ = 0;
                is_downbeat_ = (beat_count_ % 4 == 0);
                beat_count_ = (beat_count_ + 1) % 4;
            }

            if (is_clicking_) {
                // High click 1600Hz on downbeat, 1000Hz on upbeat with linear decay envelope
                const double clickFreq = is_downbeat_ ? 1600.0 : 1000.0;
                const float decay = 1.0f - (static_cast<float>(click_frame_index_) / CLICK_DURATION_FRAMES);
                const float clickSample = static_cast<float>(sin((TWO_PI * clickFreq * click_frame_index_) / sample_rate_)) * decay * 0.7f;

                sample += clickSample;
                click_frame_index_++;
                if (click_frame_index_ >= CLICK_DURATION_FRAMES) {
                    is_clicking_ = false;
                }
            }
            current_frame_++;
        }

        outputBuffer[i] = sample;
    }
}

} // namespace subtone
