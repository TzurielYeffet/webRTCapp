package com.example.webrtcapp.data.models

data class IceCandidateModel(
    val sdpMid:String,
    val sdpMLineIndex:Double,
    val sdpCandidate:String
)