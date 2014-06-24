package com.cr;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import com.cr.com.cr.render.Render;
import com.cr.com.cr.render.SourceProvider;
import com.cr.file.fileread.TXTFileReader;
import com.cr.util.L;

public class MainActivity extends Activity {

    private Render mRender = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        SurfaceView sv = new SurfaceView(this);

        final String file = "/sdcard/test.java";

        SourceProvider sourceProvider = new MySourceProvider(file);

        mRender = new Render(sourceProvider);

        sv.getHolder().addCallback(mRender);

		setContentView(sv);

        sv.setOnTouchListener(new View.OnTouchListener() {

            private int mLastY = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int curY = (int) event.getY();

                final int action = (event.getAction() & MotionEvent.ACTION_MASK);
                if (action == MotionEvent.ACTION_DOWN) {
                    mLastY = curY;
                    return true;
                }

                int offset = mLastY - curY;
                L.d("test render by offset %d", offset);
                mRender.scrollBy(5);

                mLastY = curY;
                return true;
            }
        });

//        FileReading test = new FileReading();
//        test.main(new String[] {file});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

    public static class MySourceProvider implements  SourceProvider {
        private long mBomSize;

        private TXTFileReader mReader;

        MySourceProvider(String filename) {
            mReader = new TXTFileReader();
            mReader.openFile(filename);

            mBomSize = mReader.getFileStartPos();

            L.i("open file");
            L.i("file bytes length %d", mReader.getFileSize());
        }
        @Override
        public String read(int pos, int quantityCharsAtLeast) {
            pos -= mBomSize;
            return mReader.readNextBlockData(quantityCharsAtLeast);
        }

        @Override
        public boolean havePrevious(int pos) {
            return pos > mBomSize;
        }

        @Override
        public boolean haveNext(int pos) {
            return pos + mBomSize < mReader.getFileSize();
        }

        @Override
        public int getLenOfStr(String str) {
            return (int) mReader.computeLenOfString(str);
        }
    };

}
