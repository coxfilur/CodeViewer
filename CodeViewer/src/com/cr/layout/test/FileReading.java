package com.cr.layout.test;


import java.util.ArrayList;
import java.util.List;

import com.cr.file.fileread.TXTFileReader;
import com.cr.layout.Atom;
import com.cr.layout.LayoutEngine;
import com.cr.layout.LineInfo;
import com.cr.split.Splitter;

public class FileReading {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TXTFileReader r = new TXTFileReader();
		r.openFile(args[0]);
		
		LayoutEngine engine = new LayoutEngine();

		Splitter spliter = new Splitter();
		while (!r.isFileEndReached()) {
			String block = r.readNextBlockData();
			System.out.println(block);
			List<Atom> atoms = spliter.split(block);
			
			if (atoms != null) {
				for (Atom e : atoms) {
					System.out.println(String.format("(%4d, %4d) : %s", e.from, e.to, 
							block.substring(e.from, e.to)));
				}
			}

            LayoutEngine.LayoutSettings settings = new LayoutEngine.LayoutSettings();
            settings.width = 1000;
            settings.height = 2000;
            settings.fontHeight = 30;
            settings.verticalInterval = 10;

            ArrayList<LineInfo> result = engine.layoutFast(block, atoms, settings);
			for (LineInfo e : result) {
				System.out.println(String.format("readerlayout (%4d, %4d, %s)", e.boundary.left, e.boundary.right,
						block.substring(e.fromChar, e.toChar)));
			}

            r.setCurrentOffset(r.computeLenOfString(block));
		}
		
		r.closeFile();
	}

}
