package com.example.webrtcapp.ui.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.webrtcapp.data.repositories.SocketRepository

class CallViewModelFactory(): ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(CallViewModel::class.java)){
            @Suppress("UNCHECKED_CAST")
            return CallViewModel()as T

        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}