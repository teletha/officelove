/*
-------------------------------------------------------------------------
Copyright (C) 2016 Christopher Brochtrup

This file is part of Leptonica Util.

Leptonica Util is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Leptonica Util is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Leptonica Util.  If not, see <http://www.gnu.org/licenses/>.
-------------------------------------------------------------------------
Name:        leptonica_util.c
Description: Perform image pre-processing before OCR.
Version:     1.2
Contact:     cb4960@gmail.com
-------------------------------------------------------------------------
To build, link with leptonica.lib;libtiff.lib;giflib.lib;libjpeg.lib;libpng.lib;openjpeg.lib;zlib.lib.
These libs can be built via: https://github.com/peirick/leptonica
-------------------------------------------------------------------------
*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "allheaders.h"


#define NO_VALUE    (-1)
#define LEPT_TRUE   1
#define LEPT_FALSE  0
#define LEPT_OK     0
#define LEPT_ERROR  1

#define MAX_FILE_LEN  512

/* Minimum number of foreground pixels that a line must contain for it to be part of a span.
Needed because sometimes furigana does not have a perfect gap between the text and itself. */
#define FURIGANA_MIN_FG_PIX_PER_LINE  2

/* Minimum width of a span (in pixels) for it to be included in the span list. */
#define FURIGANA_MIN_WIDTH  5

/* Maximum number of spans used during furigana removal */
#define FURIGANA_MAX_SPANS  50


typedef enum
{
  NEGATE_NO,   /* Do not negate image */
  NEGATE_YES,  /* Force negate */
  NEGATE_AUTO, /* Automatically negate if border pixels are dark */

} Negate_enum;


typedef enum
{
  REMOVE_FURIGANA_NO,         /* Do not remove furigana */
  REMOVE_FURIGANA_VERTICAL,   /* Remove furigana from vertically formatted text */
  REMOVE_FURIGANA_HORIZONTAL, /* Remove furigana from horizontally formatted text */

} Remove_furigana_enum;


/* Span of lines that contain foreground text. Used during furigana removal. */
typedef struct
{
  int start;
  int end;

} Span;


static int erase_furigana_vertical(PIX *pixs, float scale_factor);
static int erase_furigana_horizontal(PIX *pixs, float scale_factor);
static l_int32 erase_area_left_to_right(PIX *pixs, l_int32 x, l_int32 width);
static l_int32 erase_area_top_to_bottom(PIX *pixs, l_int32 y, l_int32 height);


