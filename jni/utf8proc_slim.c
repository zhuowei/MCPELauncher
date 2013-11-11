/*
 *  Copyright (c) 2009 Public Software Group e. V., Berlin, Germany
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 *  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 *  DEALINGS IN THE SOFTWARE.
 */

/*
 *  This library contains derived data from a modified version of the
 *  Unicode data files.
 *
 *  The original data files are available at
 *  http://www.unicode.org/Public/UNIDATA/
 *
 *  Please notice the copyright statement in the file "utf8proc_data.c".
 */


/*
 *  File name:    utf8proc.c
 *
 *  Description:
 *  Implementation of libutf8proc.
 */


#include "utf8proc.h"


const int8_t utf8proc_utf8class[256] = {
  1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
  1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
  1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
  1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
  1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
  1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
  1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
  1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
  0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
  0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
  0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
  0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
  2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
  2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
  3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
  4, 4, 4, 4, 4, 4, 4, 4, 0, 0, 0, 0, 0, 0, 0, 0 };

#define UTF8PROC_HANGUL_SBASE 0xAC00
#define UTF8PROC_HANGUL_LBASE 0x1100
#define UTF8PROC_HANGUL_VBASE 0x1161
#define UTF8PROC_HANGUL_TBASE 0x11A7
#define UTF8PROC_HANGUL_LCOUNT 19
#define UTF8PROC_HANGUL_VCOUNT 21
#define UTF8PROC_HANGUL_TCOUNT 28
#define UTF8PROC_HANGUL_NCOUNT 588
#define UTF8PROC_HANGUL_SCOUNT 11172
/* END is exclusive */
#define UTF8PROC_HANGUL_L_START  0x1100
#define UTF8PROC_HANGUL_L_END    0x115A
#define UTF8PROC_HANGUL_L_FILLER 0x115F
#define UTF8PROC_HANGUL_V_START  0x1160
#define UTF8PROC_HANGUL_V_END    0x11A3
#define UTF8PROC_HANGUL_T_START  0x11A8
#define UTF8PROC_HANGUL_T_END    0x11FA
#define UTF8PROC_HANGUL_S_START  0xAC00
#define UTF8PROC_HANGUL_S_END    0xD7A4


#define UTF8PROC_BOUNDCLASS_START    0
#define UTF8PROC_BOUNDCLASS_OTHER    1
#define UTF8PROC_BOUNDCLASS_CR       2
#define UTF8PROC_BOUNDCLASS_LF       3
#define UTF8PROC_BOUNDCLASS_CONTROL  4
#define UTF8PROC_BOUNDCLASS_EXTEND   5
#define UTF8PROC_BOUNDCLASS_L        6
#define UTF8PROC_BOUNDCLASS_V        7
#define UTF8PROC_BOUNDCLASS_T        8
#define UTF8PROC_BOUNDCLASS_LV       9
#define UTF8PROC_BOUNDCLASS_LVT     10

ssize_t utf8proc_iterate(
  const uint8_t *str, ssize_t strlen, int32_t *dst
) {
  int length;
  int i;
  int32_t uc = -1;
  *dst = -1;
  if (!strlen) return 0;
  length = utf8proc_utf8class[str[0]];
  if (!length) return UTF8PROC_ERROR_INVALIDUTF8;
  if (strlen >= 0 && length > strlen) return UTF8PROC_ERROR_INVALIDUTF8;
  for (i=1; i<length; i++) {
    if ((str[i] & 0xC0) != 0x80) return UTF8PROC_ERROR_INVALIDUTF8;
  }
  switch (length) {
    case 1:
    uc = str[0];
    break;
    case 2:
    uc = ((str[0] & 0x1F) <<  6) + (str[1] & 0x3F);
    if (uc < 0x80) uc = -1;
    break;
    case 3:
    uc = ((str[0] & 0x0F) << 12) + ((str[1] & 0x3F) <<  6)
      + (str[2] & 0x3F);
    if (uc < 0x800 || (uc >= 0xD800 && uc < 0xE000) ||
      (uc >= 0xFDD0 && uc < 0xFDF0)) uc = -1;
    break;
    case 4:
    uc = ((str[0] & 0x07) << 18) + ((str[1] & 0x3F) << 12)
      + ((str[2] & 0x3F) <<  6) + (str[3] & 0x3F);
    if (uc < 0x10000 || uc >= 0x110000) uc = -1;
    break;
  }
  if (uc < 0 || ((uc & 0xFFFF) >= 0xFFFE))
    return UTF8PROC_ERROR_INVALIDUTF8;
  *dst = uc;
  return length;
}
