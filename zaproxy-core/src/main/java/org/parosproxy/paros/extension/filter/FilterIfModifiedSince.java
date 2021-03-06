/*
 *
 * Paros and its related class files.
 * 
 * Paros is an HTTP/HTTPS proxy for assessing web application security.
 * Copyright (C) 2003-2004 Chinotec Technologies Company
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Clarified Artistic License
 * as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Clarified Artistic License for more details.
 * 
 * You should have received a copy of the Clarified Artistic License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
// ZAP: 2012/04/25 Added @Override annotation to all appropriate methods.
// ZAP: 2013/01/25 Removed the "(non-Javadoc)" comments.
// ZAP: 2013/03/03 Issue 546: Remove all template Javadoc comments
package org.parosproxy.paros.extension.filter;

import org.parosproxy.paros.Constant;
import org.parosproxy.paros.network.HttpHeader;
import org.parosproxy.paros.network.HttpMessage;
import org.parosproxy.paros.network.HttpRequestHeader;

public class FilterIfModifiedSince extends FilterAdaptor {

    @Override
    public int getId() {
        return 10;
    }

    @Override
    public String getName() {
        return Constant.messages.getString("filter.nocache.name");
        
    }

    @Override
    public void onHttpRequestSend(HttpMessage httpMessage) {
        HttpRequestHeader reqHeader = httpMessage.getRequestHeader();
      	if (!reqHeader.isEmpty() && reqHeader.isText()){
      		String ifModifed = reqHeader.getHeader(HttpHeader.IF_MODIFIED_SINCE);
      		if (ifModifed != null){    
      			reqHeader.setHeader(HttpHeader.IF_MODIFIED_SINCE, null);                   
      		}
      		String ifNoneMatch = reqHeader.getHeader(HttpHeader.IF_NONE_MATCH);
      		if (ifNoneMatch != null){    
      			reqHeader.setHeader(HttpHeader.IF_NONE_MATCH, null);                   
      		}
      		
      	}

    }

    @Override
    public void onHttpResponseReceive(HttpMessage httpMessage) {

    }

}
