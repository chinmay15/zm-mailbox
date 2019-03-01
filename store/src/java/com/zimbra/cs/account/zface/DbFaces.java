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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;

public class DbFaces {
    public static final String DIR_FACES = "faces";
    public static final String PATH_DIR_FACES = LC.zimbra_data_directory.value() + "/" + DIR_FACES;

    static {
        File dir = new File(PATH_DIR_FACES);
        if (!dir.exists()) {
            if (!dir.mkdir()) {
                throw new RuntimeException("Failed to create: " + PATH_DIR_FACES);
            }
        } else {
            if (!dir.isDirectory()) {
                throw new RuntimeException(PATH_DIR_FACES + " : is not a directory");
            }
            if (!(dir.canRead() && dir.canWrite())) {
                throw new RuntimeException(PATH_DIR_FACES + " : does not have required permissions");
            }
        }
    }

    public static void set(String accountId, String base64pic) throws ServiceException, IOException {
        String userDirPath = PATH_DIR_FACES + "/" + accountId;
        File userDir = new File(userDirPath);
        if (!userDir.exists()) {
            if (!userDir.mkdir()) {
                throw ServiceException.FAILURE("Failed to create: " + userDirPath, null);
            }
        } else {
            if (!userDir.isDirectory()) {
                throw ServiceException.FAILURE(userDirPath + " : is not a directory", null);
            }
            if (!(userDir.canRead() && userDir.canWrite())) {
                throw ServiceException.FAILURE(userDirPath + " : does not have required permissions", null);
            }
        }

        Base64.Decoder base64Decoder = Base64.getDecoder();
        String fileName = System.currentTimeMillis() + ".jpg";
        byte [] imageByte = base64Decoder.decode(base64pic);
        File imageFile = new File(userDirPath + "/" + fileName);
        if (!imageFile.createNewFile()) {
            throw new RuntimeException("Failed to create: " + userDirPath + fileName);
        }
        OutputStream os = new BufferedOutputStream(new FileOutputStream(imageFile));
        os.write(imageByte);
        os.close();
    }

    public static boolean delete(String accountId, String fileName) throws ServiceException {
        File imageFile = new File(PATH_DIR_FACES + "/" + accountId + "/" + fileName);
        if (!imageFile.exists()) {
            throw ServiceException.FAILURE("Image file does not exist : " + fileName, null);
        }
        if (!imageFile.delete()) {
            throw ServiceException.FAILURE("Failed to delete image file : " + fileName, null);
        }
        return true;
    }

    public static File get(String accountId, String fileName) throws ServiceException {
        File imageFile = new File(PATH_DIR_FACES + "/" + accountId + "/" + fileName);
        if (!imageFile.exists()) {
            throw ServiceException.FAILURE("Image file does not exist : " + fileName, null);
        }
        return imageFile;
    }

    public static List<File> getAllForAccount(String accountId) throws ServiceException {
        String userDirPath = PATH_DIR_FACES + "/" + accountId;
        File userDir = new File(userDirPath);
        if (!userDir.exists()) {
            throw ServiceException.NOT_FOUND("Missing : " + userDirPath);
        }
        return Arrays.asList(userDir.listFiles());
    }
}
