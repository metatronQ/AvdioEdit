package com.chenfu.avdioedit.util

import android.app.ProgressDialog
import android.content.Context
import com.chenfu.avdioedit.Interface.ResultCallback
import com.example.ndk_source.util.LogUtil
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler
import com.github.hiteshsondhi88.libffmpeg.FFmpeg

object FFmpegUtils {
    private lateinit var ffmpeg: FFmpeg
    private var progressDialog: ProgressDialog ?= null
    private lateinit var commands: Array<String>
    private lateinit var callback: ResultCallback

    /**
     *  TODO: 所有选项以双空格作为分隔符;
     *
     *  ffmpeg [全局选项] {[输入文件选项] -i 输入文件} ... {[输出文件选项] 输出文件} ...
     *  ffmpeg 命令应该符合上述格式，且需注意选项值是“。。。”，这样的话可能不能用空格做分隔符了
     */
    fun generateCmd(cmdString: String, resultCallback: ResultCallback): FFmpegUtils {
        // 以双空格作为分隔符
        commands = cmdString.split("  ").toTypedArray()
        this.callback = resultCallback
        return this
    }

    fun start(context: Context) {
        if (progressDialog == null) {
            progressDialog = ProgressDialog(context)
            progressDialog!!.setTitle(null)
        } else {
            if (progressDialog!!.isShowing) return
        }

        ffmpeg = FFmpeg.getInstance(context)
        progressDialog!!.let {
            ffmpeg.execute(commands, object : ExecuteBinaryResponseHandler() {
                override fun onSuccess(message: String) {
                    LogUtil.packageName(context).d("SUCCESS with output : $message")
                    callback.onSucceed()
                }

                override fun onFailure(message: String) {
                    LogUtil.packageName(context).e("FAILED with output : $message")
                    callback.onFailed()
                }

                override fun onProgress(message: String) {
                    LogUtil.packageName(context).d("Running command : ffmpeg $commands")
                    it.setMessage("Processing\n $message")
                }

                override fun onStart() {
                    LogUtil.packageName(context).d("Started command : ffmpeg $commands")
                    it.setMessage("Processing...")
                    it.show()
                }

                override fun onFinish() {
                    LogUtil.packageName(context).d("Finished command : ffmpeg $commands")
                    it.dismiss()
                }
            })
        }
    }

    /**
     * 超时杀死进程，供外部调用
     */
    fun killRunningProcesses() {
        ffmpeg.run {
            if (isFFmpegCommandRunning) {
                killShellProcess()
            }
        }
    }
}