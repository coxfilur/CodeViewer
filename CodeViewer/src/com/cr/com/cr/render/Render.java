package com.cr.com.cr.render;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import com.cr.layout.Atom;
import com.cr.layout.LayoutEngine;
import com.cr.layout.LineInfo;
import com.cr.split.Splitter;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.SurfaceHolder;
import com.cr.util.L;

public class Render implements SurfaceHolder.Callback {

    private SurfaceHolder mHolder;

    private LayoutEngine mEngine = new LayoutEngine();

    private Splitter mSplitter = new Splitter();

    private CacheSource mSourceInfo;

    private Paint mTextPaint = new Paint();

    private Paint mBitmapPaint = new Paint();

    private int mFontSize = 50;

    private int mInterval = 5;

    private final SourceProvider mSourceProvider;

    private Handler mHandler;

    private HandlerThread mThread;

    /** In bytes, relative to BOM if there is BOM. */
    private int mPos;


    public Render(SourceProvider sourceProvider) {
        mSourceProvider = sourceProvider;

        mThread = new HandlerThread("RenderThread");
        mThread.start();
        mHandler = new Handler(mThread.getLooper()) {
            @Override
            public void handleMessage(Message message) {
                render(message.arg1);
            }
        };

        mTextPaint.setColor(Color.GREEN);
        mTextPaint.setTextSize(mFontSize);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mHolder = holder;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        mHolder = holder;
        scrollBy(0);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mHolder = null;
    }

    public void setInterval(int interval) {
        mInterval = interval;
    }

    private static class CacheSource {
        String text;
        ArrayList<LineInfo> lines;
        List<Atom> atoms;
        int pos;
        Bitmap bitmap;
    }

    private int mTranslated = 0;

    /**
     *
     * @param distance  Positive for scroll downwardly, negative for scroll upwardly.
     */
    public void scrollBy(int distance) {
        Message msg = Message.obtain();
        msg.arg1 = distance;
        mHandler.sendMessage(msg);
    }

    /**
     * Render view.
     * @param scrollBy  Positive for scroll downwardly, negative for scroll upwardly.
     */
    private void render(int scrollBy) {
        if (scrollBy < 0) {
            return;
        }

        mTranslated += scrollBy;
        L.d("translate %d, scroll by %d", mTranslated, scrollBy);

        SurfaceHolder holder = mHolder;
        if (holder == null) {
            return;
        }

        Canvas canvas = holder.lockCanvas();
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        float cacheScreenNum = 2;

        if (mSourceInfo == null) {
            mSourceInfo = new CacheSource();
            updateSourceCache(mPos, width, height, cacheScreenNum);
        }  else if (mSourceInfo != null && mSourceInfo.bitmap != null && mTranslated + height > mSourceInfo.bitmap.getHeight() && mSourceProvider.haveNext(mPos)) {
            // not enough cache, to update the SourceInfo.
            // we have to make mTranslated and mSourceInfo consistent.
            // first, find a proper position in existed lines.

            final int properPosition = mTranslated;// TODO can be improved.
            ArrayList<LineInfo> lines = mSourceInfo.lines;
            LineInfo targetLine = null;
            for (int i = 0, n = lines.size(); i < n; i++) {
                LineInfo li = lines.get(i);
                if (li.boundary.bottom >= properPosition) {
                    targetLine = li;
                    break;
                }
            }

            if (targetLine == null) {
                L.i("cannot found line to be discarded, %d, line bottom %d", properPosition, lines.size() != 0 ? lines.get(lines.size() - 1).boundary.bottom : -1);
            } else {
                // calculate new start offset.
                int discardedLen = mSourceProvider.getLenOfStr(mSourceInfo.text.substring(0, targetLine.toChar));
                // to make mTranslated and mSourceInfo consistent.
                mTranslated += targetLine.boundary.bottom;
                mPos += discardedLen;
                updateSourceCache(mPos, width, height, cacheScreenNum);

                L.d("update source cache for not enough");
            }
        }


        canvas.save();
        canvas.translate(0, -mTranslated);

        L.d("test translated %d", mTranslated);

        if (mSourceInfo.bitmap != null) {
            canvas.drawColor(Color.BLACK);
            canvas.drawBitmap(mSourceInfo.bitmap, 0, 0, mBitmapPaint);
        }

        canvas.restore();
        holder.unlockCanvasAndPost(canvas);
    }

    private Bitmap renderBitmap(ArrayList<LineInfo> lines, int width) {
        if (lines.isEmpty()) {
            return null;
        }
        int height = lines.get(lines.size() - 1).boundary.bottom;

        L.d("last line top %d, bottom %d", lines.get(lines.size() - 1).boundary.top, height);

        Bitmap.Config config = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = Bitmap.createBitmap(width, height, config);

        L.d("create bitmap width %d, height %d", width, height);

        Canvas c = new Canvas(bitmap);
        for (LineInfo e : lines) {
            c.drawText(mSourceInfo.text.substring(e.fromChar, e.toChar),
                    e.boundary.left, e.boundary.bottom, mTextPaint);
        }
        return bitmap;
    }

    private void updateSourceCache(int textStartPos, int width, int height, float cacheScreenNum) {
        final SourceProvider provider = mSourceProvider;
        int charsNum = getQuantity(width, height, cacheScreenNum);

        L.d("chars quantity %d", charsNum);

        mSourceInfo.text = provider.read(textStartPos, charsNum);
        mSourceInfo.atoms = mSplitter.split(mSourceInfo.text);

        LayoutEngine.LayoutSettings settings = new LayoutEngine.LayoutSettings();
        settings.width = width;
        settings.height = (int) (height * cacheScreenNum);
        settings.fontHeight = mFontSize;
        settings.verticalInterval = mInterval;

        mSourceInfo.lines = mEngine.layoutFast(mSourceInfo.text, mSourceInfo.atoms, settings);
        mSourceInfo.bitmap = renderBitmap(mSourceInfo.lines, width);
    }

    private int getQuantity(int width, int height, float cacheScreenNum) {
        L.d("width %d, height %d, font size %d, interval %d, cache screen num %f", width, height, mFontSize, mInterval, cacheScreenNum);
        return (int) (width * height / (mFontSize * (mFontSize + mInterval)) * cacheScreenNum);
    }

}
