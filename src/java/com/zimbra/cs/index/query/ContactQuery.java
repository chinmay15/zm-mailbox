/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index.query;

import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.LuceneQueryOperation;
import com.zimbra.cs.index.NoTermQueryOperation;
import com.zimbra.cs.index.QueryOperation;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * Special text query to search contacts.
 *
 * @author ysasaki
 */
public final class ContactQuery extends Query {
    private final String text;
    private final String withWildcard;

    public ContactQuery(String text) {
    	this.text = text;
    	this.withWildcard = buildWildcardQuery(text);
    }

    private String buildWildcardQuery(String text) {
		List<String> tokensWithWildcards = new LinkedList<String>();
		String[] tokens = text.split("\\s");
		for (int i = 0; i < tokens.length; i++) {
			String token = tokens[i];
			if (token.endsWith("*")) {
				tokensWithWildcards.add(token);
			} else {
				tokensWithWildcards.add(token + "*");
			}
		}
		return Joiner.on(" ").join(tokensWithWildcards);
	}

	@Override
    public boolean hasTextOperation() {
        return true;
    }

    @Override
    public QueryOperation compile(Mailbox mbox, boolean bool) throws ServiceException {
    	if (text.length() == 0) {
    		return new NoTermQueryOperation();
    	}
        LuceneQueryOperation op = new LuceneQueryOperation();
        op.addClause(toQueryString(LuceneFields.L_CONTACT_DATA, withWildcard), new TermQuery(new Term(LuceneFields.L_CONTACT_DATA, withWildcard)), evalBool(bool));
        return op;
    }

    @Override
    void dump(StringBuilder out) {
        out.append("CONTACT:").append(text);
    }

    @Override
    void sanitizedDump(StringBuilder out) {
    	int numWordsInQuery = text.split("\\s").length;
        out.append("CONTACT:").append(text);
    	out.append(":");
        out.append(Strings.repeat("$TEXT,", numWordsInQuery));
        if (out.charAt(out.length()-1) == ',') {
            out.deleteCharAt(out.length()-1);
        }
    }
}
