package com.example.webrtcapp

import android.util.Log
import com.example.webrtcapp.models.MessageModel
import com.example.webrtcapp.util.NewMessageInterface
import com.google.gson.Gson
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI

class SocketRepository (private val messageInterface: NewMessageInterface){
    private val TAG ="SocketRepository"
    private var webSocket:WebSocketClient?=null
    private var userName:String?=null
    private val gson = Gson()
//    val webSocketAddress:String = "ws://192.168.1.43:3000"
//    val webSocketAddress:String = "ws://192.168.50.212:3000"
    val webSocketAddress:String = "ws://192.168.1.20:3000"

    fun initSocket(username:String){
        userName=username
        webSocket = object:WebSocketClient(URI(webSocketAddress)){
            override fun onOpen(handshakedata: ServerHandshake?) {
                sendMessageToSocket(
                    MessageModel(
                        "store_user",
                        username,
                        null,
                        null
                    )
                )
            }

            override fun onMessage(message: String?) {
                try {
                    messageInterface.onNewMessage(gson.fromJson(message,MessageModel::class.java))
                }catch (e:Exception){
                    e.printStackTrace()
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.d(TAG,"OnClose $reason")
            }

            override fun onError(ex: Exception?) {
                Log.d(TAG,"onError $ex")
            }

        }
        webSocket?.connect()
    }
    fun sendMessageToSocket(message: MessageModel){
        try {
            webSocket?.send(Gson().toJson(message))
        }catch (e:Exception){
            e.printStackTrace()
        }
    }
}