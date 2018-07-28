#pragma once

#include <intrin.h>

inline uint32_t clz(uint32_t n) {
  return uint32_t(__lzcnt(n));
}

__host__
inline uint32_t bits_needed(uint32_t n) {
  return 32 - clz(n);
}

__host__ __device__
inline uint64_t pack64(uint64_t a, uint64_t b, uint32_t shift = 32) {
  return ((uint64_t(a) << shift) | uint64_t(b));
};

__host__ __device__
inline uint64_t unpack64_msb(uint64_t ab, uint64_t shift = 32) {
    uint64_t mask = (uint64_t(1) << shift) - 1;
  return ((ab >> shift) & mask);
};

__host__ __device__
inline uint64_t unpack64_lsb(uint64_t ab, uint32_t shift = 32) {
    uint64_t mask = (uint64_t(1) << shift) - 1;
  return (ab & mask);
};

__host__ __device__
inline uint32_t pack32(uint16_t a, uint16_t b, uint32_t shift = 16) {
  return ((uint64_t(a) << shift) | uint64_t(b));
};

__host__ __device__
inline uint32_t unpack32_msb(uint32_t ab, uint32_t shift = 16) {
    uint32_t mask = (uint32_t(1) << shift) - 1;
  return ((ab >> shift) & mask);
};

__host__ __device__
inline uint32_t unpack32_lsb(uint32_t ab, uint32_t shift = 16) {
    uint32_t mask = (uint32_t(1) << shift) - 1;
  return (ab & mask);
};
