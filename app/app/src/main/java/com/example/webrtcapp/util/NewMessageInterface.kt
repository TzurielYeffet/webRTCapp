package com.example.webrtcapp.util

import com.example.webrtcapp.models.MessageModel

interface NewMessageInterface {
    fun onNewMessage(message:MessageModel)

}