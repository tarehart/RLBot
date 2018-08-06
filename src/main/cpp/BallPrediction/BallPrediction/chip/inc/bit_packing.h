#pragma once

#include <intrin.h>

inline uint64_t pack64(uint64_t a, uint64_t b, uint32_t shift = 32) {
  return ((uint64_t(a) << shift) | uint64_t(b));
}

inline uint64_t unpack64_msb(uint64_t ab, uint64_t shift = 32) {
  uint64_t mask = (uint64_t(1) << shift) - 1;
  return ((ab >> shift) & mask);
}

inline uint64_t unpack64_lsb(uint64_t ab, uint32_t shift = 32) {
  uint64_t mask = (uint64_t(1) << shift) - 1;
  return (ab & mask);
}

inline uint32_t pack32(uint16_t a, uint16_t b, uint32_t shift = 16) {
  return ((uint64_t(a) << shift) | uint64_t(b));
}

inline uint32_t unpack32_msb(uint32_t ab, uint32_t shift = 16) {
  uint32_t mask = (uint32_t(1) << shift) - 1;
  return ((ab >> shift) & mask);
}

inline uint32_t unpack32_lsb(uint32_t ab, uint32_t shift = 16) {
  uint32_t mask = (uint32_t(1) << shift) - 1;
  return (ab & mask);
}

inline uint32_t clz_plain(uint32_t x)
{
	if (!x) return 32;
	uint32_t n = 0;
	if (x <= 0x0000ffff) n += 16, x <<= 16;
	if (x <= 0x00ffffff) n += 8, x <<= 8;
	if (x <= 0x0fffffff) n += 4, x <<= 4;
	if (x <= 0x3fffffff) n += 2, x <<= 2;
	if (x <= 0x7fffffff) n++;
	return n;
}

inline uint32_t clz(uint32_t n) {
  //return uint32_t(__lzcnt(n));
	return clz_plain(n);
}

inline uint32_t clz(uint64_t n) {

#if 0
  return uint32_t(__lzcnt64(n));
#else
  uint32_t llz = clz(uint32_t(unpack64_msb(n)));
  uint32_t rlz = clz(uint32_t(unpack64_lsb(n)));

  return llz + (llz == 32) * rlz;
#endif

}

inline uint32_t bits_needed(uint32_t n) {
  return 32 - clz(n);
}