int main(int argc, char *argv[])
{
  char source_file[MAX_FILE_LEN] = "in.png";
  char dest_file[MAX_FILE_LEN] = "out.png";

  Negate_enum perform_negate = NEGATE_AUTO;
  l_float32 dark_bg_threshold = 0.5f; /* From 0.0 to 1.0, with 0 being all white and 1 being all black */

  int perform_scale = LEPT_TRUE;
  l_float32 scale_factor = 3.5f;

  int perform_unsharp_mask = LEPT_TRUE;
  l_int32 usm_halfwidth = 5;
  l_float32 usm_fract = 2.5f;

  int perform_otsu_binarize = LEPT_TRUE;
  l_int32 otsu_sx = 2000;
  l_int32 otsu_sy = 2000;
  l_int32 otsu_smoothx = 0;
  l_int32 otsu_smoothy = 0;
  l_float32 otsu_scorefract = 0.0f;

  Remove_furigana_enum remove_furigana = REMOVE_FURIGANA_NO;

  l_int32 status = LEPT_ERROR;
  l_float32 border_avg = 0.0f;
  PIX *pixs = NULL;
  char *ext = NULL;

  /* Get args.
  leptonica_util.exe in.png out.png  2 0.5  1 3.5  1 5 2.5  1 2000 2000 0 0 0.0  1 */
  if (argc >= 17)
  {
    strcpy_s(source_file, MAX_FILE_LEN, argv[1]);
    strcpy_s(dest_file, MAX_FILE_LEN, argv[2]);

    perform_negate = atoi(argv[3]);
    dark_bg_threshold = (float)atof(argv[4]);

    perform_scale = atoi(argv[5]);
    scale_factor = (float)atof(argv[6]);

    perform_unsharp_mask = atoi(argv[7]);
    usm_halfwidth = atoi(argv[8]);
    usm_fract = (float)atof(argv[9]);

    perform_otsu_binarize = atoi(argv[10]);
    otsu_sx = atoi(argv[11]);
    otsu_sy = atoi(argv[12]);
    otsu_smoothx = atoi(argv[13]);
    otsu_smoothy = atoi(argv[14]);
    otsu_scorefract = (float)atof(argv[15]);

    remove_furigana = atoi(argv[16]);
  }

  /* Read in source image */
  pixs = pixRead(source_file);

  if (pixs == NULL)
  {
    return 1;
  }

  /* Convert to grayscale */
  pixs = pixConvertRGBToGray(pixs, 0.0f, 0.0f, 0.0f);

  if (pixs == NULL)
  {
    return 2;
  }

  if (perform_negate == NEGATE_YES)
  {
    /* Negate image */
    pixInvert(pixs, pixs);

    if (pixs == NULL)
    {
      return 3;
    }
  }
  else if (perform_negate == NEGATE_AUTO)
  {
    PIX *otsu_pixs = NULL;

    status = pixOtsuAdaptiveThreshold(pixs, otsu_sx, otsu_sy, otsu_smoothx, otsu_smoothy, otsu_scorefract, NULL, &otsu_pixs);

    if (status != LEPT_OK)
    {
      return 4;
    }

    /* Get the average intensity of the border pixels,
    with average of 0.0 being completely white and 1.0 being completely black. */
    border_avg =  pixAverageOnLine(otsu_pixs, 0, 0, otsu_pixs->w - 1, 0, 1);                               /* Top */
    border_avg += pixAverageOnLine(otsu_pixs, 0, otsu_pixs->h - 1, otsu_pixs->w - 1, otsu_pixs->h - 1, 1); /* Bottom */
    border_avg += pixAverageOnLine(otsu_pixs, 0, 0, 0, otsu_pixs->h - 1, 1);                               /* Left */
    border_avg += pixAverageOnLine(otsu_pixs, otsu_pixs->w - 1, 0, otsu_pixs->w - 1, otsu_pixs->h - 1, 1); /* Right */
    border_avg /= 4.0f;

    pixDestroy(&otsu_pixs);

    /* If background is dark */
    if (border_avg > dark_bg_threshold)
    {
      /* Negate image */
      pixInvert(pixs, pixs);

      if (pixs == NULL)
      {
        return 5;
      }
    }
  }

  if (perform_scale)
  {
    /* Scale the image (linear interpolation) */
    pixs = pixScaleGrayLI(pixs, scale_factor, scale_factor);

    if (pixs == NULL)
    {
      return 6;
    }
  }

  if (perform_unsharp_mask)
  {
    /* Apply unsharp mask */
    pixs = pixUnsharpMaskingGray(pixs, usm_halfwidth, usm_fract);

    if (pixs == NULL)
    {
      return 7;
    }
  }

  if (perform_otsu_binarize)
  {
    /* Binarize */
    status = pixOtsuAdaptiveThreshold(pixs, otsu_sx, otsu_sy, otsu_smoothx, otsu_smoothy, otsu_scorefract, NULL, &pixs);

    if (status != LEPT_OK)
    {
      return 8;
    }

    /* Remove furigana? */
    if (remove_furigana == REMOVE_FURIGANA_VERTICAL)
    {
      status = erase_furigana_vertical(pixs, scale_factor);

      if (status != LEPT_OK)
      {
        return 9;
      }
    }
    else if (remove_furigana == REMOVE_FURIGANA_HORIZONTAL)
    {
      status = erase_furigana_horizontal(pixs, scale_factor);

      if (status != LEPT_OK)
      {
        return 10;
      }
    }
  }

  /* Get extension of output image */
  status = splitPathAtExtension(dest_file, NULL, &ext);

  if (status != LEPT_OK)
  {
    return 11;
  }

  /* pixWriteImpliedFormat() doesn't recognize PBM/PGM/PPM extensions */
  if ((strcmp(ext, ".pbm") == 0) || (strcmp(ext, ".pgm") == 0) || (strcmp(ext, ".ppm") == 0))
  {
    /* Write the image to file as a PNM */
    status = pixWrite(dest_file, pixs, IFF_PNM);
  }
  else
  {
    /* Write the image to file */
    status = pixWriteImpliedFormat(dest_file, pixs, 0, 0);
  }

  if (status != LEPT_OK)
  {
    return 12;
  }

  /* Free image data */
  pixDestroy(&pixs);

  return 0;

} /* main */


  /* Erase the furigana from the provided binary PIX. Works by finding spans of foreground
  text and removing the spans that are too narrow and are likely furigana.
  Use this version for vertical text.
  Returns LEPT_OK on success. */
