#pragma once

#include <oboe/Oboe.h>
#include <memory>
#include <atomic>
#include "RingBuffer.h"
#include "Metronome.h"

namespace subtone {

constexpr size_t RING_BUFFER_CAPACITY = 32768; // Power of 2 (approx 0.68s @ 48kHz)

class AudioEngine : public oboe::AudioStreamDataCallback,
                    public oboe::AudioStreamErrorCallback {
public:
    AudioEngine();
    ~AudioEngine() override;

    bool start();
    void stop();
    bool isRunning() const { return is_running_.load(); }

    /**
     * Drains PCM float samples from lock-free ring buffer into destination array.
     * Called on DSP worker thread.
     */
    size_t readInputSamples(float* destination, size_t maxSamples);

    void setMetronomeEnabled(bool enabled) { metronome_.setEnabled(enabled); }
    void setMetronomeBpm(int32_t bpm) { metronome_.setBpm(bpm); }
    void setReferenceTone(double freqHz, float amplitude) { metronome_.setReferenceTone(freqHz, amplitude); }
    void stopReferenceTone() { metronome_.stopReferenceTone(); }

    // --- oboe::AudioStreamDataCallback ---
    /**
     * STRICT REAL-TIME CONSTRAINTS:
     * - Zero heap allocations (no new/malloc)
     * - Zero mutexes / locks
     * - Zero JNI calls
     * - Execution strictly bounded
     */
    oboe::DataCallbackResult onAudioReady(
        oboe::AudioStream* audioStream,
        void* audioData,
        int32_t numFrames) override;

    // --- oboe::AudioStreamErrorCallback ---
    void onErrorAfterClose(oboe::AudioStream* audioStream, oboe::Result result) override;

private:
    std::shared_ptr<oboe::AudioStream> recording_stream_;
    std::shared_ptr<oboe::AudioStream> playback_stream_;

    LockFreeRingBuffer<float, RING_BUFFER_CAPACITY> input_ring_buffer_;
    Metronome metronome_;

    std::atomic<bool> is_running_{false};
    int32_t sample_rate_{48000};
};

} // namespace subtone
