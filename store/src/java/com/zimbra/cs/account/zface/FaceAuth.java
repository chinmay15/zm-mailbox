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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_java;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.face.EigenFaceRecognizer;
import org.opencv.imgcodecs.Imgcodecs;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;

public class FaceAuth {
    public FaceAuth() {
        // nothing for now
    }
    private static EigenFaceRecognizer efr;
    private static Map<String, Integer> accountLableMap = new ConcurrentHashMap<String, Integer>();
    static {
        ZimbraLog.soap.debug("before loading");
        Loader.load(opencv_java.class);
        ZimbraLog.soap.debug("loading finished");
        efr = EigenFaceRecognizer.create();
        ZimbraLog.soap.debug("efr created, calling training()");
        training();
    }

    public Account authenticate(Provisioning prov, String authPic) throws ServiceException {
        // TODO verify base64 data received as password with faces list
        String accountId = null;
        try {
            accountId = validatePic(authPic);
        } catch (IOException e) {
            throw ServiceException.FAILURE("Authentiction picture could not be validated", null);
        }
        if (accountId == null) {
            return null;
        }
        Account account = prov.get(AccountBy.id, accountId);
        return account;
    }

    private String convertBase64ToJPG(String string) throws IOException {
        byte[] imageByte;
        String basePath = "/tmp/";
        String fileName = "input.jpg";
        Base64.Decoder base64Decoder = Base64.getDecoder();
        imageByte = base64Decoder.decode(string);
        File imageFile = new File(basePath + fileName);

        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(imageFile));
        outputStream.write(imageByte);
        outputStream.close();
        return basePath + fileName;
    }

    private String validatePic(String base64Input) throws IOException {
        String path = convertBase64ToJPG(base64Input);
        Mat readImage = Imgcodecs.imread(path, 0);

        int[] outLabel = new int[1];
        double[] outConf = new double[1];
        ZimbraLog.soap.debug("starting prediction");
        efr.read(DbFaces.TRAINING_RESULT);
        efr.predict(readImage, outLabel, outConf);

        ZimbraLog.soap.debug("***Predicted label is " + outLabel[0] + ".***");

        ZimbraLog.soap.debug("***Confidence value is " + outConf[0] + ".***");

        int predictedLable = outLabel[0];

        for (String currentKey : accountLableMap.keySet()) {
            if (accountLableMap.get(currentKey) == predictedLable) {
                return currentKey;
            }
        }
        return null;
    }

    private static void training() {
        ZimbraLog.soap.debug("in training method");
        List<Mat> images = new ArrayList<>();

        List<Integer> labels = new ArrayList<>();
        String basePath = DbFaces.PATH_DIR_FACES;
        ZimbraLog.soap.debug("before looping on ");
        File folder = new File(basePath);
        for (File fileInFolders : folder.listFiles()) {
            int current = 1;
            String accountId = null;
            if (fileInFolders.isDirectory()) {
                accountId = fileInFolders.getName();
                for (String name : fileInFolders.list()) {
                    String filePath = basePath + "/" + accountId + "/" + name;
                    ZimbraLog.soap.debug("File to be read : " + filePath);
                    Mat readImage = Imgcodecs.imread(filePath, 0);
                    images.add(readImage);
                    labels.add(Integer.valueOf(current));
                }
            }
            accountLableMap.put(accountId, current);
            current++;
        }

        ZimbraLog.soap.debug("before mat of int");
        MatOfInt labelsMat = new MatOfInt();
        ZimbraLog.soap.debug("lables size : " + labels.size());
        labelsMat.fromList(labels);
        ZimbraLog.soap.debug("mat size : " + labelsMat.rows());

        ZimbraLog.soap.debug("starting training");
        efr.train(images, labelsMat);
        ZimbraLog.soap.debug("training finished, writing in file now");
        efr.write(DbFaces.TRAINING_RESULT);
        ZimbraLog.soap.debug("finished writing");
    }
}