static int erase_furigana_vertical(PIX *pixs, float scale_factor)
{
  int min_fg_pix_per_line = (int)(FURIGANA_MIN_FG_PIX_PER_LINE * scale_factor);
  int min_span_width = (int)(FURIGANA_MIN_WIDTH * scale_factor);
  l_uint32 x = 0;
  l_uint32 y = 0;
  int num_fg_pixels_on_line = 0;
  int good_line = LEPT_FALSE;
  int num_good_lines_in_cur_span = 0;
  int total_good_lines = 0;
  l_uint32 pixel_value = 0;
  Span span = { NO_VALUE, NO_VALUE };
  Span span_list[FURIGANA_MAX_SPANS];
  int total_spans = 0;
  int ave_span_width = 0;
  int span_idx = 0;
  Span *cur_span = NULL;
  l_int32 status = LEPT_ERROR;

  /* Get list of spans that contain fg pixels */
  for (x = 0; x < pixs->w; x++)
  {
    num_fg_pixels_on_line = 0;
    good_line = LEPT_FALSE;

    for (y = 0; y < pixs->h; y++)
    {
      status = pixGetPixel(pixs, x, y, &pixel_value);

      if (status != LEPT_OK)
      {
        return status;
      }

      /* If this is a foreground pixel */
      if (pixel_value == 1)
      {
        num_fg_pixels_on_line++;

        /* If this line has already meet the minimum number of fg pixels, stop scanning it */
        if (num_fg_pixels_on_line >= min_fg_pix_per_line)
        {
          good_line = LEPT_TRUE;
          break;
        }
      }
    }

    /* If last line is good, set it bad in order to close the span */
    if (good_line && (x == pixs->w - 1))
    {
      good_line = LEPT_FALSE;
      num_good_lines_in_cur_span++;
    }

    /* If this line has the minimum number of fg pixels */
    if (good_line)
    {
      /* Start a new span */
      if (span.start == NO_VALUE)
      {
        span.start = x;
      }

      num_good_lines_in_cur_span++;
    }
    else /* Line doesn't have enough fg pixels to consider as part of a span */
    {
      /* If a span has already been started, then end it */
      if (span.start != NO_VALUE)
      {
        /* If this span isn't too small (needed so that the average isn't skewed) */
        if (num_good_lines_in_cur_span >= min_span_width)
        {
          span.end = x;

          total_good_lines += num_good_lines_in_cur_span;

          /* Add span to the list */
          span_list[total_spans] = span;
          total_spans++;

          /* Prevent span list overflow */
          if (total_spans >= FURIGANA_MAX_SPANS)
          {
            break;
          }
        }
      }

      /* Reset span */
      span.start = NO_VALUE;
      span.end = NO_VALUE;
      num_good_lines_in_cur_span = 0;
    }
  }

  if (total_spans == 0)
  {
    return LEPT_OK;
  }

  /* Get average width of the spans */
  ave_span_width = total_good_lines / total_spans;

  x = 0;

  /* Erase areas of the PIX where either no span exists or where a span is too narrow */
  for (span_idx = 0; span_idx < total_spans; span_idx++)
  {
    cur_span = &span_list[span_idx];

    /* If span is at least of average width, erase area between the previous span and this span */
    if ((cur_span->end - cur_span->start + 1) >= (int)(ave_span_width * 0.9))
    {
      status = erase_area_left_to_right(pixs, x, cur_span->start - x);

      if (status != LEPT_OK)
      {
        return status;
      }

      x = cur_span->end + 1;
    }
  }

  /* Clear area between the end of the right-most span and the right edge of the PIX */
  if ((x != 0) && (x < (pixs->w - 1)))
  {
    status = erase_area_left_to_right(pixs, x, pixs->w - x);

    if (status != LEPT_OK)
    {
      return status;
    }
  }

  return LEPT_OK;

} /* erase_furigana_vertical */


  /* Erase the furigana from the provided binary PIX. Works by finding spans of foreground
  text and removing the spans that are too narrow and are likely furigana.
  Use this version for horizontal text.
  Returns LEPT_OK on success. */
