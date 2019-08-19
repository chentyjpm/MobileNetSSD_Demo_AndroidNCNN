// Tencent is pleased to support the open source community by making ncnn available.
//
// Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.
//
// Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
// in compliance with the License. You may obtain a copy of the License at
//
// https://opensource.org/licenses/BSD-3-Clause
//
// Unless required by applicable law or agreed to in writing, software distributed
// under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
// CONDITIONS OF ANY KIND, either express or implied. See the License for the
// specific language governing permissions and limitations under the License.

#include <stdio.h>
#include <time.h>
#include <vector>
#include <jni.h>

#include <android/log.h>

#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, "libssd", __VA_ARGS__))
#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO , "libssd", __VA_ARGS__))
#define LOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN , "libssd", __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, "libssd", __VA_ARGS__))


#include "platform.h"
#include "net.h"
#include "mobilenetssd.h"
#include "ssd-int8.mem.h"


#if 0
struct Object
{
    cv::Rect_<float> rect;
    int label;
    float prob;
};
#endif


ncnn::Net g_mobilenet;
struct ai_object_t g_obj[OBJMAX];

int cpp_mobilenet_aiinit(void)
{
    int st, et;
    double costtime;
    st = clock();

    g_mobilenet.load_param(aiparam);
    g_mobilenet.load_model(aibin);

    et = clock();

    costtime = et - st;
    LOGI("load_model cost %fs\n", costtime / CLOCKS_PER_SEC);

    return 0;
}

int cpp_mobilenet_aifini(void)
{
    int st, et;
    double costtime;
    st = clock();
    et = clock();
    costtime = et - st;
    LOGI("release_model cost %fs\n", costtime / CLOCKS_PER_SEC);
    return 0;
}

static const char* class_names[] = {"background",
                                        "aeroplane", "bicycle", "bird", "boat",
                                        "bottle", "bus", "car", "cat", "chair",
                                        "cow", "diningtable", "dog", "horse",
                                        "motorbike", "person", "pottedplant",
                                        "sheep", "sofa", "train", "tvmonitor"
                                       };

struct ai_object_t* cpp_mobilenet_aidetect(const unsigned char* pixels, int w, int h, int *objcnt)
{
    int st, et;
    double costtime;
    st = clock();
    LOGD("detect img %d X %d\n", w, h);
    ncnn::Mat in = ncnn::Mat::from_pixels_resize(pixels, ncnn::Mat::PIXEL_GRAY2BGR, w, h, 448, 448);

    const float mean_vals[3] = {127.5f, 127.5f, 127.5f};
    const float norm_vals[3] = {1.0 / 127.5, 1.0 / 127.5, 1.0 / 127.5};
    in.substract_mean_normalize(mean_vals, norm_vals);

    ncnn::Extractor g_ex = g_mobilenet.create_extractor();

    g_ex.input(0, in);
    ncnn::Mat out;
    g_ex.extract(149, out);

    et = clock();
    costtime = et - st;
    LOGD("detect cost %fs\n", costtime / CLOCKS_PER_SEC);

    for (int i = 0; i < out.h; i++)
    {
        const float* values = out.row(i);
        if (i < OBJMAX)
        {
            struct ai_object_t* obj = &g_obj[i];

            obj->label = values[0];
            obj->prob = values[1];
            obj->x = values[2];
            obj->y = values[3];
            obj->xe = values[4];
            obj->ye = values[5];
            obj->name = (char *)class_names[obj->label];

            LOGI("%d = %.5f at %f %f %f %f %s\n", obj->label, obj->prob,
                   obj->x, obj->y, obj->xe, obj->ye, obj->name);
        }

    }
    *objcnt = out.h;

    return g_obj;
}

void cpp_mobilenet_yuv420sp2rgb(const unsigned char* yuv420sp, int w, int h, unsigned char* rgb)
{
    //yuv420sp2rgb(yuv420sp, w, h, rgb);
}

#ifdef __cplusplus  
extern "C" {  
#endif  
  
int mobilenet_aiinit(void)
{
    return cpp_mobilenet_aiinit();
}

int mobilenet_aifini(void)
{
    return cpp_mobilenet_aifini();
}

struct ai_object_t* mobilenet_aidetect(const unsigned char* pixels, int w, int h, int *objcnt)
{
    return cpp_mobilenet_aidetect(pixels, w, h, objcnt);
}

void mobilenet_yuv420sp2rgb(const unsigned char* yuv420sp, int w, int h, unsigned char* rgb)
{
    cpp_mobilenet_yuv420sp2rgb(yuv420sp, w, h, rgb);
}

extern "C" JNIEXPORT jdoubleArray JNICALL
Java_com_chenty_testncnn_CameraNcnnFragment_detectyonly(JNIEnv *env, jobject thiz, jbyteArray frame, jint src_width,
                                jint src_height, jdoubleArray detect) {
    char *yuv_frame = (char*)env->GetPrimitiveArrayCritical(frame, NULL);

    int size = env->GetArrayLength(frame);
    int objectcnt = 0;
    int i;

    //shift argb to rgba
    char *yuv = (char *)malloc(size);
    memcpy(yuv, yuv_frame, size);

    env->ReleasePrimitiveArrayCritical(frame, yuv_frame, JNI_ABORT);

    struct ai_object_t *obj = cpp_mobilenet_aidetect((const unsigned char *)yuv, src_width, src_height, &objectcnt);

    free(yuv);

    double *detect_out = (double *)env->GetPrimitiveArrayCritical(detect, NULL);
    if(objectcnt <= 0)
        objectcnt = 1;

    for(i = 0 ; i < objectcnt ; i++)
    {
        detect_out[i*6 + 0] = obj->label;
        detect_out[i*6 + 1] = obj->prob;
        detect_out[i*6 + 2] = obj->x;
        detect_out[i*6 + 3] = obj->y;
        detect_out[i*6 + 4] = obj->xe;
        detect_out[i*6 + 5] = obj->ye;
        obj++;
    }


    env->ReleasePrimitiveArrayCritical(detect, detect_out, JNI_ABORT);

    return detect;
}

extern "C" JNIEXPORT void JNICALL
Java_com_chenty_testncnn_CameraNcnnFragment_initssd(
        JNIEnv *env,
        jobject /* this */) {
    mobilenet_aiinit();

}

extern "C" JNIEXPORT void JNICALL
Java_com_chenty_testncnn_CameraNcnnFragment_finissd(
        JNIEnv *env,
        jobject /* this */) {
    mobilenet_aiinit();

}

#ifdef __cplusplus  
}  
#endif