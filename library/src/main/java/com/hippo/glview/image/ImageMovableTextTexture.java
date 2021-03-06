/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.glview.image;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.hippo.glview.glrenderer.GLCanvas;
import com.hippo.image.Image;
import com.hippo.image.ImageData;

import java.util.Arrays;

public class ImageMovableTextTexture extends ImageSpriteTexture {

    private final char[] mCharacters;
    private final float[] mWidths;
    private final float mHeight;
    private final float mMaxWidth;

    public ImageMovableTextTexture(@NonNull ImageData image, int count, int[] rects,
            char[] characters, float[] widths, float height, float maxWidth) {
        super(image, count, rects);

        mCharacters = characters;
        mWidths = widths;
        mHeight = height;
        mMaxWidth = maxWidth;
    }

    public int[] getTextIndexes(String text) {
        final char[] characters = mCharacters;
        final int length = text.length();
        final int[] indexes = new int[length];
        for (int i = 0; i < length; i++) {
            final char ch = text.charAt(i);
            indexes[i] = Arrays.binarySearch(characters, ch);
        }

        return indexes;
    }

    public float getTextWidth(String text) {
        final char[] characters = mCharacters;
        final float[] widths = mWidths;
        float width = 0.0f;

        for (int i = 0, n = text.length(); i < n; i++) {
            final char ch = text.charAt(i);
            final int index = Arrays.binarySearch(characters, ch);
            if (index >= 0) {
                width += widths[index];
            } else {
                width += mMaxWidth;
            }
        }

        return width;
    }

    public float getTextWidth(int[] indexes) {
        final float[] widths = mWidths;
        float width = 0.0f;
        for (final int index : indexes) {
            if (index >= 0) {
                width += widths[index];
            } else {
                width += mMaxWidth;
            }
        }

        return width;
    }

    public float getMaxWidth() {
        return mMaxWidth;
    }

    public float getTextHeight() {
        return mHeight;
    }

    public void drawText(GLCanvas canvas, String text, int x, int y) {
        final char[] characters = mCharacters;
        final float[] widths = mWidths;

        for (int i = 0, n = text.length(); i < n; i++) {
            final char ch = text.charAt(i);
            final int index = Arrays.binarySearch(characters, ch);
            if (index >= 0) {
                drawSprite(canvas, index, x, y);
                x += widths[index];
            } else {
                x += mMaxWidth;
            }
        }
    }

    public void drawText(GLCanvas canvas, int[] indexes, int x, int y) {
        final float[] widths = mWidths;

        for (final int index : indexes) {
            if (index >= 0) {
                drawSprite(canvas, index, x, y);
                x += widths[index];
            } else {
                x += mMaxWidth;
            }
        }
    }

    /**
     * Create a TextTexture to draw text
     *
     * @param typeface the typeface
     * @param size text size
     * @param characters all Characters
     * @return the TextTexture
     */
    @Nullable
    public static ImageMovableTextTexture create(Typeface typeface, int size, int color, char[] characters) {
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTextSize(size);
        paint.setColor(color);
        paint.setTypeface(typeface);

        final Paint.FontMetricsInt fmi = paint.getFontMetricsInt();
        final int fixed = fmi.bottom;
        final int height = fmi.bottom - fmi.top;

        final int length = characters.length;
        final float[] widths = new float[length];
        paint.getTextWidths(characters, 0, length, widths);

        // Calculate bitmap size
        float maxWidth = 0.0f;
        for (final float f : widths) {
            maxWidth = Math.max(maxWidth, f);
        }
        int hCount = (int) Math.ceil(Math.sqrt(height / maxWidth * length));
        int vCount = (int) Math.ceil(Math.sqrt(maxWidth / height * length));
        if (hCount * (vCount - 1) > length) {
            vCount--;
        }
        if ((hCount - 1) * vCount > length) {
            hCount--;
        }

        final Bitmap bitmap = Bitmap.createBitmap((int) Math.ceil(hCount * maxWidth),
                (int) Math.ceil(vCount * height), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        canvas.translate(0, height - fixed);

        // Draw
        final int[] rects = new int[length * 4];
        int x = 0;
        int y = 0;
        for (int i = 0; i < length; i++) {
            final int offset = i * 4;
            rects[offset + 0] = x;
            rects[offset + 1] = y;
            rects[offset + 2] = (int) widths[i];
            rects[offset + 3] = height;

            canvas.drawText(characters, i, 1, x, y, paint);

            if (i % hCount == hCount - 1) {
                // The end of row
                x = 0;
                y += height;
            } else {
                x += maxWidth;
            }
        }

        final ImageData image = Image.create(bitmap);
        bitmap.recycle();
        if (image == null) {
            return null;
        }
        return new ImageMovableTextTexture(image, length, rects, characters, widths, height, maxWidth);
    }
}
