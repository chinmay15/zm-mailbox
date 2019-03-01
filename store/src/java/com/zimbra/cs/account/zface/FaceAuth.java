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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_java;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.face.EigenFaceRecognizer;
import org.opencv.imgcodecs.Imgcodecs;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;

public class FaceAuth {
    static EigenFaceRecognizer efr;
    static Map<String, Integer> accountLableMap = new ConcurrentHashMap<String, Integer>();
    static {
        efr = EigenFaceRecognizer.create();
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        Runnable taskTrainFace = () -> {
            try {
                training();
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        executor.scheduleAtFixedRate(taskTrainFace, 0, 300000, TimeUnit.MILLISECONDS);
    }

    public static Account authenticate(Provisioning prov, String authPic) throws ServiceException {
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

    private static String convertBase64ToJPG(String string) throws IOException {
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

    private static String validatePic(String base64Input) throws IOException {
        String path = convertBase64ToJPG(base64Input);
        Mat readImage = Imgcodecs.imread(path, 0);

        int[] outLabel = new int[1];
        double[] outConf = new double[1];
        System.out.println("Starting Prediction...");
        efr.predict(readImage, outLabel, outConf);

        System.out.println("***Predicted label is " + outLabel[0] + ".***");

        System.out.println("***Confidence value is " + outConf[0] + ".***");

        int predictedLable = outLabel[0];

        for (String currentKey : accountLableMap.keySet()) {
            if (accountLableMap.get(currentKey) == predictedLable) {
                return currentKey;
            }
        }
        return null;
    }

    private static void training() {
        ArrayList<Mat> images = new ArrayList<>();

        ArrayList<Integer> labels = new ArrayList<>();
        String basePath = "/opt/zimbra/data/faces/";
        File folder = new File(basePath);
        for (File fileInFolders : folder.listFiles()) {
            int current = 0;
            String accountId = null;
            if (fileInFolders.isDirectory()) {
                accountId = fileInFolders.getName();
                for (File imagesForUser : fileInFolders.listFiles()) {
                    Mat readImage = Imgcodecs.imread(imagesForUser.getAbsolutePath(), 0);
                    images.add(readImage);
                }
            }
            labels.add(current);
            accountLableMap.put(accountId, current);
            current++;
        }

        Loader.load(opencv_java.class);
        System.out.println("Library loaded!!");
        System.out.println("Number of images " + images.size());
        MatOfInt labelsMat = new MatOfInt();
        labelsMat.fromList(labels);

        System.out.println("Starting training...");
        efr.train(images, labelsMat);

    }
}
