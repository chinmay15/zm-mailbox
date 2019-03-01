/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2019 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.account.message;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.util.StringUtil;

@XmlRootElement(name=AccountConstants.E_ADD_FACES_REQUEST /* AddFacesRequest */)
@XmlType(propOrder = {})
public class AddFacesRequest {
    /**
     * @zm-api-field-description New Password to assign
     */
    @XmlElement(name=AccountConstants.E_AUTH_PIC /* password */, required=true)
    private String authPic;

    public AddFacesRequest() {
    }

    public AddFacesRequest(String authPic) {
        this.authPic = authPic;
    }

    /**
     * @return the authPic
     */
    public String getAuthPic() {
        return authPic;
    }

    /**
     * @param authPic the authPic to set
     */
    public void setAuthPic(String authPic) {
        this.authPic = authPic;
    }

    public void validateAddFacesRequest() throws ServiceException {
        if (StringUtil.isNullOrEmpty(this.authPic)) {
            throw ServiceException.INVALID_REQUEST("Invalid or missing authentication picture", null);
        }
    }
}
