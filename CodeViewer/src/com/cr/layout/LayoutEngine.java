package com.cr.layout;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Paint;
import android.view.ViewGroup;
import com.cr.util.L;

public class LayoutEngine {

    public static class LayoutSettings {
        /** canvas width */
        public int width;

        /** canvas height */
        public int height;

        /** space height between lines(one line's bottom to its downside line's top). */
        public int verticalInterval;

        /** Font height. */
        public int fontHeight;

    }

	/**
	 * render text
	 * @param text
     * @param atoms    Hints to given info about which text are not-dividable.
	 * @param settings Settings.
	 * 
	 */
	public ArrayList<LineInfo> layoutFast(String text, List<Atom> atoms, LayoutSettings settings) {
        final int interval = settings.verticalInterval;
        final int width = settings.width;
        final int height = settings.height;
        L.d("layout engine got height %d", height);
        final int lineHeight = settings.fontHeight;
		int y = 0;

        final ArrayList<LineInfo> result = new ArrayList<LineInfo>();

        final Paint layoutTool = new Paint();
        layoutTool.setTextSize(lineHeight);
		
		int fromChar = 0;
		
		float[] measuredWidth = new float[1];
		while(true) {		
			// skip CR LF
			for (int i = fromChar, n = text.length(); i < n; i++) {
				if (text.charAt(i) == '\n' || text.charAt(i) == '\r') {
					fromChar++;
				}
				else {
					break;
				}
			}

            if (fromChar >= text.length()) {
                break;
            }
			
			// find the next CR LF, which should not be layout in current line.
			int measureEnd = text.length();
			for (int i = fromChar, n = text.length(); i < n; i++) {
				if (text.charAt(i) == '\n') {
					measureEnd = i;
					break;
				}
			}
			
			int charsSize = layoutTool.breakText(text, fromChar, measureEnd, 
					true, width, measuredWidth);

            //L.d("text %s, from %d, width %d, consumed %d", text.substring(fromChar), fromChar, width, charsSize);

            if (charsSize + fromChar >= text.length()) {
				// fill line
				LineInfo lineInfo = new LineInfo();
				lineInfo.boundary = new Rect(0, y, (int) measuredWidth[0], y + lineHeight);
				lineInfo.fromChar = fromChar;
				lineInfo.toChar = text.length();
				result.add(lineInfo);

                L.d("exceeds text length");
				break;
			}
			
			int tailChar = fromChar + charsSize;

			// update tailChar position so as to let no atom is divided.		
			// TODO improve this.
			for (int i = 0, n = atoms.size(); i < n; i++) {
				final Atom atom = atoms.get(i);
				if (atom.contains(tailChar - 1) && atom.to != tailChar) {
					// the tail char is in the middle of an atom.
					tailChar = atom.from;
				}
			}
			
			LineInfo lineInfo = new LineInfo();
			lineInfo.boundary = new Rect(0, y, (int) measuredWidth[0], y + lineHeight);
			lineInfo.fromChar = fromChar;
			lineInfo.toChar = tailChar;
			result.add(lineInfo);
			
			fromChar = tailChar;
			
			y += (lineHeight + interval);
			if (y + lineHeight >= height) {
                L.d("exceeds total height");
				break;
			}
		}
		
		return result;
	}
	
}
