package com.wanderwildwood.einkbirding

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.wanderwildwood.einkbirding.databinding.ActivityDownloadBinding

class DownloadActivity  : BaseActivity() {
    private var binding: ActivityDownloadBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDownloadBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
    }

    override fun onResume() {
        super.onResume()
        if (Downloader.checkModels(this)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    fun download(view: View) {
        binding?.downloadProgress?.visibility = View.VISIBLE
        Downloader.downloadModels(this, binding)
    }
}
