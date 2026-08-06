package com.Sunset.REN.GitHub.image;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.SimpleResource;
import com.caverock.androidsvg.SVG;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

final class SvgDecoder implements ResourceDecoder<InputStream, SVG> {
    @Override
    public boolean handles(InputStream source, Options options) throws IOException {
        if (!source.markSupported()) return true;
        source.mark(SvgHeaderBytes);
        byte[] header = new byte[SvgHeaderBytes];
        int read = source.read(header);
        source.reset();
        if (read <= 0) return false;
        String text = new String(header, 0, read, StandardCharsets.UTF_8)
                .toLowerCase(Locale.US);
        return text.contains("<svg") || text.contains("<!doctype svg") || text.contains("http://www.w3.org/2000/svg");
    }

    @Override
    public Resource<SVG> decode(InputStream source, int width, int height, Options options) throws IOException {
        try {
            return new SimpleResource<>(SVG.getFromInputStream(source));
        } catch (Exception error) {
            throw new IOException("Cannot decode SVG", error);
        }
    }

    private static final int SvgHeaderBytes = 8192;
}