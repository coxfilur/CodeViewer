package com.cr.com.cr.render;

/**
 * Created by lichunyu on 14-6-9.
 */
public interface SourceProvider {
    /**
     * Read source.
     * @param pos                   Relative to BOM, in bytes.
     * @param quantityCharsAtLeast The desired chars quantity, at least this quantity.
     *                             But if the file end is reached, the returned can be less than this.
     * @return Source text. null if end reached.
     */
    public String read(int pos, int quantityCharsAtLeast);

    public boolean havePrevious(int pos);

    public boolean haveNext(int pos);

    /**
     * Get length of given string in bytes.
     * @param str
     */
    int getLenOfStr(String str);
}
