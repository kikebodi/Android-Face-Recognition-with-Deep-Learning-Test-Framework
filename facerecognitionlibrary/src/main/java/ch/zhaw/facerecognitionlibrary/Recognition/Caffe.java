/* Copyright 2016 Michael Sladoje and Mike Schälchli. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package ch.zhaw.facerecognitionlibrary.Recognition;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import ch.zhaw.facerecognition.Helpers.CaffeMobile;

import org.opencv.core.Mat;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.List;

import ch.zhaw.facerecognition.Helpers.FileHelper;
import ch.zhaw.facerecognition.Helpers.MatName;
import ch.zhaw.facerecognition.R;

/***************************************************************************************
 *    Title: caffe-android-demo
 *    Author: sh1r0
 *    Date: 21.06.2016
 *    Code version: -
 *    Availability: https://github.com
 *
 ***************************************************************************************/

public class Caffe implements Recognition {
    private CaffeMobile caffe;
    private Recognition rec;
    private FileHelper fh;
    String layer;

    static {
        System.loadLibrary("caffe");
        System.loadLibrary("caffe_jni");
    }

    public Caffe(Context context, int method) {
        fh = new FileHelper();
        String dataPath = fh.CAFFE_PATH;

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences((context.getApplicationContext()));
        Resources res = context.getResources();
        String modelFile = sharedPref.getString("key_modelFileCaffe", res.getString(R.string.modelFileCaffe));
        String weightsFile = sharedPref.getString("key_weightsFileCaffe", res.getString(R.string.weightsFileCaffe));
        layer = sharedPref.getString("key_outputLayerCaffe", res.getString(R.string.weightsFileCaffe));
        String[] meanValuesString = sharedPref.getString("key_meanValuesCaffe", res.getString(R.string.meanValuesCaffe)).split(",");
        if(meanValuesString.length != 3){
            meanValuesString = res.getString(R.string.meanValuesCaffe).split(",");
        }
        float[] meanValues = new float[3];
        for(int i=0; i<3; i++){
            meanValues[i] = Float.parseFloat(meanValuesString[i]);
        }

        Boolean classificationMethod = sharedPref.getBoolean("key_classificationMethodTFCaffe", true);

        caffe = new CaffeMobile();
        caffe.setNumThreads(4);
        caffe.loadModel(dataPath + modelFile, dataPath + weightsFile);
        caffe.setMean(meanValues);
        if(classificationMethod){
            rec = new SupportVectorMachine(context, method);
        } else {
            rec = new KNearestNeighbor(context, method);
        }

    }

    @Override
    public boolean train() {
        return rec.train();
    }

    @Override
    public String recognize(Mat img, String expectedLabel) {
        return rec.recognize(getRepresentationVector(img), expectedLabel);
    }

    @Override
    public void saveToFile() {

    }

    @Override
    public void saveTestData() {
        rec.saveTestData();
    }

    @Override
    public void loadFromFile() {

    }

    @Override
    public void addImage(Mat img, String label) {
        rec.addImage(getRepresentationVector(img), label);
    }

    private Mat getRepresentationVector(Mat img){
        float[][] vector = caffe.getRepresentationLayer(saveMatToImage(img), layer);

        List<Float> fVector = new ArrayList<>();
        for(float f : vector[0]){
            fVector.add(f);
        }

        return Converters.vector_float_to_Mat(fVector);
    }

    private String saveMatToImage(Mat img){
        MatName m = new MatName("caffe_vector", img);
        return fh.saveMatToImage(m, fh.CAFFE_PATH);
    }
}