#pragma once

#include <atomic>
#include <cstdint>

namespace subtone {

class Metronome {
public:
    Metronome();

    void setSampleRate(int32_t sampleRate);
    void setBpm(int32_t bpm);
    void setEnabled(bool enabled);
    void setReferenceTone(double frequencyHz, float amplitude);
    void stopReferenceTone();

    /**
     * Synthesizes audio into output buffer.
     * ZERO allocations, zero locks. Safe on real-time audio callback.
     */
    void renderAudio(float* outputBuffer, int32_t numFrames);

private:
    int32_t sample_rate_{48000};
    std::atomic<int32_t> bpm_{120};
    std::atomic<bool> enabled_{false};

    // Reference sine oscillator
    std::atomic<bool> tone_enabled_{false};
    std::atomic<float> tone_frequency_{440.0f};
    std::atomic<float> tone_amplitude_{0.0f};
    double tone_phase_{0.0};

    // Metronome tick state
    int64_t current_frame_{0};
    int64_t frames_per_beat_{24000}; // 48000 * 60 / 120
    int32_t beat_count_{0};
    int32_t click_frame_index_{0};
    bool is_clicking_{false};
    bool is_downbeat_{false};
};

} // namespace subtone
