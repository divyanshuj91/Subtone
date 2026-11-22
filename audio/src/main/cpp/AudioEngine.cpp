#include "AudioEngine.h"
#include <android/log.h>

#define TAG "SubtoneAudioEngine"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

namespace subtone {

AudioEngine::AudioEngine() {}

AudioEngine::~AudioEngine() {
    stop();
}

bool AudioEngine::start() {
    if (is_running_.load()) return true;

    // 1. Configure Low-Latency Exclusive Input Stream (Microphone)
    oboe::AudioStreamBuilder inBuilder;
    inBuilder.setDirection(oboe::Direction::Input)
        ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
        ->setSharingMode(oboe::SharingMode::Exclusive)
        ->setFormat(oboe::AudioFormat::Float)
        ->setChannelCount(oboe::ChannelCount::Mono)
        ->setDataCallback(this)
        ->setErrorCallback(this);

    oboe::Result inResult = inBuilder.openStream(recording_stream_);
    if (inResult != oboe::Result::OK) {
        LOGE("Failed to open recording stream: %s", oboe::convertToText(inResult));
        return false;
    }

    sample_rate_ = recording_stream_->getSampleRate();
    metronome_.setSampleRate(sample_rate_);

    // 2. Configure Low-Latency Exclusive Output Stream (Metronome / Reference Tone)
    oboe::AudioStreamBuilder outBuilder;
    outBuilder.setDirection(oboe::Direction::Output)
        ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
        ->setSharingMode(oboe::SharingMode::Exclusive)
        ->setFormat(oboe::AudioFormat::Float)
        ->setChannelCount(oboe::ChannelCount::Mono)
        ->setSampleRate(sample_rate_)
        ->setDataCallback(this)
        ->setErrorCallback(this);

    oboe::Result outResult = outBuilder.openStream(playback_stream_);
    if (outResult != oboe::Result::OK) {
        LOGE("Failed to open playback stream: %s", oboe::convertToText(outResult));
        recording_stream_->close();
        return false;
    }

    input_ring_buffer_.reset();

    inResult = recording_stream_->requestStart();
    outResult = playback_stream_->requestStart();

    if (inResult != oboe::Result::OK || outResult != oboe::Result::OK) {
        LOGE("Failed to start audio streams");
        stop();
        return false;
    }

    is_running_.store(true);
    LOGI("AudioEngine started: SampleRate=%d, Exclusive LowLatency mode", sample_rate_);
    return true;
}

void AudioEngine::stop() {
    if (!is_running_.exchange(false)) return;

    if (recording_stream_) {
        recording_stream_->stop();
        recording_stream_->close();
        recording_stream_.reset();
    }

    if (playback_stream_) {
        playback_stream_->stop();
        playback_stream_->close();
        playback_stream_.reset();
    }

    LOGI("AudioEngine stopped");
}

size_t AudioEngine::readInputSamples(float* destination, size_t maxSamples) {
    return input_ring_buffer_.read(destination, maxSamples);
}

oboe::DataCallbackResult AudioEngine::onAudioReady(
    oboe::AudioStream* audioStream,
    void* audioData,
    int32_t numFrames) {

    // STRICT ZERO ALLOCATIONS, ZERO LOCKS, ZERO JNI
    if (audioStream->getDirection() == oboe::Direction::Input) {
        const float* input = static_cast<const float*>(audioData);
        // Push frames into lock-free ring buffer
        input_ring_buffer_.write(input, static_cast<size_t>(numFrames));
    } else {
        float* output = static_cast<float*>(audioData);
        // Synthesize metronome click / reference sine tone
        metronome_.renderAudio(output, numFrames);
    }

    return oboe::DataCallbackResult::Continue;
}

void AudioEngine::onErrorAfterClose(oboe::AudioStream* audioStream, oboe::Result result) {
    LOGE("AudioStream error occurred: %s", oboe::convertToText(result));
}

} // namespace subtone
