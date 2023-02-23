package com.example.webrtcapp.ui.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.webrtcapp.databinding.ActivityCallBinding
import com.example.webrtcapp.data.models.MessageModel
import com.example.webrtcapp.ui.viewmodels.CallViewModel
import com.example.webrtcapp.ui.viewmodels.CallViewModelFactory
import com.example.webrtcapp.util.NewMessageInterface


class CallActivity : AppCompatActivity(), NewMessageInterface {

    lateinit var binding: ActivityCallBinding
    private lateinit var viewModel: CallViewModel
    private var username: String? = null
    private var target: String = ""
    val TAG = "CallActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.initSocket()
        viewModel = CallViewModelFactory().create(CallViewModel::class.java)
        //Set up the UI
        username = intent.getStringExtra("username")
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel.init(username!!,binding.remoteView)

        binding.apply {
            callBtn.setOnClickListener {
                viewModel.callUser(targetUserNameEt.text.toString())
//                socketRepository?.sendMessageToSocket(
//                    MessageModel(
//                        "start_call",
//                        username,
//                        targetUserNameEt.text.toString(),
//                        null
//                    )
//                )
//                target = targetUserNameEt.text.toString()
//                Log.d(TAG,"after call button pressed ")
            }
            switchCameraButton.setOnClickListener {
                viewModel.switchCamera()
                //                rtcClient?.switchCamera()
            }
            endCallButton.setOnClickListener {
                viewModel.endCall(localView, remoteView)
                setCallLayoutGone()
                setWhoToCallLayoutVisibale()
                setIncomingCallLayoutGone()
//                rtcClient?.releaseSurfaceView(localView)
//                rtcClient?.releaseSurfaceView(remoteView)
//                rtcClient?.endCall()
            }
            fileButton.setOnClickListener {
                Log.d(TAG, "stream from FILE")
                viewModel.handleFileStream(localView)
            }
        }
    }

    override fun onNewMessage(message: MessageModel) {
        Log.d(TAG, "on New Message: $message")
        when (message.type) {
            "call_response" -> {
                if (message.data == "user is unavailable") {
                    runOnUiThread {
                        Toast.makeText(this, "user is unreachable", Toast.LENGTH_LONG).show()
                    }
                } else {
                    runOnUiThread {
                        setWhoToCallLayoutGone()
                        setCallLayoutVisible()
                        binding.apply {
                            viewModel.handleCallRespond(localView, remoteView, target)
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
                            viewModel.handleOffer(
                                localView,
                                remoteView,
                                message.data!!,
                                message.target!!,
                                message.name!!
                            )
                        }
                        setRemoteViewLoadingGone()
                    }
                        binding.rejectButton.setOnClickListener {
                            setIncomingCallLayoutGone()
                    }
                }
            }
            "answer_received" ->{
                viewModel.handleAnswer(message.data)
                runOnUiThread {
                    setRemoteViewLoadingGone()
                 }
            }
            "ice_candidate" -> {
                runOnUiThread{
                    try {
                        viewModel.handleIceCandidate(message.data)

                    }catch (e:Exception){
                        e.printStackTrace()

                    }
                }
            }
        }
    }
    private fun setRemoteViewLoadingGone(){
        binding.remoteViewLoading.visibility = View.GONE
    }
    private fun setIncomingCallLayoutGone() {
        binding.incomingCallLayout.visibility = View.GONE
    }

    private fun setIncomingCallLayoutVisible() {
        binding.incomingCallLayout.visibility = View.VISIBLE
    }
    private fun setCallLayoutVisible() {
        binding.callLayout.visibility = View.VISIBLE
    }
    private fun setWhoToCallLayoutGone() {
        binding.whoToCallLayout.visibility = View.GONE
    }
    private fun setWhoToCallLayoutVisibale() {
        binding.whoToCallLayout.visibility = View.VISIBLE
    }
    private fun setCallLayoutGone() {
        binding.callLayout.visibility = View.GONE
    }
}