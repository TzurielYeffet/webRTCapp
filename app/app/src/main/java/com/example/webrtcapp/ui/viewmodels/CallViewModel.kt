package com.example.webrtcapp.ui.viewmodels

import android.app.Application
import android.util.Log
import android.view.View
import androidx.lifecycle.ViewModel
import com.example.webrtcapp.data.models.IceCandidateModel
import com.example.webrtcapp.data.models.MessageModel
import com.example.webrtcapp.data.models.RTCClient
import com.example.webrtcapp.data.repositories.SocketRepository
import com.example.webrtcapp.util.NewMessageInterface
import com.example.webrtcapp.util.PeerConnectionObserver
import com.google.gson.Gson
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer

class CallViewModel: ViewModel(),NewMessageInterface  {

    private var username:String?=null
    private var socketRepository: SocketRepository?=null
    private var rtcClient: RTCClient?=null
    private var target:String = ""
    private val gson = Gson()
    val TAG="CallViewModel"


    fun init(username:String,remoteView:SurfaceViewRenderer){
        this.username = username
        socketRepository = SocketRepository(this)
        this.username?.let { socketRepository?.initSocket(it) }
        rtcClient = RTCClient(application = Application(),username!!,socketRepository!!,object : PeerConnectionObserver(){
            override fun onIceCandidate(iceCandidate: IceCandidate?) {
                super.onIceCandidate(iceCandidate)
                rtcClient?.addIceCandidate(iceCandidate)
                val candidate = hashMapOf(
                    "sdpMid" to iceCandidate?.sdpMid,
                    "sdpMLineIndex" to iceCandidate?.sdpMLineIndex,
                    "sdpCandidate" to iceCandidate?.sdp
                )
                socketRepository?.sendMessageToSocket(
                    MessageModel(
                    "ice_candidate",username,target,candidate
                )
                )
            }

            override fun onAddStream(mediaStream: MediaStream?) {
                super.onAddStream(mediaStream)
                //return here media stream for the call activity to implement
                //            this line
                //                |
                //                |
                //                V
                mediaStream?.videoTracks?.get(0)?.addSink(remoteView)
                Log.d(TAG,"on Add Stream: $mediaStream")
            }
        })
    }
    fun handleCallRespond(localView: SurfaceViewRenderer, remoteView: SurfaceViewRenderer, target: String?){
        rtcClient?.initializeSurfaceView(localView)
        rtcClient?.initializeSurfaceView(remoteView)
        rtcClient?.startLocalVideo(localView, null)
        rtcClient?.call(target!!)
    }

    fun handleOffer(localView: SurfaceViewRenderer, remoteView: SurfaceViewRenderer, data: Any, targetName: String,senderName:String) {
        rtcClient?.initializeSurfaceView(localView)
        rtcClient?.initializeSurfaceView(remoteView)
        rtcClient?.startLocalVideo(localView, null)
        val session = SessionDescription(
            SessionDescription.Type.OFFER,
            data.toString()
        )
        rtcClient?.onRemoteSessionRecieved(session)
        rtcClient?.answer(senderName)
    }

    fun handleAnswer(data: Any?) {
        val session = SessionDescription(
                    SessionDescription.Type.ANSWER,
                    data.toString()
                )
                rtcClient?.onRemoteSessionRecieved(session)
    }


    override fun onNewMessage(message: MessageModel) {
        TODO("Not yet implemented")
    }

    fun callUser(target: String) {
        socketRepository?.sendMessageToSocket(
            MessageModel(
                        "start_call",
                        username,
                        target,
                        null
            )
        )

    }

    fun switchCamera() {
        rtcClient?.switchCamera()
    }

    fun endCall(localView: SurfaceViewRenderer, remoteView: SurfaceViewRenderer) {
        rtcClient?.releaseSurfaceView(localView)
        rtcClient?.releaseSurfaceView(remoteView)
        rtcClient?.endCall()
    }

    fun handleFileStream(localView: SurfaceViewRenderer) {
        rtcClient?.endCall()
        rtcClient?.releaseSurfaceView(localView)
        rtcClient?.initializeSurfaceView(localView)
        rtcClient?.streamFile(localView)
    }

    fun handleIceCandidate(data: Any?) {
        val receivingCandidate =gson.fromJson(gson.toJson(data!!),
            IceCandidateModel::class.java)
        rtcClient?.addIceCandidate(
            IceCandidate(
                receivingCandidate.sdpMid,
                Math.toIntExact(receivingCandidate.sdpMLineIndex.toLong()),
                receivingCandidate.sdpCandidate)
        )
    }

    fun initSocket() {
        socketRepository = SocketRepository(this)
    }


}
