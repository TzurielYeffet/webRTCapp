package com.example.webrtcapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.webrtcapp.databinding.ActivityCallBinding
import com.example.webrtcapp.data.models.IceCandidateModel
import com.example.webrtcapp.data.models.MessageModel
import com.example.webrtcapp.data.repositories.SocketRepository
import com.example.webrtcapp.util.NewMessageInterface
import com.example.webrtcapp.util.PeerConnectionObserver
import com.google.gson.Gson
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.SessionDescription

class CallActivity : AppCompatActivity(), NewMessageInterface {

    lateinit var binding:ActivityCallBinding
    private var username:String?=null
    private var socketRepository: SocketRepository?=null
    private var rtcClient:RTCClient?=null
    private var target:String = ""
    private val gson = Gson()
    val TAG="CallActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
    }

    private fun init(){
        username = intent.getStringExtra("username")
        socketRepository = SocketRepository(this)
        username?.let { socketRepository?.initSocket(it) }
        rtcClient = RTCClient(application,username!!, socketRepository!!,object:PeerConnectionObserver(){
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
                mediaStream?.videoTracks?.get(0)?.addSink(binding.remoteView)
                Log.d(TAG, "onAddStream: $mediaStream")


            }

        })

        binding.apply {
            callBtn.setOnClickListener{
                socketRepository?.sendMessageToSocket(
                    MessageModel(
                    "start_call",
                    username,
                    targetUserNameEt.text.toString(),
                    null
                )
                )
                target = targetUserNameEt.text.toString()
                Log.d(TAG,"after call button pressed ")
            }


            switchCameraButton.setOnClickListener{
                rtcClient?.switchCamera()
            }


            endCallButton.setOnClickListener {
                setCallLayoutGone()
                setWhoToCallLayoutVisibale()
                setIncomingCallLayoutGone()
                rtcClient?.releaseSurfaceView(localView)
                rtcClient?.releaseSurfaceView(remoteView)
                rtcClient?.endCall()
            }
            fileButton.setOnClickListener {
                Log.d(TAG,"stream from FILE")
//                rtcClient?.endCall()
                rtcClient?.releaseSurfaceView(localView)
                rtcClient?.initializeSurfaceView(localView)
                rtcClient?.streamFile(localView)
            }
        }

    }




    override fun onNewMessage(message: MessageModel) {
        Log.d(TAG,"on New Message: $message")
        when(message.type){
            "call_response" ->{
                if (message.data == "user is unavailable"){
                    runOnUiThread{
                        Toast.makeText(this,"user is unreachable",Toast.LENGTH_LONG ).show()
                        }
                    }else{
                        runOnUiThread{
                            setWhoToCallLayoutGone()
                            setCallLayoutVisible()
                            binding.apply {
                                rtcClient?.initializeSurfaceView(localView)
                                rtcClient?.initializeSurfaceView(remoteView)
                                rtcClient?.startLocalVideo(localView,null)
                                rtcClient?.call(targetUserNameEt.text.toString())

                            }

                    }
                    }
                }
            "offer_received" -> {
                runOnUiThread {
                    setIncomingCallLayoutVisible()
                    binding.incomingNameTV.text = "${message.name.toString()} is calling you"
                    binding.acceptButton.setOnClickListener {
                        setIncomingCallLayoutGone()
                        setCallLayoutVisible()
                        setWhoToCallLayoutGone()


                        binding.apply {
                            rtcClient?.initializeSurfaceView(localView)
                            rtcClient?.initializeSurfaceView(remoteView)
                            rtcClient?.startLocalVideo(localView,null)
                        }

                        val session = SessionDescription(
                            SessionDescription.Type.OFFER,
                            message.data.toString()
                        )
                        rtcClient?.onRemoteSessionRecieved(session)
                        rtcClient?.answer(message.name!!)
                        target = message.name!!
                        binding.remoteViewLoading.visibility=View.GONE

                    }
                    binding.rejectButton.setOnClickListener {
                        setIncomingCallLayoutGone()
                    }
                }
            }
            "answer_received" ->{
                val session = SessionDescription(
                    SessionDescription.Type.ANSWER,
                    message.data.toString()
                )
                rtcClient?.onRemoteSessionRecieved(session)
                runOnUiThread {
                    binding.remoteViewLoading.visibility = View.GONE
                }
            }
            "ice_candidate" -> {
                runOnUiThread{
                    try {
                        val receivingCandidate =gson.fromJson(gson.toJson(message.data),
                            IceCandidateModel::class.java)
                        rtcClient?.addIceCandidate(
                            IceCandidate(
                                receivingCandidate.sdpMid,
                                Math.toIntExact(receivingCandidate.sdpMLineIndex.toLong()),
                                receivingCandidate.sdpCandidate)
                        )
                    }catch (e:Exception){
                        e.printStackTrace()

                    }
                }
            }

            "file_stream" ->{
                runOnUiThread{
                    binding.apply {
                        rtcClient?.streamFile(localView)

                    }

                }
            }
            }
        }

    private fun setIncomingCallLayoutGone() {
        binding.incomingCallLayout.visibility = View.GONE
    }

    private fun setIncomingCallLayoutVisible() {
        binding.incomingCallLayout.visibility = View.VISIBLE
    }

    private fun setCallLayoutVisible(){
        binding.callLayout.visibility= View.VISIBLE
    }

    private fun setWhoToCallLayoutGone() {
        binding.whoToCallLayout.visibility= View.GONE

    }
    private fun setWhoToCallLayoutVisibale() {
        binding.whoToCallLayout.visibility = View.VISIBLE
    }
    private fun setCallLayoutGone() {
        binding.callLayout.visibility = View.GONE
    }
}