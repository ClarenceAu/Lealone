/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package com.codefollower.lealone.dbobject.index;

import java.util.ArrayList;

import com.codefollower.lealone.dbobject.table.TableBase;
import com.codefollower.lealone.engine.Session;
import com.codefollower.lealone.result.Row;
import com.codefollower.lealone.result.SearchRow;

/**
 * Cursor implementation for non-unique hash index
 *
 * @author Sergi Vladykin
 */
public class NonUniqueHashCursor implements Cursor {

    private final Session session;
    private final ArrayList<Long> positions;
    private final TableBase tableData;

    private int index = -1;

    public NonUniqueHashCursor(Session session, TableBase tableData, ArrayList<Long> positions) {
        this.session = session;
        this.tableData = tableData;
        this.positions = positions;
    }

    public Row get() {
        if (index < 0 || index >= positions.size()) {
            return null;
        }
        return tableData.getRow(session, positions.get(index));
    }

    public SearchRow getSearchRow() {
        return get();
    }

    public boolean next() {
        return positions != null && ++index < positions.size();
    }

    public boolean previous() {
        return positions != null && --index >= 0;
    }

}