static int erase_furigana_horizontal(PIX *pixs, float scale_factor)
{
  int min_fg_pix_per_line = (int)(FURIGANA_MIN_FG_PIX_PER_LINE * scale_factor);
  int min_span_width = (int)(FURIGANA_MIN_WIDTH * scale_factor);
  l_uint32 x = 0;
  l_uint32 y = 0;
  int num_fg_pixels_on_line = 0;
  int good_line = LEPT_FALSE;
  int num_good_lines_in_cur_span = 0;
  int total_good_lines = 0;
  l_uint32 pixel_value = 0;
  Span span = { NO_VALUE, NO_VALUE };
  Span span_list[FURIGANA_MAX_SPANS];
  int total_spans = 0;
  int ave_span_width = 0;
  int span_idx = 0;
  Span *cur_span = NULL;
  l_int32 status = LEPT_ERROR;

  /* Get list of spans that contain fg pixels */
  for (y = 0; y < pixs->h; y++)
  {
    num_fg_pixels_on_line = 0;
    good_line = LEPT_FALSE;

    for (x = 0; x < pixs->w; x++)
    {
      status = pixGetPixel(pixs, x, y, &pixel_value);

      if (status != LEPT_OK)
      {
        return status;
      }

      /* If this is a foreground pixel */
      if (pixel_value == 1)
      {
        num_fg_pixels_on_line++;

        /* If this line has already meet the minimum number of fg pixels, stop scanning it */
        if (num_fg_pixels_on_line >= min_fg_pix_per_line)
        {
          good_line = LEPT_TRUE;
          break;
        }
      }
    }

    /* If last line is good, set it bad in order to close the span */
    if (good_line && (y == pixs->h - 1))
    {
      good_line = LEPT_FALSE;
      num_good_lines_in_cur_span++;
    }

    /* If this line has the minimum number of fg pixels */
    if (good_line)
    {
      /* Start a new span */
      if (span.start == NO_VALUE)
      {
        span.start = y;
      }

      num_good_lines_in_cur_span++;
    }
    else /* Line doesn't have enough fg pixels to consider as part of a span */
    {
      /* If a span has already been started, then end it */
      if (span.start != NO_VALUE)
      {
        /* If this span isn't too small (needed so that the average isn't skewed) */
        if (num_good_lines_in_cur_span >= min_span_width)
        {
          span.end = y;

          total_good_lines += num_good_lines_in_cur_span;

          /* Add span to the list */
          span_list[total_spans] = span;
          total_spans++;

          /* Prevent span list overflow */
          if (total_spans >= FURIGANA_MAX_SPANS)
          {
            break;
          }
        }
      }

      /* Reset span */
      span.start = NO_VALUE;
      span.end = NO_VALUE;
      num_good_lines_in_cur_span = 0;
    }
  }

  if (total_spans == 0)
  {
    return LEPT_OK;
  }

  /* Get average width of the spans */
  ave_span_width = total_good_lines / total_spans;

  y = 0;

  /* Erase areas of the PIX where either no span exists or where a span is too narrow */
  for (span_idx = 0; span_idx < total_spans; span_idx++)
  {
    cur_span = &span_list[span_idx];

    /* If span is at least of average width, erase area between the previous span and this span */
    if ((cur_span->end - cur_span->start + 1) >= (int)(ave_span_width * 0.9))
    {
      status = erase_area_top_to_bottom(pixs, y, cur_span->start - y);

      if (status != LEPT_OK)
      {
        return status;
      }

      y = cur_span->end + 1;
    }
  }

  /* Clear area between the end of the right-most span and the right edge of the PIX */
  if ((y != 0) && (y < (pixs->h - 1)))
  {
    status = erase_area_top_to_bottom(pixs, y, pixs->h - y);

    if (status != LEPT_OK)
    {
      return status;
    }
  }

  return LEPT_OK;

} /* erase_furigana_horizontal */


  /* Clear/erase a left-to-right section of the provided binary PIX.
  Returns 0 on success. */
static l_int32 erase_area_left_to_right(PIX *pixs, l_int32 x, l_int32 width)
{
  l_int32 status = LEPT_ERROR;
  BOX box;

  box.x = x;
  box.y = 0;
  box.w = width;
  box.h = pixs->h;

  status = pixClearInRect(pixs, &box);

  return status;

} /* erase_area_left_to_right */


  /* Clear/erase a top-to-bottom section of the provided binary PIX.
  Returns 0 on success. */
static l_int32 erase_area_top_to_bottom(PIX *pixs, l_int32 y, l_int32 height)
{
  l_int32 status = LEPT_ERROR;
  BOX box;

  box.x = 0;
  box.y = y;
  box.w = pixs->w;
  box.h = height;

  status = pixClearInRect(pixs, &box);

  return status;

} /* erase_area_top_to_bottom */

