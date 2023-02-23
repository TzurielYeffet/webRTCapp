package com.example.webrtcapp.util

import com.example.webrtcapp.data.models.MessageModel

interface NewMessageInterface {
    fun onNewMessage(message: MessageModel)

}