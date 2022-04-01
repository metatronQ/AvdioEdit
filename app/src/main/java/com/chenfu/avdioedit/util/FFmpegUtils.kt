package com.chenfu.avdioedit.util

import android.app.ProgressDialog
import android.content.Context
import com.chenfu.avdioedit.Interface.ResultCallback
import com.example.ndk_source.util.LogUtil
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.FFmpeg

object FFmpegUtils {
    private var ffmpeg: FFmpeg ?= null

    /**
     * 裁剪输出应该是两段文件
     * @param options 不包含输入输出文件路径的选项
     */
    fun CropVideoDefault(context: Context, srcFilePath: String,
                         outFilePath1: String, options1: String,
                         outFilePath2: String, options2: String,
                         resultCallback: ResultCallback) {
        ffmpeg = FFmpeg.getInstance(context)
        val progressDialog = ProgressDialog(context)
        progressDialog.setTitle(null)
        val cmdString = "-i $srcFilePath $options1 $outFilePath1 $options2 $outFilePath2"
        val commands = cmdString.split(" ").toTypedArray()
        ffmpeg?.execute(commands, object : ExecuteBinaryResponseHandler() {
            override fun onSuccess(message: String) {
                LogUtil.packageName(context).d("SUCCESS with output : $message")
                resultCallback.onSucceed()
            }

            override fun onFailure(message: String) {
                LogUtil.packageName(context).e("FAILED with output : $message")
            }

            override fun onProgress(message: String) {
                LogUtil.packageName(context).d("Running command : ffmpeg $commands")
                progressDialog.setMessage("Processing\n $message")
            }

            override fun onStart() {
                LogUtil.packageName(context).d("Started command : ffmpeg $commands")
                progressDialog.setMessage("Processing...")
                progressDialog.show()
            }

            override fun onFinish() {
                LogUtil.packageName(context).d("Finished command : ffmpeg $commands")
                progressDialog.dismiss()
            }
        })
    }

    fun CropVideoByCopy(context: Context, srcFilePath: String,
                        outPath1: String, options1: String,
                        outPath2: String, options2: String,
                        resultCallback: ResultCallback) {
        val newOptions1 = "$options1 -c copy"
        val newOptions2 = "$options2 -c copy"
        CropVideoDefault(context, srcFilePath, outPath1, newOptions1, outPath2, newOptions2, resultCallback)
    }

    fun spliceAvdio() {

    }

    fun separateAvdio() {

    }

    /**
     * 超时杀死进程，供外部调用
     */
    fun killRunningProcesses() {
        ffmpeg?.run {
            if (isFFmpegCommandRunning) {
                killShellProcess()
            }
        }
    }
}