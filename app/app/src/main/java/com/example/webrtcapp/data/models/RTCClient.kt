package com.example.webrtcapp.data.models

import android.app.Application
import android.util.Log
import com.example.webrtcapp.data.repositories.SocketRepository
import org.webrtc.*
import org.webrtc.PeerConnection.Observer
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class RTCClient (
    private val application: Application,
    private val username:String,
    private val socketRepository: SocketRepository,
    private val observer: Observer
    ) {
    val TAG="RTCClient"


    init{
        initPeerConnectionFactory(application)
    }


    private val eglContext = EglBase.create()
    private val peerConnectionFactory by lazy { createPeerConnectionFactory() }
    private val iceServer = listOf(
        PeerConnection.IceServer.builder("stun:iphone-stun.strato-iphone.de:3478").createIceServer()
    )
    private val peerConnection by lazy { createPeerConnection(observer) }
    private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }
//    private val localAudioSource by lazy { peerConnectionFactory.createAudioSource(MediaConstraints()) }

//    private var localAudioTrack: AudioTrack? = null
    private var fileCapturer: FileVideoCapturer?=null
    private var localVideoTrack: VideoTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var localStream:MediaStream?=null

    private  fun initPeerConnectionFactory(application: Application){
        val peerConnectionOption = PeerConnectionFactory.InitializationOptions.builder(application)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()

        PeerConnectionFactory.initialize(peerConnectionOption)
    }

    private fun createPeerConnectionFactory():PeerConnectionFactory{
        return PeerConnectionFactory.builder()
            .setVideoEncoderFactory(
                DefaultVideoEncoderFactory(
                    eglContext.eglBaseContext,
                    true,
                    true
                )
            )
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglContext.eglBaseContext))
            .setOptions(PeerConnectionFactory.Options().apply{
                disableEncryption = true
                disableNetworkMonitor = true
            }).createPeerConnectionFactory()
    }
    private fun createPeerConnection(observer:Observer):PeerConnection?{
        return  peerConnectionFactory.createPeerConnection(iceServer,observer)
    }
    fun initializeSurfaceView(surface:SurfaceViewRenderer){
        surface.run{
            setEnableHardwareScaler(true)
            setMirror(true)
            init(eglContext.eglBaseContext,null)

        }
    }


    fun streamFile(surface: SurfaceViewRenderer){

        Log.d(TAG,"inside streamFile")
        val inputStream: InputStream = javaClass.classLoader.getResourceAsStream("res/raw/out.y4m")
        val tempFile = File.createTempFile("out", ".y4m")

        val outputStream = FileOutputStream(tempFile)
        val buffer = ByteArray(1024)
        var read: Int

        while (inputStream.read(buffer).also { read = it } != -1) {
            outputStream.write(buffer, 0, read)
        }

        inputStream.close()
        outputStream.close()

        Log.d(TAG," tempFile: ${tempFile.absolutePath}")

        fileCapturer = FileVideoCapturer(tempFile.absolutePath)
        Log.d(TAG," fileCapturer: $fileCapturer")
        startLocalVideo(surface,fileCapturer)
    }


    fun releaseSurfaceView(surface: SurfaceViewRenderer){
        surface.release()
    }

    fun startLocalVideo(surface: SurfaceViewRenderer, videoFile:FileVideoCapturer?) {
        val surfaceTextureHelper =
            SurfaceTextureHelper.create(Thread.currentThread().name, eglContext.eglBaseContext)
        localStream = peerConnectionFactory.createLocalMediaStream("local_stream")




        if(videoFile != null){
            videoCapturer?.stopCapture()
            peerConnection?.removeStream(localStream)
            videoFile?.initialize(surfaceTextureHelper,application,localVideoSource.capturerObserver)
            Log.d(TAG,"video file IS NOT null")
            videoFile.startCapture(320, 240, 30)
            localVideoTrack = peerConnectionFactory.createVideoTrack("local_track",localVideoSource)
            localVideoTrack?.addSink(surface)
//            val fileStream =peerConnectionFactory.createLocalMediaStream("local_stream")
            localStream!!.addTrack(localVideoTrack)
            peerConnection?.addStream(localStream)

        }else {
            videoCapturer = getVideoCapturer(application)
            videoCapturer?.initialize(
                surfaceTextureHelper,
                surface.context, localVideoSource.capturerObserver
            )
            videoCapturer?.startCapture(320, 240, 30)
            localVideoTrack =
                peerConnectionFactory.createVideoTrack("local_track", localVideoSource)
            localVideoTrack?.addSink(surface)
//        localAudioTrack =
//            peerConnectionFactory.createAudioTrack("local_track_audio", localAudioSource)
//            localStream = peerConnectionFactory.createLocalMediaStream("local_stream")
//        localStream.addTrack(localAudioTrack)
            localStream!!.addTrack(localVideoTrack)

            peerConnection?.addStream(localStream)
        }
    }




        private fun getVideoCapturer(application: Application):CameraVideoCapturer{
            return Camera2Enumerator(application).run{
                deviceNames.find{
                    isFrontFacing(it)
                }?.let{
                    createCapturer(it,null)
                }?: throw
                IllegalStateException()
            }
        }




    fun call(target: String) {
        val mediaConstraints= MediaConstraints()
        mediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo","true"))
        peerConnection?.createOffer(object : SdpObserver{
            override fun onCreateSuccess(description: SessionDescription?) {
                peerConnection?.setLocalDescription(object : SdpObserver{
                    override fun onCreateSuccess(p0: SessionDescription?) {

                    }

                    override fun onSetSuccess() {
                        val offer = hashMapOf(
                            "sdp" to description?.description,
                            "type" to description?.type
                        )
                        socketRepository.sendMessageToSocket(
                            MessageModel(
                            "create_offer",username,target,offer
                        )
                        )
                    }

                    override fun onCreateFailure(p0: String?) {

                    }

                    override fun onSetFailure(p0: String?) {
                    }

                },description)
            }

            override fun onSetSuccess() {
            }

            override fun onCreateFailure(p0: String?) {
            }

            override fun onSetFailure(p0: String?) {
            }

        },mediaConstraints)
    }

    fun onRemoteSessionRecieved(session: SessionDescription) {
        peerConnection?.setRemoteDescription(object :SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {
            }

            override fun onSetSuccess() {
            }

            override fun onCreateFailure(p0: String?) {
            }

            override fun onSetFailure(p0: String?) {
            }

        },session)

    }

    fun answer(target: String?) {
        val mediaConstraint = MediaConstraints()
        mediaConstraint.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo","true"))
        peerConnection?.createAnswer(object :SdpObserver{
            override fun onCreateSuccess(description: SessionDescription?) {
                peerConnection?.setLocalDescription(object:SdpObserver{
                    override fun onCreateSuccess(p0: SessionDescription?) {
                    }

                    override fun onSetSuccess() {
                        val answer = hashMapOf(
                            "sdp" to description?.description,
                            "type" to description?.type
                        )
                        socketRepository?.sendMessageToSocket(
                            MessageModel(
                            "create_answer",username,target,answer
                        )
                        )
                    }

                    override fun onCreateFailure(p0: String?) {
                    }

                    override fun onSetFailure(p0: String?) {
                    }

                },description)
            }

            override fun onSetSuccess() {
            }

            override fun onCreateFailure(p0: String?) {
            }

            override fun onSetFailure(p0: String?) {
            }

        },mediaConstraint)
    }

    fun addIceCandidate(iceCandidate: IceCandidate?) {
        peerConnection?.addIceCandidate(iceCandidate)

    }

    fun switchCamera(){
        videoCapturer?.switchCamera(null)
    }

//    fun readFromFile(state:Boolean){
//        localAudioTrack?.setEnabled(state)
//    }

    fun toggleCamera(state: Boolean){
        localVideoTrack?.setEnabled(state)
    }

    fun endCall(){
        peerConnection?.close()
    }



}