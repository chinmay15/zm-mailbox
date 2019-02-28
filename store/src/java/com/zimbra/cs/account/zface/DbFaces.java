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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;

public class DbFaces {

    private static final String TABLE_NAME = "faces";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_ACCOUNT_ID = "account_id";
    private static final String COLUMN_PIC = "pic";

    public static class Face {
        private int id;
        private String accountId;
        private String base64pic;
        @SuppressWarnings("unused")
        private Face() {
            // private default constructor to avoid usage
        }
        public Face(int id, String accountId, String base64pic) {
            this.id = id;
            this.accountId = accountId;
            this.base64pic = base64pic;
        }
        public int getId() {
            return id;
        }
        public void setId(int id) {
            this.id = id;
        }
        public String getAccountId() {
            return accountId;
        }
        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }
        public String getBase64pic() {
            return base64pic;
        }
        public void setBase64pic(String base64pic) {
            this.base64pic = base64pic;
        }
    }

    public static void set(String accountId, String base64pic) throws ServiceException {
        DbConnection conn = DbPool.getConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("INSERT INTO " + TABLE_NAME + "(" + COLUMN_ACCOUNT_ID + ", " + COLUMN_PIC + ") VALUES (?, ?)");
            stmt.setString(1, accountId);
            stmt.setString(2, base64pic);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw ServiceException.FAILURE("writing faces entry: " + accountId, e);
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    public static boolean delete(String accountId, int id) throws ServiceException {
        DbConnection conn = DbPool.getConnection();
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("DELETE FROM " + TABLE_NAME + " WHERE " + COLUMN_ACCOUNT_ID + " = ? and " + COLUMN_ID + " = ?");
            stmt.setString(1, accountId);
            stmt.setInt(2, id);
            int num = stmt.executeUpdate();
            return num == 1;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("deleting config entry for " + accountId + " with id " + id, e);
        } finally {
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    public static Face get(int id) throws ServiceException {
        DbConnection conn = DbPool.getConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_ID + " = ?");
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            if (rs.next()) {
                int dbId = rs.getInt(COLUMN_ID);
                String accountId = rs.getString(COLUMN_ACCOUNT_ID);
                String base64pic = rs.getString(COLUMN_PIC);
                return new Face(dbId, accountId, base64pic);
            }
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting config entry: " + id, e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
        return null;
    }

    public static List<Face> getAllForAccount(String accountId) throws ServiceException {
        DbConnection conn = DbPool.getConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_ACCOUNT_ID + " = ?");
            stmt.setString(1, accountId);
            rs = stmt.executeQuery();
            List<Face> list = new ArrayList<Face>();
            while (rs.next()) {
                Face face = new Face(rs.getInt(COLUMN_ID), rs.getString(COLUMN_ACCOUNT_ID), rs.getString(COLUMN_PIC));
                list.add(face);
            }
            return list;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting all config entries", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }

    public static List<Face> getAll() throws ServiceException {
        DbConnection conn = DbPool.getConnection();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.prepareStatement("SELECT * FROM " + TABLE_NAME);
            rs = stmt.executeQuery();
            List<Face> list = new ArrayList<Face>();
            while (rs.next()) {
                Face face = new Face(rs.getInt(COLUMN_ID), rs.getString(COLUMN_ACCOUNT_ID), rs.getString(COLUMN_PIC));
                list.add(face);
            }
            return list;
        } catch (SQLException e) {
            throw ServiceException.FAILURE("getting all config entries", e);
        } finally {
            DbPool.closeResults(rs);
            DbPool.closeStatement(stmt);
            DbPool.quietClose(conn);
        }
    }
}
