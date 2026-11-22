#pragma once

#include <atomic>
#include <cstddef>
#include <vector>

namespace subtone {

/**
 * Lock-Free Single-Producer Single-Consumer (SPSC) Ring Buffer.
 * Designed for the high-priority real-time audio thread:
 * - Zero allocations
 * - Zero mutexes / locks
 * - Zero syscalls / JNI
 *
 * Size must be a power of 2 for fast bitmask wrapping.
 */
template <typename T, size_t Capacity>
class LockFreeRingBuffer {
    static_assert((Capacity & (Capacity - 1)) == 0, "Capacity must be a power of 2");

public:
    LockFreeRingBuffer() : head_(0), tail_(0) {}

    /**
     * Producer (Audio callback thread): Writes elements to buffer.
     * Returns number of items written.
     */
    size_t write(const T* data, size_t count) {
        const size_t current_tail = tail_.load(std::memory_order_relaxed);
        const size_t current_head = head_.load(std::memory_order_acquire);

        const size_t available = Capacity - (current_tail - current_head);
        const size_t to_write = (count < available) ? count : available;

        for (size_t i = 0; i < to_write; ++i) {
            buffer_[(current_tail + i) & MASK] = data[i];
        }

        tail_.store(current_tail + to_write, std::memory_order_release);
        return to_write;
    }

    /**
     * Consumer (DSP worker thread): Reads elements from buffer.
     * Returns number of items read.
     */
    size_t read(T* destination, size_t count) {
        const size_t current_head = head_.load(std::memory_order_relaxed);
        const size_t current_tail = tail_.load(std::memory_order_acquire);

        const size_t available = current_tail - current_head;
        const size_t to_read = (count < available) ? count : available;

        for (size_t i = 0; i < to_read; ++i) {
            destination[i] = buffer_[(current_head + i) & MASK];
        }

        head_.store(current_head + to_read, std::memory_order_release);
        return to_read;
    }

    size_t availableToRead() const {
        const size_t current_head = head_.load(std::memory_order_relaxed);
        const size_t current_tail = tail_.load(std::memory_order_relaxed);
        return current_tail - current_head;
    }

    void reset() {
        head_.store(0, std::memory_order_relaxed);
        tail_.store(0, std::memory_order_relaxed);
    }

private:
    static constexpr size_t MASK = Capacity - 1;
    T buffer_[Capacity];
    alignas(64) std::atomic<size_t> head_;
    alignas(64) std::atomic<size_t> tail_;
};

} // namespace subtone
