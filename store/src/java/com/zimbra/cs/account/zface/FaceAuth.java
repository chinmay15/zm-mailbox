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
package com.zimbra.cs.account.zface;

import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.zface.DbFaces.Face;

public class FaceAuth {
    private static List<Face> faces = new ArrayList<Face>();
    static {
        try {
            faces = DbFaces.getAll();
        } catch (ServiceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static Account authenticate(Provisioning prov, String authPic)
            throws ServiceException {
        // TODO verify base64 data received as password with faces list
        Face result = faces.stream().filter(face -> validatePic(face)).findFirst().orElse(null);
        if (result == null) {
            return null;
        }
        Account account = prov.get(AccountBy.id, result.getAccountId());
        return account;
    }

    private static boolean validatePic(Face face) {
        return false;
    }
}
