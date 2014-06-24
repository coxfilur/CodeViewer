package com.cr.split;

import java.util.ArrayList;
import java.util.List;

import com.cr.layout.Atom;

public class Splitter {

	private static final int MASK_VAR_NAME_OR_KEYWORD = 0x7;
	
	private static final int LETTER = 0x1;
	private static final int DIGIT = 0x2;
	private static final int UNDERLINE = 0x4;
	
	private static final int SPACE = 0x8;
	private static final int LFCR = 0x10;
	private static final int BRACKET = 0x20;
	private static final int OTHERS = 0x40;
	private static final int NONE = 0x80000000;
	
	public List<Atom> split(String text) {
		ArrayList<Atom> ret = new ArrayList<Atom>();
		int curStart = 0;
		int lastType = NONE;
		int taken = -1;
		
		for (int i = 0, n = text.length(); i < n; i++) {
			char c = text.charAt(i);
			int type = getType(c);

			if (isVarNameChar(type)) {
				if (!isVarNameChar(lastType)) {
					curStart = i;
				}
			} else if (taken != curStart) {
				Atom atom = new Atom();
				atom.from = curStart;
				atom.to = i;
				ret.add(atom);
				taken = curStart;
			}
			lastType = type;
		}
		return ret;
	}

	private boolean isVarNameChar(int type) {
		return (type & MASK_VAR_NAME_OR_KEYWORD) != 0;
	}
	
	private int getType(char c) {
		if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z') {
			return LETTER;
		}
		if (c >= '0' && c <= '9') {
			return DIGIT;
		}
		if (c == '_') {
			return UNDERLINE;
		}
		if (c == '(' || c == '[' || c == '{'
				|| c == ')' || c == ']' || c == '}') {
			return BRACKET;
		}
		if (c == '\n' || c == '\r') {
			return LFCR;
		}
		if (c == ' ' || c == '\t') {
			return SPACE;
		}
		return OTHERS;
	}
	
	
}
