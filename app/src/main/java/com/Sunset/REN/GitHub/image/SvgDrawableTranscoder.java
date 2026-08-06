package com.Sunset.REN.GitHub.image;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.SimpleResource;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.caverock.androidsvg.SVG;

import java.lang.reflect.Field;

final class SvgDrawableTranscoder implements ResourceTranscoder<SVG, Drawable> {
    @Override
    public Resource<Drawable> transcode(Resource<SVG> toTranscode, Options options) {
        SVG svg = toTranscode.get();
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        SvgSize viewBoxSize = resolveViewBoxSize(svg);
        float documentWidth = resolveDocumentWidth(svg, viewBoxSize, metrics);
        float documentHeight = resolveDocumentHeight(svg, viewBoxSize, documentWidth, metrics);
        float contentMaxWidth = Math.max(
                MinReadableWidthPx,
                metrics.widthPixels - Math.round(HorizontalMarkdownPaddingDp * metrics.density)
        );
        float scale = resolveScale(documentWidth, documentHeight, contentMaxWidth, metrics);
        int width = Math.max(1, Math.round(documentWidth * scale));
        int height = Math.max(1, Math.round(documentHeight * scale));
        svg.setDocumentWidth(width);
        svg.setDocumentHeight(height);
        Picture picture = svg.renderToPicture(width, height);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setDensity(Bitmap.DENSITY_NONE);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawPicture(picture);
        IntrinsicBitmapDrawable drawable = new IntrinsicBitmapDrawable(Resources.getSystem(), bitmap, width, height);
        drawable.setBounds(0, 0, width, height);
        return new SimpleResource<>(drawable);
    }

    private static float resolveDocumentWidth(SVG svg, SvgSize viewBoxSize, DisplayMetrics metrics) {
        float documentWidth = svg.getDocumentWidth();
        if (documentWidth > 0f) return documentWidth * metrics.scaledDensity;
        if (viewBoxSize.width > 0f) return viewBoxSize.width * metrics.scaledDensity;
        return DefaultSvgWidthDp * metrics.density;
    }

    private static float resolveDocumentHeight(SVG svg, SvgSize viewBoxSize, float resolvedWidth, DisplayMetrics metrics) {
        float documentHeight = svg.getDocumentHeight();
        if (documentHeight > 0f) return documentHeight * metrics.scaledDensity;
        if (viewBoxSize.height > 0f) {
            if (viewBoxSize.width > 0f) return resolvedWidth * (viewBoxSize.height / viewBoxSize.width);
            return viewBoxSize.height * metrics.scaledDensity;
        }
        return DefaultSvgHeightDp * metrics.density;
    }

    private static float resolveScale(float documentWidth, float documentHeight, float contentMaxWidth, DisplayMetrics metrics) {
        float maxScale = contentMaxWidth / documentWidth;
        float minReadableHeight = MinReadableSvgHeightDp * metrics.density;
        float readableScale = documentHeight < minReadableHeight ? minReadableHeight / documentHeight : 1f;
        return Math.max(1f, Math.min(maxScale, readableScale));
    }

    private static SvgSize resolveViewBoxSize(SVG svg) {
        Object viewBox = svg.getDocumentViewBox();
        if (viewBox == null) return SvgSize.Empty;
        return new SvgSize(readFloatField(viewBox, "width"), readFloatField(viewBox, "height"));
    }

    private static float readFloatField(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.getFloat(target);
        } catch (Exception error) {
            return 0f;
        }
    }

    private static final class IntrinsicBitmapDrawable extends BitmapDrawable {
        private final int intrinsicWidth;
        private final int intrinsicHeight;

        IntrinsicBitmapDrawable(Resources resources, Bitmap bitmap, int intrinsicWidth, int intrinsicHeight) {
            super(resources, bitmap);
            this.intrinsicWidth = intrinsicWidth;
            this.intrinsicHeight = intrinsicHeight;
        }

        @Override
        public int getIntrinsicWidth() {
            return intrinsicWidth;
        }

        @Override
        public int getIntrinsicHeight() {
            return intrinsicHeight;
        }
    }

    private static final class SvgSize {
        static final SvgSize Empty = new SvgSize(0f, 0f);
        final float width;
        final float height;

        SvgSize(float width, float height) {
            this.width = width;
            this.height = height;
        }
    }

    private static final float DefaultSvgWidthDp = 320f;
    private static final float DefaultSvgHeightDp = 120f;
    private static final float HorizontalMarkdownPaddingDp = 48f;
    private static final float MinReadableWidthPx = 240f;
    private static final float MinReadableSvgHeightDp = 24f;
}